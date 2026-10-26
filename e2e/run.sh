#!/usr/bin/env bash
# Roteiro e2e do CtrlMesh: emulador no Mac = central, celular físico = celular.
#
# Pré-requisitos (RNF-010):
#   - Roteador (hotspot) do celular ligado e o Mac conectado a ele (RN-077: o celular só aceita centrais no
#     próprio roteador; o emulador sai pelo NAT do Mac com o IP do Mac nessa rede).
#   - Celular no cabo com depuração USB; emulador de tablet em paisagem rodando.
#   - APK de debug gerado: ./gradlew :app:assembleDebug
#   - ferramentas/liberar-celular.sh entregue pelo desenvolvimento (contrato §9).
#   - Ícone "$APP_ROTULO" (padrão: Configurações) na tela inicial do celular.
#   - Celular sem bloqueio de tela seguro facilita o cenário "bloqueio" (senão o roteiro pede para desbloquear).
#
# Uso:
#   CELULAR=<serial> [CENTRAL=emulator-5554] [CENTRAL2=emulator-5556] e2e/run.sh [cenário ...]
# Cenários padrão: preparar conexao controle volume queda encerrar app-fechado bloqueio conexao-perdida
#                  sem-liberacao autorizacoes
# Opcionais:       reinstalar duas-centrais reiniciar casa tela-virtual
# Os cenários padrão são do plano B (tela física): o Shizuku precisa estar desligado (o roteiro pede).
# tela-virtual exige o Shizuku ativo e o CtrlMesh autorizado nele; sem isso, é pulado com aviso.
set -uo pipefail
source "$(dirname "$0")/lib.sh"

: "${CELULAR:?defina CELULAR com o serial do celular (adb devices)}"
ESPERA_TELA="${ESPERA_TELA:-120}"
BRILHO_ORIGINAL=178; BLOQUEIO_ORIGINAL=30000; MIDIA_ORIGINAL=9

restaurar_ambiente() {
  cel shell svc data enable >/dev/null 2>&1
  wifi_central enable >/dev/null 2>&1
}
trap restaurar_ambiente EXIT

liberar() { "$RAIZ/ferramentas/liberar-celular.sh" "$CELULAR"; }

informar_endereco() { # informar_endereco ip  (nas configurações da central, RN-014)
  tocar_no cen resource-id botao-configuracoes; sleep 1
  tocar_no cen resource-id campo-endereco; sleep 0.5
  cen shell input keyevent KEYCODE_MOVE_END
  cen shell input keyevent $(printf 'KEYCODE_DEL %.0s' $(seq 40))
  cen shell input text "$1"
  tocar_no cen resource-id botao-salvar-endereco
  cen shell input keyevent BACK
}
pedido_aparece_em() { # pedido_aparece_em s
  local fim=$((SECONDS + $1))
  while [ $SECONDS -lt $fim ]; do pedido_de_captura_na_tela && return 0; sleep 0.5; done
  return 1
}
sem_log_desde() { ! logs "$1" "$2" | grep -Eq "$3"; }
mesma_tela() { [ "$(cel shell wm size | tr -d '\r')" = "$1" ] && [ "$(cel shell wm density | tr -d '\r')" = "$2" ]; }
tela_ligada_e_desbloqueada() {
  cel shell dumpsys power | grep -q 'mWakefulness=Awake' &&
    ! cel shell dumpsys window | grep -qE 'isKeyguardShowing=true|mShowingLockscreen=true|mDreamingLockscreen=true'
}
celular_trancado() { ! cel shell dumpsys power | grep -q 'mWakefulness=Awake' || cel shell dumpsys window | grep -qE 'isKeyguardShowing=true|mShowingLockscreen=true'; }
desbloquear_celular() {
  cel shell input keyevent KEYCODE_WAKEUP; cel shell wm dismiss-keyguard; sleep 2
  if cel shell dumpsys window | grep -qE 'isKeyguardShowing=true|mShowingLockscreen=true'; then perguntar "Desbloqueie o celular"; fi
}
indicador_sumiu() { ! indicador_visivel; }
video_ate_1280() {
  local d; d="$(logs cel | grep '^video=' | tail -1 | sed 's/^video=//')"
  [ -n "$d" ] && [ "${d%x*}" -le 1280 ] && [ "${d#*x}" -le 1280 ]
}
sem_opcao_parear() { ! ui_dump cen | grep -qi 'parear'; }
notificacao_do_app() { cel shell dumpsys notification --noredact | grep -q "pkg=$PKG"; }
recentes_na_frente() { cel shell dumpsys window | grep -m1 mCurrentFocus | grep -qiE 'recents|quickstep|launcher|overview'; }
notificacao_espelhar_em() {
  local fim=$((SECONDS + $1))
  while [ $SECONDS -lt $fim ]; do
    cel shell dumpsys notification --noredact | grep -q 'toque para espelhar na central' && return 0; sleep 0.5
  done
  return 1
}
reabrir_celular_fora_da_rede() { wifi_central disable; fechar_app cel; iniciar_app cel; sleep 2; cel shell input keyevent HOME; }

ajustes_originais() {
  cel shell settings put system screen_brightness_mode 0
  cel shell settings put system screen_brightness "$BRILHO_ORIGINAL"
  cel shell settings put system screen_off_timeout "$BLOQUEIO_ORIGINAL"
  cel shell settings put system accelerometer_rotation 0
  cel shell settings put system user_rotation 0
  definir_volume 3 "$MIDIA_ORIGINAL"
}
ajustes_restaurados() {
  [ "$(ajuste screen_brightness)" = "$BRILHO_ORIGINAL" ] && [ "$(ajuste screen_off_timeout)" = "$BLOQUEIO_ORIGINAL" ] &&
    [ "$(ajuste user_rotation)" = 0 ] && [ "$(ajuste accelerometer_rotation)" = 0 ] && [ "$(volume 3)" = "$MIDIA_ORIGINAL" ]
}
nao_silencia() { logs cen | grep -q '^aviso=este celular não permite silenciar'; }
midia_silenciada() { if nao_silencia; then [ "$(volume 3)" = "$MIDIA_ORIGINAL" ]; else [ "$(volume 3)" = 0 ]; fi; }

# Começa um espelhamento do zero: central fora da rede, celular reaberto, central entra na rede.
religar_central() {
  wifi_central disable; sleep 3
  wifi_central enable
  esperar_estado cen espelhando 25
}

# ------------------------------------------------------------------
cenario_preparar() {
  info "preparar: instalar, liberar pelo Mac, escolher papéis, endereço do celular"
  [ -f "$APK" ] || { echo "APK não encontrado: $APK"; exit 1; }
  [ -x "$RAIZ/ferramentas/liberar-celular.sh" ] || { echo "falta ferramentas/liberar-celular.sh (contrato §9)"; exit 1; }
  IP_CELULAR="${IP_CELULAR:-$(ip_do_celular_no_roteador)}"
  [ -n "$IP_CELULAR" ] || { echo "o Mac não está no roteador do celular (RN-077, RNF-010)"; exit 1; }
  echo "   celular no roteador: $IP_CELULAR"

  cel install -r "$APK" >/dev/null && cen install -r "$APK" >/dev/null || exit 1
  liberar
  cen shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow
  cen shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null
  cen shell dumpsys deviceidle whitelist +"$PKG" >/dev/null
  cen shell svc data disable

  escolher_papel cel papel-celular
  sleep 2
  local item
  for item in autorizacao-acessibilidade autorizacao-girar-tela autorizacao-sobrepor autorizacao-gravar-audio autorizacao-bateria liberacao-permanente; do
    verificar CA-086 "$item concedida pela liberação do Mac" grep -qw ok <<<"$(texto_do_no cel "$item")"
  done
  cel shell input keyevent HOME

  escolher_papel cen papel-multimidia
  sleep 2
  informar_endereco "$IP_CELULAR"
  ajustes_originais
}

shizuku() { logs cel | grep '^shizuku=' | tail -1 | sed 's/^shizuku=//'; }
garantir_plano_b() {
  [ "$(shizuku)" = ativo ] || return 0
  perguntar "Os cenários do plano B exigem o Shizuku desligado: abra o Shizuku no celular e toque em Parar"
}
# Tarefa do pacote numa tela diferente da física (displayId ≠ 0) segundo o dumpsys.
na_tela_virtual() {
  cel shell dumpsys activity activities | tr -d '\r' | awk -v p="$1" '
    /^ *Display #[0-9]+/ { d = $2; sub("#", "", d) }
    index($0, p "/") && d != "0" { achou = 1 }
    END { exit achou ? 0 : 1 }'
}
display_virtual_sumiu() { ! display_virtual_existe; }
tela_apagada() { ! cel shell dumpsys power | grep -q 'mWakefulness=Awake'; }
indicador_tela_horizontal() { cel shell dumpsys notification --noredact | grep -q 'tela horizontal na central'; }
brilho_e_bloqueio_originais() { [ "$(ajuste screen_brightness)" = "$BRILHO_ORIGINAL" ] && [ "$(ajuste screen_off_timeout)" = "$BLOQUEIO_ORIGINAL" ]; }
virtual_do_tamanho_certo() { # área da central = imagem do layout (modo virtual, sem faixas)
  local l="$1" a="$2" r al aa esperado
  r="$(logs cen | grep '^layout imagem=' | tail -1 | sed 's/.*imagem=//')"
  IFS=, read -r x1 y1 x2 y2 <<<"$r"
  al=$((x2 - x1)); aa=$((y2 - y1))
  esperado=$(( (al < 1280 ? al : 1280) / 8 * 8 ))
  [ -n "$l" ] && [ "$l" -gt "$a" ] && [ "$l" -le 1280 ] && [ "$l" -ge $((esperado - 8)) ] && [ "$l" -le "$esperado" ]
}
display_virtual_existe() { cel shell dumpsys display | grep -q '"CtrlMesh"'; }
processo_vivo() { [ -n "$(cel shell pidof "$1" | tr -d '\r')" ]; }

cenario_conexao() {
  garantir_plano_b
  info "conexão automática, sem dados móveis, sem confirmação; ajustes do celular durante o espelhamento"
  local tamanho densidade m
  tamanho="$(cel shell wm size | tr -d '\r')"; densidade="$(cel shell wm density | tr -d '\r')"
  ajustes_originais
  definir_volume 2 12; definir_volume 4 12
  cel shell svc data disable

  wifi_central disable
  fechar_app cen; iniciar_app cen
  verificar CA-078 "central em escuta mostra aguardando o celular" esperar_estado cen "aguardando o celular" 15
  cel shell input keyevent HOME
  m="$(marca cen)"
  wifi_central enable
  verificar CA-078 "central passa a conectando sozinha" esperar_log cen "$m" '^estado=conectando$' 20
  verificar CA-010 "espelha com dados móveis desligados" esperar_estado cen espelhando 20
  verificar CA-078 "primeiro quadro desenhado (imagem chegando)" esperar_log cen "$m" '^primeiro-quadro$' 10
  verificar CA-076 "nenhuma confirmação do CtrlMesh nem do Android" sem_log_desde cen "$m" 'aguardando confirmação'
  verificar CA-095 "sem Shizuku ativo o modo é tela física" esperar_log cen "$m" '^modo=fisica$' 5
  verificar CA-095 "aviso de plano B na central" esperar_log cen "$m" '^aviso=modo tela do celular — ative a tela horizontal no celular$' 5
  verificar CA-076 "nenhuma tela do CtrlMesh na frente do celular" [ "$(app_na_frente cel)" != "$PKG" ]
  verificar CA-076 "nenhum pedido de captura no celular" nao_pede_captura_durante 3
  verificar CA-089 "nenhum pedido de veículo parado" sem_log_desde cen "$m" 'veículo' 
  sleep 3
  verificar CA-026 "celular girou para paisagem" em_paisagem
  verificar CA-111 "brilho do dono não muda (nunca escurece)" [ "$(ajuste screen_brightness)" = "$BRILHO_ORIGINAL" ]
  verificar CA-057 "volume de toque intacto" [ "$(volume 2)" = 12 ]
  verificar CA-057 "volume de alarme intacto" [ "$(volume 4)" = 12 ]
  verificar CA-056 "mídia do celular silenciada (ou aviso RN-090)" midia_silenciada
  verificar CA-094 "resolução e densidade do celular inalteradas" mesma_tela "$tamanho" "$densidade"
  verificar CA-094 "plano B: nenhuma tela extra com apps" [ "$(cel shell dumpsys activity activities | grep -c 'Display #')" = 1 ]
  verificar CA-045 "indicador com Encerrar no celular" indicador_visivel
  verificar CA-025 "vídeo com lado maior ≤ 1280 px (RNF-016)" video_ate_1280

  local antes depois
  antes="$(hash_tela_central)"
  cel shell input keyevent HOME; sleep 1
  cel shell am start -a android.settings.SETTINGS >/dev/null; sleep 2
  depois="$(hash_tela_central)"
  verificar CA-039 "uso direto do celular aparece na central" [ "$antes" != "$depois" ]

  echo "   aguardando ${ESPERA_TELA}s sem toque local (CA-111)..."
  sleep "$ESPERA_TELA"
  verificar CA-111 "tela continua ligada e desbloqueada" tela_ligada_e_desbloqueada
  verificar CA-111 "brilho continua o do dono" [ "$(ajuste screen_brightness)" = "$BRILHO_ORIGINAL" ]
  verificar CA-088 "estado segue espelhando" [ "$(estado cen)" = espelhando ]
}

cenario_controle() {
  garantir_plano_b
  info "toques e botões da barra executados no celular"
  cel shell input keyevent HOME; sleep 2
  local alvo tela
  alvo="$(centro_do_no cel text "$APP_ROTULO")"
  tela="$(tamanho_tela_celular)"
  if [ -z "$alvo" ]; then falha CA-035 "ícone '$APP_ROTULO' não está na tela inicial do celular"; return; fi
  # shellcheck disable=SC2086
  tocar_imagem_no_ponto_do_celular $alvo $tela
  sleep 3
  verificar CA-035 "toque no ícone da imagem abre o app no celular" [ "$(app_na_frente cel)" = "$APP_PACOTE" ]

  tocar_botao_barra botao-voltar; sleep 2
  verificar CA-037 "Voltar executado no celular" [ "$(app_na_frente cel)" != "$APP_PACOTE" ]

  cel shell am start -a android.settings.SETTINGS >/dev/null; sleep 2
  tocar_botao_barra botao-inicio; sleep 2
  verificar CA-037 "Início executado no celular" [ "$(app_na_frente cel)" != "$APP_PACOTE" ]

  tocar_botao_barra botao-recentes; sleep 2
  verificar CA-037 "Apps recentes executado no celular" recentes_na_frente
  cel shell input keyevent HOME
}

cenario_volume() {
  info "volume do som controlado na central, sem mudar o celular"
  local m vcel vcen
  vcel="$(volume 3)"; m="$(marca cen)"
  tocar_botao_barra botao-menos
  verificar CA-062 "− baixa um nível na central" esperar_log cen "$m" '^volume nivel=' 3
  m="$(marca cen)"; tocar_botao_barra botao-mudo
  verificar CA-062 "mudo silencia" esperar_log cen "$m" 'mudo=true' 3
  m="$(marca cen)"; tocar_botao_barra botao-mudo
  verificar CA-062 "mudo de novo volta" esperar_log cen "$m" 'mudo=false' 3
  vcen="$(volume 3 cen)"
  cen shell input keyevent KEYCODE_VOLUME_DOWN; sleep 1
  verificar CA-062 "volume do sistema da central muda" [ "$(volume 3 cen)" != "$vcen" ]
  verificar CA-062 "volume do celular não mudou" [ "$(volume 3)" = "$vcel" ]
  cen shell input keyevent KEYCODE_VOLUME_UP
}

cenario_queda() {
  garantir_plano_b
  info "queda do Wi-Fi da central e reconexão em até 60 s"
  cel shell am start -a android.settings.SETTINGS >/dev/null; sleep 2
  wifi_central disable
  verificar CA-049 "central mostra reconectando em até 5 s" esperar_estado cen reconectando 6
  verificar CA-049 "celular mostra reconectando" esperar_estado cel reconectando 8
  verificar CA-049 "celular mantém o indicador" indicador_visivel
  verificar CA-028 "giro mantido durante reconectando" em_paisagem
  verificar CA-064 "celular continua silenciado" midia_silenciada
  tocar_botao_barra botao-inicio
  cen shell input tap 600 400
  sleep 10
  wifi_central enable
  verificar CA-050 "espelhamento continua sozinho" esperar_estado cen espelhando 25
  verificar CA-050 "sem novo pedido de captura" nao_pede_captura_durante 3
  verificar CA-049 "toques do intervalo não foram executados depois" [ "$(app_na_frente cel)" = "$APP_PACOTE" ]
  cel shell input keyevent HOME
}

cenario_conexao_perdida() {
  info "60 s sem conexão encerra por conexão perdida e recomeça sozinho"
  wifi_central disable
  verificar CA-051 "central mostra conexão perdida" esperar_estado cen "conexão perdida" 75
  verificar CA-051 "celular mostra conexão perdida" esperar_estado cel "encerrado motivo=conexão perdida" 10
  sleep 3
  verificar CA-064 "volume de mídia do celular volta ao anterior" [ "$(volume 3)" = "$MIDIA_ORIGINAL" ]
  verificar CA-028 "volta a retrato com giro automático desligado" ajustes_restaurados
  wifi_central enable
  verificar CA-051 "recomeça sozinho ao voltar à rede" esperar_estado cen espelhando 25
}

cenario_encerrar() {
  garantir_plano_b
  info "encerrar pelos dois lados, motivo e não recomeçar sozinho"
  local m
  m="$(marca cen)"
  tocar_botao_barra botao-encerrar
  verificar CA-092 "central mostra encerrado pela central em até 1 s (+margem)" esperar_estado cen "encerrado pela central" 3
  verificar CA-092 "celular mostra encerrado pela central" esperar_estado cel "encerrado motivo=encerrado pela central" 5
  sleep 3
  verificar CA-092 "indicador some do celular" indicador_sumiu
  verificar CA-092 "celular restaura orientação, brilho, bloqueio e volume" ajustes_restaurados
  verificar CA-047 "não recomeça sozinho na mesma rede" nunca_log cen "$m" '^estado=conectando$' 8
  tocar_no cen resource-id botao-espelhar
  verificar CA-047 "espelhar na central recomeça" esperar_estado cen espelhando 20

  cel shell cmd statusbar expand-notifications; sleep 1
  tocar_no cel text Encerrar
  cel shell cmd statusbar collapse
  verificar CA-092 "central mostra encerrado pelo celular" esperar_estado cen "encerrado pelo celular" 5
  verificar CA-092 "nenhum motivo de pareamento" sem_log_desde cen "$m" pareamento

  wifi_central disable; sleep 5; wifi_central enable
  verificar CA-047 "central sai da rede e volta: recomeça sozinho" esperar_estado cen espelhando 25
}

cenario_app_fechado() {
  garantir_plano_b
  info "CtrlMesh fechado no celular"
  local m
  m="$(marca cen)"
  fechar_app cel
  verificar CA-091 "central mostra app fechado ou aparelho desligado" esperar_estado cen "app fechado ou aparelho desligado" 8
  verificar CA-091 "sem passar por reconectando" sem_log_desde cen "$m" '^estado=reconectando$' 
  wifi_central disable
  iniciar_app cel; sleep 3
  verificar CA-028 "orientação, brilho e bloqueio restaurados quando o CtrlMesh executa de novo" ajustes_restaurados
  verificar CA-057 "volume de mídia restaurado quando o CtrlMesh executa de novo" [ "$(volume 3)" = "$MIDIA_ORIGINAL" ]
  cel shell input keyevent HOME
  wifi_central enable
  verificar CA-083 "depois de fechar o app, volta sem Permitir" esperar_estado cen espelhando 25
  verificar CA-083 "sem pedido de captura" nao_pede_captura_durante 3
}

cenario_bloqueio() {
  garantir_plano_b
  info "celular bloqueado e desbloqueado com liberação permanente"
  cel shell input keyevent KEYCODE_SLEEP
  verificar CA-088 "central mostra desbloqueie o celular" esperar_estado cen "desbloqueie o celular" 8
  tocar_botao_barra botao-inicio; cen shell input tap 600 400; sleep 2
  verificar CA-088 "toques na central não desbloqueiam o celular" celular_trancado
  desbloquear_celular
  verificar CA-088 "imagem volta sozinha ao desbloquear" esperar_estado cen espelhando 15
  verificar CA-083 "bloquear/desbloquear não pede Permitir" nao_pede_captura_durante 3
}

cenario_sem_liberacao() {
  garantir_plano_b
  info "sem liberação permanente: Permitir uma vez, válido enquanto o app vive (RN-074)"
  local sdk m
  sdk="$(cel shell getprop ro.build.version.sdk | tr -d '\r')"
  cel shell appops set "$PKG" PROJECT_MEDIA default
  wifi_central disable
  fechar_app cel; iniciar_app cel; sleep 2; cel shell input keyevent HOME
  m="$(marca cen)"
  wifi_central enable
  verificar CA-084 "central mostra aguardando confirmação" esperar_estado cen "aguardando confirmação no celular" 20
  verificar CA-087 "pedido de captura aparece sozinho (com sobreposição)" pedido_aparece_em 10
  verificar CA-085 "central mostra captura sem liberação permanente" esperar_log cen "$m" '^aviso=captura sem liberação permanente' 5
  verificar CA-085 "celular mostra captura sem liberação permanente" esperar_log cel "$m" '^aviso=captura sem liberação permanente' 5
  verificar CA-090 "celular não é silenciado enquanto espera Permitir" [ "$(volume 3)" = "$MIDIA_ORIGINAL" ]

  verificar CA-021 "30 s sem toque: não confirmado no celular" esperar_estado cen "não confirmado no celular" 35
  cel shell input keyevent BACK
  verificar CA-021 "nenhum novo pedido sozinho" nao_pede_captura_durante 10
  tocar_no cen resource-id botao-tentar-de-novo
  verificar CA-021 "tentar de novo mostra o pedido outra vez" pedido_aparece_em 15
  permitir_captura
  verificar CA-084 "imagem depois do Permitir" esperar_estado cen espelhando 10
  sleep 2
  verificar CA-090 "depois do Permitir o celular é silenciado" midia_silenciada

  wifi_central disable
  esperar_estado cen "conexão perdida" 75
  wifi_central enable
  verificar CA-084 "central fora da rede >60 s e de volta: sem novo Permitir" esperar_estado cen espelhando 25
  verificar CA-084 "sem pedido de captura" nao_pede_captura_durante 3

  reabrir_celular_fora_da_rede; wifi_central enable
  verificar CA-084 "app fechado: Permitir é pedido de novo" pedido_aparece_em 20
  permitir_captura
  esperar_estado cen espelhando 10

  if [ "$sdk" -ge 35 ]; then
    cel shell input keyevent KEYCODE_SLEEP; sleep 2
    desbloquear_celular
    verificar CA-084 "Android 15+: bloquear e desbloquear pede Permitir de novo" pedido_aparece_em 15
    permitir_captura
    esperar_estado cen espelhando 10
  fi

  info "sem liberação e sem aparecer sobre outros apps"
  cel shell appops set "$PKG" SYSTEM_ALERT_WINDOW deny
  reabrir_celular_fora_da_rede; wifi_central enable
  verificar CA-087 "notificação toque para espelhar na central" notificacao_espelhar_em 20
  verificar CA-087 "pedido só aparece depois do toque na notificação" nao_pede_captura_durante 5
  cel shell cmd statusbar expand-notifications; sleep 1
  tocar_no cel text "toque para espelhar na central"
  verificar CA-087 "toque na notificação mostra o pedido" pedido_aparece_em 10
  permitir_captura
  esperar_estado cen espelhando 10

  liberar
  reabrir_celular_fora_da_rede; wifi_central enable
  esperar_estado cen espelhando 25
}

cenario_autorizacoes() {
  garantir_plano_b
  info "autorizações faltando no celular: girar a tela, gravar áudio e acessibilidade"
  local servicos m
  wifi_central disable
  cel shell appops set "$PKG" WRITE_SETTINGS deny
  cel shell pm revoke "$PKG" android.permission.RECORD_AUDIO
  servicos="$(cel shell settings get secure enabled_accessibility_services | tr -d '\r')"
  cel shell settings put secure enabled_accessibility_services "$(tr ':' '\n' <<<"$servicos" | grep -v "^$PKG/" | paste -sd: -)"
  [ -n "$(cel shell settings get secure enabled_accessibility_services | tr -d '\r' | grep -v null)" ] || cel shell settings delete secure enabled_accessibility_services
  ajustes_originais
  fechar_app cel; iniciar_app cel; sleep 3
  verificar CA-068 "celular mostra girar a tela faltando" grep -q falta <<<"$(texto_do_no cel autorizacao-girar-tela)"
  verificar CA-068 "celular mostra gravar áudio faltando" grep -q falta <<<"$(texto_do_no cel autorizacao-gravar-audio)"
  verificar CA-023 "celular mostra acessibilidade faltando" grep -q falta <<<"$(texto_do_no cel autorizacao-acessibilidade)"
  cel shell input keyevent HOME; sleep 1
  m="$(marca cen)"
  wifi_central enable
  esperar_estado cen espelhando 25
  verificar CA-068 "central mostra girar a tela faltando" esperar_log cen "$m" '^aviso=falta no celular: girar a tela$' 5
  verificar CA-059 "central mostra sem som" esperar_log cen "$m" '^aviso=sem som: autorização de áudio não concedida no celular$' 5
  verificar CA-023 "central mostra o motivo do controle" esperar_log cen "$m" '^aviso=sem controle: autorização de acessibilidade' 5
  sleep 3
  verificar CA-029 "celular continua em retrato" [ "$(ajuste user_rotation)" = 0 ]
  verificar CA-059 "volume do celular não muda" [ "$(volume 3)" = "$MIDIA_ORIGINAL" ]
  local alvo tela
  alvo="$(centro_do_no cel text "$APP_ROTULO")"; tela="$(tamanho_tela_celular)"
  if [ -n "$alvo" ]; then
    # shellcheck disable=SC2086
    tocar_imagem_no_ponto_do_celular $alvo $tela; sleep 3
    verificar CA-023 "toques na central não têm efeito" [ "$(app_na_frente cel)" != "$APP_PACOTE" ]
  fi
  liberar
  reabrir_celular_fora_da_rede; wifi_central enable
  esperar_estado cen espelhando 25
}

cenario_reinstalar() {
  info "reinstalação: liberação desfeita (CA-085) e central recém-instalada sem pareamento (CA-074)"
  local m
  wifi_central disable
  cel uninstall "$PKG" >/dev/null; cel install "$APK" >/dev/null
  cel shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null
  escolher_papel cel papel-celular; sleep 2
  verificar CA-085 "celular mostra liberação faltando" grep -q falta <<<"$(texto_do_no cel liberacao-permanente)"
  cel shell input keyevent HOME
  m="$(marca cen)"; wifi_central enable
  verificar CA-085 "central mostra captura sem liberação permanente" esperar_log cen "$m" '^aviso=captura sem liberação permanente' 25
  esperar_estado cen "aguardando confirmação no celular" 10 && permitir_captura
  liberar
  fechar_app cel; iniciar_app cel; sleep 2
  verificar CA-085 "depois de refazer a liberação o aviso some" grep -qw ok <<<"$(texto_do_no cel liberacao-permanente)"
  cel shell input keyevent HOME

  wifi_central disable
  cen uninstall "$PKG" >/dev/null; cen install "$APK" >/dev/null
  cen shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow
  cen shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null
  escolher_papel cen papel-multimidia; sleep 2
  verificar CA-074 "nenhuma opção de parear" sem_opcao_parear
  informar_endereco "${IP_CELULAR:-$(ip_do_celular_no_roteador)}"
  m="$(marca cen)"; wifi_central enable
  verificar CA-074 "central nova espelha sem código nem confirmação" esperar_estado cen espelhando 25
  verificar CA-074 "sem pedido de captura no celular" nao_pede_captura_durante 3
}

cenario_duas_centrais() {
  info "duas centrais: uma por vez (CA-073)"
  [ -n "${CENTRAL2:-}" ] || { falha CA-073 "defina CENTRAL2 com o serial do segundo emulador"; return; }
  local c1="$CENTRAL"
  esperar_estado cen espelhando 25
  CENTRAL="$CENTRAL2"
  cen install -r "$APK" >/dev/null
  cen shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow
  escolher_papel cen papel-multimidia; sleep 2
  informar_endereco "${IP_CELULAR:-$(ip_do_celular_no_roteador)}"
  verificar CA-073 "segunda central mostra celular em uso" esperar_estado cen "celular em uso por outra central" 25
  CENTRAL="$c1"; tocar_botao_barra botao-encerrar
  CENTRAL="$CENTRAL2"
  verificar CA-073 "segunda central espelha sozinha quando a primeira encerra" esperar_estado cen espelhando 15
  tocar_botao_barra botao-encerrar
  CENTRAL="$c1"
}

cenario_reiniciar() {
  info "reiniciar os dois aparelhos sem abrir o app (CA-077)"
  cel reboot; cen reboot
  cel wait-for-device; cen wait-for-device
  until [ "$(cel shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 2; done
  until [ "$(cen shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 2; done
  sleep 10
  verificar CA-077 "celular em escuta sem abrir o app" esperar_estado cel escuta 30
  verificar CA-077 "notificação permanente do CtrlMesh" notificacao_do_app
  perguntar "Se o celular pedir, desbloqueie-o (sem abrir o CtrlMesh). Ligue o roteador se ele desligou no reinício e conecte o Mac a ele"
  verificar CA-077 "espelhamento começa sem abrir o app" esperar_estado cen espelhando 60
}

cenario_tela_virtual() {
  info "modo tela virtual via Shizuku (RN-092 a RN-108)"
  iniciar_app cel; sleep 2; cel shell input keyevent HOME
  if [ "$(shizuku)" != ativo ]; then
    echo "   PULADO: Shizuku não está ativo no celular (log shizuku=$(shizuku)). Ligue-o pela depuração sem fio,"
    echo "   autorize o CtrlMesh nele e rode de novo: e2e/run.sh tela-virtual"
    return 0
  fi
  local m mc tamanho densidade frente pkg dv l a
  ajustes_originais
  tamanho="$(cel shell wm size | tr -d '\r')"; densidade="$(cel shell wm density | tr -d '\r')"
  cel shell appops set "$PKG" PROJECT_MEDIA default   # RN-099: não depende da liberação pelo Mac
  wifi_central disable; fechar_app cel; iniciar_app cel; sleep 2; cel shell input keyevent HOME
  cel shell am start -a android.settings.SETTINGS >/dev/null; sleep 2
  frente="$(app_na_frente cel)"
  m="$(marca cel)"; mc="$(marca cen)"
  wifi_central enable
  verificar CA-095 "com Shizuku ativo o modo é tela virtual" esperar_log cel "$m" '^modo=virtual$' 25
  verificar CA-101 "espelha sem Permitir e sem liberação pelo Mac" esperar_estado cen espelhando 25
  verificar CA-101 "nenhum pedido de captura" nao_pede_captura_durante 3
  verificar CA-095 "sem aviso de plano B" sem_log_desde cen "$mc" '^aviso=modo tela do celular'
  dv="$(logs cel "$m" | grep '^display_virtual=[0-9]' | tail -1 | sed -E 's/^display_virtual=([0-9]+)x([0-9]+).*/\1 \2/')"
  read -r l a <<<"$dv"
  verificar CA-096 "tela virtual horizontal com lado maior = min(1280, área da central)" virtual_do_tamanho_certo "$l" "$a"
  verificar CA-094 "tela física do celular sem mudar de resolução" mesma_tela "$tamanho" "$densidade"
  verificar CA-094 "existe a tela virtual da central" display_virtual_existe
  verificar CA-097 "central mostra o início do CtrlMesh com apps" esperar_log cen "$mc" '^inicio=[1-9]' 10
  verificar CA-109 "indicador informa tela horizontal na central" indicador_tela_horizontal

  pkg="$(ui_dump cen | grep -o 'resource-id="app-[^"]*"' | sed 's/resource-id="app-//; s/"$//' | grep -m1 -E '^com\.spotify\.music$' || true)"
  [ -n "$pkg" ] || pkg="$(ui_dump cen | grep -o 'resource-id="app-[^"]*"' | head -1 | sed 's/resource-id="app-//; s/"$//')"
  m="$(marca cel)"
  tocar_no cen resource-id "app-$pkg"
  verificar CA-098 "tocar no app abre $pkg na tela virtual" esperar_log cel "$m" "^app_aberto=$pkg\$" 10
  sleep 2
  verificar CA-098 "$pkg está na tela virtual, não na física" na_tela_virtual "$pkg"
  verificar CA-100 "tela física continua com o mesmo app na frente" [ "$(app_na_frente cel)" = "$frente" ]
  verificar CA-100 "tela física não girou" [ "$(ajuste user_rotation)" = 0 ]
  verificar CA-100 "brilho e bloqueio da tela física não mudaram" brilho_e_bloqueio_originais
  verificar CA-107 "volume de mídia do celular não mudou" [ "$(volume 3)" = "$MIDIA_ORIGINAL" ]

  tocar_botao_barra botao-voltar; sleep 2
  verificar CA-099 "Voltar não mexe na tela física" [ "$(app_na_frente cel)" = "$frente" ]
  mc="$(marca cen)"
  tocar_botao_barra botao-inicio
  verificar CA-099 "Início mostra o início do CtrlMesh" esperar_log cen "$mc" '^inicio=' 5
  verificar CA-099 "Início não mexe na tela física" [ "$(app_na_frente cel)" = "$frente" ]
  tocar_no cen resource-id "app-$pkg"; sleep 2

  cel shell input keyevent KEYCODE_SLEEP; sleep 4
  verificar CA-100 "celular bloqueado não interrompe o espelhamento" [ "$(estado cen)" = espelhando ]
  desbloquear_celular

  cel shell settings put system screen_brightness 102
  cel shell input keyevent HOME
  echo "   aguardando 45s sem toque no celular (CA-113)..."
  sleep 45
  verificar CA-113 "tela física se bloqueia sozinha após 30 s" tela_apagada
  verificar CA-113 "espelhamento continua com a tela física apagada" [ "$(estado cen)" = espelhando ]
  verificar CA-113 "brilho continua em 40%" [ "$(ajuste screen_brightness)" = 102 ]
  verificar CA-113 "tempo de bloqueio não mudou" [ "$(ajuste screen_off_timeout)" = "$BLOQUEIO_ORIGINAL" ]
  desbloquear_celular
  cel shell settings put system screen_brightness "$BRILHO_ORIGINAL"

  wifi_central disable
  verificar CA-108 "queda: central em reconectando" esperar_estado cen reconectando 8
  sleep 10
  verificar CA-108 "tela virtual mantida durante reconectando" display_virtual_existe
  wifi_central enable
  verificar CA-108 "reconecta sozinho" esperar_estado cen espelhando 25
  verificar CA-108 "app continua na tela virtual como estava" na_tela_virtual "$pkg"

  m="$(marca cel)"
  tocar_botao_barra botao-encerrar
  esperar_estado cen "encerrado pela central" 5
  verificar CA-108 "ao encerrar a tela virtual é fechada" esperar_log cel "$m" '^display_virtual=fechada$' 5
  verificar CA-094 "tela virtual some ao encerrar" display_virtual_sumiu
  verificar CA-108 "$pkg continua aberto no celular" processo_vivo "$pkg"
  if [ "$(cel shell getprop ro.product.model | tr -d '\r')" = RMX3963 ]; then
    verificar CA-110 "RMX3963: tela virtual criada no tamanho certo" virtual_do_tamanho_certo "$l" "$a"
    verificar CA-110 "RMX3963: Spotify abriu na tela virtual" [ "$pkg" = com.spotify.music ]
  fi
  liberar
  tocar_no cen resource-id botao-espelhar; esperar_estado cen espelhando 25
}

cenario_casa() {
  info "Wi-Fi de casa: celular não aceita centrais fora do próprio roteador (CA-075)"
  [ -n "${IP_CASA:-}" ] || { falha CA-075 "defina IP_CASA com o IP do celular no Wi-Fi de casa"; return; }
  perguntar "Desligue o roteador do celular, ponha celular e Mac no Wi-Fi de casa"
  informar_endereco "$IP_CASA"
  local m; m="$(marca cel)"
  verificar CA-075 "celular recusa a central" esperar_log cel "$m" '^recusada=' 30
  verificar CA-075 "nenhuma imagem na central" [ "$(estado cen)" != espelhando ]
  perguntar "Ligue o roteador do celular e conecte o Mac a ele"
  IP_CELULAR="$(ip_do_celular_no_roteador)"
  informar_endereco "$IP_CELULAR"
  verificar CA-075 "no roteador do celular o espelhamento começa" esperar_estado cen espelhando 30
}

# ------------------------------------------------------------------
CENARIOS=("$@")
[ ${#CENARIOS[@]} -gt 0 ] || CENARIOS=(preparar conexao controle volume queda encerrar app-fechado bloqueio conexao-perdida sem-liberacao autorizacoes)
for c in "${CENARIOS[@]}"; do
  "cenario_${c//-/_}"
done

printf '\n== resultado: %d passaram, %d falharam\n' "${#PASSOU[@]}" "${#FALHAS[@]}"
for f in "${FALHAS[@]}"; do printf '   %s\n' "$f"; done
[ ${#FALHAS[@]} -eq 0 ]

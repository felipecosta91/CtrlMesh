#!/usr/bin/env bash
# Liberação pelo Mac (contrato §9, F-01, RN-073, RN-091). Uso: ferramentas/liberar-celular.sh <serial-adb>
# Celular no cabo com depuração USB. Mostra o resultado de cada passo; o que falhar o celular orienta (RN-072).
set -u
SERIAL="${1:?uso: $0 <serial-adb>}"
PKG=com.ctrlmesh
SERVICO="$PKG/$PKG.celular.ServicoDeControle"
FALTAM=()
a() { adb -s "$SERIAL" shell "$@"; }
passo() { # passo "descrição" "o que conceder à mão" comando...  (falha = código != 0 ou "Exception" na saída)
  local desc="$1" manual="$2" saida codigo; shift 2
  saida="$("$@" 2>&1)"; codigo=$?
  saida="${saida//$'\r'/}"
  if [ "$codigo" -eq 0 ] && ! grep -q 'Exception' <<<"$saida"; then
    printf 'ok     %s\n' "$desc"
  else
    printf 'FALHOU %s: %s\n' "$desc" "$(head -1 <<<"$saida")"
    FALTAM+=("$manual")
  fi
}

passo "captura de tela permanente (PROJECT_MEDIA)" "nada: sem liberação, toque em Permitir no celular quando ele pedir (RN-074)" a appops set "$PKG" PROJECT_MEDIA allow
passo "aparecer sobre outros apps" "Ajustes > Apps > CtrlMesh > Aparecer sobre outros apps" a appops set "$PKG" SYSTEM_ALERT_WINDOW allow
passo "girar a tela, brilho e bloqueio (WRITE_SETTINGS)" "Ajustes > Apps > CtrlMesh > Modificar configurações do sistema" a appops set "$PKG" WRITE_SETTINGS allow
passo "gravar áudio" "Ajustes > Apps > CtrlMesh > Permissões > Microfone" a pm grant "$PKG" android.permission.RECORD_AUDIO
passo "notificações" "Ajustes > Apps > CtrlMesh > Notificações" a pm grant "$PKG" android.permission.POST_NOTIFICATIONS
passo "fora da economia de bateria" "Ajustes > Apps > CtrlMesh > Bateria > Sem restrições" a dumpsys deviceidle whitelist +"$PKG"

ativos="$(a settings get secure enabled_accessibility_services | tr -d '\r')"
case "$ativos" in
  null|"") novos="$SERVICO" ;;
  *"$SERVICO"*) novos="$ativos" ;;
  *) novos="$ativos:$SERVICO" ;;
esac
passo "acessibilidade (controle)" "Ajustes > Acessibilidade > CtrlMesh (liberar configurações restritas antes, se pedido)" a settings put secure enabled_accessibility_services "$novos"
passo "acessibilidade ligada" "Ajustes > Acessibilidade > CtrlMesh" a settings put secure accessibility_enabled 1

if [ ${#FALTAM[@]} -gt 0 ]; then
  echo
  echo "Conceda à mão no celular (o app também mostra o que falta, com atalho):"
  printf '  - %s\n' "${FALTAM[@]}" | sort -u
fi
echo
if a appops get "$PKG" PROJECT_MEDIA | grep -q allow; then
  echo "liberação permanente de captura aplicada"
  exit 0
fi
echo "liberação permanente de captura NÃO aplicada"
exit 1

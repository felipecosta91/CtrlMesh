# Funções do roteiro e2e do CtrlMesh. Uso: source e2e/lib.sh (via e2e/run.sh).
# Central = emulador no Mac (atrás do NAT do Mac: só ela abre conexão). Celular = aparelho físico.

PKG=com.ctrlmesh
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="${APK:-$RAIZ/app/build/outputs/apk/debug/app-debug.apk}"
CENTRAL="${CENTRAL:-emulator-5554}"
APP_ROTULO="${APP_ROTULO:-Configurações}"   # ícone na tela inicial do celular usado no teste de toque
APP_PACOTE="${APP_PACOTE:-com.android.settings}"
FALHAS=()
PASSOU=()

cel() { adb -s "$CELULAR" "$@"; }
cen() { adb -s "$CENTRAL" "$@"; }
lado() { if [ "$1" = cel ]; then echo "$CELULAR"; else echo "$CENTRAL"; fi; }

info() { printf '\n== %s\n' "$*"; }
ok() { PASSOU+=("$1"); printf '   PASSOU  %s\n' "$1"; }
falha() { FALHAS+=("$1: $2"); printf '   FALHOU  %s: %s\n' "$1" "$2"; }
verificar() { # verificar CA-xxx "descrição" comando...
  local ca="$1" desc="$2"; shift 2
  if "$@"; then ok "$ca $desc"; else falha "$ca" "$desc"; fi
}
perguntar() { # passo manual assistido
  printf '\n>> %s\n   Enter quando pronto... ' "$*"
  read -r _
}

# ---------- logcat (contrato §7.2) ----------
marca() { adb -s "$(lado "$1")" shell date +%s | tr -d '\r'; }
logs() { # logs lado [marca]
  local s; s="$(lado "$1")"
  if [ -n "${2:-}" ]; then adb -s "$s" logcat -d -v raw -T "$2.000" -s CtrlMesh:I 2>/dev/null | tr -d '\r'
  else adb -s "$s" logcat -d -v raw -s CtrlMesh:I 2>/dev/null | tr -d '\r'; fi
}
estado() { logs "$1" | grep '^estado=' | tail -1 | sed 's/^estado=//'; }
esperar_log() { # esperar_log lado marca regex timeout_s
  local fim=$((SECONDS + $4))
  while [ $SECONDS -lt $fim ]; do
    logs "$1" "$2" | grep -Eq "$3" && return 0
    sleep 0.5
  done
  return 1
}
nunca_log() { # nunca_log lado marca regex durante_s
  local fim=$((SECONDS + $4))
  while [ $SECONDS -lt $fim ]; do
    logs "$1" "$2" | grep -Eq "$3" && return 1
    sleep 0.5
  done
  return 0
}
esperar_estado() { # esperar_estado lado "texto" timeout_s  (estado atual = última linha estado=)
  local fim=$((SECONDS + $3))
  while [ $SECONDS -lt $fim ]; do
    [ "$(estado "$1")" = "$2" ] && return 0
    sleep 0.5
  done
  return 1
}

# ---------- interface ----------
ui_dump() { # ui_dump lado -> XML da tela
  local s; s="$(lado "$1")"
  for _ in 1 2 3; do
    adb -s "$s" shell uiautomator dump /sdcard/ctrlmesh-ui.xml >/dev/null 2>&1 && \
      adb -s "$s" exec-out cat /sdcard/ctrlmesh-ui.xml && return 0
    sleep 1
  done
  return 1
}
centro_do_no() { # centro_do_no lado atributo valor -> "x y"
  ui_dump "$1" | python3 -c '
import sys, re, xml.etree.ElementTree as ET
attr, valor = sys.argv[1], sys.argv[2]
for n in ET.fromstring(sys.stdin.read()).iter("node"):
    if n.get(attr, "") == valor or (attr == "text" and valor in n.get("text", "")):
        l, t, r, b = map(int, re.findall(r"\d+", n.get("bounds")))
        print((l + r) // 2, (t + b) // 2); break
' "$2" "$3"
}
tocar_no() { # tocar_no lado atributo valor
  local p; p="$(centro_do_no "$1" "$2" "$3")"
  [ -n "$p" ] || return 1
  adb -s "$(lado "$1")" shell input tap $p
}
tocar_botao_barra() { # via log "botao=<tag> L,T,R,B" (a central redesenha vídeo o tempo todo)
  local r; r="$(logs cen | grep "^botao=$1 " | tail -1 | awk '{print $2}')"
  [ -n "$r" ] || return 1
  IFS=, read -r l t rr b <<<"$r"
  cen shell input tap $(((l + rr) / 2)) $(((t + b) / 2))
}
tocar_imagem_no_ponto_do_celular() { # converte px do celular -> px da central pelo layout logado
  local px="$1" py="$2" lw="$3" lh="$4" r
  r="$(logs cen | grep '^layout imagem=' | tail -1 | sed 's/.*imagem=//')"
  [ -n "$r" ] || return 1
  IFS=, read -r l t rr b <<<"$r"
  cen shell input tap $((l + px * (rr - l) / lw)) $((t + py * (b - t) / lh))
}
tamanho_tela_celular() { ui_dump cel | python3 -c '
import sys, re, xml.etree.ElementTree as ET
n = next(ET.fromstring(sys.stdin.read()).iter("node"))
l, t, r, b = map(int, re.findall(r"\d+", n.get("bounds"))); print(r, b)'; }

app_na_frente() { # app_na_frente lado -> pacote em primeiro plano
  adb -s "$(lado "$1")" shell dumpsys activity activities | tr -d '\r' |
    grep -E 'mResumedActivity|topResumedActivity|ResumedActivity:' | head -1 | sed -E 's/.* ([a-zA-Z0-9_.]+)\/.*/\1/'
}
pedido_de_captura_na_tela() {
  cel shell dumpsys activity activities | grep -q MediaProjectionPermissionActivity
}
nao_pede_captura_durante() { # nao_pede_captura_durante s
  local fim=$((SECONDS + $1))
  while [ $SECONDS -lt $fim ]; do pedido_de_captura_na_tela && return 1; sleep 0.5; done
  return 0
}
permitir_captura() { tocar_no cel resource-id android:id/button1; }

hash_tela_central() { cen exec-out screencap -p | md5; }

# ---------- ajustes do celular ----------
ajuste() { cel shell settings get system "$1" | tr -d '\r'; }
volume() { # volume <stream 3=mídia 2=toque 4=alarme> [lado]
  local s; s="$(lado "${2:-cel}")"
  adb -s "$s" shell cmd media_session volume --stream "$1" --get 2>/dev/null | tr -d '\r' |
    grep -Eo 'volume is [0-9]+' | grep -Eo '[0-9]+'
}
definir_volume() { cel shell cmd media_session volume --stream "$1" --set "$2" >/dev/null 2>&1; }
tela_acesa() { cel shell dumpsys power | grep -q 'mWakefulness=Awake'; }
em_paisagem() { [ "$(ajuste accelerometer_rotation)" = 0 ] && [[ "$(ajuste user_rotation)" =~ ^[13]$ ]]; }
indicador_visivel() { cel shell dumpsys notification --noredact | tr -d '\r' | grep -A40 "pkg=$PKG" | grep -q 'Encerrar'; }

# ---------- rede da central ----------
wifi_central() { cen shell svc wifi "$1"; } # enable|disable

# ---------- preparação ----------
iniciar_app() { adb -s "$(lado "$1")" shell am start -n "$PKG/.MainActivity" >/dev/null; }
fechar_app() { adb -s "$(lado "$1")" shell am force-stop "$PKG"; }

escolher_papel() { # escolher_papel lado papel-celular|papel-multimidia
  iniciar_app "$1"; sleep 2
  ui_dump "$1" | grep -q "resource-id=\"$2\"" && tocar_no "$1" resource-id "$2"
  return 0
}

ip_do_celular_no_roteador() {
  # O Mac está no roteador do celular (RNF-010): o gateway do Mac é o próprio celular.
  local gw; gw="$(route -n get default 2>/dev/null | awk '/gateway/{print $2}')"
  cel shell ip -4 -o addr | tr -d '\r' | grep -q "inet $gw/" && echo "$gw"
}
texto_do_no() { # texto_do_no lado resource-id -> textos do nó e descendentes
  ui_dump "$1" | python3 -c '
import sys, xml.etree.ElementTree as ET
alvo = sys.argv[1]
for n in ET.fromstring(sys.stdin.read()).iter("node"):
    if n.get("resource-id", "") == alvo:
        print(" ".join(filter(None, (m.get("text", "") + " " + m.get("content-desc", "") for m in n.iter("node"))))); break
' "$2"
}

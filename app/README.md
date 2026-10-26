# CtrlMesh — app Android (`:app`)

Um APK, dois papéis: **celular** (é espelhado e controlado) e **multimídia/central** (mostra e controla).
Contrato: `docs/contrato.md` v3.0. Regras: `docs/regras-de-negocio.md` v1.3.

## Estrutura

| Pacote | Conteúdo |
|---|---|
| `com.ctrlmesh.protocolo` | Regra pura (JVM): mensagens, aperto ECDH + AES-GCM, banda, fila de envio, detector de queda |
| `com.ctrlmesh.central` | Sessão pura da central, layout, descoberta, volume; `ControladorCentral` (rede), `Reprodutor` (H.264/Opus), `TelaCentral` (Compose) |
| `com.ctrlmesh.celular` | Sessão pura do celular, fila de centrais, rede do roteador, plano de áudio, restauração; `ControladorCelular` (servidor TCP), `Captura` (MediaProjection + MediaCodec), `ServicoDeControle` (acessibilidade), `TelaCelular` |
| `com.ctrlmesh` | `MainActivity`, `ServicoDeEscuta` (primeiro plano, os dois papéis), `ReceptorDeInicio`, `Conexao` (TCP cifrado + batimento), `ConfiguracaoDoApp`, `Notificacoes`, `MapeadorDeToque` |

Script da liberação pelo Mac: `ferramentas/liberar-celular.sh <serial>` (contrato §9).

## Como rodar

```bash
# regra pura + lint
./gradlew :app:testDebugUnitTest :app:lint

# instalar (central = emulador, celular = aparelho)
ANDROID_SERIAL=emulator-5554 ./gradlew :app:installDebug
ANDROID_SERIAL=<serial-do-celular> ./gradlew :app:installDebug
ferramentas/liberar-celular.sh <serial-do-celular>

# instrumentados (emulador em paisagem ligado)
./gradlew :app:connectedDebugAndroidTest

# e2e (Mac no roteador do celular, celular no cabo, emulador ligado)
./gradlew :app:assembleDebug && CELULAR=<serial-do-celular> e2e/run.sh
```

Na central do emulador, informe o IP do celular em Ajustes → "Endereço do celular" (o gateway do emulador é o Mac).

## Decisões técnicas

- **Sem dependências novas**: só o que o `build.gradle.kts` já trazia (Compose/Material3). Cifra, HKDF, codecs e rede com APIs do JDK/Android.
- **Transporte**: um TCP por central na porta 47800; a central sempre conecta (NAT do emulador). `Conexao` tem três threads: leitura, envio (`FilaDeEnvio`: controle > som > vídeo, descarta vídeo até o próximo quadro-chave) e batimento (Ping 1 s, queda após 3 s sem quadros). EOF/ECONNRESET = app fechado; demais erros = queda.
- **Central**: `ConexaoAberta` só é emitida depois do aperto, para o motivo de encerramento não sumir enquanto a central tenta de novo. O decodificador guarda o GOP atual e o reaplica quando a superfície é recriada.
- **Celular**: a vaga da central que caiu fica reservada durante a janela de 60 s (decisão do dono); qualquer `Ola` em "reconectando" assume a vaga (`FilaDeCentrais.retomar`). Sub-redes do roteador = interfaces IPv4 ativas que não são redes de cliente do `ConnectivityManager`.
- **Captura**: uma `VirtualDisplay` por projeção (Android 14+ não permite outra); o codificador é recriado a cada conexão e a cada giro (novo `VideoConfig` + quadro-chave). Resolução = tela do celular reduzida para caber na área do `Ola`, alinhada a 16 px. Pedido de captura por `AtividadeDeCaptura` transparente; com a liberação permanente (`PROJECT_MEDIA`) o sistema responde sem diálogo. Captura recusada = `NAO_CONFIRMADO_NO_CELULAR` na hora.
- **Tela ligada (RN-031)**: `SCREEN_OFF_TIMEOUT` vai a 24 h junto com brilho mínimo e é restaurado pela `RestauracaoDoCelular`.
- **RN-090**: heurística — com a mídia em 0 e música tocando, 3 s de PCM capturado todo zerado marcam o celular como "não silencia" (guardado em `ConfiguracaoDoApp`), o volume volta e a central recebe `NAO_SILENCIA`.
- **Toques**: o gesto é montado de DESCE ao último SOBE e despachado de uma vez com `dispatchGesture` (toque, longo, arrastar, deslizar, pinça). Arrasto "ao vivo" fica para `continueStroke` se a latência incomodar.
- **Tipos de serviço em primeiro plano**: `connectedDevice` para escutar (permitido no boot); `mediaProjection`/`microphone` só depois do "Permitir"/liberação.

## Modo tela virtual via Shizuku (contrato §10)

- **Dependências:** `dev.rikka.shizuku:api` e `provider` 13.1.5 (confirmadas no Maven Central). `ShizukuProvider` no manifesto.
- **Ajudante** (`celular/virtual/AjudanteShell`): UserService do Shizuku, com uid de shell, num processo próprio. É um `Binder` simples, sem AIDL, para não mexer nos `buildFeatures`; os códigos estão em `Ajudante`. Faz o mesmo que o scrcpy-server:
  - tela virtual confiável com `DisplayManager.createVirtualDisplay`, sobre a superfície do codificador do app;
  - `am start --display`/`am display move-stack` para abrir ou mover apps;
  - `InputManager.injectInputEvent` com `displayId` para toques e teclas;
  - som por `AudioRecord(REMOTE_SUBMIX)`, que desvia o som de mídia do alto-falante enquanto grava.
  O PCM volta ao app por um pipe e é codificado em Opus aqui. Comandos externos só com argumentos em lista, e o pacote vindo da rede é validado por expressão regular.
- **Tela vazia:** detectada por `am stack list` a cada 1 s.
- **Toque:** vai ao ajudante já em pixels da tela virtual.
- **Falha ao criar a tela virtual:** o espelhamento cai para a tela do celular antes de enviar `Modo`.

## Limitações conhecidas

- `CONTEUDO_PROTEGIDO`, `COMPARTILHANDO_SO_UM_APP` e `CONTROLE_BLOQUEADO_PELO_SISTEMA` não são detectados no celular (o Android não expõe isso de forma confiável); a central exibe se receber.
- `ajuste-inicio-automatico` não aparece: não há API para saber se a central tem esse ajuste próprio.
- Banda fixa de 8 Mbps no codificador; a degradação é feita pelo descarte de vídeo na fila.

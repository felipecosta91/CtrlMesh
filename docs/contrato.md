# CtrlMesh — Contrato celular ↔ multimídia

- **Versão:** 3.3 (derivada de `docs/regras-de-negocio.md` v1.7; 3.3: barra embaixo e brilho nunca alterado — RN-109 a RN-112 —, sem mudança no fio; 3.2: modo tela virtual via Shizuku — RN-092 a RN-108 —, compatível no fio; 3.1: tamanho do vídeo pelo RNF-016)
- **Motivo da reescrita:** a v1.0 das regras mudou a direção do produto (acesso remoto com servidor NestJS + Flutter → espelhamento do celular Android na central multimídia Android pelo roteador do celular, sem servidor). Nada da v2.0 deste contrato (backend ↔ frontend) vale; `backend/` e `frontend/` são legado e não são usados.
- **Uso:** derivado das regras de negócio; a implementação segue este contrato.
- **Testes que verificam este contrato:** `app/src/test/**` (JUnit), `app/src/androidTest/**` (instrumentados) e `e2e/` (emulador + celular). Rastreabilidade em `docs/rastreabilidade.md`.
- **Mudança de contrato:** só compatível (novo tipo de mensagem, novo bit de aviso, novo valor de enum no fim). Quebra volta à esteira.

---

## 1. Projeto e identificação

| Item | Valor |
|---|---|
| Projeto Gradle | raiz do repositório (`settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`), módulo único `:app` |
| `namespace` e `applicationId` | `com.ctrlmesh` (um único APK para os dois papéis) |
| `minSdk` / `targetSdk` / `compileSdk` | 26 (central Android 8, RNF-007) / 35 / 35. No papel celular, o app recusa Android < 10 com a mensagem "o papel celular exige Android 10 ou superior". |
| Atividade de entrada | `com.ctrlmesh.MainActivity` (launcher; mostra escolha de papel, tela do celular ou tela da central) |
| Serviço de acessibilidade | `com.ctrlmesh.celular.ServicoDeControle` |
| Serviço de escuta (primeiro plano, os dois papéis) | `com.ctrlmesh.ServicoDeEscuta` |
| Receptor de início | `com.ctrlmesh.ReceptorDeInicio` (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`) |
| UI | Jetpack Compose, raiz com `Modifier.semantics { testTagsAsResourceId = true }` |
| Script da liberação pelo Mac (F-01, RN-073, RN-091) | `ferramentas/liberar-celular.sh <serial-adb>` — entregue pelo `desenvolvimento`; ver §9 |
| Idioma | pt-BR (RNF-009) |

Dependências de teste já declaradas em `app/build.gradle.kts` (o `desenvolvimento` pode acrescentar dependências, não remover as de teste).

---

## 2. Rede, descoberta e conexão

- **Transporte:** uma conexão TCP por central. **Porta fixa `47800`** (`Protocolo.PORTA`).
- **Quem escuta:** só o celular (`ServicoDeEscuta`, papel celular) escuta em `0.0.0.0:47800`.
- **Quem conecta:** **sempre a central** (RNF-010: o emulador está atrás do NAT do Mac; o celular nunca abre conexão para a central).
- **Para onde a central conecta** (`Descoberta.alvos`), em ordem, sem repetição:
  1. endereço informado nas configurações da central (RN-014, prioridade da RN-079), se houver;
  2. gateway IPv4 da rede Wi-Fi atual da central (o roteador do celular é o gateway da própria rede — RN-079).
  Sem nenhum dos dois: nenhuma tentativa. No emulador o gateway é o Mac (`10.0.2.2`), por isso o endereço do celular é informado (CA-016).
- **Tentativas:** enquanto `EstadoCentral.deveConectar` for verdadeiro, a central tenta cada alvo com timeout de conexão de 2 s e repete a lista a cada 2 s.
- **Só o próprio roteador (RN-077):** ao aceitar uma conexão, o celular verifica `RedeDoRoteador.aceita(enderecoDaCentral, subredesDoRoteador)`, onde `subredesDoRoteador` são as sub-redes IPv4 das interfaces do roteador (hotspot) ativas do celular. Se recusar, **fecha o socket imediatamente, sem enviar o aperto** (nenhum byte). Endereço de loopback nunca é aceito. No teste, o emulador chega ao celular pelo IP do Mac na rede do roteador, que é aceito.
- **Batimento (RNF-005):** cada lado envia `Ping` a cada 1000 ms e responde `Pong`. `DetectorDeQueda` declara queda após **3000 ms sem receber nenhum quadro**.
- **Distinção queda × app fechado (CA-091):**
  - fim de stream (EOF) ou `ECONNRESET` **sem** `Encerrar` antes → `APP_FECHADO_OU_DESLIGADO`, sem passar por "reconectando";
  - silêncio além do limite, erro de rede ou perda da rede Wi-Fi local → queda → "reconectando".
  - Ao fechar normalmente (fim da tarefa, `onDestroy`, desligamento do aparelho com `ACTION_SHUTDOWN`), o app envia `Encerrar(APP_FECHADO_OU_DESLIGADO)` antes de fechar o socket, se puder.
- **Encerramento:** depois de enviar ou receber `Encerrar`, os dois lados fecham a conexão. Recomeçar = nova conexão.
- **Retomada (RN-054):** durante `Reconectando`, a central abre uma nova conexão e envia `Ola`; o celular em `Reconectando` trata qualquer `Ola` como retomada da sessão (sem novo "Permitir") e responde com `Situacao`, `Estado(ESPELHANDO)`, `VideoConfig` + quadro-chave e `AudioConfig`.
- **Janela de reconexão:** 60 000 ms nos dois lados, contados por cada lado a partir da própria detecção da queda.

---

## 3. Aperto de mão e cifra (RNF-006)

Logo após a conexão TCP, a central envia o aperto; o celular responde com o seu. Depois disso, tudo é cifrado.

**Aperto** (`Aperto.TAMANHO = 74` bytes, em claro):

| Bytes | Campo |
|---|---|
| 0–7 | ASCII `CTRLMESH` |
| 8 | versão do protocolo = `1` (`Protocolo.VERSAO`) |
| 9–73 | chave pública efêmera EC P-256, formato X9.62 não comprimido (65 bytes, começa com `0x04`) |

Mágica, versão ou tamanho diferentes → `ProtocoloException`, e quem recebeu fecha a conexão.

**Derivação:** segredo = ECDH P-256 (32 bytes). `okm = HKDF-SHA256(salt = publicaDaCentral ‖ publicaDoCelular, ikm = segredo, info = "ctrlmesh v1", L = 64)`. Chave central→celular = `okm[0..32)`, chave celular→central = `okm[32..64)`. Um par de chaves novo a cada conexão.

**Quadro no fio:** `u32 BE N` + `N` bytes de AES-256-GCM (tag de 16 bytes incluída), `N ≤ 4 MiB + 16`. Nonce de 12 bytes = `u32 BE direção` (`1` central→celular, `2` celular→central) ‖ `u64 BE contador` (começa em 0, +1 por quadro). O receptor usa o contador esperado: quadro repetido, fora de ordem ou alterado falha na decifra e a conexão é fechada como queda. Texto claro = mensagem (§4).

Nada é gravado em disco (imagem, som, toques) em nenhum dos lados.

---

## 4. Mensagens

Inteiros big-endian. `str` = `u16` tamanho + UTF-8. `bytes` = `u32` tamanho + dados. `lista<bytes>` = `u8` quantidade + cada `bytes`. `bool` = `u8` 0/1. `f32` = IEEE 754. Primeiro byte = tipo.

| Tipo | Mensagem | Sentido | Conteúdo |
|---|---|---|---|
| `0x01` | `Ola` | C→P | `str nomeCentral`, `u16 larguraArea`, `u16 alturaArea` (área de imagem da central = tela menos a faixa da barra embaixo, px — RNF-016, RN-110) |
| `0x02` | `Estado` | P→C | `u8 EstadoRemoto` |
| `0x03` | `Situacao` | P→C | `u32` máscara de `Aviso` (bit = `Aviso.bit`) |
| `0x04` | `Modo` | P→C | `u8 ModoEspelhamento` — enviado uma vez por espelhamento, antes de `Estado(ESPELHANDO)` (v3.2) |
| `0x05` | `Area` | C→P | `u16 larguraArea`, `u16 alturaArea` — a área de imagem da central mudou (v3.2) |
| `0x10` | `VideoConfig` | P→C | `u16 largura`, `u16 altura`, `str codec` (`"video/avc"`), `lista<bytes> csd` (SPS, PPS) |
| `0x11` | `VideoQuadro` | P→C | `i64 ptsUs`, `bool chave`, resto = dados (Annex B) |
| `0x20` | `AudioConfig` | P→C | `u32 taxa` (48000), `u8 canais` (2), `str codec` (`"audio/opus"`), `lista<bytes> csd` (csd-0..2 do Opus) |
| `0x21` | `AudioQuadro` | P→C | `i64 ptsUs`, resto = dados |
| `0x30` | `Toque` | C→P | `u8 ponteiro`, `u8 AcaoToque`, `f32 x`, `f32 y` (normalizados 0..1 sobre o quadro), `u16 larguraQuadro`, `u16 alturaQuadro` (do quadro tocado), `i64 tempoMs` (relógio monotônico da central) |
| `0x31` | `Tecla` | C→P | `u8 TeclaDeSistema` (no modo tela virtual, age só na tela virtual — §10) |
| `0x40` | `ListaDeApps` | P→C | `u16 n` + `n` × (`str pacote`, `str rotulo`, `bytes iconePng`) — apps com ícone na tela inicial; ícone PNG 96×96 px, ≤ 16 KiB (v3.2) |
| `0x41` | `AbrirApp` | C→P | `str pacote` (v3.2) |
| `0x42` | `TelaVirtualVazia` | P→C | sem conteúdo — nenhum app na frente da tela virtual; a central mostra o início do CtrlMesh (v3.2) |
| `0x50` | `Encerrar` | ambos | `u8 MotivoEncerramento` |
| `0x60` | `Ping` | ambos | `u32 seq` |
| `0x61` | `Pong` | ambos | `u32 seq` |

- **Tipo desconhecido** → `Mensagem.Desconhecida(tipo)`, ignorada (compatibilidade futura).
- **Sentido errado** (ex.: `VideoQuadro` chegando ao celular, `Toque` chegando à central) → `ProtocoloException`. Não existe espelhamento inverso (RN-001).
- **Conteúdo truncado, tamanho inválido ou enum fora da tabela** → `ProtocoloException`; a conexão é fechada, o app não cai.

### Enums (valor no fio)

| Enum | Valores |
|---|---|
| `EstadoRemoto` | `AGUARDANDO_CONFIRMACAO=1`, `ESPELHANDO=2`, `DESBLOQUEIE=3`, `EM_USO_POR_OUTRA_CENTRAL=4` |
| `AcaoToque` | `DESCE=0`, `MOVE=1`, `SOBE=2`, `CANCELA=3` |
| `TeclaDeSistema` | `VOLTAR=1` (`GLOBAL_ACTION_BACK`), `INICIO=2` (`GLOBAL_ACTION_HOME`), `RECENTES=3` (`GLOBAL_ACTION_RECENTS`) |
| `ModoEspelhamento` | `TELA_VIRTUAL=1`, `TELA_FISICA=2` (sem `Modo` recebido, a central assume `TELA_FISICA`) |
| `MotivoEncerramento` | `ENCERRADO_PELO_CELULAR=1`, `ENCERRADO_PELA_CENTRAL=2`, `CONEXAO_PERDIDA=3`, `APP_FECHADO_OU_DESLIGADO=4`, `NAO_CONFIRMADO_NO_CELULAR=5` |

`MotivoEncerramento.texto` (RN-089): `"encerrado pelo celular"`, `"encerrado pela central"`, `"conexão perdida"`, `"app fechado ou aparelho desligado"`, `"não confirmado no celular"`. Não existe motivo de pareamento.

### Avisos (`Aviso`, bit e texto exibido)

| Bit | `Aviso` | Texto |
|---|---|---|
| 0 | `FALTA_ACESSIBILIDADE` | `sem controle: autorização de acessibilidade não concedida no celular` |
| 1 | `FALTA_GIRAR_TELA` | `falta no celular: girar a tela` |
| 2 | `FALTA_SOBREPOR` | `falta no celular: aparecer sobre outros apps` |
| 3 | `FALTA_GRAVAR_AUDIO` | `sem som: autorização de áudio não concedida no celular` |
| 4 | `FALTA_BATERIA` | `o sistema pode fechar o CtrlMesh e o espelhamento pode não começar sozinho` |
| 5 | `SEM_LIBERACAO_PERMANENTE` | `captura sem liberação permanente — refaça a liberação pelo Mac para não precisar tocar em Permitir` |
| 6 | `CONTROLE_BLOQUEADO_PELO_SISTEMA` | `sem controle: o Android do celular bloqueia o controle` |
| 7 | `COMPARTILHANDO_SO_UM_APP` | `o celular está compartilhando só um app; encerre e escolha a tela inteira` |
| 8 | `LIGACAO_EM_ANDAMENTO` | `ligação em andamento — o som está no celular` |
| 9 | `NAO_SILENCIA` | `este celular não permite silenciar sem cortar o som da central` |
| 10 | `CONTEUDO_PROTEGIDO` | `conteúdo protegido pelo app` |
| 11 | `MODO_TELA_DO_CELULAR` | `modo tela do celular — ative a tela horizontal no celular` (aviso de plano B, RN-105) |
| 12 | `LIGACAO_NO_CELULAR` | `ligação em andamento — atenda no celular` (RN-101, modo tela virtual) |

O celular envia `Situacao` logo após o aperto e sempre que o conjunto muda. A central exibe o texto de cada aviso recebido. `FALTA_BATERIA` também é exibido localmente em cada aparelho (RN-015).

### Mídia

- **Vídeo (RNF-016):** H.264 (`MediaCodec`), ≥ 30 qps. Tamanho = `TamanhoDoVideo.calcular(telaDoCelular, areaDoOla)`: a tela do celular reduzida (nunca ampliada), com proporção preservada, até caber na área informada no `Ola` **e** ter o lado maior ≤ 1280 px; lados arredondados para baixo a múltiplos de 16. Baixo atraso e fluidez têm prioridade sobre nitidez (o bitrate pode cair para cumprir RNF-001/RNF-003); o texto do celular deve continuar legível (verificação manual). O celular loga `video=<L>x<A>` a cada `VideoConfig` (§7.2). `VideoConfig` + quadro-chave no início, na retomada e a cada mudança de orientação do celular (RN-046).
- **Pedido de captura (RN-021):** no Android 14+, o celular pede a captura com `MediaProjectionConfig.createConfigForDefaultDisplay()` (tela inteira); se o sistema ainda oferecer "um app" e o dono escolher, o celular envia `Aviso.COMPARTILHANDO_SO_UM_APP`.
- **Som:** Opus 48 kHz estéreo (RNF-014), quadros de 20 ms, capturado com `AudioPlaybackCapture` (`USAGE_MEDIA`, `USAGE_GAME`, `USAGE_UNKNOWN`). Na central, toca em `AudioTrack` com `USAGE_MEDIA` e foco de áudio `AUDIOFOCUS_GAIN` (RN-068), com ganho de `VolumeCentral.ganho`.
- **Prioridade (RN-056, RN-067):** `PoliticaDeBanda.alocar` define o bitrate; `FilaDeEnvio` no celular entrega controle → som → vídeo e, acima do limite, descarta vídeo (nunca som) até o próximo quadro-chave.

---

## 5. Estados e transições

### 5.1 Central — `SessaoCentral.reduzir(atual, evento, agoraMs): EstadoCentral`

Estado inicial: `AguardandoCelular`. Janela: `SessaoCentral.JANELA_DE_RECONEXAO_MS = 60_000`.

| Estado | `texto` (RN-083) | `deveConectar` | `encaminhaToques` | `tocaSom` | `imagemDesatualizada` |
|---|---|---|---|---|---|
| `AguardandoCelular` | `aguardando o celular` | sim | não | não | não |
| `Conectando` | `conectando` | não | não | não | não |
| `AguardandoConfirmacao` | `aguardando confirmação no celular` | não | não | não | não |
| `Espelhando` | `espelhando` | não | sim | sim | não |
| `DesbloqueieCelular` | `desbloqueie o celular` | não | não | não | não |
| `Reconectando(desdeMs)` | `reconectando` | sim | não | não | sim |
| `EmUsoPorOutraCentral` | `celular em uso por outra central` | não | não | não | não |
| `Encerrado(motivo)` | `motivo.texto` | sim só para `CONEXAO_PERDIDA` e `APP_FECHADO_OU_DESLIGADO` | não | não | não |

Transições:

| De | Evento | Para |
|---|---|---|
| `AguardandoCelular`, `Encerrado(*)` | `ConexaoAberta` | `Conectando` |
| `Conectando`, `Reconectando`, `AguardandoConfirmacao`, `Espelhando`, `DesbloqueieCelular`, `EmUsoPorOutraCentral` | `Recebeu(Estado(x))` | estado de `x` (`AGUARDANDO_CONFIRMACAO`→`AguardandoConfirmacao`, `ESPELHANDO`→`Espelhando`, `DESBLOQUEIE`→`DesbloqueieCelular`, `EM_USO_POR_OUTRA_CENTRAL`→`EmUsoPorOutraCentral`) |
| qualquer conectado ou `Reconectando` | `Recebeu(Encerrar(m))` | `Encerrado(m)` |
| qualquer conectado ou `Reconectando` | `EncerrarTocado` | `Encerrado(ENCERRADO_PELA_CENTRAL)` (envia `Encerrar`) |
| `AguardandoConfirmacao`, `Espelhando`, `DesbloqueieCelular` | `CelularFechouConexao` | `Encerrado(APP_FECHADO_OU_DESLIGADO)` |
| `Conectando`, `EmUsoPorOutraCentral` | `CelularFechouConexao` ou `Queda` ou `RedeIndisponivel` | `AguardandoCelular` |
| `AguardandoConfirmacao`, `Espelhando`, `DesbloqueieCelular` | `Queda` ou `RedeIndisponivel` | `Reconectando(agoraMs)` |
| `Reconectando(d)` | `Relogio(t)` com `t − d ≥ 60 000` | `Encerrado(CONEXAO_PERDIDA)` |
| `Encerrado(*)` | `RedeDisponivel` (a central voltou à rede) | `AguardandoCelular` |
| `Encerrado(*)` | `EspelharTocado` ou `TentarDeNovoTocado` | `AguardandoCelular` |
| demais combinações | — | sem mudança |

"Conectado" = `Conectando`, `AguardandoConfirmacao`, `Espelhando`, `DesbloqueieCelular`, `EmUsoPorOutraCentral`.

Consequências: encerrado pelo dono ou "não confirmado" não reconecta sozinho na mesma rede (RN-049, RN-020); não existe estado de "veículo parado" (RN-087); toques em `Reconectando` são descartados, nunca enfileirados (RN-053).

### 5.2 Celular — `SessaoCelular`

`SessaoCelular.inicial(liberacaoPermanente: Boolean, versaoAndroid: Int)` → `estado = Escuta`, `capturaViva = false`. `reduzir(evento, agoraMs): SessaoCelular` é puro (data class imutável). `capturaViva` = token de captura do "Permitir" ainda válido no processo (RN-074); um processo novo começa sem ele.

Prazos: `SessaoCelular.PRAZO_CONFIRMACAO_MS = 30_000`, `SessaoCelular.JANELA_DE_RECONEXAO_MS = 60_000`.

| De | Evento | Para |
|---|---|---|
| `Escuta`, `Encerrado(*)` | `CentralConectou` | `Espelhando` se `liberacaoPermanente` ou `capturaViva`; senão `AguardandoConfirmacao(agoraMs)` |
| `Reconectando` | `CentralConectou` | `Espelhando` (retomada, sem "Permitir") |
| `AguardandoConfirmacao` | `PermitirTocado` | `Espelhando`, `capturaViva = true` |
| `AguardandoConfirmacao(d)` | `Relogio(t)`, `t − d ≥ 30 000` | `Encerrado(NAO_CONFIRMADO_NO_CELULAR)` |
| `Espelhando` | `CelularBloqueado` | `Bloqueado`; se não há `liberacaoPermanente` e `versaoAndroid ≥ 35`, `capturaViva = false` |
| `Bloqueado` | `CelularDesbloqueado` | `Espelhando` se `liberacaoPermanente` ou `capturaViva`; senão `AguardandoConfirmacao(agoraMs)` |
| `AguardandoConfirmacao`, `Espelhando`, `Bloqueado` | `Queda` | `Reconectando(agoraMs)` |
| `Reconectando(d)` | `Relogio(t)`, `t − d ≥ 60 000` | `Encerrado(CONEXAO_PERDIDA)` |
| qualquer ativo | `EncerrarTocado` | `Encerrado(ENCERRADO_PELO_CELULAR)` |
| qualquer ativo | `Recebeu(Encerrar(m))` | `Encerrado(m)` |
| qualquer ativo | `CentralFechouConexao` | `Encerrado(APP_FECHADO_OU_DESLIGADO)` |

"Ativo" = `AguardandoConfirmacao`, `Espelhando`, `Bloqueado`, `Reconectando`.

`capturaViva` sobrevive a `Encerrado` (RN-074: entre conexões, enquanto o app vive).

Propriedades derivadas:

| Estado | `estadoRemoto` | `mostraIndicador` (RN-047) | `ajustesAplicados` (giro, mudo, bloqueio) | `enviaSom` | `pedePermitir` |
|---|---|---|---|---|---|
| `Escuta` | — | não | não | não | não |
| `AguardandoConfirmacao` | `AGUARDANDO_CONFIRMACAO` | não | não | não | sim |
| `Espelhando` | `ESPELHANDO` | sim | sim | sim | não |
| `Bloqueado` | `DESBLOQUEIE` | sim | sim | não | não |
| `Reconectando` | — | sim | sim | não | não |
| `Encerrado` | — | não | não | não | não |

Quando `ajustesAplicados` passa a falso, o app chama `RestauracaoDoCelular.restaurar()`.

### 5.3 Uma central por vez (RN-078) — `FilaDeCentrais`

`chegou(id: Long): Boolean` → `true` se a conexão virou a ativa, `false` se ficou em espera (o celular envia `Estado(EM_USO_POR_OUTRA_CENTRAL)` e mantém a conexão com `Ping`). `saiu(id: Long): Long?` → remove; se era a ativa, devolve a próxima em espera (por ordem de chegada), que passa a ser ativa e recebe o fluxo normal. `ativa: Long?`. Uma central em `Reconectando` continua ativa até a janela vencer.

---

## 6. Tipos puros que a implementação expõe (para `app/src/test`)

Todos em Kotlin, sem dependência de Android (rodam na JVM).

```kotlin
// com.ctrlmesh.protocolo
enum class Papel { CELULAR, MULTIMIDIA }
class ProtocoloException(mensagem: String) : Exception(mensagem)
enum class EstadoRemoto { AGUARDANDO_CONFIRMACAO, ESPELHANDO, DESBLOQUEIE, EM_USO_POR_OUTRA_CENTRAL }
enum class AcaoToque { DESCE, MOVE, SOBE, CANCELA }
enum class TeclaDeSistema { VOLTAR, INICIO, RECENTES }
enum class MotivoEncerramento { ENCERRADO_PELO_CELULAR, ENCERRADO_PELA_CENTRAL, CONEXAO_PERDIDA, APP_FECHADO_OU_DESLIGADO, NAO_CONFIRMADO_NO_CELULAR; val texto: String }
enum class Aviso { /* §4, nesta ordem */; val bit: Int; val texto: String }

sealed interface Mensagem {
    data class Ola(val nomeCentral: String, val larguraArea: Int, val alturaArea: Int) : Mensagem
    data class Estado(val estado: EstadoRemoto) : Mensagem
    data class Situacao(val avisos: Set<Aviso>) : Mensagem
    class VideoConfig(val largura: Int, val altura: Int, val codec: String, val csd: List<ByteArray>) : Mensagem
    class VideoQuadro(val ptsUs: Long, val chave: Boolean, val dados: ByteArray) : Mensagem
    class AudioConfig(val taxa: Int, val canais: Int, val codec: String, val csd: List<ByteArray>) : Mensagem
    class AudioQuadro(val ptsUs: Long, val dados: ByteArray) : Mensagem
    data class Toque(val ponteiro: Int, val acao: AcaoToque, val x: Float, val y: Float,
                     val larguraQuadro: Int, val alturaQuadro: Int, val tempoMs: Long) : Mensagem
    data class Tecla(val tecla: TeclaDeSistema) : Mensagem
    data class Encerrar(val motivo: MotivoEncerramento) : Mensagem
    data class Ping(val seq: Int) : Mensagem
    data class Pong(val seq: Int) : Mensagem
    data class Desconhecida(val tipo: Int) : Mensagem
}

object Protocolo {
    const val PORTA = 47800
    const val VERSAO = 1
    fun codificar(mensagem: Mensagem): ByteArray                       // tipo + conteúdo (sem tamanho, sem cifra)
    fun decodificar(quadro: ByteArray, receptor: Papel): Mensagem       // lança ProtocoloException
}

object Aperto {
    const val TAMANHO = 74
    fun codificar(chavePublica: ByteArray): ByteArray
    fun decodificar(bytes: ByteArray): ByteArray                        // devolve a chave pública; lança ProtocoloException
}

class CanalSeguro {
    fun cifrar(quadro: ByteArray): ByteArray
    fun decifrar(blob: ByteArray): ByteArray                            // lança java.security.GeneralSecurityException
    companion object {
        fun gerarParDeChaves(): java.security.KeyPair                   // EC secp256r1
        fun publicaX962(par: java.security.KeyPair): ByteArray          // 65 bytes
        fun criar(papel: Papel, meuPar: java.security.KeyPair, publicaDoOutro: ByteArray): CanalSeguro
    }
}

data class Alocacao(val audioKbps: Int, val videoKbps: Int)
object PoliticaDeBanda { fun alocar(bandaKbps: Int): Alocacao }

object TamanhoDoVideo {                                                 // RNF-016
    const val LADO_MAXIMO = 1280
    fun calcular(larguraTela: Int, alturaTela: Int, larguraArea: Int, alturaArea: Int): Pair<Int, Int>
}         // som constante; vídeo absorve a falta; soma ≤ banda

class FilaDeEnvio(limiteBytes: Int) {
    fun ofertar(mensagem: Mensagem)
    fun proxima(): Mensagem?                                            // controle > som > vídeo; descarta vídeo até o próximo quadro-chave
}

class DetectorDeQueda(inicioMs: Long, limiteMs: Long = 3_000) {
    fun recebeu(agoraMs: Long)
    fun caiu(agoraMs: Long): Boolean
}
```

```kotlin
// com.ctrlmesh.central
sealed interface EstadoCentral {
    data object AguardandoCelular : EstadoCentral
    data object Conectando : EstadoCentral
    data object AguardandoConfirmacao : EstadoCentral
    data object Espelhando : EstadoCentral
    data object DesbloqueieCelular : EstadoCentral
    data class Reconectando(val desdeMs: Long) : EstadoCentral
    data object EmUsoPorOutraCentral : EstadoCentral
    data class Encerrado(val motivo: MotivoEncerramento) : EstadoCentral
    val texto: String
    val deveConectar: Boolean
    val encaminhaToques: Boolean
    val tocaSom: Boolean
    val imagemDesatualizada: Boolean
}
sealed interface EventoCentral {
    data object RedeDisponivel : EventoCentral
    data object RedeIndisponivel : EventoCentral
    data object ConexaoAberta : EventoCentral
    data class Recebeu(val mensagem: Mensagem) : EventoCentral
    data object Queda : EventoCentral
    data object CelularFechouConexao : EventoCentral
    data object EncerrarTocado : EventoCentral
    data object EspelharTocado : EventoCentral
    data object TentarDeNovoTocado : EventoCentral
    data class Relogio(val agoraMs: Long) : EventoCentral
}
object SessaoCentral {
    const val JANELA_DE_RECONEXAO_MS = 60_000L
    val inicial: EstadoCentral                                          // AguardandoCelular
    fun reduzir(atual: EstadoCentral, evento: EventoCentral, agoraMs: Long): EstadoCentral
}

data class Retangulo(val esquerda: Int, val topo: Int, val direita: Int, val base: Int) {
    val largura: Int; val altura: Int                                  // direita/base exclusivos
}
data class LayoutCentral(val barra: Retangulo, val imagem: Retangulo)
object Layout {
    // v3.3 (RN-109, RN-110): barra = faixa embaixo, largura total, altura `alturaBarra`
    //   (Retangulo(0, alturaTela - alturaBarra, larguraTela, alturaTela)).
    // imagem: inteira, proporção preservada, a maior que cabe na área acima da barra
    //   (larguraTela × (alturaTela - alturaBarra)), centralizada na horizontal e com a base encostada no topo da barra.
    fun calcular(larguraTela: Int, alturaTela: Int, larguraImagem: Int, alturaImagem: Int, alturaBarra: Int): LayoutCentral
}
data class PontoNormalizado(val x: Float, val y: Float)

object Descoberta { fun alvos(enderecoInformado: String?, gateway: String?): List<String> }

class VolumeCentral(niveis: Int = 10) {                                 // começa no nível máximo, sem mudo
    val nivel: Int; val mudo: Boolean; val ganho: Float                 // ganho = 0 se mudo, senão nivel/niveis
    fun menos(); fun mais(); fun alternarMudo()                         // "−"/"+" limitados a 0..niveis
}
```

```kotlin
// com.ctrlmesh (toque nos dois lados)
object MapeadorDeToque {
    fun naCentral(layout: com.ctrlmesh.central.LayoutCentral, x: Float, y: Float): com.ctrlmesh.central.PontoNormalizado?  // null fora da imagem (faixas, barra)
    fun noCelular(toque: Mensagem.Toque, larguraTela: Int, alturaTela: Int): Pair<Float, Float>?  // px; null se a orientação do quadro difere da tela atual
}
```

```kotlin
// com.ctrlmesh.celular
sealed interface EstadoCelular {
    data object Escuta : EstadoCelular
    data class AguardandoConfirmacao(val desdeMs: Long) : EstadoCelular
    data object Espelhando : EstadoCelular
    data object Bloqueado : EstadoCelular
    data class Reconectando(val desdeMs: Long) : EstadoCelular
    data class Encerrado(val motivo: MotivoEncerramento) : EstadoCelular
}
sealed interface EventoCelular {
    data object CentralConectou : EventoCelular
    data object PermitirTocado : EventoCelular
    data object CelularBloqueado : EventoCelular
    data object CelularDesbloqueado : EventoCelular
    data object Queda : EventoCelular
    data object CentralFechouConexao : EventoCelular
    data object EncerrarTocado : EventoCelular
    data class Recebeu(val mensagem: Mensagem) : EventoCelular
    data class Relogio(val agoraMs: Long) : EventoCelular
}
data class SessaoCelular(val estado: EstadoCelular, val capturaViva: Boolean, val liberacaoPermanente: Boolean, val versaoAndroid: Int) {
    fun reduzir(evento: EventoCelular, agoraMs: Long): SessaoCelular
    val estadoRemoto: EstadoRemoto?; val mostraIndicador: Boolean; val ajustesAplicados: Boolean
    val enviaSom: Boolean; val pedePermitir: Boolean
    companion object {
        const val PRAZO_CONFIRMACAO_MS = 30_000L
        const val JANELA_DE_RECONEXAO_MS = 60_000L
        fun inicial(liberacaoPermanente: Boolean, versaoAndroid: Int): SessaoCelular
    }
}

class FilaDeCentrais { fun chegou(id: Long): Boolean; fun saiu(id: Long): Long?; val ativa: Long? }

data class Subrede(val endereco: String, val prefixo: Int)              // IPv4
object RedeDoRoteador { fun aceita(enderecoDaCentral: String, redesDoRoteador: List<Subrede>): Boolean }

data class PlanoDeAudio(val enviarSom: Boolean, val silenciarCelular: Boolean, val avisos: Set<Aviso>) {
    companion object { fun decidir(podeGravarAudio: Boolean, silenciarZeraSomEnviado: Boolean): PlanoDeAudio }
}

interface AjustesDoCelular {                                            // implementação real: Settings.System + AudioManager
    var rotacao: Int               // 0..3 (Surface.ROTATION_*), Settings.System.USER_ROTATION
    var giroAutomatico: Boolean    // ACCELEROMETER_ROTATION
    var brilho: Int                // 0..255, SCREEN_BRIGHTNESS
    var brilhoAutomatico: Boolean  // SCREEN_BRIGHTNESS_MODE
    var tempoDeBloqueioMs: Int     // SCREEN_OFF_TIMEOUT
    var volumeMidia: Int           // STREAM_MUSIC
    var volumeToque: Int           // STREAM_RING
    var volumeAlarme: Int          // STREAM_ALARM
}
data class AjustesSalvos(val rotacao: Int, val giroAutomatico: Boolean, val brilho: Int, val brilhoAutomatico: Boolean,
                         val tempoDeBloqueioMs: Int, val volumeMidia: Int)
interface ArmazenamentoDeRestauracao {                                  // implementação real: arquivo/SharedPreferences (sobrevive à morte do app)
    fun salvar(ajustes: AjustesSalvos); fun ler(): AjustesSalvos?; fun apagar()
}
class RestauracaoDoCelular(ajustes: AjustesDoCelular, armazenamento: ArmazenamentoDeRestauracao) {
    // Salva o estado original só se ainda não houver um salvo; depois: paisagem (giroAutomatico=false, rotacao=1)
    // se podeGirar; volume de mídia 0 se silenciarMidia. Nunca mexe em brilho (RN-112), toque ou alarme.
    fun aplicar(podeGirar: Boolean, silenciarMidia: Boolean)
    // Volta ao salvo e apaga; sem nada salvo, não faz nada. Chamado ao encerrar e ao iniciar o app (RN-028, RN-060).
    fun restaurar()
}
```

No plano B, a tela ligada durante o espelhamento (RN-111) é responsabilidade da implementação (ex.: `FLAG_KEEP_SCREEN_ON` na janela do indicador), sempre com o brilho do dono; se ela mexer em `tempoDeBloqueioMs`, a restauração cobre. O CtrlMesh nunca altera `brilho` nem `brilhoAutomatico` (RN-112).

---

## 7. Configuração e componentes Android (para `app/src/androidTest`)

```kotlin
// com.ctrlmesh
class ConfiguracaoDoApp(context: android.content.Context) {   // persistida (SharedPreferences "ctrlmesh")
    var papel: Papel?                // null = perguntar na abertura (RN-080)
    var enderecoDoCelular: String?   // RN-014; só na central
    fun redefinir()                  // apaga papel, endereço e ajustes salvos
}
```

- `ServicoDeEscuta` inicia com o app aberto em qualquer papel e com `ReceptorDeInicio.onReceive(context, Intent(ACTION_BOOT_COMPLETED))`. No papel celular, mostra notificação permanente (`FLAG_ONGOING_EVENT`) enquanto escuta (RN-012).
- A central lê `ConfiguracaoDoApp` ao (re)conectar; mudar o endereço vale para a próxima tentativa.
- Central em primeiro plano (RN-084): ao entrar em `Espelhando`, se `Settings.canDrawOverlays`, traz `MainActivity` para a frente; senão publica notificação com o texto `toque para ver o celular`, cujo toque abre `MainActivity`.
- Central: `MainActivity` em `SCREEN_ORIENTATION_SENSOR_LANDSCAPE` no papel multimídia (RN-024). Celular: orientação livre.

### 7.1 Tags e textos da interface (Compose)

Escolha de papel (RN-080): botões com textos `Celular` e `Multimídia`, tags `papel-celular`, `papel-multimidia`.

Central em sessão:

| Tag | Conteúdo |
|---|---|
| `estado` | texto de `EstadoCentral.texto` |
| `instrucao` | em `AguardandoConfirmacao`: `toque em Permitir no celular` |
| `imagem` | área do vídeo (só existe com `VideoConfig` recebido) |
| `imagem-desatualizada` | rótulo visível só em `Reconectando`, texto `imagem desatualizada` |
| `barra` | barra horizontal embaixo, largura total, fundo semitransparente, numa faixa reservada com a altura mínima para os alvos de 76 dp; nunca cobre a imagem (RN-109, RN-110). Contém, da esquerda para a direita, os botões abaixo e o `estado` |
| `botao-voltar`, `botao-inicio`, `botao-recentes`, `botao-menos`, `botao-mais`, `botao-mudo`, `botao-ajustes`, `botao-encerrar` (nessa ordem) | controles da barra, todos ≥ 76 dp (RN-044); `botao-encerrar` visível direto em todo estado conectado ou `Reconectando` (RN-045) |
| `botao-espelhar` | em `Encerrado(*)` exceto `NAO_CONFIRMADO_NO_CELULAR`, texto `espelhar` |
| `botao-tentar-de-novo` | em `Encerrado(NAO_CONFIRMADO_NO_CELULAR)`, texto `tentar de novo` |
| `avisos` | um texto por `Aviso` ativo (§4) |
| `sons-nao-enviaveis` | botão `alguns sons não chegam à central`; ao tocar, mostra `ligações`, `alarmes`, `toques`, `notificações`, `voz de navegação`, `apps que bloqueiam a captura` (RN-063) |
| `botao-configuracoes` | abre configurações da central fora do espelhamento |
| `botao-ajustes` | `Ajustes`: abre as mesmas configurações (com `campo-endereco`) sem encerrar o espelhamento (RN-109) |
| `campo-endereco`, `botao-salvar-endereco` | endereço do celular (RN-014) |
| `botao-redefinir` | `Redefinir o CtrlMesh` (RN-080), nas configurações dos dois papéis |

No papel multimídia, a tela principal sempre tem `estado` e `botao-configuracoes`; fora de `Espelhando`, mostra também a lista de ajustes abaixo do estado (a lista não bloqueia a conexão).

Primeira vez / ajustes da central (RN-085, RN-011, RN-015): itens com `semantics(mergeDescendants = true)` e texto contendo `ok` ou `falta`, com botão `Abrir ajuste` quando falta: `ajuste-sobrepor` (`Aparecer sobre outros apps`), `ajuste-bateria` (`Fora da economia de bateria`), `ajuste-inicio-automatico` (`Inicialização automática`, só quando a central tiver o ajuste próprio), `ajuste-rede` (`Salve a rede do celular com conexão automática` — orientação; o app não consegue ler redes salvas). Aviso de bateria com tag `aviso-bateria` e o texto de `Aviso.FALTA_BATERIA`. Não existe nenhuma opção de ligar o roteador do celular, parear, desfazer pareamento ou espelhar a central no celular.

Celular (RN-072, RN-075, RN-091): itens `autorizacao-acessibilidade`, `autorizacao-girar-tela`, `autorizacao-sobrepor`, `autorizacao-gravar-audio`, `autorizacao-bateria`, `liberacao-permanente`, cada um com `mergeDescendants` e texto contendo `ok` ou `falta`, e botão `Abrir ajuste` quando falta (exceto `liberacao-permanente`, que mostra o texto de `Aviso.SEM_LIBERACAO_PERMANENTE`). `aviso-bateria` como na central.

Celular, Shizuku (RN-102): item `shizuku` (`mergeDescendants`) com o texto de `EstadoShizuku.texto`; `shizuku-passo-a-passo` com os passos instalar / ligar pela depuração sem fio / autorizar o CtrlMesh; botão `botao-autorizar-shizuku` (`Autorizar no Shizuku`) quando `SEM_AUTORIZACAO`.

Central, modo tela virtual (RN-095): `inicio-apps` (lista rolável, mostrada em vez de `imagem` enquanto não há app na frente da tela virtual); um item `app-<pacote>` por app, com ícone (`icone-<pacote>`) e o rótulo, na ordem de `InicioDoCtrlMesh.ordenar`; tocar envia `AbrirApp(pacote)`. Nesse modo, `imagem` ocupa toda a área acima da barra (sem faixas).

Notificações do celular: escuta (permanente); `toque para ativar a tela horizontal` (Shizuku desligado com o celular num Wi-Fi; abre o app Shizuku `moe.shizuku.privileged.api`; some quando o Shizuku fica ativo — RN-104); no modo tela virtual o indicador inclui `tela horizontal na central` (RN-108); `toque para espelhar na central` (sem sobreposição, RN-019); indicador de espelhamento com o nome da central e ação `Encerrar`, `FLAG_ONGOING_EVENT|FLAG_NO_CLEAR` (RN-047).

### 7.2 Log para o roteiro e2e

Tag `CtrlMesh`, nível INFO, uma linha por evento, sem conteúdo de imagem, som ou toques:

| Linha | Quando |
|---|---|
| `estado=<texto>` | toda mudança de estado. Central: `EstadoCentral.texto` (em `Encerrado` é o texto do motivo). Celular: `escuta`, `aguardando confirmação`, `espelhando`, `bloqueado`, `reconectando` ou `encerrado motivo=<MotivoEncerramento.texto>` |
| `indicador=<nome da central>` | celular: indicador de espelhamento passou a ser exibido |
| `video=<L>x<A>` | celular: a cada `VideoConfig` enviado (RNF-016) |
| `shizuku=ativo\|desligado\|sem-autorizacao` | celular: ao iniciar e a cada mudança do estado do Shizuku |
| `modo=virtual\|fisica` | celular e central: modo do espelhamento, uma vez por espelhamento |
| `display_virtual=<L>x<A>@<dpi> id=<displayId>` | celular: tela virtual criada ou redimensionada |
| `display_virtual=fechada` | celular: tela virtual fechada |
| `app_aberto=<pacote>` | celular: app aberto na tela virtual por `AbrirApp` ou trazido por Recentes |
| `inicio=<n>` | central: início do CtrlMesh exibido com `n` apps |
| `primeiro-quadro` | central: primeiro quadro de vídeo desenhado após entrar em `Espelhando` |
| `layout imagem=L,T,R,B` | central: a cada novo layout (px da tela) |
| `botao=<tag> L,T,R,B` | central: posição de cada botão da barra a cada novo layout |
| `aviso=<texto>` | central e celular: a cada aviso que passa a ser exibido |
| `recusada=<endereço>` | celular: conexão recusada pela RN-077 |
| `volume nivel=<n> mudo=<bool>` | central: a cada mudança de `VolumeCentral` |

---

## 8. Erros

| Situação | Comportamento |
|---|---|
| Aperto inválido / versão diferente | fecha a conexão; a central fica em `AguardandoCelular` e loga `aviso=versão incompatível — atualize o CtrlMesh nos dois aparelhos` |
| `ProtocoloException` ou falha de decifra | fecha a conexão e trata como queda |
| Central fora do roteador do celular | celular fecha sem enviar nada (log `recusada=`) |
| Segunda central | `Estado(EM_USO_POR_OUTRA_CENTRAL)`, conexão mantida |
| Captura recusada/expirada | `Encerrar(NAO_CONFIRMADO_NO_CELULAR)` |

---

## 9. Liberação pelo Mac (`ferramentas/liberar-celular.sh <serial>`)

Entregue pelo `desenvolvimento`; usado pelo dono (F-01) e pelo `e2e/`. Executa, com o celular no cabo, e mostra o resultado de cada passo (o que falhar fica para o celular orientar, RN-091):

```sh
adb -s "$1" shell appops set com.ctrlmesh PROJECT_MEDIA allow
adb -s "$1" shell appops set com.ctrlmesh SYSTEM_ALERT_WINDOW allow
adb -s "$1" shell appops set com.ctrlmesh WRITE_SETTINGS allow
adb -s "$1" shell pm grant com.ctrlmesh android.permission.RECORD_AUDIO
adb -s "$1" shell pm grant com.ctrlmesh android.permission.POST_NOTIFICATIONS
adb -s "$1" shell dumpsys deviceidle whitelist +com.ctrlmesh
adb -s "$1" shell settings put secure enabled_accessibility_services "<serviços já ativos>:com.ctrlmesh/com.ctrlmesh.celular.ServicoDeControle"   # acrescenta, não substitui
adb -s "$1" shell settings put secure accessibility_enabled 1
```

Sai com código 0 se a liberação de captura (`appops get com.ctrlmesh PROJECT_MEDIA` = `allow`) foi aplicada.

---

## 10. Modo tela virtual via Shizuku (v3.2 — RN-092 a RN-108, F-08)

### 10.1 Shizuku

| Item | Requisito |
|---|---|
| Dependências | `dev.rikka.shizuku:api:13.1.5` e `dev.rikka.shizuku:provider:13.1.5`; `ShizukuProvider` declarado no manifesto (`android:authorities="com.ctrlmesh.shizuku"`, `android:permission="android.permission.INTERACT_ACROSS_USERS_FULL"`, `multiprocess=false`, `exported=true`) |
| Detecção | `EstadoShizuku.de(binderVivo = Shizuku.pingBinder(), autorizado = Shizuku.checkSelfPermission() == PERMISSION_GRANTED)`; reavaliado em `addBinderReceivedListener`/`addBinderDeadListener` e ao abrir o app |
| Pedido de autorização | `Shizuku.requestPermission(Shizuku.CODIGO_PEDIDO = 4701)` ao tocar `botao-autorizar-shizuku`; resultado por `addRequestPermissionResultListener` |
| Ajudante | `Shizuku.bindUserService` com `UserServiceArgs(ComponentName(pacote, "com.ctrlmesh.celular.virtual.AjudanteShell")).processNameSuffix("ajudante").daemon(false)`; roda com uid de shell (2000). A interface AIDL é interna ao celular (livre para o desenvolvimento) e cobre: criar/redimensionar/fechar a tela virtual, iniciar atividade num `displayId`, injetar toques e teclas no `displayId`, mover tarefa da tela física para a virtual e capturar o som (RN-106) |
| Não liga o Shizuku | O CtrlMesh nunca inicia o servidor do Shizuku (RN-103) |

### 10.2 Escolha do modo (RN-093, RN-105)

| `EstadoShizuku` | `ModoEspelhamento` | Avisos extras |
|---|---|---|
| `ATIVO` | `TELA_VIRTUAL` | — |
| `DESLIGADO` | `TELA_FISICA` | `MODO_TELA_DO_CELULAR` |
| `SEM_AUTORIZACAO` | `TELA_FISICA` | `MODO_TELA_DO_CELULAR` |

O modo é fixado ao começar o espelhamento (`ModoDaSessao.iniciar`) e mantido até `encerrar()`, inclusive durante "reconectando" e na retomada; mudanças do Shizuku no meio só valem para o próximo espelhamento. Se o ajudante falhar ao criar a tela virtual, o celular cai para `TELA_FISICA` com `MODO_TELA_DO_CELULAR` antes de enviar `Modo`.

### 10.3 Tela virtual

- Criada pelo ajudante (`DisplayManager.createVirtualDisplay`, nome `CtrlMesh`, flags `PUBLIC|OWN_CONTENT_ONLY|TRUSTED|SUPPORTS_TOUCH|OWN_FOCUS` quando disponíveis) só durante o espelhamento (RN-094). A tela física nunca muda de resolução (RN-092).
- Tamanho = `TamanhoDaTelaVirtual.calcular(larguraArea, alturaArea)`: horizontal, proporção da área da central, lado maior = `min(1280, lado maior da área)`, lados arredondados para baixo a múltiplos de 8. Densidade = `TamanhoDaTelaVirtual.densidade(ladoMaior)` = `round(160 × ladoMaior / 960)`, mínimo 120 (1280 → 213 dpi).
- `Area` recebida → `VirtualDisplay.resize` com o novo tamanho e densidade, novo `VideoConfig` + quadro-chave, sem interromper.
- O vídeo é dessa tela (sem `MediaProjection`, sem "Permitir", sem liberação pelo Mac — RN-099); a central a desenha preenchendo a área acima da barra (`Layout.calcularTelaVirtual`), sem faixas. A área enviada no `Ola`/`Area` é a tela menos a faixa da barra (RN-110).
- `AbrirApp(pacote)`: abre a atividade de entrada do pacote na tela virtual (`ActivityOptions.setLaunchDisplayId`); se a tarefa já existir na tela física, ela é movida para a virtual (RN-096). Envia `app_aberto=` no log.
- Teclas (RN-097): `VOLTAR` = Voltar injetado no `displayId` virtual; `INICIO` = a central mostra o início do CtrlMesh localmente e envia `Tecla(INICIO)`, que no celular nunca leva a tela inicial do celular para a tela virtual; `RECENTES` = alterna para a tarefa anterior que está na tela virtual. Nenhuma tecla age na tela física.
- Quando nenhuma atividade fica na frente da tela virtual (ex.: Voltar saiu do app), o celular envia `TelaVirtualVazia`.
- Ao encerrar (qualquer motivo): tela virtual fechada; os apps continuam vivos no celular (RN-107). Durante `Reconectando`: tela virtual e apps mantidos; na retomada, `Modo`, `VideoConfig` + quadro-chave da mesma tela.
- `SessaoCelular` com `telaVirtual = true`: `CentralConectou` vai direto a `Espelhando` (sem "Permitir"); `CelularBloqueado`/`CelularDesbloqueado` não mudam o estado (RN-098); `ajustesAplicados` é sempre falso (não gira, não mexe em brilho, bloqueio nem volume — RN-098, RN-106).

### 10.4 Toque na tela virtual

`Layout.calcularTelaVirtual(larguraTela, alturaTela, alturaBarra)`: `barra` como em `Layout.calcular`; `imagem` = `Retangulo(0, 0, larguraTela, alturaTela - alturaBarra)`. `MapeadorDeToque.naCentral` sobre esse layout nunca devolve `null` dentro da imagem (sem faixas); `MapeadorDeToque.noCelular(toque, larguraVirtual, alturaVirtual)` dá o pixel da tela virtual. O mecanismo de injeção (ajudante com `InputManager.injectInputEvent` + `displayId`, ou `dispatchGesture` com `displayId`) é livre; o comportamento observável é: o toque age no ponto correspondente da tela virtual e nunca na tela física; toques na tela física não chegam à virtual.

### 10.5 Som (RN-106)

Capturado pelo ajudante (permissão de shell), sem pedido e sem mexer no volume do celular: enquanto o espelhamento dura, o som de mídia que o Android permite desviar sai só na central; ao encerrar, volta a sair no celular. Ligações continuam no celular, e a central mostra `LIGACAO_NO_CELULAR` durante a ligação.

### 10.6 Tipos puros (para `app/src/test`)

```kotlin
// com.ctrlmesh.protocolo
enum class ModoEspelhamento { TELA_VIRTUAL, TELA_FISICA }
// Aviso ganha, no fim: MODO_TELA_DO_CELULAR (bit 11), LIGACAO_NO_CELULAR (bit 12)
data class AppDoCelular(val pacote: String, val rotulo: String, val iconePng: ByteArray)   // equals/hashCode comparam o conteúdo do ícone
// Mensagem ganha:
//   data class Modo(val modo: ModoEspelhamento) : Mensagem
//   data class Area(val larguraArea: Int, val alturaArea: Int) : Mensagem
//   data class ListaDeApps(val apps: List<AppDoCelular>) : Mensagem
//   data class AbrirApp(val pacote: String) : Mensagem
//   data object TelaVirtualVazia : Mensagem
object TamanhoDaTelaVirtual {
    const val LADO_MAXIMO = 1280
    fun calcular(larguraArea: Int, alturaArea: Int): Pair<Int, Int>
    fun densidade(ladoMaior: Int): Int
}

// com.ctrlmesh.celular
enum class EstadoShizuku { ATIVO, DESLIGADO, SEM_AUTORIZACAO;
    val texto: String   // "Shizuku ativo", "Shizuku desligado", "CtrlMesh sem autorização no Shizuku"
    companion object { fun de(binderVivo: Boolean, autorizado: Boolean): EstadoShizuku } }
object EscolhaDeModo {
    fun escolher(shizuku: EstadoShizuku): ModoEspelhamento
    fun avisos(modo: ModoEspelhamento): Set<Aviso>
}
class ModoDaSessao {
    val atual: ModoEspelhamento?
    fun iniciar(shizuku: EstadoShizuku): ModoEspelhamento   // já iniciado: devolve o atual
    fun encerrar()
}
// SessaoCelular ganha `val telaVirtual: Boolean = false` (último parâmetro) e
// `inicial(liberacaoPermanente, versaoAndroid, telaVirtual = false)`; comportamento em §10.3.

// com.ctrlmesh.central
object InicioDoCtrlMesh { fun ordenar(apps: List<AppDoCelular>): List<AppDoCelular> }   // Collator pt-BR, força PRIMARY
// Layout ganha: fun calcularTelaVirtual(larguraTela: Int, alturaTela: Int, alturaBarra: Int): LayoutCentral
```

### 10.7 Quebras

Nenhuma no fio: um celular sem v3.2 não envia `Modo` (a central assume `TELA_FISICA`); uma central sem v3.2 ignora os tipos `0x04`/`0x40`/`0x42` como `Desconhecida`. Na API Kotlin, só acréscimos (parâmetro com padrão em `SessaoCelular`).

---

## 11. Mudanças da v3.3 (regras v1.7)

- **Barra embaixo (RN-109, RN-110):** último parâmetro de `Layout.calcular`/`calcularTelaVirtual` passa a ser `alturaBarra` (antes `larguraBarra`). Posicionalmente compatível; o **significado muda** (quebra de comportamento da API Kotlin, não do fio). Novo botão `botao-ajustes`.
- **Brilho (RN-111, RN-112):** `RestauracaoDoCelular.aplicar` não mexe mais em brilho; `BRILHO_MINIMO` sai do contrato.
- **Fio:** sem mudança; a área do `Ola`/`Area` agora desconta a faixa de baixo em vez da lateral.


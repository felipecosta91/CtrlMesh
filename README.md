# CtrlMesh

Um "Android Auto" caseiro: espelha e controla o seu **celular Android** na **central multimídia Android** do carro, pelo Wi-Fi do próprio celular. Não precisa de internet, servidor ou cabo no carro.

> Feito para **centrais multimídia com Android comum**, como as centrais "Android" de mercado em que se instalam apps por APK. **Não** é para carros com Android Auto de fábrica nem com Android Automotive OS.

## O que ele faz

- Você liga o **roteador (hotspot) do celular**, a multimídia entra nessa rede e o espelhamento **começa sozinho**.
- A multimídia mostra a **lista dos seus apps** numa tela **horizontal** própria. Cada app abre já no layout horizontal, como num tablet.
- **Tudo que você toca na multimídia acontece no celular.** Abriu o Spotify e deu play na central? A música toca.
- O **som do celular sai na multimídia**. As ligações continuam no celular.
- A tela do celular fica livre: você pode continuar usando o celular normalmente.
- Barra de controles na parte de baixo: Voltar, Início, Recentes, volume (−, +, Mudo), Ajustes e Encerrar.
- Se a conexão cair, ele reconecta sozinho.

## Como funciona

```
 Celular (CtrlMesh modo "celular")              Multimídia (CtrlMesh modo "multimídia")
 ├─ roteador Wi-Fi ligado  ◄──────── Wi-Fi ───────── entra na rede do celular
 ├─ Shizuku: cria uma tela virtual horizontal        ├─ conecta no celular (porta 47800)
 ├─ abre os apps nessa tela                          ├─ mostra a imagem e toca o som
 └─ envia imagem (H.264) e som (Opus) ──────────►    └─ envia toques e botões ──► celular
```

O tráfego é cifrado (ECDH P-256 + AES-256-GCM) com uma chave nova a cada conexão. O celular só aceita multimídias que estejam conectadas no roteador dele.

## Requisitos

| | Celular | Multimídia |
|---|---|---|
| Android | 8.0 ou mais novo (testado no Android 16) | 8.0 ou mais novo |
| Precisa de | Roteador Wi-Fi (hotspot), app **Shizuku** | Wi-Fi e permissão para instalar APK |
| Root | Não | Não |

## Download

Baixe na página de [**Releases**](../../releases):

- `CtrlMesh-celular.apk`: instale no celular.
- `CtrlMesh-multimidia.apk`: instale na multimídia.

Os dois arquivos são **o mesmo app**. Na primeira abertura, você escolhe o papel do aparelho ("celular" ou "multimídia").

## Instalação

### 1. No celular

1. Instale o `CtrlMesh-celular.apk`. O Android vai pedir para autorizar a instalação de fontes desconhecidas.
2. Abra o CtrlMesh e escolha **celular**.
3. Libere o que a tela do app pedir:
   - **Acessibilidade do CtrlMesh**: é ela que executa os toques. Se aparecer cinza ou como "configuração restrita", vá em *Configurações → Apps → CtrlMesh → ⋮ → Permitir configurações restritas* e tente de novo.
   - Microfone (gravar áudio), notificações, aparecer sobre outros apps, alterar configurações do sistema e bateria sem restrição.
4. Configure o **Shizuku**, que é o próximo passo.

### 2. Shizuku (no celular)

O [Shizuku](https://shizuku.rikka.app/) é um app gratuito que roda um "ajudante" com a mesma permissão da depuração por cabo (ADB), sem root. O CtrlMesh usa esse ajudante para:

- criar a **tela virtual horizontal** onde os apps abrem;
- capturar a imagem e o **som** sem o aviso "Transmitir/Permitir" a cada viagem;
- executar toques e botões nessa tela.

**Configuração (uma vez só):**

1. Instale o **Shizuku** pela Play Store.
2. Ative as **Opções do desenvolvedor**: *Configurações → Sobre o telefone → toque 7× em "Número da versão"*.
3. Com o celular conectado a um **Wi-Fi** (o de casa, por exemplo), abra o Shizuku → **Iniciar via depuração sem fio** → **Parear**. Siga as instruções do app para digitar o código de pareamento.
4. Toque em **Iniciar**. O Shizuku deve mostrar "Shizuku está em execução".
5. Abra o CtrlMesh → **Autorizar no Shizuku** → **Permitir sempre**.

**Depois de reiniciar o celular**, o Shizuku para. Para religá-lo, use uma destas opções:

- conectado a um Wi-Fi, abra o Shizuku e toque em **Iniciar** (ou ative *Iniciar ao ligar* nas configurações dele, se existir na sua versão);
- pelo computador, com o cabo: use o comando que o Shizuku mostra em *Iniciar via conexão com o computador*.

A depuração sem fio **não funciona com o celular sendo o roteador**, então religue o Shizuku antes de sair, num Wi-Fi. Enquanto o celular não reiniciar, ele continua ativo.

**Sem o Shizuku**, o CtrlMesh continua funcionando no **modo de reserva**:

- espelha a tela normal do celular, girada para a horizontal quando o app permite;
- apps que só funcionam em pé aparecem com faixas pretas dos lados;
- o Android pede **"Transmitir/Permitir"** a cada espelhamento;
- a multimídia avisa que está nesse modo.

### 3. Na multimídia

1. Copie o `CtrlMesh-multimidia.apk` para um pen drive e instale pelo gerenciador de arquivos da central. Autorize "fontes desconhecidas" se ela pedir.
2. Abra o CtrlMesh e escolha **multimídia**.
3. Libere o que a tela de primeira vez mostrar: **aparecer sobre outros apps** e **bateria sem restrição**. Se a central tiver uma lista própria de "início automático", marque o CtrlMesh nela.
4. Conecte a multimídia no **roteador do celular** e marque **conexão automática** nessa rede.

Não é preciso informar endereço: a multimídia encontra o celular sozinha pela rede do roteador.

## Uso no dia a dia

1. Entre no carro e **ligue o roteador do celular**. O Shizuku precisa estar ativo, ou seja, o celular não pode ter reiniciado desde a última vez que você o ligou.
2. A multimídia entra na rede e o CtrlMesh vem para a frente com a lista dos seus apps.
3. Toque num app para abri-lo. Use a barra de baixo para Voltar, Início, Recentes, volume e Encerrar.

O CtrlMesh da multimídia inicia sozinho quando a central liga e também quando ela se conecta a um Wi-Fi. Isso cobre as centrais que só entram em espera em vez de desligar.

## Limitações e cuidados

- **Segurança:** qualquer aparelho com o CtrlMesh no modo multimídia, conectado ao roteador do celular, consegue espelhar o celular. A proteção é a **senha do roteador**, então use uma senha forte.
- **Uso ao dirigir:** não há bloqueio com o veículo em movimento. Use com responsabilidade e respeite a legislação.
- Apps com conteúdo protegido (por exemplo, alguns de streaming de vídeo ou de banco) podem aparecer pretos.
- O desempenho depende do decodificador de vídeo da central. Por padrão, a imagem vai em até 1280 px a 30 quadros/s.
- Celulares Realme, Oppo ou OnePlus (ColorOS) bloqueiam algumas permissões dadas pelo computador. Conceda as permissões pelo próprio celular, como descrito acima.
- Os APKs da release são compilações de **debug** (assinatura de desenvolvimento), feitas para instalação manual.

## Desenvolvimento

Projeto Android único (Kotlin, Gradle, Jetpack Compose), com os dois papéis no mesmo APK.

```
app/        código do app (com.ctrlmesh) e testes (app/src/test, app/src/androidTest)
e2e/        roteiro ponta a ponta: emulador = multimídia, celular físico = celular
docs/       regras de negócio, contrato celular↔multimídia e rastreabilidade RN → CA → teste
```

```bash
./gradlew :app:testDebugUnitTest :app:lint                                  # testes de unidade + lint
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest       # instrumentados no emulador
./gradlew :app:assembleDebug                                                # gera o APK
```

Para testar sem carro, use um emulador de tablet em paisagem como multimídia e o celular físico. O Mac precisa estar conectado ao roteador do celular. Na multimídia do emulador, informe o IP do celular em *Ajustes → Endereço do celular*, porque o emulador fica atrás do NAT do Mac. Detalhes técnicos estão em [`app/README.md`](app/README.md).

- [`docs/regras-de-negocio.md`](docs/regras-de-negocio.md): regras de negócio e critérios de aceite (fonte da verdade).
- [`docs/contrato.md`](docs/contrato.md): protocolo entre celular e multimídia.
- [`docs/rastreabilidade.md`](docs/rastreabilidade.md): de cada regra até o teste que a cobre.

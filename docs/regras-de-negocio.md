# CtrlMesh — Regras de Negócio

- **Versão:** 1.7
- **Data:** 2026-10-26
- **Mudanças da v1.7:** pedidos do dono. (1) A barra de controles da central passa a ser horizontal, embaixo, leve e semitransparente, com Voltar, Início, Recentes, −, +, Mudo, Ajustes e Encerrar, numa faixa reservada que não cobre a imagem. (2) O CtrlMesh nunca escurece a tela do celular. (3) O RNF-001 registra o pedido de mais velocidade, sem mudar as metas. Substituídas: RN-031 (→ RN-111), RN-043 (→ RN-110) e RN-070 (→ RN-109); CA-031 (→ CA-111) e CA-069 (→ CA-112). Entram RN-109 a RN-112 e CA-111 a CA-113. Sem perguntas novas.
- **Mudanças da v1.6:** o dono aceitou a sugestão padrão da P-09, que foi para "Decididas"; a RN-106 já a descrevia e só deixou de ser provisória. Não há perguntas em aberto.
- **Mudanças da v1.5:** no espelhamento real, apps só-retrato (tela inicial do Realme, Spotify, WhatsApp) apareciam em pé na central; o dono quer a central sempre em horizontal e aceita adaptar a tela ("não precisa reproduzir fielmente o layout do celular"). Entra o **modo tela virtual** (principal): com o app Shizuku ativo, o celular cria uma tela virtual horizontal só para a central, a central mostra um início próprio com os apps do celular, e imagem e som vêm dessa tela sem "Permitir". Sem o Shizuku, vale sozinho o **modo tela física** (plano B, o comportamento até a v1.4). Riscadas: RN-030 (→ RN-092) e CA-030 (→ CA-094). Entram RN-092 a RN-108, CA-094 a CA-110, F-08, RNF-017 (premissa verificada no celular do dono) e P-09 (não bloqueia o MVP); o RNF-010 foi ajustado. As RN-021, RN-025 a RN-029, RN-031, RN-034, RN-060, RN-062, RN-073 a RN-075, RN-086 e RN-090 passam a valer só no plano B (RN-093).
- **Mudanças da v1.4:** no espelhamento real, a navegação e a abertura de apps ficaram lentas, e o dono decidiu: "se for para melhorar o desempenho, pode reduzir um pouco a qualidade". O RNF-002 foi riscado e substituído pelo RNF-016: imagem com o lado maior de no máximo 1280 px (ou a área da central, se menor), texto legível, e baixo atraso e fluidez com prioridade sobre nitidez. As metas de atraso (RNF-001, RNF-003, RNF-012 e RNF-013) não mudaram.
- **Mudanças da v1.3:** o dono aceitou as sugestões padrão de P-07 e P-08, que foram para "Decididas"; RN-077 e RN-091 deixam de ser provisórias. Não há perguntas em aberto.
- **Mudanças da v1.2:** o dono decidiu P-01, P-03, P-04, P-05 e P-06. P-01: a captura é liberada uma única vez pelo Mac, com o celular no cabo, e o espelhamento começa sem "Permitir"; se o Android do celular não respeitar essa liberação, vale a alternativa de um "Permitir" que fica valendo enquanto o app estiver vivo. P-03 e P-06: sugestões padrão aceitas. P-04: sem pareamento; qualquer central com o CtrlMesh no modo multimídia conectada ao roteador do celular espelha. P-05: sem restrição com o veículo em movimento. Também ficou explícita a primeira vez na central. Riscadas: RN-002, RN-004 a RN-009, RN-013, RN-016, RN-018, RN-032, RN-051, RN-057, RN-058, RN-065 e RN-069; CA-001, CA-002, CA-004 a CA-009, CA-012, CA-014, CA-015, CA-019, CA-032, CA-048, CA-020, CA-054, CA-055, CA-063, CA-066 e CA-070. Entram RN-073 a RN-091, CA-071 a CA-093, RNF-015 e P-07 e P-08 (nenhuma bloqueia o MVP); F-01 e F-02 foram reescritos e o F-06 passa a ser "Primeira vez na central" (o "Desfazer pareamento" deixou de existir). A decisão D-07 da v0.5 (ocultar imagem em movimento) foi substituída. RN-031, RN-060 e RN-062 deixam de ser provisórias; RNF-004, RNF-006 e RNF-010 foram ajustados; fluxos, glossário e escopo foram reescritos sem pareamento e sem restrição de movimento.
- **Mudanças da v1.1:** o dono decidiu a P-02: o som do celular vai pelo Wi-Fi, junto com a imagem, e toca no CtrlMesh da central (motivo: na central, o som do Bluetooth só sai com o app "Música Bluetooth" aberto, e o espelhamento fica na frente). Substituídas sem renumeração: RN-017 (→ RN-072), RN-040 (→ RN-059), RN-041 (→ RN-060), RN-042 (→ RN-070) e RN-048 (→ RN-071); CA-018 (→ CA-068), CA-040 (→ CA-058), CA-041 (→ CA-056), CA-042 (→ CA-069) e CA-046 (→ CA-070). Entram RN-059 a RN-072, CA-056 a CA-070, F-07, RNF-012 a RNF-014 e P-06; RNF-002, RNF-006 e RNF-010 foram ajustados para incluir o som.
- **Status:** a v1.0 **substitui integralmente a v0.5 por mudança de direção do produto pedida pelo dono**. O CtrlMesh deixa de ser um acesso remoto genérico entre computadores e celulares de um grupo (com serviço de intermediação na internet) e passa a ser só o espelhamento do celular Android do dono na central multimídia Android do carro, pela rede Wi-Fi do roteador (hotspot) do celular, sem internet nem servidor, começando sozinho quando a central entra nessa rede. A numeração de RNs, CAs, fluxos, RNFs e perguntas recomeça do 1: **nenhuma RN ou CA da v0.5 vale na v1.0**, e testes, contrato e rastreabilidade que referenciam a v0.5 precisam ser refeitos a partir deste documento. Da v0.5 foram reaproveitados, com nova numeração e adaptados ao novo cenário: confirmação de captura do Android, autorização de acessibilidade, giro do celular para paisagem e restauração ao encerrar, imagem inteira com faixas, controles na sobra com alvos de 76 dp, Voltar/Início/Recentes, indicador de sessão e encerramento pelos dois lados, reconexão em 60 s com descarte de toques, proteção de conteúdo, restrição com veículo em movimento e as decisões "central Android comum" e "ocultar imagem em movimento". Itens marcados "(provisório — ver P-xx)" valem até a resposta da pergunta correspondente em [Perguntas em aberto](#8-perguntas-em-aberto).
- **Regra de manutenção:** a partir desta versão, nenhuma RN publicada é apagada; regras alteradas mantêm a linha original riscada no formato `- **RN-0XX** — ~~texto antigo~~ (substituída por RN-0YY)` e a nova regra vai para o fim do grupo adequado; o mesmo vale para CAs (`substituído por CA-0YY`) e para decisões de perguntas que mudarem. Só uma nova mudança de direção pedida pelo dono gera outra versão maior com renumeração.

---

## 1. Visão e escopo

### Visão
O CtrlMesh espelha o celular Android do dono na central multimídia Android do carro: o dono liga o roteador do celular, a central entra nessa rede e a tela do celular aparece sozinha na central, em horizontal; tudo o que o dono toca na central acontece no celular (ex.: abrir o Spotify na central toca a música do celular). Os dois aparelhos ficam sempre ativos esperando um pelo outro, sem internet e sem servidor. Depois de uma configuração de primeira vez em cada aparelho (no celular, com uma liberação feita uma vez pelo Mac), nenhuma ação é necessária por viagem além de ligar o roteador do celular. A confiança é a própria rede: só quem está no roteador do celular, que tem senha, consegue espelhá-lo.

### Dentro do MVP
- Um celular Android e uma central multimídia Android comum (com apps instaláveis), sem pareamento: a central espelha o celular cujo roteador ela está usando.
- Configuração de primeira vez no celular (incluindo a liberação permanente da captura pelo Mac) e na central (modo multimídia, aparecer sobre outros apps, inicialização automática e bateria).
- Comunicação direta pela rede Wi-Fi do roteador do celular, sem internet e sem servidor.
- CtrlMesh sempre ativo nos dois aparelhos, iniciando com o aparelho e esperando a conexão.
- Espelhamento iniciado sozinho quando os dois se encontram na rede, sem toque na central nem "Permitir" no celular, com o CtrlMesh da central vindo para a frente sozinho; se o Android do celular não respeitar a liberação permanente, um "Permitir" que vale enquanto o app estiver vivo.
- Modo tela virtual (principal): com o Shizuku ativo no celular, uma tela virtual horizontal só para a central, com um início próprio do CtrlMesh listando os apps do celular, apps abertos em layout horizontal, tela física do celular livre e nenhum "Permitir".
- Modo tela física (plano B, automático quando o Shizuku não está ativo): tela do celular em paisagem na central, com o celular girado para paisagem durante o espelhamento.
- Controle por toque na central executado no celular (toque, toque longo, arrastar, deslizar, pinça) e ações Voltar, Início e Apps recentes.
- Indicador de espelhamento no celular e encerramento pelos dois lados.
- Queda do Wi-Fi e reconexão automática.
- Imagem, toques e som iguais com o veículo parado ou em movimento, sem restrição.
- Som de mídia do celular enviado pela rede junto com a imagem e tocado pelo CtrlMesh na central, com volume controlado na central e o celular silenciado durante o espelhamento (ou não silenciado, com aviso, quando o celular não permitir).

### Fora do MVP
- Internet, servidor, conta de usuário, acesso pela internet e grupo de dispositivos (direção da v0.5).
- Windows, macOS, Linux, iOS e navegador.
- Sentido inverso (central espelhada no celular) e troca de papéis.
- Pareamento, código de confirmação e lista de centrais autorizadas.
- Espelhamento simultâneo em várias centrais.
- Espelhamento entre aparelhos fora da rede do roteador do celular (ex.: Wi-Fi de casa).
- Restrição de uso com o veículo em movimento.
- Ligar o roteador do celular pelo CtrlMesh (o Android não permite que apps comuns liguem o roteador com compartilhamento de internet).
- Som de ligações, alarmes, toques, notificações, voz de navegação e de apps que bloqueiam a captura de som na central (o Android não permite capturá-los).
- Microfone, notificações, área de transferência e transferência de arquivos.
- Android Auto e Android Automotive OS.
- Root e mudança de resolução da tela física do celular. (A tela virtual entrou no MVP na v1.5, pelo Shizuku.)
- Ligar o Shizuku pelo CtrlMesh (impossível) e cabo USB na central para religar o Shizuku.
- Botões de volante e controles físicos do carro.
- Desbloquear o celular pela central e contornar qualquer proteção do sistema ou de apps.

---

## 2. Glossário

| Termo | Definição |
|---|---|
| **Dono** | A única pessoa que usa o CtrlMesh; o celular e a central são dele. |
| **Celular** | O celular Android do dono com o CtrlMesh no papel "celular": é espelhado e controlado. |
| **Central** | A central multimídia Android do carro com o CtrlMesh no modo multimídia (papel "central"): mostra a tela do celular e envia os toques. No ambiente de teste, é um emulador Android no Mac. |
| **Modo multimídia** | O papel "central" do CtrlMesh, escolhido na primeira abertura. Qualquer aparelho nesse modo, conectado ao roteador do celular, pode espelhá-lo. |
| **Roteador do celular** | Função do Android que transforma o celular em ponto de acesso Wi-Fi (hotspot). |
| **Rede do celular** | A rede Wi-Fi criada pelo roteador do celular. |
| **Espelhamento** | Período em que a central mostra a tela do celular e envia os toques a ele. |
| **Escuta** | Estado em que o CtrlMesh fica em segundo plano esperando o outro aparelho aparecer na rede, sem o dono abrir o app. |
| **Confirmação de captura** | Toque em "Permitir" (ou equivalente) que o Android do celular pede para outro app ver a tela, quando a liberação permanente de captura não está valendo. |
| **Liberação permanente de captura** | Autorização dada uma única vez pelo Mac, com o celular no cabo e a depuração USB ligada, que dispensa a confirmação de captura em todos os espelhamentos, inclusive depois de reiniciar o celular. |
| **Autorização de acessibilidade** | Autorização do Android, concedida uma vez no celular, que permite ao CtrlMesh executar os toques vindos da central. |
| **Autorização de girar a tela** | Autorização do Android, concedida uma vez no celular, que permite ao CtrlMesh pôr o celular em paisagem durante o espelhamento. |
| **Autorização de aparecer sobre outros apps** | Autorização do Android, concedida uma vez em cada aparelho: no celular, permite ao CtrlMesh iniciar a captura sem o dono abrir o app; na central, permite ao CtrlMesh vir para a frente sozinho quando o espelhamento começa. |
| **Indicador de espelhamento** | Sinal visível no celular durante todo o espelhamento, com o nome da central e um botão para encerrar. |
| **Conteúdo protegido** | Tela ou app que o Android ou o próprio app impede de capturar (ex.: apps de banco e de vídeo por assinatura). |
| **Janela de reconexão** | Tempo em que um espelhamento interrompido por queda de rede pode ser retomado sem ação do dono. |
| **Som do celular** | Som de mídia, jogos e apps sem classificação que toca no celular e que o Android permite capturar; é o que o CtrlMesh envia à central. |
| **Som não enviável** | Som que o Android não permite capturar: ligações, alarmes, toques, notificações, voz de navegação e o som de apps que bloqueiam a captura. |
| **Autorização de gravar áudio** | Autorização do Android, concedida uma vez no celular, exigida para o CtrlMesh capturar o som do celular. |
| **Tela física** | A tela de verdade do celular, que o dono vê e toca no aparelho. |
| **Tela virtual** | Tela extra, horizontal e invisível no aparelho, que o celular cria só para a central durante o espelhamento. |
| **Modo tela virtual** | Modo principal: a central mostra e controla a tela virtual. Exige o Shizuku ativo. |
| **Modo tela física (plano B)** | Modo usado quando o Shizuku não está ativo: a central espelha a tela física, como até a v1.4. |
| **Shizuku** | App de terceiros instalado no celular que, depois de ligado pela depuração sem fio, dá ao CtrlMesh as permissões para criar a tela virtual e capturar imagem e som sem "Permitir". Desliga quando o celular reinicia. |
| **Início do CtrlMesh** | Tela própria do CtrlMesh, na tela virtual, com a lista dos apps do celular (ícone e nome). |
| **Faixas** | Áreas da tela da central fora da imagem do celular, que sobram porque as proporções das telas são diferentes. |

---

## 3. Personas e contextos de uso

| Persona | Contexto | Celular → Central | Necessidade principal |
|---|---|---|---|
| **Dono** entrando no carro | Automotivo | Celular no suporte ou no bolso → central do carro | Ligar o roteador do celular e ver a tela dele na central sem mexer em mais nada, para usar Spotify, mapas e outros apps, com a música saindo no som do carro pelo CtrlMesh, sem abrir o app "Música Bluetooth" da central. |
| **Dono** testando o app | Desenvolvimento | Celular físico → emulador Android no Mac | Reproduzir o fluxo do carro com o Mac conectado ao roteador do celular, e fazer pelo Mac a liberação permanente de captura. |

| Contexto | Orientação | Entrada | Pontos de atenção |
|---|---|---|---|
| Central | Sempre paisagem, tela larga e baixa (ex.: 1024×600, 1280×720, 1920×720) | Toque | Alvos grandes, som do Bluetooth só sai com o app "Música Bluetooth" na frente, outro app (rádio, mapa) costuma estar na frente quando o celular conecta, central que "dorme" ao desligar o carro e fecha apps, ajuste próprio de inicialização automática em algumas centrais. |
| Celular | Retrato por padrão; paisagem durante o espelhamento | Toque local continua valendo | Liberação permanente de captura feita pelo Mac (sem ela, "Permitir" no Android 14+), tela precisa ficar ligada e desbloqueada, economia de bateria fechando o app, ligações recebidas. |

---

## 4. Regras de negócio

### 4.1 Aparelhos e papéis
- **RN-001** — O CtrlMesh tem dois papéis fixos: o celular é sempre o espelhado e controlado, e a central é sempre a que mostra e controla; não existe espelhamento no sentido inverso.
- **RN-002** — ~~O papel de cada aparelho (celular ou central) é escolhido uma única vez, na primeira abertura; trocar o papel exige redefinir o CtrlMesh naquele aparelho, o que desfaz o pareamento.~~ (substituída por RN-080)
- **RN-003** — A central é uma central multimídia Android comum, onde o dono instala apps; Android Auto e Android Automotive OS não são suportados.
- **RN-004** — ~~Cada aparelho tem no máximo um par; parear com outro aparelho substitui o par anterior, que deixa de poder espelhar ou ser espelhado.~~ (substituída por RN-078)
- **RN-078** — O celular espelha para uma central por vez: a primeira que se conectar; outra central que se conectar enquanto isso mostra "celular em uso por outra central" e espelha sozinha assim que o espelhamento atual terminar ou a primeira central sair da rede. Substitui a RN-004.
- **RN-080** — O papel de cada aparelho ("celular" ou "multimídia") é escolhido uma única vez, na primeira abertura; trocar o papel exige redefinir o CtrlMesh naquele aparelho. Substitui a RN-002.

### 4.2 Confiança na rede e segurança
- **RN-005** — ~~O pareamento é feito uma única vez, com os dois aparelhos na mesma rede: o dono abre "parear" no celular, a central lista os celulares que estão com "parear" aberto, o dono escolhe o seu, os dois mostram o mesmo código de pareamento e o dono confirma no celular que o código é igual. Fora da tela "parear", o celular não aparece em nenhuma lista. (provisório — ver P-04)~~ (substituída por RN-076)
- **RN-006** — ~~Se o código for recusado no celular ou não for confirmado em 60 segundos, nenhum par é criado e os dois aparelhos mostram "pareamento não concluído".~~ (substituída por RN-076)
- **RN-007** — ~~Depois de pareados, o CtrlMesh não pede mais nenhuma confirmação própria para espelhar; as únicas exceções são a confirmação de captura do Android (RN-018) e a confirmação de veículo parado (RN-058).~~ (substituída por RN-081)
- **RN-008** — ~~O celular só espelha para a central pareada e só executa toques vindos dela; qualquer outro aparelho na rede do celular, mesmo com o CtrlMesh, não vê a tela, não controla o celular e não descobre o nome do celular nem do dono.~~ (substituída por RN-076 e RN-077)
- **RN-009** — ~~O dono desfaz o pareamento com uma única ação em qualquer um dos dois aparelhos; um espelhamento em andamento é encerrado com o motivo "pareamento desfeito", e o outro aparelho deixa de ser par na próxima vez que os dois se encontrarem. Para voltar, é preciso parear de novo.~~ (substituída por RN-076)
- **RN-076** — Não existe pareamento nem código: qualquer aparelho com o CtrlMesh no modo multimídia conectado ao roteador do celular pode espelhar e controlar o celular, sem confirmação. A proteção é a senha do roteador do celular: quem a conhece e tem o CtrlMesh no modo multimídia consegue espelhar o celular. Substitui as RN-005, RN-006, RN-008 e RN-009.
- **RN-077** — O celular só espelha para centrais conectadas ao roteador do próprio celular; aparelhos com o CtrlMesh em outra rede em que o celular também esteja (ex.: Wi-Fi de casa) não conseguem espelhá-lo. Substitui a RN-008.
- **RN-081** — O CtrlMesh não pede nenhuma confirmação própria para espelhar, em nenhum dos aparelhos; a única confirmação possível é a do Android do celular, quando a liberação permanente de captura não estiver valendo (RN-074). Substitui a RN-007.

### 4.3 Rede e conexão automática
- **RN-010** — Celular e central se comunicam só pela rede Wi-Fi local em que os dois estão, sem internet e sem servidor; o espelhamento funciona com os dados móveis do celular desligados.
- **RN-011** — O CtrlMesh não liga o roteador do celular nem escolhe a rede Wi-Fi da central: o dono liga o roteador no celular, e a central entra na rede do celular pelo próprio sistema, como em qualquer Wi-Fi salva. Na instalação da central, o CtrlMesh orienta o dono a salvar a rede do celular com conexão automática.
- **RN-012** — Nos dois aparelhos, o CtrlMesh inicia sozinho quando o aparelho liga ou a central volta do modo de espera, e fica em escuta em segundo plano sem o dono abrir o app; no celular, o Android exige uma notificação permanente enquanto o CtrlMesh está em escuta.
- **RN-013** — ~~Quando o celular e a central pareados passam a estar na mesma rede, em qualquer ordem (o roteador ligado antes ou depois da central), o espelhamento começa sozinho, sem nenhum toque na central; valem só as exceções da RN-007.~~ (substituída por RN-082)
- **RN-014** — A central encontra o celular na rede sem o dono informar nada; se não o encontrar, o dono pode informar o endereço do celular nas configurações da central, e esse endereço passa a ser tentado em toda conexão.
- **RN-015** — Em cada aparelho, o CtrlMesh pede uma única vez para ficar fora da economia de bateria do sistema; enquanto isso não for concedido, o aparelho mostra o aviso "o sistema pode fechar o CtrlMesh e o espelhamento pode não começar sozinho".
- **RN-016** — ~~A central sempre mostra o estado atual: "aguardando o celular", "conectando", "aguardando confirmação no celular", "espelhando", "reconectando" ou "veículo em movimento".~~ (substituída por RN-083)
- **RN-079** — Se a central encontrar mais de um celular com o CtrlMesh na rede, ela espelha o que é o roteador da rede; um endereço informado nas configurações da central (RN-014) tem prioridade.
- **RN-082** — Quando a central no modo multimídia e o celular passam a estar na rede do roteador do celular, em qualquer ordem (o roteador ligado antes ou depois da central), o espelhamento começa sozinho, sem nenhum toque na central nem no celular; vale só a exceção da RN-081. Substitui a RN-013.
- **RN-083** — A central sempre mostra o estado atual: "aguardando o celular", "conectando", "aguardando confirmação no celular" (só na situação da RN-074), "espelhando", "desbloqueie o celular", "reconectando" ou "celular em uso por outra central". Substitui a RN-016.
- **RN-084** — Quando o espelhamento começa, o CtrlMesh da central vem para a frente sozinho, sobre qualquer app que esteja aberto (ex.: rádio ou mapa da central), sem toque do dono; isso exige a autorização de aparecer sobre outros apps na central; sem ela, a central mostra uma notificação "toque para ver o celular" e o som do celular já toca (RN-059).
- **RN-085** — Na primeira vez na central, o CtrlMesh verifica e orienta, com atalho para cada ajuste: modo multimídia, aparecer sobre outros apps, inicialização automática (quando a central tiver esse ajuste próprio), fora da economia de bateria e rede do celular salva com conexão automática (RN-011, RN-015); depois disso, nenhuma ação é necessária na central a cada viagem, e o CtrlMesh mostra na central qual desses ajustes ainda falta.

### 4.4 Autorizações do celular e confirmação de captura
- **RN-017** — ~~O CtrlMesh verifica no celular as autorizações necessárias (acessibilidade, girar a tela, aparecer sobre outros apps e economia de bateria), mostra quais faltam com um atalho para o ponto do sistema onde concedê-las, e a central mostra, ao conectar, quais faltam no celular.~~ (substituída por RN-072)
- **RN-018** — ~~Sempre que o Android do celular exigir confirmação de captura (no Android 14 ou superior, a cada novo espelhamento e depois de o celular ser bloqueado), o celular mostra o pedido do sistema, a central mostra "toque em Permitir no celular" e o espelhamento só começa depois do toque; o CtrlMesh não contorna essa exigência. (provisório — ver P-01)~~ (substituída por RN-073 e RN-074)
- **RN-019** — Com a autorização de aparecer sobre outros apps, o pedido de confirmação aparece sozinho no celular; sem ela, o celular mostra uma notificação "toque para espelhar na central", e o pedido só aparece depois desse toque. Com o celular bloqueado, o pedido só aparece depois de o dono desbloqueá-lo.
- **RN-020** — Se a confirmação não acontecer em 30 segundos, a tentativa é cancelada, a central mostra "não confirmado no celular" com a ação "tentar de novo", e o CtrlMesh não pede de novo sozinho até a próxima vez que os dois se encontrarem na rede.
- **RN-021** — O CtrlMesh pede a captura da tela inteira do celular; se o sistema oferecer e o dono escolher compartilhar só um app, a central mostra só esse app com o aviso "o celular está compartilhando só um app; encerre e escolha a tela inteira".
- **RN-022** — O controle exige a autorização de acessibilidade, concedida uma única vez no celular; sem ela, o espelhamento funciona só com imagem, os toques na central não têm efeito e a central mostra o motivo. Quando o Android exigir liberar "configurações restritas" para apps instalados fora da loja, o CtrlMesh orienta o dono a fazê-lo.
- **RN-023** — Se o Android do celular impedir a autorização de acessibilidade para apps que não são de acessibilidade (ex.: proteção avançada do Android 17 ou superior), o espelhamento funciona só com imagem e a central mostra o motivo; o CtrlMesh não contorna esse bloqueio.
- **RN-072** — O CtrlMesh verifica no celular as autorizações necessárias (acessibilidade, girar a tela, aparecer sobre outros apps, gravar áudio e economia de bateria), mostra quais faltam com um atalho para o ponto do sistema onde concedê-las, e a central mostra, ao conectar, quais faltam no celular. Substitui a RN-017.
- **RN-073** — A captura de tela do celular é liberada uma única vez, na instalação, pelo Mac, com o celular ligado no cabo e a depuração USB ativa (liberação permanente de captura). Com ela valendo, todo espelhamento começa sem "Permitir", inclusive depois de reiniciar o celular, de o app ser fechado pelo sistema ou de o celular ser bloqueado e desbloqueado; depois da liberação, a depuração USB pode ser desligada. Para iniciar a captura sem o dono abrir o app, o celular também precisa da autorização de aparecer sobre outros apps (RN-019). Substitui a RN-018.
- **RN-074** — Se o Android do celular não respeitar a liberação permanente de captura (ou ela não tiver sido feita), vale esta alternativa: no primeiro espelhamento, o dono toca "Permitir" uma vez (RN-019 a RN-021) e a captura fica ativa em segundo plano enquanto o app estiver vivo, inclusive entre conexões (a central sair e voltar da rede não pede de novo); o "Permitir" é pedido de novo só depois de o celular reiniciar, de o app ser fechado ou morto pelo sistema, ou de o celular ser bloqueado no Android 15 ou superior. Enquanto a captura fica ativa entre conexões, o aviso de captura do próprio Android continua aparecendo no celular. Substitui a RN-018.
- **RN-075** — O CtrlMesh detecta, no celular, se a liberação permanente de captura está valendo; quando não estiver (ex.: depois de reinstalar o app), o celular e a central mostram "captura sem liberação permanente — refaça a liberação pelo Mac para não precisar tocar em Permitir", e vale a RN-074.
- **RN-091** — No mesmo passo de liberação pelo Mac, também são concedidas as demais autorizações do celular (acessibilidade, girar a tela, aparecer sobre outros apps, gravar áudio e economia de bateria); as que o Android do celular não aceitar por esse caminho são concedidas no próprio celular, com a orientação da RN-072.

### 4.5 Imagem e orientação
- **RN-024** — A central mostra a tela do celular em tela cheia, sempre em paisagem; a interface do CtrlMesh na central nunca fica em retrato.
- **RN-025** — Com a autorização de girar a tela, o celular gira para paisagem sozinho assim que o espelhamento começa, sem opção de desligar esse giro.
- **RN-026** — Com o celular em paisagem, os apps que aceitam paisagem se reorganizam e ocupam a imagem; os apps travados em retrato continuam em retrato e aparecem inteiros, com faixas dos lados.
- **RN-027** — A imagem do celular aparece sempre inteira, preservando a proporção, sem distorção e sem corte; a sobra vira faixas.
- **RN-028** — Ao encerrar o espelhamento, por qualquer motivo, o celular volta à orientação e ao ajuste de giro automático que tinha antes; se o encerramento ocorreu porque o CtrlMesh foi fechado ou o celular desligou, isso é restaurado na próxima vez que o CtrlMesh executar no celular. Durante "reconectando" e com o veículo em movimento, o giro é mantido.
- **RN-029** — Sem a autorização de girar a tela, a orientação do celular não muda: a imagem aparece em retrato, inteira, com faixas dos lados, e a central mostra que a autorização falta.
- **RN-030** — ~~O CtrlMesh não altera a resolução do celular nem cria nele uma tela virtual do tamanho da central.~~ (substituída por RN-092)
- **RN-111** — No plano B, durante o espelhamento, a tela do celular fica ligada e não se bloqueia sozinha, com o brilho que o dono usa (o CtrlMesh nunca escurece a tela), para o dono poder mexer também pelo celular; ao encerrar, o tempo de bloqueio volta ao que era antes. Motivo: com a tela apagada o Android não gera imagem, e a partir do Android 15 bloquear o celular encerra a captura. Substitui a RN-031 e, como ela, vale só no plano B (RN-093).
- **RN-112** — O CtrlMesh nunca muda o brilho da tela do celular, em nenhum modo; no modo tela virtual, também não mexe no tempo de bloqueio (RN-098).
- **RN-092** — O CtrlMesh nunca altera a resolução da tela física do celular; no modo tela virtual, ele cria no celular uma tela virtual separada só para a central (RN-094), e no modo tela física não cria nenhuma. Substitui a RN-030.
- **RN-031** — ~~Durante o espelhamento, a tela do celular fica ligada e não se bloqueia sozinha, com o brilho no mínimo; ao encerrar, o brilho e o tempo de bloqueio voltam ao que eram antes. Motivo: com a tela apagada o Android não gera imagem, e a partir do Android 15 bloquear o celular encerra a captura.~~ (substituída por RN-111)
- **RN-032** — ~~Se o dono bloquear o celular ou apagar a tela durante o espelhamento, a central mostra "celular bloqueado — desbloqueie o celular"; depois do desbloqueio, o espelhamento volta sozinho ou, se o sistema exigir, depois de nova confirmação de captura (RN-018). A central nunca desbloqueia o celular nem digita senha ou PIN. (provisório — ver P-03)~~ (substituída por RN-086)
- **RN-033** — Conteúdo protegido aparece na central como área escura com o aviso "conteúdo protegido pelo app"; o restante continua funcionando e o CtrlMesh não contorna a proteção.
- **RN-034** — Uma ligação recebida não interrompe o espelhamento: a tela da ligação aparece na central e pode ser atendida ou recusada pelo toque; o áudio da ligação segue o caminho normal do celular.
- **RN-086** — Se o dono bloquear o celular ou apagar a tela durante o espelhamento, a central mostra "desbloqueie o celular"; quando o celular é desbloqueado, o espelhamento volta sozinho, sem "Permitir", graças à liberação permanente de captura (RN-073); na situação da RN-074, no Android 15 ou superior, o "Permitir" é pedido de novo. A central nunca desbloqueia o celular nem digita senha ou PIN. Substitui a RN-032.

### 4.6 Controle
- **RN-035** — Toques na imagem são executados no ponto correspondente da tela do celular: toque, toque longo, arrastar, deslizar e pinça com dois dedos (ex.: tocar no ícone do Spotify na central abre o Spotify no celular, e tocar em "play" toca a música).
- **RN-036** — Toques nas faixas fora da imagem não são enviados ao celular.
- **RN-037** — A central tem sempre à disposição as ações Voltar, Início e Apps recentes do celular.
- **RN-038** — Texto é digitado pelo teclado do próprio celular, que aparece na imagem e é operado pelo toque; a central não abre um teclado próprio.
- **RN-039** — O uso direto do celular continua funcionando durante o espelhamento; o CtrlMesh nunca bloqueia o toque local.

### 4.7 Som
- **RN-040** — ~~O CtrlMesh não transmite som pela rede: o som do celular sai por onde o celular o envia normalmente (Bluetooth do carro, quando conectado, ou alto-falante do celular); ex.: o Spotify aberto pela central toca no som do carro se o celular estiver no Bluetooth do carro. (provisório — ver P-02)~~ (substituída por RN-059)
- **RN-041** — ~~O CtrlMesh não altera o volume nem a saída de som do celular.~~ (substituída por RN-060)
- **RN-059** — Durante o espelhamento, o som do celular é enviado pela rede junto com a imagem e toca no CtrlMesh da central, desde o início e sem nenhuma ação do dono, com o CtrlMesh da central na frente (ex.: o Spotify aberto pela central toca no som do carro sem abrir o app "Música Bluetooth"). Substitui a RN-040.
- **RN-060** — Durante o espelhamento, o CtrlMesh silencia o volume de mídia do celular, para o som não tocar dobrado, e o restaura ao nível anterior ao encerrar por qualquer motivo; se o encerramento ocorreu porque o CtrlMesh foi fechado ou o celular desligou, o volume é restaurado na próxima vez que o CtrlMesh executar no celular. O CtrlMesh não altera os volumes de ligação, alarme, toque e notificação, nem a saída de som do celular (ex.: Bluetooth). Substitui a RN-041. Exceção na RN-090.
- **RN-061** — Enviar o som exige a autorização de gravar áudio, concedida uma única vez no celular; sem ela, o espelhamento funciona sem som, o volume do celular não é silenciado e a central mostra "sem som: autorização de áudio não concedida no celular".
- **RN-062** — O som não enviável nunca chega à central e o CtrlMesh não contorna essa proteção: ligações, alarmes, toques e notificações continuam saindo normalmente no celular; a voz de navegação e o som de apps que bloqueiam a captura, quando tocam pelo volume de mídia, ficam sem som enquanto o celular estiver silenciado (RN-060).
- **RN-063** — Durante uma ligação, a central mostra "ligação em andamento — o som está no celular"; e a central tem sempre disponível, junto aos controles de som, a informação "alguns sons não chegam à central", que, ao ser tocada, lista os sons não enviáveis.
- **RN-064** — O volume do som do celular é controlado na central: pelo volume de mídia da central (inclusive os botões do carro) e pelos controles "−", "+" e "mudo" da barra, que baixam ou sobem um nível ou silenciam e reativam o som no nível anterior; nada disso muda o volume do celular.
- **RN-065** — ~~Com o veículo em movimento, o som do celular continua tocando na central e "−", "+" e "mudo" continuam disponíveis, mesmo com a imagem oculta e os toques bloqueados (RN-057).~~ (substituída por RN-087)
- **RN-066** — Durante "reconectando", a central não toca som e o celular continua silenciado; o som desse intervalo não é tocado depois; ao reconectar, o som volta sozinho; se o espelhamento terminar, vale a restauração da RN-060.
- **RN-067** — Com a rede degradada, o CtrlMesh preserva primeiro a resposta aos toques, depois o som e por último a imagem: a qualidade da imagem cai antes de o som ter cortes.
- **RN-068** — Na central, o som do celular toca como mídia: quando ele começa, os outros apps de mídia da central (ex.: rádio, "Música Bluetooth") param conforme o comportamento do sistema da central, e o CtrlMesh não os retoma ao encerrar.
- **RN-069** — ~~Enquanto o espelhamento ainda não começou (ex.: "aguardando confirmação no celular"), nenhum som é enviado e o volume do celular não é silenciado; o silenciamento começa junto com o envio do som. A confirmação de "veículo parado" (RN-058) só segura a imagem: o som começa assim que a captura é confirmada.~~ (substituída por RN-088)
- **RN-088** — Enquanto o espelhamento ainda não começou (ex.: "conectando" ou "aguardando confirmação no celular"), nenhum som é enviado e o volume do celular não é silenciado; o silenciamento começa junto com o envio do som. Substitui a RN-069.
- **RN-090** — Se, no celular do dono, silenciar o volume de mídia também zerar o som enviado à central, o CtrlMesh não silencia o celular (o som toca nos dois) e a central mostra "este celular não permite silenciar sem cortar o som da central". Exceção à RN-060.

### 4.8 Interface da central
- **RN-042** — ~~Os controles do espelhamento (Voltar, Início, Apps recentes, Encerrar e o estado da conexão) ficam em uma barra lateral no lado esquerdo da tela da central.~~ (substituída por RN-070)
- **RN-043** — ~~A barra lateral ocupa a faixa esquerda quando há faixa suficiente; quando não há, a imagem é reduzida para caber ao lado da barra; a barra nunca cobre a imagem.~~ (substituída por RN-110)
- **RN-044** — Todo controle da central tem área de toque mínima de 76 dp.
- **RN-045** — Encerrar o espelhamento está acessível na central com no máximo uma interação.
- **RN-046** — Quando o celular muda de orientação (ex.: um app travado em retrato é aberto), a central reorganiza imagem e barra sozinha, sem interromper o espelhamento.
- **RN-070** — ~~Os controles do espelhamento (Voltar, Início, Apps recentes, "−", "+", "mudo", Encerrar e o estado da conexão) ficam em uma barra lateral no lado esquerdo da tela da central. Substitui a RN-042.~~ (substituída por RN-109)
- **RN-109** — Os controles do espelhamento ficam numa barra horizontal na parte de baixo da tela da central, de visual leve, com fundo semitransparente, nesta ordem: Voltar, Início, Apps recentes, "−", "+", "Mudo", "Ajustes" e "Encerrar", mais um indicador discreto do estado da conexão (RN-083). "Ajustes" abre as configurações do CtrlMesh na central sem encerrar o espelhamento. Substitui a RN-070.
- **RN-110** — A barra ocupa uma faixa reservada embaixo, com a altura mínima que garante os alvos da RN-044 (76 dp); a imagem fica inteira logo acima dela e nunca é coberta pela barra, para não esconder a navegação dos apps; a área de imagem da central (RN-027, RN-094) é a tela menos essa faixa. Substitui a RN-043.

### 4.9 Indicador e encerramento
- **RN-047** — Durante todo o espelhamento, inclusive em "reconectando", o celular mostra o indicador de espelhamento com o nome da central e o botão de encerrar, visível sem abrir o app e sem opção de ocultá-lo (além do aviso de captura que o próprio Android mostra).
- **RN-048** — ~~Celular e central podem encerrar o espelhamento a qualquer momento com uma única ação; imagem e controle cessam de imediato nos dois lados, depois o indicador some e o celular restaura orientação, brilho e tempo de bloqueio (RN-028, RN-031).~~ (substituída por RN-071)
- **RN-049** — Um espelhamento encerrado pelo dono, em qualquer lado, não recomeça sozinho enquanto os dois continuarem na mesma conexão; recomeça sozinho na próxima vez que os dois se encontrarem na rede, ou antes, pela ação "espelhar" na central.
- **RN-050** — Fechar o CtrlMesh ou desligar qualquer um dos aparelhos encerra o espelhamento; a central sair da rede ou o roteador do celular ser desligado é tratado como queda (RN-052).
- **RN-051** — ~~Ao término, os dois lados mostram o motivo: encerrado pelo celular, encerrado pela central, conexão perdida, app fechado ou aparelho desligado, pareamento desfeito ou não confirmado no celular.~~ (substituída por RN-089)
- **RN-071** — Celular e central podem encerrar o espelhamento a qualquer momento com uma única ação; imagem, som e controle cessam de imediato nos dois lados, depois o indicador some e o celular restaura orientação, brilho, tempo de bloqueio e volume de mídia (RN-028, RN-031, RN-060). Substitui a RN-048.
- **RN-089** — Ao término, os dois lados mostram o motivo: encerrado pelo celular, encerrado pela central, conexão perdida, app fechado ou aparelho desligado, ou não confirmado no celular (só na situação da RN-074). Substitui a RN-051.

### 4.10 Queda e reconexão
- **RN-052** — Quando a conexão cai, o espelhamento entra em "reconectando" nos dois lados: a central mostra a última imagem marcada como desatualizada e o celular mantém o indicador.
- **RN-053** — Toques feitos na central durante "reconectando" são descartados, nunca enviados depois.
- **RN-054** — O CtrlMesh tenta reconectar sozinho durante uma janela de reconexão de 60 segundos; se conseguir, o espelhamento continua sem nenhuma ação do dono e sem nova confirmação de captura no celular.
- **RN-055** — Se a janela terminar sem reconexão, o espelhamento é encerrado com o motivo "conexão perdida", e recomeça sozinho na próxima vez que os dois se encontrarem na rede (RN-013), com nova confirmação de captura se o sistema exigir.
- **RN-056** — Com a rede degradada, o CtrlMesh reduz a qualidade da imagem antes de reduzir a resposta aos toques.

### 4.11 Veículo em movimento
- **RN-057** — ~~Com o veículo em movimento, a imagem do celular fica oculta e os toques na imagem ficam bloqueados na central, com a mensagem "imagem oculta com o veículo em movimento"; o espelhamento não é encerrado, Encerrar continua disponível, o som do celular continua e tudo volta sozinho quando o veículo para.~~ (substituída por RN-087)
- **RN-058** — ~~Se a central não informar se o veículo está em movimento, a imagem só aparece depois de o dono tocar uma vez em "veículo parado" na central, a cada novo espelhamento; reconexões dentro da janela não pedem de novo. (provisório — ver P-05)~~ (substituída por RN-087)
- **RN-087** — O CtrlMesh não restringe nada com o veículo em movimento: imagem, toques, som e controles de som funcionam igual com o veículo parado ou em movimento, sem pedir confirmação de veículo parado. Substitui as RN-057, RN-058 e RN-065.

### 4.12 Modo tela virtual e plano B
- **RN-093** — Cada espelhamento usa um de dois modos, escolhido sozinho no início: **tela virtual**, quando o Shizuku está ativo e o CtrlMesh está autorizado nele; **tela física (plano B)**, caso contrário. O modo não muda no meio de um espelhamento. As RN-021, RN-025 a RN-029, RN-031, RN-034, RN-060, RN-062, RN-073 a RN-075, RN-086 e RN-090 valem só no plano B; as demais valem nos dois modos, salvo quando uma regra deste grupo disser outra coisa.
- **RN-094** — No modo tela virtual, o celular cria uma tela virtual horizontal do tamanho da área de imagem da central, com o lado maior de no máximo 1280 px (RNF-016); a central a mostra inteira, sem faixas, e ela existe só enquanto durar o espelhamento.
- **RN-095** — No modo tela virtual, a central começa mostrando o início do CtrlMesh: a lista dos apps do celular que têm ícone na tela inicial, com ícone e nome, em ordem alfabética, operada por toque.
- **RN-096** — Tocar num app do início do CtrlMesh abre o app na tela virtual, em layout horizontal; um app que se recuse a ficar em horizontal aparece inteiro, com faixas dos lados. Se o app já estiver aberto na tela física, ele passa para a tela virtual.
- **RN-097** — No modo tela virtual, Voltar, Início e Apps recentes da barra agem só na tela virtual: Voltar volta dentro do app aberto nela, Início volta ao início do CtrlMesh e Apps recentes alterna entre os apps abertos na tela virtual.
- **RN-098** — No modo tela virtual, a tela física do celular continua livre e independente: o dono usa o celular normalmente, pode bloqueá-lo ou apagar a tela sem interromper o espelhamento, e o CtrlMesh não gira a tela física nem mexe no brilho ou no tempo de bloqueio; os toques da central agem só na tela virtual e os toques na tela física agem só nela.
- **RN-099** — No modo tela virtual, imagem e som começam sem nenhum "Permitir" e sem a liberação pelo Mac (RN-073).
- **RN-100** — No modo tela virtual, ao tocar num campo de texto, o teclado do celular aparece na tela virtual e é operado pela central.
- **RN-101** — No modo tela virtual, uma ligação recebida aparece na tela física do celular, não na tela virtual; a central mostra "ligação em andamento — atenda no celular" e o espelhamento continua.
- **RN-102** — Na primeira vez, o CtrlMesh mostra no celular o passo a passo do Shizuku (instalar, ligar pela depuração sem fio e autorizar o CtrlMesh dentro dele) e mostra sempre o estado: "Shizuku ativo", "Shizuku desligado" ou "CtrlMesh sem autorização no Shizuku". A autorização dada ao CtrlMesh no Shizuku persiste, inclusive depois de reiniciar o celular.
- **RN-103** — O CtrlMesh não liga o Shizuku sozinho. Depois de o celular reiniciar, o Shizuku fica desligado até o dono religá-lo com o celular conectado a um Wi-Fi, ou até ele subir sozinho, se a opção dele de iniciar ao ligar funcionar no celular. Uma vez ligado, o Shizuku continua ativo ao trocar de rede e ao ligar o roteador do celular, até o próximo reinício.
- **RN-104** — Com o Shizuku desligado e o celular conectado a um Wi-Fi, o celular mostra a notificação "toque para ativar a tela horizontal", que abre o Shizuku; a notificação some quando o Shizuku fica ativo.
- **RN-105** — Sem o Shizuku ativo, o espelhamento começa sozinho no modo tela física (plano B), com as regras dele, e a central mostra o aviso discreto "modo tela do celular — ative a tela horizontal no celular". Quando o Shizuku volta a ficar ativo, a próxima conexão usa a tela virtual sozinha.
- **RN-106** — No modo tela virtual, o som do celular vai para a central sem nenhum pedido e sem o CtrlMesh mexer no volume do celular: enquanto o espelhamento dura, o som que o Android permite desviar sai só na central, e volta a sair no celular ao encerrar; ligações continuam no celular.
- **RN-107** — Ao encerrar no modo tela virtual, por qualquer motivo, a tela virtual é fechada e os apps que estavam nela continuam abertos no celular; durante "reconectando", a tela virtual e os apps abertos nela são mantidos, e, ao reconectar dentro da janela, a central volta a mostrá-los como estavam.
- **RN-108** — No modo tela virtual, o indicador de espelhamento do celular (RN-047) informa também "tela horizontal na central".

---

## 5. Fluxos principais

### F-01 Primeira vez no celular
1. O dono instala o CtrlMesh no celular e escolhe o papel "celular" (RN-001, RN-080).
2. Com o celular no cabo do Mac e a depuração USB ligada, o dono faz pelo Mac a liberação permanente de captura (RN-073, RNF-015) e, no mesmo passo, as demais autorizações que o Android aceitar por esse caminho (RN-091); depois, pode desligar a depuração USB.
3. No celular, o CtrlMesh mostra as autorizações que ainda faltam e leva aos pontos do sistema: acessibilidade (inclusive "configurações restritas", se preciso), girar a tela, aparecer sobre outros apps, gravar áudio e economia de bateria (RN-072, RN-022, RN-061, RN-015), e mostra se a liberação permanente está valendo (RN-075).
4. O celular entra em escuta (RN-012).

### F-02 Conexão automática (a cada viagem)
1. Os dois aparelhos estão em escuta desde que ligaram (RN-012).
2. O dono liga o roteador do celular; a central entra na rede pelo sistema (RN-011), em qualquer ordem (RN-082).
3. A central encontra o celular sozinha ou pelo endereço informado (RN-014, RN-079) e mostra "conectando" (RN-083); o celular só aceita centrais no próprio roteador (RN-077) e uma por vez (RN-078).
4. Com a liberação permanente valendo, a captura começa sem "Permitir" (RN-073); sem ela, vale a alternativa da RN-074: um "Permitir" que fica valendo enquanto o app estiver vivo (RN-019 a RN-021).
5. O CtrlMesh da central vem para a frente sozinho (RN-084) e o espelhamento começa (F-03), sem nenhuma confirmação do CtrlMesh (RN-081).

### F-03 Espelhamento e controle
1. No plano B, o celular gira para paisagem (RN-025) e mantém a tela ligada com o brilho que o dono usa (RN-111, RN-112); o indicador aparece no celular (RN-047).
2. A central mostra a imagem inteira em paisagem, logo acima da barra semitransparente embaixo (RN-024, RN-027, RN-109, RN-110); apps em retrato aparecem com faixas (RN-026, RN-046).
3. Toques na imagem são executados no celular (RN-035); Voltar, Início e Recentes na barra (RN-037); texto pelo teclado do celular (RN-038).
4. O som do celular toca na central e o celular fica silenciado (RN-059, RN-060; ver F-07).
5. Conteúdo protegido aparece escuro (RN-033); ligação recebida aparece e pode ser atendida (RN-034); celular bloqueado pausa e volta sozinho ao desbloquear (RN-086).
6. Com o veículo em movimento, tudo funciona igual (RN-087).

### F-04 Queda e reconexão
1. A conexão cai (ex.: a central saiu da rede ou o roteador foi desligado) → "reconectando" nos dois lados (RN-050, RN-052), toques descartados (RN-053), central sem som e celular ainda silenciado (RN-066).
2. Reconectou em até 60 s → continua sem ação e sem nova confirmação, com o som de volta sozinho (RN-054, RN-066).
3. Não reconectou → encerrado por "conexão perdida", com o volume do celular restaurado; recomeça sozinho na próxima vez que os dois se encontrarem (RN-055, RN-060).

### F-05 Encerramento
1. O dono encerra no celular ou na central com uma ação (RN-045, RN-071), ou ocorre um evento de encerramento (RN-050, RN-055).
2. Imagem, som e controle cessam; o indicador some; o celular restaura orientação, brilho, tempo de bloqueio e volume de mídia (RN-028, RN-031, RN-060, RN-071).
3. Os dois lados mostram o motivo (RN-089).
4. Encerrado pelo dono → só recomeça na próxima conexão ou pela ação "espelhar" na central (RN-049).

### F-06 Primeira vez na central
1. O dono instala o CtrlMesh na central e escolhe o modo multimídia (RN-080).
2. O CtrlMesh orienta, com atalho para cada ajuste: aparecer sobre outros apps, inicialização automática (se a central tiver esse ajuste), fora da economia de bateria e rede do celular salva com conexão automática (RN-085, RN-011, RN-015).
3. A central entra em escuta (RN-012). A partir daí, nenhuma ação na central a cada viagem: quando o celular conecta, o CtrlMesh vem para a frente sozinho (RN-084).

(O antigo F-06 "Desfazer pareamento" deixou de existir com a decisão da P-04.)

### F-07 Som do celular na central
1. Enquanto o espelhamento não começa, nada de som é enviado e o celular não é silenciado (RN-088).
2. Ao começar, o som de mídia do celular passa a tocar no CtrlMesh da central, e outros apps de mídia da central param (RN-059, RN-068); o volume de mídia do celular é silenciado (RN-060), exceto se isso zerar o som enviado, caso em que o celular não é silenciado e a central avisa (RN-090).
3. Sem a autorização de gravar áudio: espelhamento sem som, celular não silenciado e aviso na central (RN-061).
4. Ligações, alarmes, toques e notificações continuam no celular; voz de navegação e apps que bloqueiam a captura não chegam à central; durante uma ligação, a central avisa que o som está no celular (RN-062, RN-063).
5. O dono ajusta o som na central pelo volume da central ou por "−", "+" e "mudo", sem mudar o celular (RN-064, RN-070).
6. Veículo em movimento: o som e os controles de som funcionam igual (RN-087).
7. Queda: sem som na central até reconectar; nada é tocado depois (RN-066). Rede degradada: a imagem piora antes de o som cortar (RN-067).
8. Encerramento: o som para e o volume do celular volta ao anterior (RN-060, RN-071).

### F-08 Modo tela virtual
1. Primeira vez no celular: o CtrlMesh guia a instalação do Shizuku, a ativação pela depuração sem fio e a autorização do CtrlMesh nele (RN-102).
2. Depois de reiniciar o celular: o dono religa o Shizuku num Wi-Fi, ou ele sobe sozinho; enquanto estiver desligado num Wi-Fi, o celular mostra "toque para ativar a tela horizontal" (RN-103, RN-104).
3. Ao começar o espelhamento, o modo é escolhido sozinho (RN-093): sem Shizuku ativo, plano B, com aviso na central (RN-105); com Shizuku ativo, tela virtual.
4. No modo tela virtual, o celular cria a tela virtual horizontal (RN-094), sem "Permitir" (RN-099), e a central mostra o início do CtrlMesh (RN-095).
5. O dono toca num app; ele abre em horizontal na tela virtual (RN-096); Voltar, Início e Recentes agem nela (RN-097); o teclado aparece nela (RN-100).
6. O som vai para a central sem mexer no volume do celular (RN-106); a tela física continua livre, e uma ligação aparece no celular, com aviso na central (RN-098, RN-101).
7. Queda: a tela virtual é mantida durante a janela de reconexão; ao encerrar, ela é fechada e os apps continuam abertos no celular (RN-107).

---

## 6. Critérios de aceite

Legenda de verificação: **[Teste]** = verificável no ambiente de teste (emulador Android no Mac como central + celular físico, ver RNF-010); **[Carro]** = só manual, no carro com a central real.

### Aparelhos e papéis
- **CA-001 → RN-001** [Teste] — ~~Dado um celular e uma central pareados, quando o dono procura na central ou no celular uma forma de espelhar a central no celular, então ela não existe.~~ (substituído por CA-071)
- **CA-002 → RN-002** [Teste] — ~~Dado uma instalação nova, quando o app é aberto pela primeira vez, então o dono escolhe "celular" ou "central"; e, depois de reiniciar o app, a escolha não é pedida de novo; e, quando o dono redefine o CtrlMesh, então o pareamento é desfeito e o papel volta a ser perguntado.~~ (substituído por CA-072)
- **CA-003 → RN-003** [Teste] — Dado o CtrlMesh instalado em um Android comum no papel "central" (o emulador), quando o app abre, então funciona normalmente como central.
- **CA-004 → RN-004** [Teste] — ~~Dado a central A pareada com o celular, quando o celular é pareado com a central B, então A não consegue mais espelhar o celular e mostra que o par foi desfeito.~~ (substituído por CA-073)
- **CA-071 → RN-001** [Teste] — Dado um celular e uma central no modo multimídia na rede do celular, quando o dono procura na central ou no celular uma forma de espelhar a central no celular, então ela não existe.
- **CA-072 → RN-080** [Teste] — Dado uma instalação nova, quando o app é aberto pela primeira vez, então o dono escolhe "celular" ou "multimídia"; depois de reiniciar o app, a escolha não é pedida de novo; e, quando o dono redefine o CtrlMesh, então o papel volta a ser perguntado.
- **CA-073 → RN-078** [Teste] — Dado dois emuladores no modo multimídia no mesmo Mac, conectado ao roteador do celular, e o primeiro já espelhando, quando o segundo se conecta, então ele mostra "celular em uso por outra central" e não recebe imagem; e, quando o primeiro encerra ou sai da rede, então o segundo começa a espelhar sozinho.

### Confiança na rede e segurança
- **CA-005 → RN-005** [Teste] — ~~Dado o celular e a central na mesma rede e sem par, quando o dono abre "parear" no celular, então a central lista o celular; quando o dono o escolhe, então os dois mostram o mesmo código de 6 dígitos; e, quando o dono confirma no celular, então os dois ficam pareados; e, com "parear" fechado no celular, então a central não o lista.~~ (substituído por CA-074)
- **CA-006 → RN-006** [Teste] — ~~Dado um pareamento em andamento, quando o dono recusa o código no celular ou passam 60 segundos sem confirmação, então nenhum par é criado e os dois mostram "pareamento não concluído".~~ (substituído por CA-074)
- **CA-007 → RN-007, RN-013** [Teste] — ~~Dado o celular e a central pareados, desbloqueado e com todas as autorizações, em Android que não exige confirmação de captura ou já confirmada, quando os dois se encontram na rede, então o espelhamento começa sem nenhuma tela do CtrlMesh pedindo confirmação em qualquer um dos lados.~~ (substituído por CA-076)
- **CA-008 → RN-008** [Teste] — ~~Dado o celular pareado com a central e um segundo aparelho com o CtrlMesh no papel "central" na mesma rede, quando esse segundo aparelho tenta encontrar ou espelhar o celular, então não vê o nome do celular, não recebe imagem e não consegue enviar toques.~~ (substituído por CA-075)
- **CA-009 → RN-009, RN-051** [Teste] — ~~Dado um espelhamento em andamento, quando o dono desfaz o pareamento no celular, então o espelhamento termina e a central mostra "pareamento desfeito"; e, na próxima vez que os dois se encontram na rede, nenhum espelhamento começa até um novo pareamento.~~ (substituído por CA-074)
- **CA-074 → RN-076** [Teste] — Dado um emulador recém-instalado no modo multimídia, que nunca se conectou a este celular, quando ele entra na rede do roteador do celular, então o espelhamento começa sem código, sem pareamento e sem confirmação em nenhum dos aparelhos; e não existe no app opção de parear, desfazer pareamento ou listar centrais autorizadas.
- **CA-075 → RN-077** [Teste] — Dado o celular e o Mac conectados ao mesmo Wi-Fi de casa, com o roteador do celular desligado, quando o emulador no modo multimídia tenta espelhar o celular pelo endereço dele, então nenhuma imagem é enviada e nenhum toque é executado no celular; e, quando o Mac passa para o roteador do celular, então o espelhamento começa.
- **CA-076 → RN-081, RN-082** [Teste] — Dado o celular com a liberação permanente de captura e todas as autorizações, e o emulador no modo multimídia com todos os ajustes, quando os dois se encontram na rede do celular, então o espelhamento começa sem nenhuma tela de confirmação, nem do CtrlMesh nem do Android, em qualquer um dos lados.

### Rede e conexão automática
- **CA-010 → RN-010** [Teste] — Dado o celular com o roteador ligado e os dados móveis desligados, e a central na rede do celular, quando os dois se encontram, então o espelhamento funciona normalmente.
- **CA-011 → RN-011** [Teste] — Dado uma instalação nova no papel "central", quando o dono conclui a configuração, então o CtrlMesh mostra a orientação para salvar a rede do celular com conexão automática; e o CtrlMesh não oferece ligar o roteador do celular.
- **CA-012 → RN-012** [Teste] — ~~Dado o celular e a central pareados, quando cada aparelho é reiniciado e ninguém abre o CtrlMesh, então os dois ficam em escuta, o celular mostra a notificação permanente do CtrlMesh, e o espelhamento começa quando os dois se encontram na rede.~~ (substituído por CA-077)
- **CA-013 → RN-012** [Carro] — Dado a central em modo de espera depois de o carro ser desligado, quando o carro é ligado de novo, então o CtrlMesh volta à escuta sem o dono abrir o app.
- **CA-014 → RN-013, RN-016** [Teste] — ~~Dado o celular em escuta e a central em escuta fora da rede do celular, quando a central entra na rede (no teste: o Wi-Fi do emulador é ligado com o Mac na mesma rede do celular), então a central passa de "aguardando o celular" a "conectando" e o espelhamento começa sem nenhum toque na central; e, dado a central já na rede, quando o CtrlMesh do celular passa a escutar depois dela, então o espelhamento também começa sozinho.~~ (substituído por CA-078)
- **CA-015 → RN-013** [Carro] — ~~Dado a central ligada com a rede do celular salva, quando o dono liga o roteador do celular, então a central entra na rede sozinha e o espelhamento começa sem toque na central.~~ (substituído por CA-079)
- **CA-016 → RN-014** [Teste] — Dado uma rede em que a central não encontra o celular sozinha (como o emulador), quando o dono informa o endereço do celular nas configurações da central, então a central encontra o celular nessa e nas próximas conexões.
- **CA-017 → RN-015** [Teste] — Dado um aparelho em que o CtrlMesh ainda está sujeito à economia de bateria, quando o dono abre o CtrlMesh, então vê o aviso "o sistema pode fechar o CtrlMesh e o espelhamento pode não começar sozinho" com o atalho para corrigir; e, depois de corrigir, o aviso some.
- **CA-077 → RN-012, RN-082** [Teste] — Dado o celular e o emulador configurados, quando cada aparelho é reiniciado e ninguém abre o CtrlMesh, então os dois ficam em escuta, o celular mostra a notificação permanente do CtrlMesh, e o espelhamento começa quando os dois se encontram na rede do celular.
- **CA-078 → RN-082, RN-083** [Teste] — Dado o celular em escuta e o emulador em escuta fora da rede, quando o emulador entra na rede (no teste: o Wi-Fi do emulador é ligado com o Mac no roteador do celular), então a central passa de "aguardando o celular" a "conectando" e o espelhamento começa sem nenhum toque na central nem no celular; e, dado o emulador já na rede, quando o CtrlMesh do celular passa a escutar depois dele, então o espelhamento também começa sozinho.
- **CA-079 → RN-082, RN-084** [Carro] — Dado a central ligada, com a rede do celular salva e o rádio aberto na frente, quando o dono liga o roteador do celular, então a central entra na rede sozinha, o CtrlMesh vem para a frente sobre o rádio e o espelhamento começa sem toque na central nem no celular.
- **CA-080 → RN-084** [Teste] — Dado o emulador com a autorização de aparecer sobre outros apps e outro app aberto na frente, quando o espelhamento começa, então o CtrlMesh vem para a frente sozinho; e, dado o emulador sem essa autorização, então aparece a notificação "toque para ver o celular", o som do celular já toca no emulador, e tocar na notificação traz o CtrlMesh para a frente.
- **CA-081 → RN-085** [Teste] — Dado uma instalação nova no modo multimídia, quando o dono conclui a primeira vez, então o CtrlMesh mostrou, com atalho, cada ajuste (aparecer sobre outros apps, fora da economia de bateria, inicialização automática quando houver, rede do celular salva); e, enquanto algum faltar, a central mostra qual falta; e, depois de tudo concedido e de reiniciar o emulador, nenhuma ação na central é necessária para o próximo espelhamento.
- **CA-082 → RN-079** [Teste] — Dado o emulador com o endereço do celular informado e outro celular com o CtrlMesh na mesma rede, quando o emulador se conecta, então espelha o celular do endereço informado.

### Autorizações e confirmação de captura
- **CA-018 → RN-017** [Teste] — ~~Dado o celular sem a autorização de girar a tela, quando o dono abre o CtrlMesh no celular, então vê essa autorização como faltante com um atalho; e, quando a central conecta, então ela também mostra que essa autorização falta no celular.~~ (substituído por CA-068)
- **CA-019 → RN-018** [Teste] — ~~Dado um celular Android 14 ou superior, quando a central se conecta a ele, então o celular mostra o pedido de captura do sistema, a central mostra "toque em Permitir no celular", e a imagem só aparece depois do toque em "Permitir".~~ (substituído por CA-084)
- **CA-020 → RN-019** [Teste] — ~~Dado um celular desbloqueado com a autorização de aparecer sobre outros apps, quando a central se conecta, então o pedido de captura aparece sozinho; dado um celular sem essa autorização, então aparece a notificação "toque para espelhar na central", e o pedido só surge depois do toque nela; e, dado o celular bloqueado, então o pedido só aparece depois do desbloqueio.~~ (substituído por CA-087)
- **CA-021 → RN-020** [Teste] — Dado o pedido de captura na tela do celular, quando passam 30 segundos sem toque, então a central mostra "não confirmado no celular" com "tentar de novo", nenhum novo pedido aparece sozinho enquanto a conexão continua, e "tentar de novo" mostra o pedido outra vez.
- **CA-022 → RN-021** [Teste] — Dado um celular cujo pedido de captura oferece "um app" ou "tela inteira", quando o dono escolhe "um app", então a central mostra só esse app com o aviso "o celular está compartilhando só um app; encerre e escolha a tela inteira".
- **CA-023 → RN-022** [Teste] — Dado o celular sem a autorização de acessibilidade, quando o espelhamento começa, então a imagem aparece, os toques na central não têm efeito e a central mostra o motivo; e, dado o CtrlMesh instalado fora da loja em Android que bloqueia a autorização, então o CtrlMesh mostra como liberar as configurações restritas.
- **CA-024 → RN-023** [Teste, requer celular Android 17+] — Dado o celular com a proteção avançada ligada, quando o espelhamento começa, então a imagem aparece, os toques não têm efeito e a central mostra que o Android do celular bloqueia o controle.
- **CA-068 → RN-072** [Teste] — Dado o celular sem a autorização de girar a tela e sem a de gravar áudio, quando o dono abre o CtrlMesh no celular, então vê as duas como faltantes, cada uma com um atalho; e, quando a central conecta, então ela também mostra que as duas faltam no celular.
- **CA-083 → RN-073** [Teste] — Verifica se a liberação permanente funciona no celular do dono. Dado o celular do dono com a liberação permanente de captura feita pelo Mac e, em seguida, a depuração USB desligada e o cabo removido, quando o emulador se conecta, então a imagem aparece sem nenhum pedido "Permitir"; e, depois de reiniciar o celular, de forçar o fechamento do CtrlMesh nele e de bloquear e desbloquear o celular, então em cada caso o espelhamento volta sem "Permitir". Se algum desses passos pedir "Permitir", o celular do dono está na situação da RN-074 (CA-084).
- **CA-084 → RN-074** [Teste] — Dado o celular sem a liberação permanente (no teste: liberação desfeita pelo Mac), quando o emulador se conecta pela primeira vez, então o celular pede "Permitir" e a central mostra "toque em Permitir no celular"; depois do toque, quando o emulador sai da rede por mais de 60 segundos e volta, então o espelhamento recomeça sem novo "Permitir" e o aviso de captura do Android continua aparecendo no celular nesse intervalo; e, depois de reiniciar o celular ou forçar o fechamento do CtrlMesh nele, então o "Permitir" é pedido de novo; e, num celular Android 15 ou superior, depois de bloquear e desbloquear, então o "Permitir" também é pedido de novo.
- **CA-085 → RN-075** [Teste] — Dado o celular com a liberação permanente feita, quando o CtrlMesh é desinstalado e instalado de novo e o emulador se conecta, então o celular e a central mostram "captura sem liberação permanente — refaça a liberação pelo Mac para não precisar tocar em Permitir" e vale o comportamento do CA-084; e, depois de refazer a liberação, então o aviso some.
- **CA-086 → RN-091** [Teste] — Dado o celular recém-instalado, quando o dono faz o passo de liberação pelo Mac, então ao final o CtrlMesh no celular mostra como concedidas a liberação permanente e as autorizações que o Android aceitou por esse caminho, e lista com atalho só as que ainda faltam.
- **CA-087 → RN-019, RN-074** [Teste] — Dado o celular sem a liberação permanente e desbloqueado, com a autorização de aparecer sobre outros apps, quando a central se conecta, então o pedido de captura aparece sozinho; dado o celular sem essa autorização, então aparece a notificação "toque para espelhar na central", e o pedido só surge depois do toque nela; e, dado o celular bloqueado, então o pedido só aparece depois do desbloqueio.

### Imagem e orientação
- **CA-025 → RN-024, RN-027** [Teste] — Dado um espelhamento em andamento, quando o dono olha a central, então a imagem do celular aparece em tela cheia paisagem, inteira, sem corte nem distorção; e, ao girar o emulador para retrato, então a interface da central continua em paisagem.
- **CA-026 → RN-025** [Teste] — Dado o celular em retrato com a autorização de girar a tela, quando o espelhamento começa, então o celular passa para paisagem sem ação do dono e não há opção de desligar o giro.
- **CA-027 → RN-026, RN-046** [Teste] — Dado o celular girado para paisagem, quando o dono abre pela central um app travado em retrato, então o app continua em retrato e aparece inteiro com faixas dos lados; e, ao voltar a um app que aceita paisagem, então ele ocupa a imagem sem ação do dono.
- **CA-028 → RN-028** [Teste] — Dado o celular em retrato com giro automático desligado, girado para paisagem pelo CtrlMesh, quando o espelhamento entra em "reconectando", então continua em paisagem; quando termina por "encerrar" ou "conexão perdida", então volta a retrato com o giro automático desligado; e, se o CtrlMesh foi fechado no celular, então a orientação original volta quando o CtrlMesh executar de novo.
- **CA-029 → RN-029** [Teste] — Dado o celular sem a autorização de girar a tela, quando o espelhamento começa, então o celular continua em retrato, a imagem aparece inteira com faixas e a central mostra que a autorização falta.
- **CA-030 → RN-030** [Teste] — ~~Dado um espelhamento em andamento, quando o dono verifica a resolução do celular, então ela é a mesma de antes do espelhamento e nenhuma tela extra foi criada.~~ (substituído por CA-094)
- **CA-094 → RN-092** [Teste] — Dado um espelhamento em andamento em qualquer modo, quando o dono verifica a resolução da tela física do celular, então ela é a mesma de antes; e, no plano B, nenhuma tela extra foi criada; no modo tela virtual, existe só a tela virtual da central, que some ao encerrar.
- **CA-031 → RN-031** [Teste] — ~~Dado o celular com brilho de 70% e bloqueio automático em 30 segundos, quando o espelhamento dura 2 minutos sem toque local, então a tela do celular continua ligada e desbloqueada com o brilho no mínimo; e, ao encerrar, o brilho volta a 70% e o bloqueio automático a 30 segundos.~~ (substituído por CA-111)
- **CA-111 → RN-111** [Teste] — Dado o plano B (Shizuku desligado), o celular com brilho de 70% e bloqueio automático em 30 segundos, quando o espelhamento dura 2 minutos sem toque local, então a tela do celular continua ligada e desbloqueada com o brilho em 70%; e, ao encerrar, o bloqueio automático volta a 30 segundos e o brilho continua em 70%.
- **CA-113 → RN-112, RN-098** [Teste] — Dado o modo tela virtual, o celular com brilho de 40% e bloqueio automático em 30 segundos, quando o espelhamento dura 2 minutos, então o brilho continua em 40% e a tela física se bloqueia sozinha após 30 segundos sem toque, sem interromper o espelhamento.
- **CA-032 → RN-032** [Teste] — ~~Dado um espelhamento em andamento, quando o dono aperta o botão de bloqueio do celular, então a central mostra "celular bloqueado — desbloqueie o celular" e nenhum toque na central desbloqueia o celular; e, depois do desbloqueio, a imagem volta sozinha ou após o toque em "Permitir", se o sistema pedir.~~ (substituído por CA-088)
- **CA-033 → RN-033** [Teste] — Dado um espelhamento em andamento, quando o dono abre no celular um app que bloqueia captura, então a central mostra área escura com "conteúdo protegido pelo app" e Voltar e Início continuam funcionando.
- **CA-034 → RN-034** [Teste] — Dado um espelhamento em andamento, quando o celular recebe uma ligação, então a tela da ligação aparece na central e tocar em atender ou recusar na central atende ou recusa no celular, sem encerrar o espelhamento.
- **CA-088 → RN-086** [Teste] — Dado um espelhamento em andamento com a liberação permanente valendo, quando o dono aperta o botão de bloqueio do celular, então a central mostra "desbloqueie o celular" e nenhum toque na central desbloqueia o celular; e, quando o dono desbloqueia o celular, então a imagem volta sozinha, sem "Permitir".

### Controle
- **CA-035 → RN-035** [Teste] — Dado um espelhamento com a autorização de acessibilidade, quando o dono toca no ícone do Spotify na imagem, então o Spotify abre no celular; quando toca em "play", então a música começa a tocar no celular; e toque longo, arrastar, deslizar e pinça na central produzem o mesmo efeito no celular.
- **CA-036 → RN-036** [Teste] — Dado um app em retrato com faixas dos lados, quando o dono toca nas faixas, então nada acontece no celular.
- **CA-037 → RN-037** [Teste] — Dado um espelhamento em andamento, quando o dono toca em Voltar, Início e Apps recentes na barra, então o celular executa a ação correspondente.
- **CA-038 → RN-038** [Teste] — Dado um campo de texto aberto pela central, quando o teclado do celular aparece na imagem e o dono toca nas teclas, então o texto é digitado no celular, e a central não abre teclado próprio.
- **CA-039 → RN-039** [Teste] — Dado um espelhamento em andamento, quando o dono toca diretamente na tela do celular, então os toques locais funcionam e aparecem na central.

### Som
- **CA-040 → RN-040, RN-041** [Carro] — ~~Dado o celular conectado ao Bluetooth do carro, quando o dono abre o Spotify pela central e toca uma música, então a música sai no som do carro, e o volume do celular continua o mesmo de antes do espelhamento.~~ (substituído por CA-058)
- **CA-041 → RN-040** [Teste] — ~~Dado o celular sem Bluetooth conectado, quando o dono toca uma música pela central, então a música sai no alto-falante do celular e nenhum som sai na central.~~ (substituído por CA-056)
- **CA-056 → RN-059, RN-060** [Teste] — Dado o celular com volume de mídia em 60% e a autorização de gravar áudio, quando o dono abre o Spotify pela central e toca uma música, então a música sai na central (no teste, nos alto-falantes do Mac) e não sai no celular; e, quando o dono encerra o espelhamento, então a música volta a sair no celular com o volume de mídia em 60%.
- **CA-057 → RN-060** [Teste] — Dado o celular com volumes de toque e alarme em 80% e volume de mídia em 60%, quando o espelhamento começa, então só o volume de mídia é silenciado; e, se o CtrlMesh for fechado no celular durante o espelhamento, então o volume de mídia volta a 60% quando o CtrlMesh executar de novo no celular.
- **CA-058 → RN-059, RN-068** [Carro] — Dado a central com o rádio tocando e o celular conectado ao Bluetooth do carro, quando o espelhamento começa e o dono toca uma música pelo Spotify na central, então a música sai no som do carro com o CtrlMesh na frente, sem abrir o app "Música Bluetooth", e o rádio para.
- **CA-059 → RN-061** [Teste] — Dado o celular sem a autorização de gravar áudio, quando o espelhamento começa, então a imagem e o controle funcionam, nenhum som sai na central, o volume do celular não muda e a central mostra "sem som: autorização de áudio não concedida no celular".
- **CA-060 → RN-062, RN-063** [Teste] — Dado um espelhamento com música tocando, quando o celular recebe uma ligação, então o toque e o áudio da ligação saem no celular e não na central, e a central mostra "ligação em andamento — o som está no celular"; e, quando um alarme toca no celular, então ele sai no celular e não na central.
- **CA-061 → RN-062, RN-063** [Teste] — Dado um espelhamento em andamento, quando o dono toca, pela central, o som de um app que bloqueia a captura, então nada sai na central nem no celular silenciado; e, quando o dono toca em "alguns sons não chegam à central", então vê a lista: ligações, alarmes, toques, notificações, voz de navegação e apps que bloqueiam a captura.
- **CA-062 → RN-064** [Teste] — Dado uma música tocando na central, quando o dono toca "−" ou "+", então o som na central baixa ou sobe um nível; quando toca "mudo" e depois de novo, então o som silencia e volta no nível anterior; quando usa o volume do sistema da central (no teste, os botões de volume do emulador), então o som também muda; e em nenhum caso o volume do celular muda.
- **CA-063 → RN-065** [Carro] — ~~Dado um espelhamento com música tocando, quando o veículo entra em movimento, então a imagem é ocultada, mas a música continua na central e "−", "+" e "mudo" funcionam.~~ (substituído por CA-089)
- **CA-064 → RN-066** [Teste] — Dado uma música tocando na central, quando o Wi-Fi do emulador é desligado, então a central para de tocar e o celular continua sem som; quando o Wi-Fi volta em até 60 segundos, então a música volta a tocar na central do ponto em que está no celular, sem repetir o trecho do intervalo; e, se passarem 60 segundos, então o volume de mídia do celular volta ao anterior.
- **CA-065 → RN-067** [Teste] — Dado uma música tocando, quando a banda da rede é limitada a 1 Mbps, então a qualidade da imagem cai e a música continua sem cortes perceptíveis.
- **CA-066 → RN-069, RN-058** [Teste] — ~~Dado o celular tocando música e o pedido de captura na tela, quando a central mostra "aguardando confirmação no celular", então a música continua saindo no celular e nada sai na central; quando o dono toca "Permitir", então a música passa para a central, inclusive enquanto a central ainda espera o toque em "veículo parado".~~ (substituído por CA-090)
- **CA-067 → RNF-012, RN-059** [Teste] — Dado um vídeo de teste com sinal visual e sonoro simultâneos tocando no celular, quando ele é exibido na central, então o som não fica mais de 45 ms adiantado nem mais de 125 ms atrasado em relação à imagem.
- **CA-090 → RN-088** [Teste] — Dado o celular sem a liberação permanente, tocando música, com o pedido de captura na tela, quando a central mostra "aguardando confirmação no celular", então a música continua saindo no celular e nada sai na central; e, quando o dono toca "Permitir", então a música passa para a central e o celular é silenciado.
- **CA-093 → RN-090** [Teste, requer celular em que silenciar a mídia zera o som enviado] — Dado um celular em que silenciar o volume de mídia também zera o som enviado, quando o espelhamento começa com música tocando, então o volume de mídia do celular não é alterado, a música sai na central e no celular, e a central mostra "este celular não permite silenciar sem cortar o som da central". No celular do dono, o CA-056 mostra qual das duas situações vale.

### Interface da central
- **CA-042 → RN-042, RN-043** [Teste] — ~~Dado um espelhamento com o celular em paisagem, quando o dono olha a central, então a barra com Voltar, Início, Apps recentes, Encerrar e o estado fica à esquerda e não cobre a imagem; e, com um app em retrato, então a barra ocupa a faixa esquerda.~~ (substituído por CA-069)
- **CA-043 → RN-044** [Teste] — Dado a central em espelhamento, quando os controles da barra são medidos, então nenhum tem área de toque menor que 76 dp.
- **CA-044 → RN-045** [Teste] — Dado qualquer estado do espelhamento, quando o dono quer encerrar na central, então chega a "Encerrar" com no máximo uma interação.
- **CA-069 → RN-070, RN-043** [Teste] — ~~Dado um espelhamento com o celular em paisagem, quando o dono olha a central, então a barra com Voltar, Início, Apps recentes, "−", "+", "mudo", Encerrar e o estado fica à esquerda e não cobre a imagem; e, com um app em retrato, então a barra ocupa a faixa esquerda.~~ (substituído por CA-112)
- **CA-112 → RN-109, RN-110** [Teste] — Dado um espelhamento em qualquer modo, quando o dono olha a central, então a barra fica embaixo, horizontal, com fundo semitransparente, com Voltar, Início, Apps recentes, "−", "+", "Mudo", "Ajustes" e "Encerrar" nessa ordem e o estado da conexão; a imagem fica inteira acima da barra, e a barra de navegação de um app (ex.: abas na parte de baixo do Spotify) fica visível e tocável; e, quando o dono toca "Ajustes", então as configurações do CtrlMesh abrem e o espelhamento continua.

### Indicador e encerramento
- **CA-045 → RN-047** [Teste] — Dado um espelhamento em andamento, inclusive em "reconectando", quando o dono olha o celular em qualquer tela, então vê o indicador com o nome da central e o botão de encerrar, sem opção de ocultá-lo.
- **CA-046 → RN-048, RN-051** [Teste] — ~~Dado um espelhamento em andamento, quando o dono encerra no celular, então a imagem some da central e os toques deixam de ter efeito, depois o indicador some do celular, e a central mostra "encerrado pelo celular"; e, encerrando pela central, o celular mostra "encerrado pela central".~~ (substituído por CA-070)
- **CA-047 → RN-049** [Teste] — Dado um espelhamento encerrado pelo dono, quando os dois continuam na mesma rede, então o espelhamento não recomeça sozinho; quando o dono toca "espelhar" na central, então recomeça; e, quando a central sai da rede e volta, então recomeça sozinho.
- **CA-048 → RN-050, RN-051** [Teste] — ~~Dado um espelhamento em andamento, quando o CtrlMesh é fechado no celular, então a central mostra "app fechado ou aparelho desligado" sem passar por "reconectando".~~ (substituído por CA-091)
- **CA-070 → RN-071, RN-051** [Teste] — ~~Dado um espelhamento com música tocando, quando o dono encerra no celular, então a imagem e o som somem da central e os toques deixam de ter efeito, depois o indicador some do celular, o volume de mídia do celular volta ao anterior, e a central mostra "encerrado pelo celular"; e, encerrando pela central, o celular mostra "encerrado pela central".~~ (substituído por CA-092)
- **CA-091 → RN-050, RN-089** [Teste] — Dado um espelhamento em andamento, quando o CtrlMesh é fechado no celular, então a central mostra "app fechado ou aparelho desligado" sem passar por "reconectando".
- **CA-092 → RN-071, RN-089** [Teste] — Dado um espelhamento com música tocando, quando o dono encerra no celular, então a imagem e o som somem da central e os toques deixam de ter efeito, depois o indicador some do celular, o volume de mídia do celular volta ao anterior, e a central mostra "encerrado pelo celular"; e, encerrando pela central, o celular mostra "encerrado pela central"; e em nenhum caso aparece o motivo "pareamento desfeito".

### Queda e reconexão
- **CA-049 → RN-050, RN-052, RN-053** [Teste] — Dado um espelhamento em andamento, quando o Wi-Fi da central é desligado, então os dois lados mostram "reconectando", a central mostra a última imagem marcada como desatualizada, o celular mantém o indicador, e toques feitos na central nesse intervalo não são executados no celular depois.
- **CA-050 → RN-054** [Teste] — Dado um espelhamento em "reconectando" em celular Android 14 ou superior, quando a conexão volta em até 60 segundos, então o espelhamento continua sem nenhuma ação e sem novo pedido de captura no celular.
- **CA-051 → RN-055** [Teste] — Dado um espelhamento em "reconectando", quando passam 60 segundos sem conexão, então os dois lados mostram "conexão perdida"; e, quando os dois voltam a se encontrar na rede, então o espelhamento recomeça sozinho, com pedido de captura se o sistema exigir.
- **CA-052 → RN-056** [Teste] — Dado um espelhamento em andamento, quando a banda da rede é limitada, então a qualidade da imagem diminui e a resposta aos toques continua dentro do RNF-001.
- **CA-053 → RN-050, RN-052** [Carro] — Dado um espelhamento em andamento, quando o dono desliga o roteador do celular e o liga de novo em até 60 segundos, então o espelhamento continua sozinho.

### Modo tela virtual e plano B
Todos verificáveis com o emulador como central e o celular com o Shizuku ativo e o CtrlMesh autorizado nele, salvo quando o "Dado" disser outra coisa.
- **CA-095 → RN-093, RN-105** [Teste] — Dado o Shizuku ativo, quando o emulador se conecta, então o espelhamento usa a tela virtual; dado o Shizuku desligado, então usa a tela física, com o aviso "modo tela do celular — ative a tela horizontal no celular"; e, se o Shizuku for ligado durante um espelhamento no plano B, então o modo não muda até encerrar, e a próxima conexão usa a tela virtual sozinha.
- **CA-096 → RN-094** [Teste] — Dado o emulador com área de imagem de 1280×720 ou maior, quando o espelhamento começa, então a tela virtual é horizontal, com o lado maior de 1280 px, e aparece inteira na central, sem faixas; e, dado um emulador com área menor, então a tela virtual tem o tamanho dessa área.
- **CA-097 → RN-095** [Teste] — Dado o espelhamento no modo tela virtual, quando ele começa, então a central mostra o início do CtrlMesh com os apps do celular que têm ícone na tela inicial, com ícone e nome, em ordem alfabética.
- **CA-098 → RN-096** [Teste] — Dado o início do CtrlMesh, quando o dono toca no Spotify, então o Spotify abre na tela virtual em layout horizontal; quando toca no WhatsApp, então ele também aparece horizontal ou, se recusar, inteiro com faixas; e a tela inicial do Realme nunca aparece na central; e, dado o Spotify aberto na tela física, quando o dono o abre pela central, então ele passa para a tela virtual.
- **CA-099 → RN-097** [Teste] — Dado um app aberto na tela virtual, quando o dono toca Voltar, então volta dentro do app; quando toca Início, então a central mostra o início do CtrlMesh; quando há dois apps abertos e o dono toca Apps recentes, então alterna entre eles; e em nenhum caso a tela física do celular muda.
- **CA-100 → RN-098** [Teste] — Dado o Spotify na tela virtual, quando o dono abre outro app na tela física e depois bloqueia o celular, então a central continua mostrando o Spotify e respondendo aos toques; a tela física não gira, o brilho e o tempo de bloqueio do celular não mudam, e toques na central não aparecem na tela física.
- **CA-101 → RN-099** [Teste] — Dado o Shizuku ativo e a liberação pelo Mac desfeita, quando o emulador se conecta, então imagem e som aparecem sem nenhum pedido "Permitir" no celular.
- **CA-102 → RN-100** [Teste] — Dado um campo de texto num app da tela virtual, quando o dono toca nele pela central, então o teclado do celular aparece na tela virtual e o texto digitado pela central entra no campo.
- **CA-103 → RN-101** [Teste] — Dado o espelhamento no modo tela virtual, quando o celular recebe uma ligação, então a tela da ligação aparece na tela física, a central mostra "ligação em andamento — atenda no celular" e o espelhamento continua.
- **CA-104 → RN-102** [Teste] — Dado o celular sem o Shizuku, quando o dono abre o CtrlMesh, então vê o passo a passo e o estado "Shizuku desligado"; depois de autorizar o CtrlMesh no Shizuku e reiniciar o celular, então a autorização continua dada (o estado só pede religar o Shizuku); e, dado o Shizuku ativo sem a autorização, então o estado é "CtrlMesh sem autorização no Shizuku".
- **CA-105 → RN-103** [Teste] — Dado o Shizuku ativo, quando o dono liga o roteador do celular, então o Shizuku continua ativo e o espelhamento usa a tela virtual; e, depois de reiniciar o celular sem religar o Shizuku, então o CtrlMesh não o liga sozinho e o espelhamento usa o plano B.
- **CA-106 → RN-104** [Teste] — Dado o Shizuku desligado e o celular conectado a um Wi-Fi, quando o dono olha as notificações, então vê "toque para ativar a tela horizontal", e tocar nela abre o Shizuku; e, depois de o Shizuku ficar ativo, então a notificação some.
- **CA-107 → RN-106** [Teste] — Dado o celular com volume de mídia em 60% e o Spotify tocando na tela virtual, quando o espelhamento está em andamento, então a música sai na central (nos alto-falantes do Mac) e não sai no celular, sem nenhum pedido, e o volume de mídia do celular continua em 60%; e, ao encerrar, então a música volta a sair no celular.
- **CA-108 → RN-107** [Teste] — Dado o Spotify aberto na tela virtual, quando o Wi-Fi do emulador cai e volta em até 60 segundos, então a central volta a mostrar o Spotify como estava; e, quando o espelhamento é encerrado, então a tela virtual some e o Spotify continua aberto no celular.
- **CA-109 → RN-108, RN-047** [Teste] — Dado o espelhamento no modo tela virtual, quando o dono olha o celular, então o indicador de espelhamento informa "tela horizontal na central" e tem o botão de encerrar.
- **CA-110 → RNF-017** [Teste] — Dado o celular do dono (Realme RMX3963, Android 16) com o Shizuku ativo, quando o espelhamento começa, então a tela virtual de 1280×720 é criada, o Spotify abre nela em layout horizontal e o áudio chega à central sem pedido, repetindo a verificação registrada no RNF-017.

### Veículo em movimento
- **CA-054 → RN-057** [Carro] — ~~Dado um espelhamento com música tocando, quando o veículo entra em movimento, então a imagem é ocultada com a mensagem, os toques na imagem não têm efeito, a música continua e "Encerrar" funciona; e, quando o veículo para, então imagem e toques voltam sem ação.~~ (substituído por CA-089)
- **CA-055 → RN-058** [Teste] — ~~Dado uma central que não informa movimento (o emulador), quando os dois se encontram na rede, então a imagem só aparece depois do toque em "veículo parado"; e, numa reconexão dentro da janela, então o toque não é pedido de novo.~~ (substituído por CA-089)
- **CA-089 → RN-087** [Teste e Carro] — No teste: dado o emulador, quando os dois se encontram na rede, então a imagem aparece sem pedido de "veículo parado". No carro: dado um espelhamento com música tocando, quando o veículo entra em movimento, então a imagem continua visível, os toques continuam funcionando, a música continua e "−", "+" e "mudo" funcionam, sem nenhuma mensagem de restrição.

---

## 7. Requisitos não funcionais

| ID | Requisito |
|---|---|
| **RNF-001 Latência** | Na rede do celular, o tempo entre um toque na central e a imagem atualizada na central é ≤ 150 ms em 95% dos casos. (meta inicial) Na v1.7, o dono pediu mais velocidade na navegação e na abertura de apps; a meta foi mantida, porque o problema observado é não cumpri-la, não a meta em si. A prioridade de atraso sobre nitidez já está no RNF-016. |
| **RNF-002 Fluidez e qualidade** | ~~A imagem é atualizada a pelo menos 30 quadros por segundo, com resolução igual à menor entre a da área de imagem na central e a do celular; com rede degradada, a qualidade cai antes da resposta aos toques e antes do som (RN-056, RN-067). (meta inicial)~~ (substituído por RNF-016) |
| **RNF-016 Fluidez e qualidade** | A imagem é atualizada a pelo menos 30 quadros por segundo e é enviada com o lado maior de no máximo 1280 px, ou do tamanho da área de imagem na central, se esta for menor; o texto do celular continua legível na central. Baixo atraso e fluidez têm prioridade sobre nitidez: para cumprir o RNF-001 e o RNF-003, o CtrlMesh pode reduzir a nitidez da imagem. Com rede degradada, a qualidade cai antes da resposta aos toques e antes do som (RN-056, RN-067). Substitui o RNF-002. (meta inicial) |
| **RNF-003 Início** | Da central entrar na rede do celular (ou do toque em "Permitir", quando exigido) até a primeira imagem na central: ≤ 5 segundos. O celular gira para paisagem em até 2 segundos após a primeira imagem e é restaurado em até 2 segundos após o encerramento. (meta inicial) |
| **RNF-004 Encerramento** | Após "encerrar", imagem, som e controle cessam em até 1 segundo. |
| **RNF-005 Detecção de queda** | A queda é detectada e sinalizada nos dois lados em até 5 segundos. |
| **RNF-006 Segurança** | Toda comunicação entre celular e central (imagem, som e toques) continua cifrada, com uma chave nova combinada a cada conexão, sem ação do dono; a cifragem não depende de pareamento. **O que muda sem pareamento (P-04):** não há como um aparelho provar ao outro quem é, então a cifragem protege contra quem só escuta a rede, mas não impede que um aparelho com o CtrlMesh no modo multimídia, conectado ao roteador do celular, espelhe e controle o celular (RN-076); a barreira é a senha do roteador do celular e a regra de só aceitar centrais no próprio roteador (RN-077). Nada sai da rede local, nada é enviado a servidor e nenhuma imagem, som ou toque é gravado em nenhum dos aparelhos. |
| **RNF-007 Versões** | Celular: Android 10 ou superior. Central: Android 8 ou superior. Referência de teste: celular físico do dono e emulador de tablet Android em paisagem. (provisório — o dono pode ajustar) |
| **RNF-008 Bateria** | Em escuta, sem espelhamento, o CtrlMesh consome no máximo 2% da bateria do celular por dia. (meta inicial) |
| **RNF-009 Idioma** | Interface em português do Brasil. |
| **RNF-010 Ambiente de teste** | A central é um emulador Android no Mac; o celular físico roda o CtrlMesh. Restrições reais desse ambiente, que valem como requisito: (1) o Mac precisa estar conectado ao roteador do celular (RN-077; ver P-07), e é também o Mac que faz a liberação permanente de captura com o celular no cabo (RNF-015); (2) o emulador fica atrás de uma rede virtual própria: ele alcança o celular, mas o celular não alcança o emulador e não o encontra por anúncio na rede, então a conexão parte sempre da central e o endereço do celular é informado nas configurações da central (RN-014); (3) "a central entrar na rede do celular" é simulado ligando o Wi-Fi do emulador; (4) dois emuladores no mesmo Mac fazem o papel de duas centrais (CA-073); (5) o som enviado à central sai nos alto-falantes do Mac, e os botões de volume do emulador fazem o papel do volume da central; (6) parada do rádio e do app "Música Bluetooth" da central, central em modo de espera e detecção real da rede do celular só são verificáveis no carro (CAs marcados [Carro]); (7) para os CAs do modo tela virtual, o Shizuku é ligado no celular com ele conectado a um Wi-Fi, antes de ligar o roteador do celular, e continua ativo depois (RN-103); para os CAs do plano B, o Shizuku é desligado. |
| **RNF-012 Sincronia de som e imagem** | Na central, o som do celular não fica mais de 45 ms adiantado nem mais de 125 ms atrasado em relação à imagem correspondente. (meta inicial) |
| **RNF-013 Latência do som** | Na rede do celular, o tempo entre o som produzido no celular e sua reprodução na central é ≤ 200 ms em 95% dos casos, sem cortes perceptíveis; tocar "play" na central e ouvir o som na central leva no máximo o RNF-001 somado a esse tempo. (meta inicial) |
| **RNF-014 Qualidade do som** | Som estéreo, com qualidade de música sem perda perceptível; com 1 Mbps disponível, o som continua sem cortes, ainda que a imagem piore (RN-067). (meta inicial) |
| **RNF-011 Distribuição** | O CtrlMesh é instalado fora da loja (arquivo de instalação) nos dois aparelhos; por isso o celular pode exigir liberar "configurações restritas" para a acessibilidade (RN-022). |
| **RNF-015 Liberação permanente de captura** | Restrição real da P-01: a liberação é feita pelo Mac com as ferramentas de depuração do Android (adb), com o celular no cabo e a depuração USB ligada, concedendo ao CtrlMesh a autorização de projeção de mídia do sistema (`appops set <pacote> PROJECT_MEDIA allow`). Ela vale por aparelho e por instalação: desinstalar o CtrlMesh a desfaz (RN-075). Não há documentação oficial do Google garantindo que o Android 14 ou superior respeite essa autorização; por isso existe a alternativa da RN-074 e o CA-083 verifica o celular do dono. |
| **RNF-017 Premissa verificada do modo tela virtual** | Verificado no celular do dono (Realme RMX3963, Android 16, sem root), com a mesma permissão que o Shizuku dá, obtida pelo scrcpy: uma tela virtual de 1280×720 foi criada, o Spotify abriu nela em layout horizontal e o áudio foi capturado sem nenhum pedido. O modo tela virtual depende do app Shizuku (de terceiros), ligado pela depuração sem fio, que desliga a cada reinício do celular; por isso existe o plano B (RN-105). O CA-110 repete essa verificação com o CtrlMesh. |

---

## 8. Perguntas em aberto

### Rodada atual (v1.6)

**Não há perguntas em aberto.**

### Decididas pelo dono na v1.6

**P-09 — No modo tela virtual, que som vai para a central?** (decidida na v1.6)
- *Por que importa:* com a permissão do Shizuku, o CtrlMesh pode desviar para a central o som do celular sem "Permitir" e sem zerar o volume, mas o desvio é do som do celular como um todo, não só dos apps da tela virtual. Assim, a voz de navegação e as notificações tocadas pelo celular também iriam para a central (no plano B, isso não é possível). Se o dono usar o celular fisicamente (ex.: um vídeo na tela física), esse som também sairia na central.
- *Opções:* (a) todo som que o Android permite desviar vai só para a central durante o espelhamento, e volta ao celular ao encerrar; ligações continuam no celular; (b) só o som de mídia vai para a central, e o restante (notificações, navegação) continua no celular, mesmo que toque dobrado quando um app usar os dois; (c) o som é copiado para a central e também continua saindo no celular.
- *Decisão:* (a), pelo dono, na v1.6: todo som que o Android permite desviar sai só na central durante o espelhamento e volta ao celular ao encerrar; as ligações continuam no celular. Aplicada em RN-106.

Pontos que dependem de verificação, e não de decisão: se o Android do celular do dono respeita a liberação permanente de captura (CA-083) e se silenciar a mídia mantém o som enviado (CA-056), ambos só no plano B, com regra alternativa (RN-074 e RN-090); e o modo tela virtual com o CtrlMesh no celular do dono (CA-110).

### Decididas pelo dono na v1.3

**P-07 — O celular aceita centrais só no próprio roteador?** (decidida na v1.3)
- *Por que importa:* sem pareamento, a única barreira é a rede. Se o celular aceitar centrais em qualquer Wi-Fi em que ele esteja (ex.: Wi-Fi de casa, do trabalho, de um hotel), qualquer pessoa nessa rede com o CtrlMesh no modo multimídia poderia ver e controlar o celular. Por outro lado, no ambiente de teste isso obriga o Mac a se conectar ao roteador do celular, em vez de usar o Wi-Fi de casa.
- *Opções:* (a) só no roteador do próprio celular, inclusive no teste (Mac conectado ao roteador do celular); (b) em qualquer Wi-Fi, aceitando o risco acima; (c) só no roteador do celular, com um modo de teste ligado manualmente no celular que aceita o Wi-Fi atual até o celular reiniciar.
- *Decisão:* (a), pelo dono, na v1.3: só no roteador do próprio celular, inclusive no teste (Mac conectado ao roteador do celular). Aplicada em RN-077 e RNF-010.

**P-08 — O passo no Mac libera também as outras autorizações do celular?** (decidida na v1.3)
- *Por que importa:* com o celular já no cabo e a depuração USB ligada, o mesmo passo pode conceder acessibilidade, girar a tela, aparecer sobre outros apps, gravar áudio e a exclusão da economia de bateria, evitando que o dono procure cada ajuste no celular (inclusive "configurações restritas"). Alguns fabricantes podem não aceitar alguma delas por esse caminho.
- *Opções:* (a) sim, o passo no Mac concede tudo o que o Android aceitar e o celular orienta só o que faltar; (b) não, o Mac só faz a liberação de captura e o resto é feito no celular.
- *Decisão:* (a), pelo dono, na v1.3: o passo no Mac concede todas as autorizações que o Android aceitar e o celular orienta só o que faltar. Aplicada em RN-091 e F-01.

### Decididas pelo dono na v1.2

**P-01 — Como lidar com a confirmação de captura do Android a cada espelhamento?** (decidida na v1.2)
- *Por que importa:* no Android 14 ou superior, o sistema exige um toque em "Permitir" no celular a cada novo espelhamento, e a autorização não pode ser reaproveitada; a partir do Android 15, bloquear o celular também encerra a captura. Isso torna impossível, num celular comum, o espelhamento começar com **zero** toques toda vez que você entra no carro.
- *Opções:* (a) aceitar um toque em "Permitir" no celular a cada entrada no carro (o pedido aparece sozinho no celular); (b) (a) + manter a captura viva enquanto o roteador do celular estiver ligado, mesmo sem a central conectada, para não pedir de novo após paradas curtas, ao custo de bateria e do aviso de captura do Android sempre aceso; (c) investigar um modo sem toque usando a depuração sem fio do Android (como fazem ferramentas de desenvolvedor), que exige ativar opções de desenvolvedor e pode não funcionar com o celular sendo o roteador; (d) só celulares com root.
- *Sugestão padrão da v1.0:* ~~(a). Aplicada em RN-018 a RN-020.~~ (substituída pela decisão da v1.2)
- *Decisão:* pelo dono, na v1.2: liberação permanente de captura feita uma única vez pelo Mac, com o celular no cabo e a depuração USB ligada, como parte da instalação; depois disso, o espelhamento começa sem "Permitir", inclusive após reiniciar o celular. Se o Android do celular não respeitar essa liberação, vale a alternativa: um "Permitir" que deixa a captura viva em segundo plano enquanto o app estiver vivo, pedido de novo após reinício, morte do app ou bloqueio no Android 15+. Aplicada em RN-073 a RN-075, RN-081, RN-086, RNF-015, F-01 e F-02.

**P-03 — A tela do celular pode ficar ligada e desbloqueada durante o espelhamento?** (decidida na v1.2)
- *Por que importa:* com a tela apagada o Android não gera imagem, e a partir do Android 15 bloquear encerra a captura e exige novo "Permitir". Então o celular no bolso ou no suporte precisa ficar com a tela ligada e desbloqueada, o que gasta bateria e permite toques acidentais no bolso.
- *Opções:* (a) tela ligada e desbloqueada com brilho no mínimo durante o espelhamento, restaurando tudo ao encerrar; se o dono bloquear, a central pede para desbloquear; (b) como (a), mas com brilho normal; (c) tentar seguir com a tela apagada, aceitando que não funcionará na maioria dos celulares.
- *Decisão da v1.2:* ~~(a), pelo dono: tela ligada com brilho no mínimo e tudo restaurado ao encerrar; se o dono bloquear, a central mostra "desbloqueie o celular" e o espelhamento volta sozinho ao desbloquear, sem "Permitir" (por causa da P-01). Aplicada em RN-031 e RN-086.~~ (substituída pela decisão da v1.7)
- *Decisão:* (b), pelo dono, na v1.7: no plano B, tela ligada com o brilho que o dono usa (o CtrlMesh nunca escurece a tela), tempo de bloqueio restaurado ao encerrar; o bloqueio pelo dono continua como na RN-086; no modo tela virtual, nada muda na tela física. Aplicada em RN-111, RN-112 e RN-086.

**P-04 — Como é o pareamento da primeira vez?** (decidida na v1.2)
- *Por que importa:* qualquer aparelho que entrar no roteador do celular (um passageiro, por exemplo) não pode ver a tela. Sem pareamento, bastaria ter o CtrlMesh instalado e estar na rede.
- *Opções:* (a) uma vez: "parear" no celular, escolha na central e confirmação de um código de 6 dígitos no celular; (b) confiar em qualquer central que entre na rede do celular, já que a rede tem senha; (c) (a) + confirmar no celular a cada espelhamento.
- *Sugestão padrão da v1.0:* ~~(a). Aplicada em RN-005 a RN-009.~~ (substituída pela decisão da v1.2)
- *Decisão:* (b), pelo dono, na v1.2: confiar na rede, sem código nem pareamento; qualquer central com o CtrlMesh no modo multimídia conectada ao roteador do celular espelha. A cifragem continua, sem identificação dos aparelhos (RNF-006). Aplicada em RN-076 a RN-081, RN-089 e RNF-006.

**P-05 — E quando a central não informa se o carro está andando?** (decidida na v1.2)
- *Por que importa:* na v0.5 você decidiu ocultar a imagem com o veículo em movimento; muitas centrais genéricas não informam velocidade, e o emulador também não. Pedir um toque de "veículo parado" vai contra o "começar sozinho".
- *Opções:* (a) um toque em "veículo parado" na central a cada novo espelhamento; (b) sem restrição quando a central não informar movimento, por conta do dono; (c) usar o GPS do celular para saber se o carro está andando, ocultando a imagem acima de uma velocidade baixa.
- *Sugestão padrão da v1.0:* ~~(a). Aplicada em RN-058.~~ (substituída pela decisão da v1.2)
- *Decisão:* pelo dono, na v1.2: sem restrição nenhuma; imagem e toques sempre liberados, também com o veículo em movimento, mesmo quando a central informa movimento. Isso também substitui a decisão D-07 da v0.5. Aplicada em RN-087.

**P-06 — Como silenciar o celular sem perder sons importantes?** (decidida na v1.2)
- *Por que importa:* o Android não deixa um app silenciar só o som que ele está enviando; o caminho disponível é silenciar o volume de mídia do celular inteiro. Com isso, a voz de navegação (que o Android não deixa enviar) e o som de apps que bloqueiam a captura, quando usam o volume de mídia, ficam sem som em lugar nenhum. Além disso, não há confirmação oficial de que o som enviado continua chegando à central com o volume de mídia do celular zerado; isso precisa ser confirmado no celular do dono (CA-056) antes de a regra valer.
- *Opções:* (a) silenciar o volume de mídia do celular durante o espelhamento, aceitando que a voz de navegação e apps protegidos fiquem mudos; ligações, alarmes, toques e notificações continuam no celular; (b) não silenciar o celular, aceitando o som dobrado (celular e central); (c) (a), e, nos celulares em que o volume zerado também zerar o som enviado, cair para (b) com o aviso "este celular não permite silenciar sem cortar o som da central".
- *Decisão:* (c), pelo dono, na v1.2: silenciar a mídia do celular; se nesse celular isso também zerar o som enviado, não silenciar e avisar na central. Aplicada em RN-060, RN-062 e RN-090.

### Decididas pelo dono na v1.0
- **D-01** — Comunicação só pela rede Wi-Fi do roteador do celular, sem internet nem servidor (RN-010, RN-011).
- **D-02** — Espelhamento começa sozinho quando a central entra na rede do celular; os dois aparelhos ficam sempre em escuta (RN-012, RN-082).
- **D-03** — A tela do celular aparece na central em horizontal (RN-024 a RN-029).
- **D-04** — Tudo o que é tocado na central acontece no celular (RN-035 a RN-038).
- **D-05** — Ambiente de teste: emulador Android no Mac como central e celular físico na mesma rede (RNF-010).

**P-02 — Para onde vai o som do celular?** (decidida na v1.1)
- *Por que importava:* o som poderia seguir pelo Bluetooth do carro, sem o CtrlMesh mexer nele, ou ir pela Wi-Fi para a central.
- *Opções:* (a) caminho normal do celular (Bluetooth do carro ou alto-falante); (b) som de mídia pela Wi-Fi para a central; (c) (a) no MVP e (b) depois.
- *Decisão da v1.0 (sugestão padrão):* ~~(c). Aplicada em RN-040, RN-041.~~ (substituída pela decisão da v1.1)
- *Decisão:* (b), pelo dono, na v1.1: o som vai pela Wi-Fi junto com a imagem e toca no CtrlMesh da central, porque na central o som do Bluetooth só sai com o app "Música Bluetooth" aberto, e o espelhamento fica na frente. O celular fica silenciado durante o espelhamento para não tocar dobrado. Aplicada em RN-059 a RN-069, RN-070, RN-071 e RNF-012 a RNF-014.

### Decisões da v0.5 mantidas
- **D-06** — Central alvo: Android comum com apps instaláveis; Android Auto e Android Automotive OS fora (era P-04 da v0.5; RN-003).
- **D-07** — ~~Com o veículo em movimento, imagem oculta e controle bloqueado (era P-05 da v0.5; RN-057).~~ (substituída pela decisão da P-05 na v1.2: sem restrição, RN-087)

# CtrlMesh — Rastreabilidade

- **Versão:** 3.3, derivada de `docs/regras-de-negocio.md` v1.7 (barra embaixo e brilho nunca alterado: CA-031 → CA-111, CA-069 → CA-112, novo CA-113; v1.6: modo tela virtual, CA-030 → CA-094). Contrato: `docs/contrato.md` v3.3.
- **Motivo da reescrita:** mudança de direção do produto na v1.0 das regras (acesso remoto com servidor NestJS + Flutter → espelhamento do celular na central multimídia pelo roteador do celular, sem servidor). Nenhuma linha da versão anterior (v0.5, `backend/test`, `frontend/test`) vale.
- **Escopo:** só CAs ativos (85). CAs riscados/substituídos não têm testes; a cobertura está no substituto.
- **Tabela automática gerada** a partir dos nomes dos testes (todo teste começa com o ID do CA — nos instrumentados, em forma de identificador, ex.: `ca072_...`, porque o DEX com minSdk 26 não aceita espaços em nomes de método; os testes de cifra e de tamanho do vídeo começam com `RNF-006`/`RNF-016`, que não têm CA) e das chamadas `verificar CA-xxx` de `e2e/run.sh`.

## Resumo

- CAs ativos: 85. Com teste automático: 76. Só manuais: 9 (CA-013, CA-034, CA-038, CA-053, CA-058, CA-067, CA-079, CA-102, CA-106).
- Linhas automáticas: 341 (JUnit 139, instrumentado 65, e2e 137).
- Cenários e2e opcionais, assistidos (o roteiro pede uma ação ao operador): `duas-centrais` (CA-073), `reiniciar` (CA-077), `casa` (CA-075), `reinstalar` (CA-074, CA-085), `tela-virtual` (CA-094 a CA-110; exige o Shizuku ativo e autorizado, senão é pulado com aviso). Os cenários padrão são do plano B e pedem o Shizuku desligado.

## Testes automáticos

| RN | CA | Arquivo | Teste | Automático/manual |
|---|---|---|---|---|
| RN-003 | CA-003 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca003_emAndroidComumOAppAbreEFuncionaComoCentral | automático (instrumentado) |
| RN-010 | CA-010 | `e2e/run.sh` | espelha com dados móveis desligados | automático (e2e) |
| RN-011 | CA-011 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca081_ca011_ca017_primeiraVezNaCentralMostraCadaAjusteQueFaltaComAtalhoESomeQuandoConcedido | automático (instrumentado) |
| RN-014 | CA-016 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca016_enderecoInformadoNasConfiguracoesValeNestaENasProximasConexoes | automático (instrumentado) |
| RN-014 | CA-016 | `app/src/test/java/com/ctrlmesh/central/DescobertaEVolumeTest.kt` | CA-016 endereco informado e tentado em toda conexao, antes do roteador | automático (JUnit) |
| RN-015 | CA-017 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca081_ca011_ca017_primeiraVezNaCentralMostraCadaAjusteQueFaltaComAtalhoESomeQuandoConcedido | automático (instrumentado) |
| RN-015 | CA-017 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca017_celularSujeitoAEconomiaDeBateriaMostraOAvisoEEleSomeAoCorrigir | automático (instrumentado) |
| RN-015 | CA-017 | `app/src/test/java/com/ctrlmesh/protocolo/AvisosTest.kt` | CA-017 aviso de economia de bateria | automático (JUnit) |
| RN-020 | CA-021 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca021_naoConfirmadoMostraTentarDeNovoNaoRepeteSozinhoETentarDeNovoPedeOutraVez | automático (instrumentado) |
| RN-020 | CA-021 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-021 sem toque em Permitir por 30 s a tentativa e cancelada como nao confirmada | automático (JUnit) |
| RN-020 | CA-021 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-021 nao confirmado nao tenta de novo sozinho e tentar de novo reconecta | automático (JUnit) |
| RN-020 | CA-021 | `e2e/run.sh` | 30 s sem toque: não confirmado no celular | automático (e2e) |
| RN-020 | CA-021 | `e2e/run.sh` | nenhum novo pedido sozinho | automático (e2e) |
| RN-020 | CA-021 | `e2e/run.sh` | tentar de novo mostra o pedido outra vez | automático (e2e) |
| RN-021 | CA-022 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca059_ca093_ca085_ca022_ca060_ca033_ca023_ca024_ca029_avisosDoCelularAparecemNaCentral | automático (instrumentado) |
| RN-021 | CA-022 | `app/src/test/java/com/ctrlmesh/protocolo/AvisosTest.kt` | CA-022 aviso de compartilhamento de um app so | automático (JUnit) |
| RN-022 | CA-023 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca068_ca023_celularMostraGirarATelaSobreporEAcessibilidadeFaltandoComAtalho | automático (instrumentado) |
| RN-022 | CA-023 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca059_ca093_ca085_ca022_ca060_ca033_ca023_ca024_ca029_avisosDoCelularAparecemNaCentral | automático (instrumentado) |
| RN-022 | CA-023 | `e2e/run.sh` | celular mostra acessibilidade faltando | automático (e2e) |
| RN-022 | CA-023 | `e2e/run.sh` | central mostra o motivo do controle | automático (e2e) |
| RN-022 | CA-023 | `e2e/run.sh` | toques na central não têm efeito | automático (e2e) |
| RN-023 | CA-024 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca059_ca093_ca085_ca022_ca060_ca033_ca023_ca024_ca029_avisosDoCelularAparecemNaCentral | automático (instrumentado) |
| RN-024, RN-027 | CA-025 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca025_imagemEmTelaCheiaPaisagemInteiraESemDistorcaoMesmoComOAparelhoEmRetrato | automático (instrumentado) |
| RN-024, RN-027 | CA-025 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca025_imagemChegaEOPrimeiroQuadroEDesenhadoSemDistorcaoDeCorNemTelaPreta | automático (instrumentado) |
| RN-024, RN-027 | CA-025 | `app/src/test/java/com/ctrlmesh/central/LayoutCentralTest.kt` | CA-025 em telas de central comuns a imagem fica inteira, sem corte nem distorcao | automático (JUnit) |
| RN-024, RN-027 | CA-025 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-025 video chega a central com dimensoes, codec e quadros intactos | automático (JUnit) |
| RN-024, RN-027 | CA-025 | `e2e/run.sh` | vídeo com lado maior ≤ 1280 px (RNF-016) | automático (e2e) |
| RN-025 | CA-026 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-026 com autorizacao de girar o celular vai para paisagem ao comecar | automático (JUnit) |
| RN-025 | CA-026 | `e2e/run.sh` | celular girou para paisagem | automático (e2e) |
| RN-026, RN-046 | CA-027 | `app/src/test/java/com/ctrlmesh/MapeadorDeToqueTest.kt` | CA-027 com app em retrato o toque usa a imagem em retrato | automático (JUnit) |
| RN-026, RN-046 | CA-027 | `app/src/test/java/com/ctrlmesh/central/LayoutCentralTest.kt` | CA-027 app em retrato aparece inteiro com faixas dos lados, acima da barra | automático (JUnit) |
| RN-026, RN-046 | CA-027 | `app/src/test/java/com/ctrlmesh/central/LayoutCentralTest.kt` | CA-027 ao voltar a um app em paisagem a imagem volta a ocupar a area | automático (JUnit) |
| RN-028 | CA-028 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-028 ao encerrar volta a retrato com giro automatico desligado | automático (JUnit) |
| RN-028 | CA-028 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-028 giro automatico ligado antes volta ligado | automático (JUnit) |
| RN-028 | CA-028 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-028 app fechado durante o espelhamento - orientacao volta quando o CtrlMesh executa de novo | automático (JUnit) |
| RN-028 | CA-028 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-028 durante reconectando o giro e os demais ajustes sao mantidos | automático (JUnit) |
| RN-028 | CA-028 | `e2e/run.sh` | giro mantido durante reconectando | automático (e2e) |
| RN-028 | CA-028 | `e2e/run.sh` | volta a retrato com giro automático desligado | automático (e2e) |
| RN-028 | CA-028 | `e2e/run.sh` | orientação, brilho e bloqueio restaurados quando o CtrlMesh executa de novo | automático (e2e) |
| RN-029 | CA-029 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca059_ca093_ca085_ca022_ca060_ca033_ca023_ca024_ca029_avisosDoCelularAparecemNaCentral | automático (instrumentado) |
| RN-029 | CA-029 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-029 sem autorizacao de girar a orientacao do celular nao muda | automático (JUnit) |
| RN-029 | CA-029 | `app/src/test/java/com/ctrlmesh/central/LayoutCentralTest.kt` | CA-029 sem girar a tela a imagem em retrato aparece inteira com faixas | automático (JUnit) |
| RN-029 | CA-029 | `e2e/run.sh` | celular continua em retrato | automático (e2e) |
| RN-033 | CA-033 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca059_ca093_ca085_ca022_ca060_ca033_ca023_ca024_ca029_avisosDoCelularAparecemNaCentral | automático (instrumentado) |
| RN-033 | CA-033 | `app/src/test/java/com/ctrlmesh/protocolo/AvisosTest.kt` | CA-033 aviso de conteudo protegido | automático (JUnit) |
| RN-035 | CA-035 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca035_toqueNoCentroDaImagemChegaAoCelularNoCentroDoQuadro | automático (instrumentado) |
| RN-035 | CA-035 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca035_deslizarEPincaChegamAoCelularComMovimentoEDoisDedos | automático (instrumentado) |
| RN-035 | CA-035 | `app/src/test/java/com/ctrlmesh/MapeadorDeToqueTest.kt` | CA-035 toque na imagem vai para o ponto correspondente do celular | automático (JUnit) |
| RN-035 | CA-035 | `app/src/test/java/com/ctrlmesh/MapeadorDeToqueTest.kt` | CA-035 no celular o ponto normalizado vira pixel da tela atual | automático (JUnit) |
| RN-035 | CA-035 | `app/src/test/java/com/ctrlmesh/MapeadorDeToqueTest.kt` | CA-035 toque feito sobre imagem de orientacao antiga nao cai em lugar errado no celular | automático (JUnit) |
| RN-035 | CA-035 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-035 toques de dois dedos chegam ao celular com ponteiro, acao e posicao | automático (JUnit) |
| RN-035 | CA-035 | `e2e/run.sh` | toque no ícone da imagem abre o app no celular | automático (e2e) |
| RN-036 | CA-036 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca036_toquesNasFaixasNaoChegamAoCelular | automático (instrumentado) |
| RN-036 | CA-036 | `app/src/test/java/com/ctrlmesh/MapeadorDeToqueTest.kt` | CA-036 toques nas faixas e na barra de baixo nao vao para o celular | automático (JUnit) |
| RN-037 | CA-037 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca037_voltarInicioERecentesDaBarraChegamAoCelular | automático (instrumentado) |
| RN-037 | CA-037 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-037 Voltar, Inicio e Recentes chegam ao celular como teclas distintas | automático (JUnit) |
| RN-037 | CA-037 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-037 bytes das teclas no fio seguem o contrato | automático (JUnit) |
| RN-037 | CA-037 | `e2e/run.sh` | Voltar executado no celular | automático (e2e) |
| RN-037 | CA-037 | `e2e/run.sh` | Início executado no celular | automático (e2e) |
| RN-037 | CA-037 | `e2e/run.sh` | Apps recentes executado no celular | automático (e2e) |
| RN-039 | CA-039 | `e2e/run.sh` | uso direto do celular aparece na central | automático (e2e) |
| RN-044 | CA-043 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca043_controlesDaBarraTemNoMinimo76Dp | automático (instrumentado) |
| RN-045 | CA-044 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca049_ca044_quedaMostraReconectandoComImagemDesatualizadaEncerrarAMaoEToquesDescartados | automático (instrumentado) |
| RN-045 | CA-044 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca092_ca044_encerrarNaCentralComUmToqueAvisaOCelularComOMotivo | automático (instrumentado) |
| RN-045 | CA-044 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-044 encerrar funciona em qualquer estado do espelhamento | automático (JUnit) |
| RN-047 | CA-045 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-045 indicador continua durante reconectando | automático (JUnit) |
| RN-047 | CA-045 | `e2e/run.sh` | indicador com Encerrar no celular | automático (e2e) |
| RN-049 | CA-047 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca047_encerradoPeloDonoNaoRecomecaSozinhoEEspelharRecomeca | automático (instrumentado) |
| RN-049 | CA-047 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-047 encerrado pelo dono nao recomeca sozinho na mesma rede | automático (JUnit) |
| RN-049 | CA-047 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-047 espelhar na central recomeca | automático (JUnit) |
| RN-049 | CA-047 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-047 central sai da rede e volta e o espelhamento recomeca sozinho | automático (JUnit) |
| RN-049 | CA-047 | `e2e/run.sh` | não recomeça sozinho na mesma rede | automático (e2e) |
| RN-049 | CA-047 | `e2e/run.sh` | espelhar na central recomeça | automático (e2e) |
| RN-049 | CA-047 | `e2e/run.sh` | central sai da rede e volta: recomeça sozinho | automático (e2e) |
| RN-050, RN-052, RN-053 | CA-049 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca049_ca044_quedaMostraReconectandoComImagemDesatualizadaEncerrarAMaoEToquesDescartados | automático (instrumentado) |
| RN-050, RN-052, RN-053 | CA-049 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-049 queda leva a reconectando com imagem desatualizada e toques descartados | automático (JUnit) |
| RN-050, RN-052, RN-053 | CA-049 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-049 Wi-Fi da central desligado tambem leva a reconectando | automático (JUnit) |
| RN-050, RN-052, RN-053 | CA-049 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-049 batimento atravessa nos dois sentidos | automático (JUnit) |
| RN-050, RN-052, RN-053 | CA-049 | `app/src/test/java/com/ctrlmesh/protocolo/RedeDegradadaTest.kt` | CA-049 queda e detectada em ate 5 segundos sem dados e nao antes de 1 segundo | automático (JUnit) |
| RN-050, RN-052, RN-053 | CA-049 | `e2e/run.sh` | central mostra reconectando em até 5 s | automático (e2e) |
| RN-050, RN-052, RN-053 | CA-049 | `e2e/run.sh` | celular mostra reconectando | automático (e2e) |
| RN-050, RN-052, RN-053 | CA-049 | `e2e/run.sh` | celular mantém o indicador | automático (e2e) |
| RN-050, RN-052, RN-053 | CA-049 | `e2e/run.sh` | toques do intervalo não foram executados depois | automático (e2e) |
| RN-054 | CA-050 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-050 aplicar de novo numa retomada nao perde os valores originais | automático (JUnit) |
| RN-054 | CA-050 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-050 central volta em ate 60 s e o espelhamento continua sem Permitir | automático (JUnit) |
| RN-054 | CA-050 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-050 reconexao dentro de 60 segundos continua o espelhamento sem acao | automático (JUnit) |
| RN-054 | CA-050 | `app/src/test/java/com/ctrlmesh/protocolo/RedeDegradadaTest.kt` | CA-050 qualquer quadro recebido renova o prazo | automático (JUnit) |
| RN-054 | CA-050 | `e2e/run.sh` | espelhamento continua sozinho | automático (e2e) |
| RN-054 | CA-050 | `e2e/run.sh` | sem novo pedido de captura | automático (e2e) |
| RN-055 | CA-051 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-051 60 s sem a central encerra por conexao perdida e desfaz ajustes | automático (JUnit) |
| RN-055 | CA-051 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-051 60 segundos sem conexao encerra por conexao perdida e recomeca sozinho | automático (JUnit) |
| RN-055 | CA-051 | `e2e/run.sh` | central mostra conexão perdida | automático (e2e) |
| RN-055 | CA-051 | `e2e/run.sh` | celular mostra conexão perdida | automático (e2e) |
| RN-055 | CA-051 | `e2e/run.sh` | recomeça sozinho ao voltar à rede | automático (e2e) |
| RN-056 | CA-052 | `app/src/test/java/com/ctrlmesh/protocolo/RedeDegradadaTest.kt` | CA-052 quanto menor a banda, menor o video, sem passar da banda | automático (JUnit) |
| RN-056 | CA-052 | `app/src/test/java/com/ctrlmesh/protocolo/RedeDegradadaTest.kt` | CA-052 mensagens de controle saem antes de som e video | automático (JUnit) |
| RN-059, RN-060 | CA-056 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-056 com autorizacao de audio o som vai para a central e o celular e silenciado | automático (JUnit) |
| RN-059, RN-060 | CA-056 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-056 som chega a central com configuracao e quadros intactos | automático (JUnit) |
| RN-059, RN-060 | CA-056 | `e2e/run.sh` | mídia do celular silenciada (ou aviso RN-090) | automático (e2e) |
| RN-060 | CA-057 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-057 so o volume de midia e silenciado e volta a 60 por cento | automático (JUnit) |
| RN-060 | CA-057 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-057 app fechado durante o espelhamento - volume volta quando o CtrlMesh executa de novo | automático (JUnit) |
| RN-060 | CA-057 | `e2e/run.sh` | volume de toque intacto | automático (e2e) |
| RN-060 | CA-057 | `e2e/run.sh` | volume de alarme intacto | automático (e2e) |
| RN-060 | CA-057 | `e2e/run.sh` | volume de mídia restaurado quando o CtrlMesh executa de novo | automático (e2e) |
| RN-061 | CA-059 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca059_ca093_ca085_ca022_ca060_ca033_ca023_ca024_ca029_avisosDoCelularAparecemNaCentral | automático (instrumentado) |
| RN-061 | CA-059 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-059 sem autorizacao de audio o volume do celular nao muda | automático (JUnit) |
| RN-061 | CA-059 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-059 sem autorizacao de audio, sem som, sem silenciar e com aviso | automático (JUnit) |
| RN-061 | CA-059 | `e2e/run.sh` | central mostra sem som | automático (e2e) |
| RN-061 | CA-059 | `e2e/run.sh` | volume do celular não muda | automático (e2e) |
| RN-062, RN-063 | CA-060 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca059_ca093_ca085_ca022_ca060_ca033_ca023_ca024_ca029_avisosDoCelularAparecemNaCentral | automático (instrumentado) |
| RN-062, RN-063 | CA-060 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca060_avisoDeLigacaoSomeQuandoALigacaoTerminaSemEncerrar | automático (instrumentado) |
| RN-062, RN-063 | CA-060 | `app/src/test/java/com/ctrlmesh/protocolo/AvisosTest.kt` | CA-060 aviso de ligacao em andamento | automático (JUnit) |
| RN-062, RN-063 | CA-061 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca061_algunsSonsNaoChegamACentralListaOsSonsNaoEnviaveis | automático (instrumentado) |
| RN-064 | CA-062 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca062_menosMaisEMudoNaoMandamNadaAoCelular | automático (instrumentado) |
| RN-064 | CA-062 | `app/src/test/java/com/ctrlmesh/central/DescobertaEVolumeTest.kt` | CA-062 menos e mais mudam um nivel, dentro dos limites | automático (JUnit) |
| RN-064 | CA-062 | `app/src/test/java/com/ctrlmesh/central/DescobertaEVolumeTest.kt` | CA-062 mudo silencia e volta no nivel anterior | automático (JUnit) |
| RN-064 | CA-062 | `e2e/run.sh` | − baixa um nível na central | automático (e2e) |
| RN-064 | CA-062 | `e2e/run.sh` | mudo silencia | automático (e2e) |
| RN-064 | CA-062 | `e2e/run.sh` | mudo de novo volta | automático (e2e) |
| RN-064 | CA-062 | `e2e/run.sh` | volume do sistema da central muda | automático (e2e) |
| RN-064 | CA-062 | `e2e/run.sh` | volume do celular não mudou | automático (e2e) |
| RN-066 | CA-064 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-064 durante reconectando o celular nao envia som e continua silenciado | automático (JUnit) |
| RN-066 | CA-064 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-064 durante reconectando a central nao toca som | automático (JUnit) |
| RN-066 | CA-064 | `e2e/run.sh` | celular continua silenciado | automático (e2e) |
| RN-066 | CA-064 | `e2e/run.sh` | volume de mídia do celular volta ao anterior | automático (e2e) |
| RN-067 | CA-065 | `app/src/test/java/com/ctrlmesh/protocolo/RedeDegradadaTest.kt` | CA-065 com 1 Mbps o som mantem a qualidade e so o video cai | automático (JUnit) |
| RN-067 | CA-065 | `app/src/test/java/com/ctrlmesh/protocolo/RedeDegradadaTest.kt` | CA-065 fila cheia descarta video e nunca som | automático (JUnit) |
| RN-067 | CA-065 | `app/src/test/java/com/ctrlmesh/protocolo/RedeDegradadaTest.kt` | CA-065 depois de descartar video, o proximo video entregue e quadro-chave | automático (JUnit) |
| RN-072 | CA-068 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca068_ca023_celularMostraGirarATelaSobreporEAcessibilidadeFaltandoComAtalho | automático (instrumentado) |
| RN-072 | CA-068 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca068_centralMostraQueFaltamGirarATelaEGravarAudioNoCelular | automático (instrumentado) |
| RN-072 | CA-068 | `app/src/test/java/com/ctrlmesh/protocolo/AvisosTest.kt` | CA-068 bits dos avisos no fio seguem o contrato | automático (JUnit) |
| RN-072 | CA-068 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-068 central recebe as autorizacoes que faltam no celular | automático (JUnit) |
| RN-072 | CA-068 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-068 todos os avisos atravessam a rede e situacao vazia tambem | automático (JUnit) |
| RN-072 | CA-068 | `e2e/run.sh` | celular mostra girar a tela faltando | automático (e2e) |
| RN-072 | CA-068 | `e2e/run.sh` | celular mostra gravar áudio faltando | automático (e2e) |
| RN-072 | CA-068 | `e2e/run.sh` | central mostra girar a tela faltando | automático (e2e) |
| RN-001 | CA-071 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca074_ca071_celularNaoTemPareamentoNemOpcaoDeEspelharACentral | automático (instrumentado) |
| RN-001 | CA-071 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca074_ca071_centralNaoTemPareamentoNemEspelhamentoInverso | automático (instrumentado) |
| RN-001 | CA-071 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-071 nao existe sentido inverso - celular rejeita imagem e som, central rejeita toques e teclas | automático (JUnit) |
| RN-001 | CA-071 | `app/src/test/java/com/ctrlmesh/protocolo/TelaVirtualProtocoloTest.kt` | CA-071 mensagens novas so valem no sentido do contrato | automático (JUnit) |
| RN-080 | CA-072 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca072_primeiraAberturaPerguntaOPapelNaoPerguntaDeNovoERedefinirPerguntaOutraVez | automático (instrumentado) |
| RN-078 | CA-073 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca073_segundaCentralMostraCelularEmUsoEEspelhaSozinhaQuandoLiberada | automático (instrumentado) |
| RN-078 | CA-073 | `app/src/test/java/com/ctrlmesh/celular/FilaERedeTest.kt` | CA-073 primeira central espelha e a segunda fica em espera | automático (JUnit) |
| RN-078 | CA-073 | `app/src/test/java/com/ctrlmesh/celular/FilaERedeTest.kt` | CA-073 quando a primeira sai a segunda espelha sozinha | automático (JUnit) |
| RN-078 | CA-073 | `app/src/test/java/com/ctrlmesh/celular/FilaERedeTest.kt` | CA-073 central em espera que sai nao muda a ativa | automático (JUnit) |
| RN-078 | CA-073 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-073 segunda central mostra em uso e espelha sozinha quando liberada | automático (JUnit) |
| RN-078 | CA-073 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-073 central recebe cada estado do celular, inclusive em uso por outra central | automático (JUnit) |
| RN-078 | CA-073 | `e2e/run.sh` | segunda central mostra celular em uso | automático (e2e) |
| RN-078 | CA-073 | `e2e/run.sh` | segunda central espelha sozinha quando a primeira encerra | automático (e2e) |
| RN-076 | CA-074 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca074_ca071_celularNaoTemPareamentoNemOpcaoDeEspelharACentral | automático (instrumentado) |
| RN-076 | CA-074 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca074_ca071_centralNaoTemPareamentoNemEspelhamentoInverso | automático (instrumentado) |
| RN-076 | CA-074 | `e2e/run.sh` | nenhuma opção de parear | automático (e2e) |
| RN-076 | CA-074 | `e2e/run.sh` | central nova espelha sem código nem confirmação | automático (e2e) |
| RN-076 | CA-074 | `e2e/run.sh` | sem pedido de captura no celular | automático (e2e) |
| RN-077 | CA-075 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca075_conexaoQueNaoVemDoRoteadorDoCelularEFechadaSemNenhumByte | automático (instrumentado) |
| RN-077 | CA-075 | `app/src/test/java/com/ctrlmesh/celular/FilaERedeTest.kt` | CA-075 central no roteador do proprio celular e aceita | automático (JUnit) |
| RN-077 | CA-075 | `app/src/test/java/com/ctrlmesh/celular/FilaERedeTest.kt` | CA-075 central em outra rede do celular, como o Wi-Fi de casa, e recusada | automático (JUnit) |
| RN-077 | CA-075 | `app/src/test/java/com/ctrlmesh/celular/FilaERedeTest.kt` | CA-075 loopback, endereco invalido e IPv6 sao recusados | automático (JUnit) |
| RN-077 | CA-075 | `e2e/run.sh` | celular recusa a central | automático (e2e) |
| RN-077 | CA-075 | `e2e/run.sh` | nenhuma imagem na central | automático (e2e) |
| RN-077 | CA-075 | `e2e/run.sh` | no roteador do celular o espelhamento começa | automático (e2e) |
| RN-081, RN-082 | CA-076 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-076 com liberacao permanente o espelhamento comeca sem Permitir | automático (JUnit) |
| RN-081, RN-082 | CA-076 | `e2e/run.sh` | nenhuma confirmação do CtrlMesh nem do Android | automático (e2e) |
| RN-081, RN-082 | CA-076 | `e2e/run.sh` | nenhuma tela do CtrlMesh na frente do celular | automático (e2e) |
| RN-081, RN-082 | CA-076 | `e2e/run.sh` | nenhum pedido de captura no celular | automático (e2e) |
| RN-012, RN-082 | CA-077 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca077_aoLigarOAparelhoSemAbrirOAppOCelularEntraEmEscutaComNotificacaoPermanente | automático (instrumentado) |
| RN-012, RN-082 | CA-077 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca077_aoLigarACentralSemAbrirOAppElaProcuraOCelularSozinha | automático (instrumentado) |
| RN-012, RN-082 | CA-077 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-077 celular comeca em escuta, sem indicador nem ajustes | automático (JUnit) |
| RN-012, RN-082 | CA-077 | `e2e/run.sh` | celular em escuta sem abrir o app | automático (e2e) |
| RN-012, RN-082 | CA-077 | `e2e/run.sh` | notificação permanente do CtrlMesh | automático (e2e) |
| RN-012, RN-082 | CA-077 | `e2e/run.sh` | espelhamento começa sem abrir o app | automático (e2e) |
| RN-082, RN-083 | CA-078 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca078_centralPassaDeAguardandoOCelularAEspelhandoSemNenhumToque | automático (instrumentado) |
| RN-082, RN-083 | CA-078 | `app/src/test/java/com/ctrlmesh/central/DescobertaEVolumeTest.kt` | CA-078 sem endereco informado a central procura o roteador da rede | automático (JUnit) |
| RN-082, RN-083 | CA-078 | `app/src/test/java/com/ctrlmesh/central/DescobertaEVolumeTest.kt` | CA-078 sem rede e sem endereco nao ha tentativa | automático (JUnit) |
| RN-082, RN-083 | CA-078 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-078 textos de estado da central | automático (JUnit) |
| RN-082, RN-083 | CA-078 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-078 central em escuta tenta conectar e espelha sem nenhum toque | automático (JUnit) |
| RN-082, RN-083 | CA-078 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-078 sem celular na rede a central volta a aguardar e continua tentando | automático (JUnit) |
| RN-082, RN-083 | CA-078 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-078 porta e versao fixas do contrato | automático (JUnit) |
| RN-082, RN-083 | CA-078 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-078 central se apresenta com nome e area de imagem | automático (JUnit) |
| RN-082, RN-083 | CA-078 | `e2e/run.sh` | central em escuta mostra aguardando o celular | automático (e2e) |
| RN-082, RN-083 | CA-078 | `e2e/run.sh` | central passa a conectando sozinha | automático (e2e) |
| RN-082, RN-083 | CA-078 | `e2e/run.sh` | primeiro quadro desenhado (imagem chegando) | automático (e2e) |
| RN-084 | CA-080 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca080_comSobreposicaoACentralVemParaAFrenteSozinhaQuandoOEspelhamentoComeca | automático (instrumentado) |
| RN-084 | CA-080 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca080_semSobreposicaoApareceANotificacaoToqueParaVerOCelularEElaTrazACentral | automático (instrumentado) |
| RN-085 | CA-081 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca081_ca011_ca017_primeiraVezNaCentralMostraCadaAjusteQueFaltaComAtalhoESomeQuandoConcedido | automático (instrumentado) |
| RN-079 | CA-082 | `app/src/test/java/com/ctrlmesh/central/DescobertaEVolumeTest.kt` | CA-082 com endereco informado a central espelha esse celular primeiro | automático (JUnit) |
| RN-073 | CA-083 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-083 com liberacao permanente, reinicio, fechamento e bloqueio nao pedem Permitir | automático (JUnit) |
| RN-073 | CA-083 | `e2e/run.sh` | depois de fechar o app, volta sem Permitir | automático (e2e) |
| RN-073 | CA-083 | `e2e/run.sh` | sem pedido de captura | automático (e2e) |
| RN-073 | CA-083 | `e2e/run.sh` | bloquear/desbloquear não pede Permitir | automático (e2e) |
| RN-074 | CA-084 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca084_semLiberacaoACentralMostraToqueEmPermitirNoCelular | automático (instrumentado) |
| RN-074 | CA-084 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-084 sem liberacao o primeiro espelhamento pede Permitir e a central e avisada | automático (JUnit) |
| RN-074 | CA-084 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-084 depois do Permitir, a central sair da rede por mais de 60 s e voltar nao pede de novo | automático (JUnit) |
| RN-074 | CA-084 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-084 depois de reiniciar o celular ou matar o app o Permitir e pedido de novo | automático (JUnit) |
| RN-074 | CA-084 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-084 no Android 15 ou superior bloquear e desbloquear pede Permitir de novo | automático (JUnit) |
| RN-074 | CA-084 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-084 no Android 14 bloquear e desbloquear nao pede Permitir de novo | automático (JUnit) |
| RN-074 | CA-084 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-084 sem liberacao permanente a central aguarda confirmacao no celular sem som nem toques | automático (JUnit) |
| RN-074 | CA-084 | `e2e/run.sh` | central mostra aguardando confirmação | automático (e2e) |
| RN-074 | CA-084 | `e2e/run.sh` | imagem depois do Permitir | automático (e2e) |
| RN-074 | CA-084 | `e2e/run.sh` | central fora da rede >60 s e de volta: sem novo Permitir | automático (e2e) |
| RN-074 | CA-084 | `e2e/run.sh` | sem pedido de captura | automático (e2e) |
| RN-074 | CA-084 | `e2e/run.sh` | app fechado: Permitir é pedido de novo | automático (e2e) |
| RN-074 | CA-084 | `e2e/run.sh` | Android 15+: bloquear e desbloquear pede Permitir de novo | automático (e2e) |
| RN-075 | CA-085 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca085_ca086_celularMostraSeALiberacaoPermanenteDeCapturaEstaValendo | automático (instrumentado) |
| RN-075 | CA-085 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca059_ca093_ca085_ca022_ca060_ca033_ca023_ca024_ca029_avisosDoCelularAparecemNaCentral | automático (instrumentado) |
| RN-075 | CA-085 | `app/src/test/java/com/ctrlmesh/protocolo/AvisosTest.kt` | CA-085 aviso de captura sem liberacao permanente | automático (JUnit) |
| RN-075 | CA-085 | `e2e/run.sh` | central mostra captura sem liberação permanente | automático (e2e) |
| RN-075 | CA-085 | `e2e/run.sh` | celular mostra captura sem liberação permanente | automático (e2e) |
| RN-075 | CA-085 | `e2e/run.sh` | celular mostra liberação faltando | automático (e2e) |
| RN-075 | CA-085 | `e2e/run.sh` | central mostra captura sem liberação permanente | automático (e2e) |
| RN-075 | CA-085 | `e2e/run.sh` | depois de refazer a liberação o aviso some | automático (e2e) |
| RN-091 | CA-086 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca085_ca086_celularMostraSeALiberacaoPermanenteDeCapturaEstaValendo | automático (instrumentado) |
| RN-091 | CA-086 | `e2e/run.sh` | $item concedida pela liberação do Mac | automático (e2e) |
| RN-019, RN-074 | CA-087 | `e2e/run.sh` | pedido de captura aparece sozinho (com sobreposição) | automático (e2e) |
| RN-019, RN-074 | CA-087 | `e2e/run.sh` | notificação toque para espelhar na central | automático (e2e) |
| RN-019, RN-074 | CA-087 | `e2e/run.sh` | pedido só aparece depois do toque na notificação | automático (e2e) |
| RN-019, RN-074 | CA-087 | `e2e/run.sh` | toque na notificação mostra o pedido | automático (e2e) |
| RN-086 | CA-088 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca088_celularBloqueadoMostraDesbloqueieENenhumToqueEEnviado | automático (instrumentado) |
| RN-086 | CA-088 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-088 bloqueado avisa a central para desbloquear e mantem indicador e ajustes | automático (JUnit) |
| RN-086 | CA-088 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-088 celular bloqueado mostra desbloqueie e volta sozinho ao desbloquear | automático (JUnit) |
| RN-086 | CA-088 | `e2e/run.sh` | estado segue espelhando | automático (e2e) |
| RN-086 | CA-088 | `e2e/run.sh` | central mostra desbloqueie o celular | automático (e2e) |
| RN-086 | CA-088 | `e2e/run.sh` | toques na central não desbloqueiam o celular | automático (e2e) |
| RN-086 | CA-088 | `e2e/run.sh` | imagem volta sozinha ao desbloquear | automático (e2e) |
| RN-087 | CA-089 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca089_aImagemApareceSemPedidoDeVeiculoParado | automático (instrumentado) |
| RN-087 | CA-089 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-089 nao existe pedido de veiculo parado entre conectar e espelhar | automático (JUnit) |
| RN-087 | CA-089 | `e2e/run.sh` | nenhum pedido de veículo parado | automático (e2e) |
| RN-088 | CA-090 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-090 enquanto espera Permitir nada de som e celular nao e silenciado | automático (JUnit) |
| RN-088 | CA-090 | `e2e/run.sh` | celular não é silenciado enquanto espera Permitir | automático (e2e) |
| RN-088 | CA-090 | `e2e/run.sh` | depois do Permitir o celular é silenciado | automático (e2e) |
| RN-050, RN-089 | CA-091 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca091_appFechadoNoCelularMostraOMotivoSemPassarPorReconectando | automático (instrumentado) |
| RN-050, RN-089 | CA-091 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-091 app da central fechado encerra sem reconectando | automático (JUnit) |
| RN-050, RN-089 | CA-091 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-091 app fechado no celular encerra sem passar por reconectando | automático (JUnit) |
| RN-050, RN-089 | CA-091 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-091 celular avisando app fechado tambem encerra sem reconectando | automático (JUnit) |
| RN-050, RN-089 | CA-091 | `e2e/run.sh` | central mostra app fechado ou aparelho desligado | automático (e2e) |
| RN-050, RN-089 | CA-091 | `e2e/run.sh` | sem passar por reconectando | automático (e2e) |
| RN-071, RN-089 | CA-092 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca092_encerradoPeloCelularImagemSomeToquesParamEMotivoAparece | automático (instrumentado) |
| RN-071, RN-089 | CA-092 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca092_ca044_encerrarNaCentralComUmToqueAvisaOCelularComOMotivo | automático (instrumentado) |
| RN-071, RN-089 | CA-092 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-092 restaurar sem espelhamento anterior nao mexe em nada | automático (JUnit) |
| RN-071, RN-089 | CA-092 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-092 encerrar no celular corta tudo, some o indicador e desfaz ajustes | automático (JUnit) |
| RN-071, RN-089 | CA-092 | `app/src/test/java/com/ctrlmesh/celular/SessaoCelularTest.kt` | CA-092 encerrar na central chega ao celular com o motivo | automático (JUnit) |
| RN-071, RN-089 | CA-092 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-092 encerrado pelo celular corta imagem, som e toques e mostra o motivo | automático (JUnit) |
| RN-071, RN-089 | CA-092 | `app/src/test/java/com/ctrlmesh/central/SessaoCentralTest.kt` | CA-092 encerrar na central leva o motivo encerrado pela central | automático (JUnit) |
| RN-071, RN-089 | CA-092 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-092 encerramento leva o motivo ao outro lado nos dois sentidos | automático (JUnit) |
| RN-071, RN-089 | CA-092 | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | CA-092 textos dos motivos de encerramento, sem pareamento | automático (JUnit) |
| RN-071, RN-089 | CA-092 | `e2e/run.sh` | central mostra encerrado pela central em até 1 s (+margem) | automático (e2e) |
| RN-071, RN-089 | CA-092 | `e2e/run.sh` | celular mostra encerrado pela central | automático (e2e) |
| RN-071, RN-089 | CA-092 | `e2e/run.sh` | indicador some do celular | automático (e2e) |
| RN-071, RN-089 | CA-092 | `e2e/run.sh` | celular restaura orientação, brilho, bloqueio e volume | automático (e2e) |
| RN-071, RN-089 | CA-092 | `e2e/run.sh` | central mostra encerrado pelo celular | automático (e2e) |
| RN-071, RN-089 | CA-092 | `e2e/run.sh` | nenhum motivo de pareamento | automático (e2e) |
| RN-090 | CA-093 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca059_ca093_ca085_ca022_ca060_ca033_ca023_ca024_ca029_avisosDoCelularAparecemNaCentral | automático (instrumentado) |
| RN-090 | CA-093 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-093 celular que zera o som enviado ao silenciar nao e silenciado e a central avisa | automático (JUnit) |
| RN-092 | CA-094 | `e2e/run.sh` | resolução e densidade do celular inalteradas | automático (e2e) |
| RN-092 | CA-094 | `e2e/run.sh` | plano B: nenhuma tela extra com apps | automático (e2e) |
| RN-092 | CA-094 | `e2e/run.sh` | tela física do celular sem mudar de resolução | automático (e2e) |
| RN-092 | CA-094 | `e2e/run.sh` | existe a tela virtual da central | automático (e2e) |
| RN-092 | CA-094 | `e2e/run.sh` | tela virtual some ao encerrar | automático (e2e) |
| RN-093, RN-105 | CA-095 | `app/src/androidTest/java/com/ctrlmesh/central/TelaVirtualInstrumentadoTest.kt` | ca095_planoBMostraOAvisoDeModoTelaDoCelularESemInicioDoCtrlMesh | automático (instrumentado) |
| RN-093, RN-105 | CA-095 | `app/src/androidTest/java/com/ctrlmesh/central/TelaVirtualInstrumentadoTest.kt` | ca095_celularSemModoTelaVirtualContinuaNoPlanoB | automático (instrumentado) |
| RN-093, RN-105 | CA-095 | `app/src/test/java/com/ctrlmesh/celular/ModoTelaVirtualTest.kt` | CA-095 tabela de decisao do modo | automático (JUnit) |
| RN-093, RN-105 | CA-095 | `app/src/test/java/com/ctrlmesh/celular/ModoTelaVirtualTest.kt` | CA-095 Shizuku ligado no meio do plano B so vale na proxima conexao | automático (JUnit) |
| RN-093, RN-105 | CA-095 | `app/src/test/java/com/ctrlmesh/protocolo/TelaVirtualProtocoloTest.kt` | CA-095 central recebe o modo do espelhamento | automático (JUnit) |
| RN-093, RN-105 | CA-095 | `app/src/test/java/com/ctrlmesh/protocolo/TelaVirtualProtocoloTest.kt` | CA-095 aviso de plano B e de ligacao no celular | automático (JUnit) |
| RN-093, RN-105 | CA-095 | `e2e/run.sh` | sem Shizuku ativo o modo é tela física | automático (e2e) |
| RN-093, RN-105 | CA-095 | `e2e/run.sh` | aviso de plano B na central | automático (e2e) |
| RN-093, RN-105 | CA-095 | `e2e/run.sh` | com Shizuku ativo o modo é tela virtual | automático (e2e) |
| RN-093, RN-105 | CA-095 | `e2e/run.sh` | sem aviso de plano B | automático (e2e) |
| RN-094 | CA-096 | `app/src/androidTest/java/com/ctrlmesh/central/TelaVirtualInstrumentadoTest.kt` | ca098_ca096_tocarNumAppEnviaAbrirAppEAImagemDaTelaVirtualOcupaAAreaSemFaixas | automático (instrumentado) |
| RN-094 | CA-096 | `app/src/test/java/com/ctrlmesh/central/InicioETelaVirtualTest.kt` | CA-096 CA-112 na tela virtual a imagem ocupa toda a area acima da barra, sem faixas | automático (JUnit) |
| RN-094 | CA-096 | `app/src/test/java/com/ctrlmesh/central/InicioETelaVirtualTest.kt` | CA-096 toque em qualquer ponto da imagem vai para a tela virtual, sem faixas | automático (JUnit) |
| RN-094 | CA-096 | `app/src/test/java/com/ctrlmesh/central/InicioETelaVirtualTest.kt` | CA-096 toque normalizado vira pixel da tela virtual | automático (JUnit) |
| RN-094 | CA-096 | `app/src/test/java/com/ctrlmesh/protocolo/TelaVirtualProtocoloTest.kt` | CA-096 central informa nova area da imagem | automático (JUnit) |
| RN-094 | CA-096 | `app/src/test/java/com/ctrlmesh/protocolo/TelaVirtualProtocoloTest.kt` | CA-096 tela virtual horizontal com lado maior de 1280 em area grande | automático (JUnit) |
| RN-094 | CA-096 | `app/src/test/java/com/ctrlmesh/protocolo/TelaVirtualProtocoloTest.kt` | CA-096 area menor que 1280 - tela virtual do tamanho da area | automático (JUnit) |
| RN-094 | CA-096 | `app/src/test/java/com/ctrlmesh/protocolo/TelaVirtualProtocoloTest.kt` | CA-096 densidade definida pelo tamanho da tela virtual | automático (JUnit) |
| RN-094 | CA-096 | `e2e/run.sh` | tela virtual horizontal com lado maior = min(1280, área da central) | automático (e2e) |
| RN-095 | CA-097 | `app/src/androidTest/java/com/ctrlmesh/central/TelaVirtualInstrumentadoTest.kt` | ca097_inicioDoCtrlMeshMostraOsAppsComIconeENomeEmOrdemAlfabetica | automático (instrumentado) |
| RN-095 | CA-097 | `app/src/test/java/com/ctrlmesh/central/InicioETelaVirtualTest.kt` | CA-097 inicio do CtrlMesh em ordem alfabetica, sem diferenciar maiusculas e acentos | automático (JUnit) |
| RN-095 | CA-097 | `app/src/test/java/com/ctrlmesh/protocolo/TelaVirtualProtocoloTest.kt` | CA-097 lista de apps chega a central com pacote, rotulo e icone | automático (JUnit) |
| RN-095 | CA-097 | `e2e/run.sh` | central mostra o início do CtrlMesh com apps | automático (e2e) |
| RN-096 | CA-098 | `app/src/androidTest/java/com/ctrlmesh/central/TelaVirtualInstrumentadoTest.kt` | ca098_ca096_tocarNumAppEnviaAbrirAppEAImagemDaTelaVirtualOcupaAAreaSemFaixas | automático (instrumentado) |
| RN-096 | CA-098 | `app/src/test/java/com/ctrlmesh/protocolo/TelaVirtualProtocoloTest.kt` | CA-098 abrir app chega ao celular com o pacote | automático (JUnit) |
| RN-096 | CA-098 | `e2e/run.sh` | tocar no app abre $pkg na tela virtual | automático (e2e) |
| RN-096 | CA-098 | `e2e/run.sh` | $pkg está na tela virtual, não na física | automático (e2e) |
| RN-097 | CA-099 | `app/src/androidTest/java/com/ctrlmesh/central/TelaVirtualInstrumentadoTest.kt` | ca099_inicioDaBarraVoltaAoInicioDoCtrlMeshEVoltarERecentesVaoAoCelular | automático (instrumentado) |
| RN-097 | CA-099 | `app/src/androidTest/java/com/ctrlmesh/central/TelaVirtualInstrumentadoTest.kt` | ca099_telaVirtualVaziaMostraOInicioDoCtrlMesh | automático (instrumentado) |
| RN-097 | CA-099 | `app/src/test/java/com/ctrlmesh/protocolo/TelaVirtualProtocoloTest.kt` | CA-099 tela virtual vazia avisa a central para mostrar o inicio | automático (JUnit) |
| RN-097 | CA-099 | `e2e/run.sh` | Voltar não mexe na tela física | automático (e2e) |
| RN-097 | CA-099 | `e2e/run.sh` | Início mostra o início do CtrlMesh | automático (e2e) |
| RN-097 | CA-099 | `e2e/run.sh` | Início não mexe na tela física | automático (e2e) |
| RN-098 | CA-100 | `app/src/test/java/com/ctrlmesh/celular/ModoTelaVirtualTest.kt` | CA-100 bloquear o celular nao interrompe o modo tela virtual e nada e ajustado na tela fisica | automático (JUnit) |
| RN-098 | CA-100 | `e2e/run.sh` | tela física continua com o mesmo app na frente | automático (e2e) |
| RN-098 | CA-100 | `e2e/run.sh` | tela física não girou | automático (e2e) |
| RN-098 | CA-100 | `e2e/run.sh` | brilho e bloqueio da tela física não mudaram | automático (e2e) |
| RN-098 | CA-100 | `e2e/run.sh` | celular bloqueado não interrompe o espelhamento | automático (e2e) |
| RN-099 | CA-101 | `app/src/test/java/com/ctrlmesh/celular/ModoTelaVirtualTest.kt` | CA-101 no modo tela virtual o espelhamento comeca sem Permitir e sem liberacao pelo Mac | automático (JUnit) |
| RN-099 | CA-101 | `e2e/run.sh` | espelha sem Permitir e sem liberação pelo Mac | automático (e2e) |
| RN-099 | CA-101 | `e2e/run.sh` | nenhum pedido de captura | automático (e2e) |
| RN-101 | CA-103 | `app/src/androidTest/java/com/ctrlmesh/central/TelaVirtualInstrumentadoTest.kt` | ca103_ligacaoNoModoTelaVirtualMostraAtendaNoCelularSemEncerrar | automático (instrumentado) |
| RN-101 | CA-103 | `app/src/test/java/com/ctrlmesh/protocolo/TelaVirtualProtocoloTest.kt` | CA-103 aviso de ligacao no modo tela virtual | automático (JUnit) |
| RN-102 | CA-104 | `app/src/androidTest/java/com/ctrlmesh/PapelEAjustesTest.kt` | ca104_celularSemShizukuMostraOPassoAPassoEOEstadoShizukuDesligado | automático (instrumentado) |
| RN-102 | CA-104 | `app/src/test/java/com/ctrlmesh/celular/ModoTelaVirtualTest.kt` | CA-104 estado do Shizuku com os textos do contrato | automático (JUnit) |
| RN-103 | CA-105 | `app/src/test/java/com/ctrlmesh/celular/ModoTelaVirtualTest.kt` | CA-105 depois de reiniciar sem religar o Shizuku o espelhamento usa o plano B | automático (JUnit) |
| RN-106 | CA-107 | `app/src/test/java/com/ctrlmesh/celular/ModoTelaVirtualTest.kt` | CA-107 no modo tela virtual o volume do celular nao e mexido | automático (JUnit) |
| RN-106 | CA-107 | `e2e/run.sh` | volume de mídia do celular não mudou | automático (e2e) |
| RN-107 | CA-108 | `app/src/test/java/com/ctrlmesh/celular/ModoTelaVirtualTest.kt` | CA-108 queda mantem a sessao virtual e a retomada em 60 s volta sem Permitir | automático (JUnit) |
| RN-107 | CA-108 | `e2e/run.sh` | queda: central em reconectando | automático (e2e) |
| RN-107 | CA-108 | `e2e/run.sh` | tela virtual mantida durante reconectando | automático (e2e) |
| RN-107 | CA-108 | `e2e/run.sh` | reconecta sozinho | automático (e2e) |
| RN-107 | CA-108 | `e2e/run.sh` | app continua na tela virtual como estava | automático (e2e) |
| RN-107 | CA-108 | `e2e/run.sh` | ao encerrar a tela virtual é fechada | automático (e2e) |
| RN-107 | CA-108 | `e2e/run.sh` | $pkg continua aberto no celular | automático (e2e) |
| RN-108, RN-047 | CA-109 | `app/src/test/java/com/ctrlmesh/celular/ModoTelaVirtualTest.kt` | CA-109 indicador aparece no modo tela virtual e some ao encerrar | automático (JUnit) |
| RN-108, RN-047 | CA-109 | `e2e/run.sh` | indicador informa tela horizontal na central | automático (e2e) |
| RNF-017 | CA-110 | `e2e/run.sh` | RMX3963: tela virtual criada no tamanho certo | automático (e2e) |
| RNF-017 | CA-110 | `e2e/run.sh` | RMX3963: Spotify abriu na tela virtual | automático (e2e) |
| RN-111 | CA-111 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-111 brilho do dono continua em 70 por cento e bloqueio volta a 30 s ao encerrar | automático (JUnit) |
| RN-111 | CA-111 | `app/src/test/java/com/ctrlmesh/celular/AjustesESomTest.kt` | CA-111 brilho automatico do dono nao e desligado | automático (JUnit) |
| RN-111 | CA-111 | `e2e/run.sh` | brilho do dono não muda (nunca escurece) | automático (e2e) |
| RN-111 | CA-111 | `e2e/run.sh` | tela continua ligada e desbloqueada | automático (e2e) |
| RN-111 | CA-111 | `e2e/run.sh` | brilho continua o do dono | automático (e2e) |
| RN-109, RN-110 | CA-112 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca112_barraEmbaixoHorizontalComOsControlesEmOrdemEImagemInteiraLogoAcima | automático (instrumentado) |
| RN-109, RN-110 | CA-112 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca112_comAppEmRetratoAImagemFicaInteiraAcimaDaBarraComFaixasDosLados | automático (instrumentado) |
| RN-109, RN-110 | CA-112 | `app/src/androidTest/java/com/ctrlmesh/central/CentralInstrumentadoTest.kt` | ca112_ajustesAbreAsConfiguracoesSemEncerrarOEspelhamento | automático (instrumentado) |
| RN-109, RN-110 | CA-112 | `app/src/test/java/com/ctrlmesh/central/InicioETelaVirtualTest.kt` | CA-096 CA-112 na tela virtual a imagem ocupa toda a area acima da barra, sem faixas | automático (JUnit) |
| RN-109, RN-110 | CA-112 | `app/src/test/java/com/ctrlmesh/central/LayoutCentralTest.kt` | CA-112 celular em paisagem - barra embaixo na largura toda e imagem inteira logo acima | automático (JUnit) |
| RN-112, RN-098 | CA-113 | `e2e/run.sh` | tela física se bloqueia sozinha após 30 s | automático (e2e) |
| RN-112, RN-098 | CA-113 | `e2e/run.sh` | espelhamento continua com a tela física apagada | automático (e2e) |
| RN-112, RN-098 | CA-113 | `e2e/run.sh` | brilho continua em 40% | automático (e2e) |
| RN-112, RN-098 | CA-113 | `e2e/run.sh` | tempo de bloqueio não mudou | automático (e2e) |
| RNF-006 | — | `app/src/test/java/com/ctrlmesh/protocolo/CanalSeguroTest.kt` | RNF-006 aperto leva a chave publica e rejeita outra magica ou versao | automático (JUnit) |
| RNF-006 | — | `app/src/test/java/com/ctrlmesh/protocolo/CanalSeguroTest.kt` | RNF-006 central e celular trocam quadros cifrados nos dois sentidos | automático (JUnit) |
| RNF-006 | — | `app/src/test/java/com/ctrlmesh/protocolo/CanalSeguroTest.kt` | RNF-006 quadro alterado, repetido ou devolvido ao remetente e rejeitado | automático (JUnit) |
| RNF-006 | — | `app/src/test/java/com/ctrlmesh/protocolo/CanalSeguroTest.kt` | RNF-006 cada conexao usa chave nova | automático (JUnit) |
| RNF-006 | — | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | RNF-006 tipo desconhecido e ignorado para compatibilidade futura | automático (JUnit) |
| RNF-006 | — | `app/src/test/java/com/ctrlmesh/protocolo/ProtocoloTest.kt` | RNF-006 quadro truncado, vazio ou com valor fora da tabela e rejeitado | automático (JUnit) |
| RNF-016 | — | `app/src/test/java/com/ctrlmesh/protocolo/TamanhoDoVideoTest.kt` | RNF-016 central grande - lado maior limitado a 1280 px | automático (JUnit) |
| RNF-016 | — | `app/src/test/java/com/ctrlmesh/protocolo/TamanhoDoVideoTest.kt` | RNF-016 central menor que 1280 - vale a area da central | automático (JUnit) |
| RNF-016 | — | `app/src/test/java/com/ctrlmesh/protocolo/TamanhoDoVideoTest.kt` | RNF-016 celular em retrato ou com tela pequena | automático (JUnit) |
| RNF-006 | — | `app/src/test/java/com/ctrlmesh/protocolo/TelaVirtualProtocoloTest.kt` | RNF-006 lista de apps truncada e rejeitada | automático (JUnit) |

## Testes manuais

`[Carro]` = central real no carro. `[Teste]` = emulador + celular, mas o resultado depende de ouvir, de um app/celular específico ou de uma ligação.

| RN | CA | Arquivo | Teste (roteiro) | Automático/manual |
|---|---|---|---|---|
| RN-012 | CA-013 | — | [Carro] Espelhando com a central do carro; desligar o carro e esperar a central entrar em espera (≥ 5 min). Ligar o carro. Sem tocar na central: o CtrlMesh volta a "aguardando o celular" e, com o roteador ligado, espelha sozinho. | manual |
| RN-082, RN-084 | CA-079 | — | [Carro] Rede do celular salva na central com conexão automática; rádio aberto na frente. Ligar o roteador do celular. Sem tocar em nada: a central entra na rede, o CtrlMesh vem para a frente sobre o rádio e mostra a tela do celular. | manual |
| RN-059, RN-068 | CA-058 | — | [Carro] Rádio tocando; celular no Bluetooth do carro. Espelhamento começa; abrir o Spotify pela central e tocar uma música: sai no som do carro com o CtrlMesh na frente, sem abrir "Música Bluetooth", e o rádio para. Ao encerrar, o rádio não volta sozinho. | manual |
| RN-050, RN-052 | CA-053 | — | [Carro] Espelhando: desligar o roteador do celular e ligar de novo em até 60 s. Central e celular mostram "reconectando" e o espelhamento continua sozinho, sem "Permitir". | manual |
| RN-087 | CA-089 (carro) | — | [Carro] Espelhando com música: andar com o carro. Imagem visível, toques funcionando, música e "−", "+", "mudo" funcionando, nenhuma mensagem de restrição. (Parte do emulador é automática.) | manual |
| RN-034 | CA-034 | — | [Teste] Espelhando: ligar para o celular de outro telefone. A tela da ligação aparece na central; tocar em atender atende no celular; repetir e tocar em recusar. O espelhamento continua. | manual |
| RN-038 | CA-038 | — | [Teste] Espelhando: abrir pela central a busca das Configurações do celular; o teclado do celular aparece na imagem; digitar "wifi" tocando nas teclas na central. O texto aparece no celular; a central não abre teclado próprio. | manual |
| RNF-012, RN-059 | CA-067 | — | [Teste] Tocar no celular um vídeo de teste de sincronia (flash + bipe a cada segundo) espelhado na central; filmar a tela do emulador e o som do Mac a 240 qps; medir a diferença entre flash e bipe em 10 ocorrências: som entre −45 ms e +125 ms da imagem. | manual |
| RN-021 | CA-022 | — | [Teste] Em celular cujo pedido de captura ofereça "um app": escolher "um app". A central mostra só esse app e o aviso de compartilhamento de um app (texto coberto por teste automático). | complemento manual |
| RN-022 | CA-023 | — | [Teste] App instalado pelo APK em Android 13+ sem "configurações restritas" liberadas: abrir o CtrlMesh no celular; ele mostra como liberar as configurações restritas antes da acessibilidade. | complemento manual |
| RN-023 | CA-024 | — | [Teste, celular Android 17+] Proteção avançada ligada: espelhar; imagem aparece, toques sem efeito e a central mostra "sem controle: o Android do celular bloqueia o controle". | complemento manual |
| RN-026, RN-046 | CA-027 | — | [Teste] Espelhando em paisagem: abrir pela central um app travado em retrato. Continua em retrato, inteiro, com faixas; voltar a um app que aceita paisagem: ocupa a imagem, sem interromper. | complemento manual |
| RN-033 | CA-033 | — | [Teste] Abrir no celular um app de banco/streaming que bloqueia captura. A central mostra área escura com "conteúdo protegido pelo app"; Voltar e Início funcionam. | complemento manual |
| RN-035 | CA-035 | — | [Teste] Pela central: abrir o Spotify, tocar "play" (música toca); toque longo num ícone (menu aparece), arrastar um ícone, deslizar a tela inicial, pinça no Maps (zoom). Mesmo efeito no celular. | complemento manual |
| RN-036 | CA-036 | — | [Teste] Com um app real em retrato espelhado, tocar nas faixas laterais: nada acontece no celular. | complemento manual |
| RN-047 | CA-045 | — | [Teste] Espelhando e em "reconectando": puxar a barra de notificações e tentar dispensar o indicador; não some e não há opção de ocultá-lo. | complemento manual |
| RN-059, RN-060 | CA-056 | — | [Teste] Mídia em 60%, tocar música pelo Spotify na central: sai nos alto-falantes do Mac e não no celular; encerrar: volta a sair no celular em 60%. Se sair nos dois, vale a RN-090 (CA-093). | complemento manual |
| RN-062, RN-063 | CA-060 | — | [Teste] Espelhando com música: receber ligação (toque e voz no celular, nada na central, aviso de ligação na central); disparar um alarme (sai no celular, não na central). | complemento manual |
| RN-062, RN-063 | CA-061 | — | [Teste] Tocar pela central o som de um app que bloqueia captura: nada na central nem no celular silenciado. (A lista de sons não enviáveis é automática.) | complemento manual |
| RN-064 | CA-062 | — | [Teste] Ouvir no Mac: "−"/"+" mudam um nível, "mudo" silencia e volta no nível anterior, botões de volume do emulador mudam o som. | complemento manual |
| RN-066 | CA-064 | — | [Teste] Música tocando: desligar o Wi-Fi do emulador ~10 s e religar. Som para na central e volta do ponto atual do celular, sem repetir o trecho. | complemento manual |
| RN-067 | CA-065 | — | [Teste] `adb -s emulator-5554 emu network speed 1000:1000`; música tocando: imagem piora, música sem cortes perceptíveis por 2 min. Restaurar com `emu network speed full`. | complemento manual |
| RN-056 | CA-052 | — | [Teste] Com banda limitada (como acima), medir 20 toques (filmagem a 240 qps do toque na central até a mudança na imagem): ≤ 150 ms em 95% (RNF-001), com a imagem visivelmente mais pobre. | complemento manual |
| RN-084 | CA-080 | — | [Teste] Sem a autorização de sobreposição na central: além da notificação (automático), confirmar que o som do celular já toca no Mac antes de tocar nela. | complemento manual |
| RN-079 | CA-082 | — | [Teste] Segundo celular com o CtrlMesh no roteador do primeiro; emulador com o endereço do primeiro: espelha o primeiro. (A ordem de tentativa é automática.) | complemento manual |
| RN-074 | CA-084 | — | [Teste] Sem liberação, depois do "Permitir": com o emulador fora da rede > 60 s, o aviso de captura do Android continua no celular. | complemento manual |
| RN-019, RN-074 | CA-087 | — | [Teste] Sem liberação, celular bloqueado: o emulador conecta e o pedido de captura só aparece depois de desbloquear. | complemento manual |
| RN-090 | CA-093 | — | [Teste, celular em que silenciar a mídia zera o som enviado] Espelhar com música: volume do celular inalterado, música nos dois, aviso de RN-090 na central. | complemento manual |
| RN-109, RN-110 | CA-112 | — | [Teste] Conferir que o fundo da barra é semitransparente e o visual leve; com o Spotify aberto, as abas de baixo do app ficam visíveis acima da barra e respondem ao toque. (Posição, ordem, 76 dp e Ajustes são automáticos.) | complemento manual |
| RN-100 | CA-102 | — | [Teste] Modo tela virtual: tocar pela central num campo de busca de um app na tela virtual; o teclado do celular aparece na tela virtual (na central) e o texto digitado pela central entra no campo; a tela física não mostra teclado. | manual |
| RN-101 | CA-103 | — | [Teste] Modo tela virtual: ligar para o celular; a tela da ligação aparece na tela física, a central mostra "ligação em andamento — atenda no celular" e continua espelhando. (Exibição do aviso é automática.) | complemento manual |
| RN-102 | CA-104 | — | [Teste] Autorizar o CtrlMesh no Shizuku, reiniciar o celular: o estado mostra "Shizuku desligado" (só pede religar) e, ao religar, "Shizuku ativo" sem novo pedido; com a autorização revogada no app Shizuku: "CtrlMesh sem autorização no Shizuku". (Estado sem Shizuku é automático.) | complemento manual |
| RN-103 | CA-105 | — | [Teste] Shizuku ativo no Wi-Fi de casa; ligar o roteador do celular: Shizuku continua ativo e `e2e/run.sh tela-virtual` passa. Reiniciar o celular sem religar o Shizuku: o CtrlMesh não o liga e o espelhamento usa o plano B (log `modo=fisica`). | complemento manual |
| RN-104 | CA-106 | — | [Teste] Shizuku desligado com o celular conectado a um Wi-Fi: notificação "toque para ativar a tela horizontal"; tocar abre o Shizuku; ao ativá-lo, a notificação some. | manual |
| RN-106 | CA-107 | — | [Teste] Mídia em 60%, Spotify tocando na tela virtual: a música sai no Mac e não no celular, sem pedido; ao encerrar, volta a sair no celular. (Volume inalterado é automático.) | complemento manual |
| RN-094 | CA-096 | — | [Teste] Emulador com área de imagem menor que 1280 px (ex.: AVD 1024×600): a tela virtual tem o tamanho dessa área (log `display_virtual=`). | complemento manual |
| RNF-017 | CA-110 | — | [Teste] No RMX3963: além do automático (tamanho e Spotify na tela virtual), ouvir o som do Spotify no Mac sem pedido e conferir o Spotify em layout horizontal. | complemento manual |
| RN-096 | CA-098 | — | [Teste] Abrir o WhatsApp pela central (horizontal, ou inteiro com faixas); a tela inicial do Realme nunca aparece na central; com o Spotify aberto na tela física, abri-lo pela central o move para a tela virtual. | complemento manual |
| RN-097 | CA-099 | — | [Teste] Dois apps abertos na tela virtual: Apps recentes alterna entre eles; Voltar volta dentro do app. (Tela física inalterada é automática.) | complemento manual |
| RN-088 | CA-090 | — | [Teste] Sem liberação, com música tocando e o pedido na tela: música continua no celular, nada no Mac; após "Permitir", passa para o Mac e o celular silencia. (Volume do celular é automático.) | complemento manual |

## Pendências (para as regras de negócio; nenhuma bloqueia os testes)

1. **RN-078 × RN-054 (CA-073):** quando a central ativa cai e há outra central esperando, a segunda espelha na hora ou só depois da janela de 60 s? O contrato reserva a vaga durante "reconectando" (a segunda assume quando a janela vence ou a primeira encerra); os testes só exigem que a segunda espelhe sozinha.
2. **RN-085 (CA-081) "rede do celular salva":** o Android não deixa um app comum ler as redes Wi-Fi salvas. O contrato trata esse item como orientação (`ajuste-rede`), sem "ok"/"falta".
3. **RN-012 / CA-077 no celular bloqueado após reinício:** com tela bloqueada não há imagem (RN-086); o roteiro pede ao operador para desbloquear.
4. **RN-074 "aviso de captura do próprio Android":** não verificável por `adb` de forma estável; ficou manual.

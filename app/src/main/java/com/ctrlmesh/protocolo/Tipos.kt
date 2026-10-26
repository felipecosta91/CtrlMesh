package com.ctrlmesh.protocolo

enum class Papel { CELULAR, MULTIMIDIA }

class ProtocoloException(mensagem: String) : Exception(mensagem)

/** Valor no fio = posição + 1 (contrato §4). */
enum class EstadoRemoto { AGUARDANDO_CONFIRMACAO, ESPELHANDO, DESBLOQUEIE, EM_USO_POR_OUTRA_CENTRAL }

/** Valor no fio = posição. */
enum class AcaoToque { DESCE, MOVE, SOBE, CANCELA }

/** Valor no fio = posição + 1. */
enum class TeclaDeSistema { VOLTAR, INICIO, RECENTES }

/** Valor no fio = posição + 1. Sem `Modo` recebido, a central assume TELA_FISICA (contrato §10.7). */
enum class ModoEspelhamento { TELA_VIRTUAL, TELA_FISICA }

/** App com ícone na tela inicial do celular (contrato §4, `ListaDeApps`). */
class AppDoCelular(val pacote: String, val rotulo: String, val iconePng: ByteArray) {
    override fun equals(other: Any?) = other is AppDoCelular && other.pacote == pacote && other.rotulo == rotulo &&
        other.iconePng.contentEquals(iconePng)
    override fun hashCode() = (pacote.hashCode() * 31 + rotulo.hashCode()) * 31 + iconePng.contentHashCode()
    override fun toString() = "AppDoCelular($pacote, $rotulo, ${iconePng.size} bytes)"
}

/** Valor no fio = posição + 1. */
enum class MotivoEncerramento(val texto: String) {
    ENCERRADO_PELO_CELULAR("encerrado pelo celular"),
    ENCERRADO_PELA_CENTRAL("encerrado pela central"),
    CONEXAO_PERDIDA("conexão perdida"),
    APP_FECHADO_OU_DESLIGADO("app fechado ou aparelho desligado"),
    NAO_CONFIRMADO_NO_CELULAR("não confirmado no celular"),
}

enum class Aviso(val texto: String) {
    FALTA_ACESSIBILIDADE("sem controle: autorização de acessibilidade não concedida no celular"),
    FALTA_GIRAR_TELA("falta no celular: girar a tela"),
    FALTA_SOBREPOR("falta no celular: aparecer sobre outros apps"),
    FALTA_GRAVAR_AUDIO("sem som: autorização de áudio não concedida no celular"),
    FALTA_BATERIA("o sistema pode fechar o CtrlMesh e o espelhamento pode não começar sozinho"),
    SEM_LIBERACAO_PERMANENTE("captura sem liberação permanente — refaça a liberação pelo Mac para não precisar tocar em Permitir"),
    CONTROLE_BLOQUEADO_PELO_SISTEMA("sem controle: o Android do celular bloqueia o controle"),
    COMPARTILHANDO_SO_UM_APP("o celular está compartilhando só um app; encerre e escolha a tela inteira"),
    LIGACAO_EM_ANDAMENTO("ligação em andamento — o som está no celular"),
    NAO_SILENCIA("este celular não permite silenciar sem cortar o som da central"),
    CONTEUDO_PROTEGIDO("conteúdo protegido pelo app"),
    MODO_TELA_DO_CELULAR("modo tela do celular — ative a tela horizontal no celular"),
    LIGACAO_NO_CELULAR("ligação em andamento — atenda no celular"),
    ;

    val bit: Int get() = ordinal
}

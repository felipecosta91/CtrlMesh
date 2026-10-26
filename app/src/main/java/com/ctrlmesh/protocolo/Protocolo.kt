package com.ctrlmesh.protocolo

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer

sealed interface Mensagem {
    data class Ola(val nomeCentral: String, val larguraArea: Int, val alturaArea: Int) : Mensagem
    data class Estado(val estado: EstadoRemoto) : Mensagem
    data class Situacao(val avisos: Set<Aviso>) : Mensagem
    data class Modo(val modo: ModoEspelhamento) : Mensagem
    data class Area(val larguraArea: Int, val alturaArea: Int) : Mensagem
    data class ListaDeApps(val apps: List<AppDoCelular>) : Mensagem
    data class AbrirApp(val pacote: String) : Mensagem
    data object TelaVirtualVazia : Mensagem
    class VideoConfig(val largura: Int, val altura: Int, val codec: String, val csd: List<ByteArray>) : Mensagem
    class VideoQuadro(val ptsUs: Long, val chave: Boolean, val dados: ByteArray) : Mensagem
    class AudioConfig(val taxa: Int, val canais: Int, val codec: String, val csd: List<ByteArray>) : Mensagem
    class AudioQuadro(val ptsUs: Long, val dados: ByteArray) : Mensagem
    data class Toque(
        val ponteiro: Int, val acao: AcaoToque, val x: Float, val y: Float,
        val larguraQuadro: Int, val alturaQuadro: Int, val tempoMs: Long,
    ) : Mensagem
    data class Tecla(val tecla: TeclaDeSistema) : Mensagem
    data class Encerrar(val motivo: MotivoEncerramento) : Mensagem
    data class Ping(val seq: Int) : Mensagem
    data class Pong(val seq: Int) : Mensagem
    data class Desconhecida(val tipo: Int) : Mensagem
}

object Protocolo {
    const val PORTA = 47800
    const val VERSAO = 1

    private const val OLA = 0x01
    private const val ESTADO = 0x02
    private const val SITUACAO = 0x03
    private const val MODO = 0x04
    private const val AREA = 0x05
    private const val LISTA_DE_APPS = 0x40
    private const val ABRIR_APP = 0x41
    private const val TELA_VIRTUAL_VAZIA = 0x42
    private const val VIDEO_CONFIG = 0x10
    private const val VIDEO_QUADRO = 0x11
    private const val AUDIO_CONFIG = 0x20
    private const val AUDIO_QUADRO = 0x21
    private const val TOQUE = 0x30
    private const val TECLA = 0x31
    private const val ENCERRAR = 0x50
    private const val PING = 0x60
    private const val PONG = 0x61

    private val paraCelular = setOf(OLA, TOQUE, TECLA, AREA, ABRIR_APP)
    private val paraCentral = setOf(
        ESTADO, SITUACAO, MODO, VIDEO_CONFIG, VIDEO_QUADRO, AUDIO_CONFIG, AUDIO_QUADRO, LISTA_DE_APPS, TELA_VIRTUAL_VAZIA,
    )

    fun codificar(mensagem: Mensagem): ByteArray {
        val bytes = ByteArrayOutputStream()
        val saida = DataOutputStream(bytes)
        with(saida) {
            when (mensagem) {
                is Mensagem.Ola -> { writeByte(OLA); str(mensagem.nomeCentral); writeShort(mensagem.larguraArea); writeShort(mensagem.alturaArea) }
                is Mensagem.Estado -> { writeByte(ESTADO); writeByte(mensagem.estado.ordinal + 1) }
                is Mensagem.Situacao -> { writeByte(SITUACAO); writeInt(mensagem.avisos.fold(0) { m, a -> m or (1 shl a.bit) }) }
                is Mensagem.Modo -> { writeByte(MODO); writeByte(mensagem.modo.ordinal + 1) }
                is Mensagem.Area -> { writeByte(AREA); writeShort(mensagem.larguraArea); writeShort(mensagem.alturaArea) }
                is Mensagem.ListaDeApps -> {
                    writeByte(LISTA_DE_APPS); writeShort(mensagem.apps.size)
                    mensagem.apps.forEach { str(it.pacote); str(it.rotulo); writeInt(it.iconePng.size); write(it.iconePng) }
                }
                is Mensagem.AbrirApp -> { writeByte(ABRIR_APP); str(mensagem.pacote) }
                Mensagem.TelaVirtualVazia -> writeByte(TELA_VIRTUAL_VAZIA)
                is Mensagem.VideoConfig -> {
                    writeByte(VIDEO_CONFIG); writeShort(mensagem.largura); writeShort(mensagem.altura)
                    str(mensagem.codec); lista(mensagem.csd)
                }
                is Mensagem.VideoQuadro -> { writeByte(VIDEO_QUADRO); writeLong(mensagem.ptsUs); writeBoolean(mensagem.chave); write(mensagem.dados) }
                is Mensagem.AudioConfig -> {
                    writeByte(AUDIO_CONFIG); writeInt(mensagem.taxa); writeByte(mensagem.canais)
                    str(mensagem.codec); lista(mensagem.csd)
                }
                is Mensagem.AudioQuadro -> { writeByte(AUDIO_QUADRO); writeLong(mensagem.ptsUs); write(mensagem.dados) }
                is Mensagem.Toque -> {
                    writeByte(TOQUE); writeByte(mensagem.ponteiro); writeByte(mensagem.acao.ordinal)
                    writeFloat(mensagem.x); writeFloat(mensagem.y)
                    writeShort(mensagem.larguraQuadro); writeShort(mensagem.alturaQuadro); writeLong(mensagem.tempoMs)
                }
                is Mensagem.Tecla -> { writeByte(TECLA); writeByte(mensagem.tecla.ordinal + 1) }
                is Mensagem.Encerrar -> { writeByte(ENCERRAR); writeByte(mensagem.motivo.ordinal + 1) }
                is Mensagem.Ping -> { writeByte(PING); writeInt(mensagem.seq) }
                is Mensagem.Pong -> { writeByte(PONG); writeInt(mensagem.seq) }
                is Mensagem.Desconhecida -> writeByte(mensagem.tipo)
            }
        }
        return bytes.toByteArray()
    }

    fun decodificar(quadro: ByteArray, receptor: Papel): Mensagem {
        if (quadro.isEmpty()) throw ProtocoloException("quadro vazio")
        val b = ByteBuffer.wrap(quadro)
        val tipo = b.get().toInt() and 0xFF
        if (receptor == Papel.MULTIMIDIA && tipo in paraCelular || receptor == Papel.CELULAR && tipo in paraCentral) {
            throw ProtocoloException("mensagem 0x%02x no sentido errado".format(tipo))
        }
        return try {
            when (tipo) {
                OLA -> Mensagem.Ola(b.str(), b.u16(), b.u16())
                ESTADO -> Mensagem.Estado(enumDoFio(EstadoRemoto.entries, b.u8() - 1))
                SITUACAO -> {
                    val mascara = b.int
                    Mensagem.Situacao(Aviso.entries.filter { mascara and (1 shl it.bit) != 0 }.toSet())
                }
                MODO -> Mensagem.Modo(enumDoFio(ModoEspelhamento.entries, b.u8() - 1))
                AREA -> Mensagem.Area(b.u16(), b.u16())
                LISTA_DE_APPS -> Mensagem.ListaDeApps(List(b.u16()) { AppDoCelular(b.str(), b.str(), b.bytes(b.int)) })
                ABRIR_APP -> Mensagem.AbrirApp(b.str())
                TELA_VIRTUAL_VAZIA -> Mensagem.TelaVirtualVazia
                VIDEO_CONFIG -> Mensagem.VideoConfig(b.u16(), b.u16(), b.str(), b.lista())
                VIDEO_QUADRO -> Mensagem.VideoQuadro(b.long, b.bool(), b.resto())
                AUDIO_CONFIG -> Mensagem.AudioConfig(b.int, b.u8(), b.str(), b.lista())
                AUDIO_QUADRO -> Mensagem.AudioQuadro(b.long, b.resto())
                TOQUE -> Mensagem.Toque(
                    b.u8(), enumDoFio(AcaoToque.entries, b.u8()), b.float, b.float, b.u16(), b.u16(), b.long,
                )
                TECLA -> Mensagem.Tecla(enumDoFio(TeclaDeSistema.entries, b.u8() - 1))
                ENCERRAR -> Mensagem.Encerrar(enumDoFio(MotivoEncerramento.entries, b.u8() - 1))
                PING -> Mensagem.Ping(b.int)
                PONG -> Mensagem.Pong(b.int)
                else -> Mensagem.Desconhecida(tipo)
            }
        } catch (_: BufferUnderflowException) {
            throw ProtocoloException("quadro 0x%02x truncado".format(tipo))
        } catch (_: IllegalArgumentException) {
            throw ProtocoloException("quadro 0x%02x com tamanho inválido".format(tipo))
        }
    }

    private fun <T> enumDoFio(valores: List<T>, indice: Int): T =
        valores.getOrNull(indice) ?: throw ProtocoloException("valor $indice fora da tabela")

    private fun DataOutputStream.str(texto: String) {
        val bytes = texto.toByteArray(Charsets.UTF_8)
        writeShort(bytes.size)
        write(bytes)
    }

    private fun DataOutputStream.lista(itens: List<ByteArray>) {
        writeByte(itens.size)
        itens.forEach { writeInt(it.size); write(it) }
    }

    private fun ByteBuffer.u8() = get().toInt() and 0xFF
    private fun ByteBuffer.u16() = short.toInt() and 0xFFFF
    private fun ByteBuffer.bool() = when (u8()) {
        0 -> false
        1 -> true
        else -> throw ProtocoloException("bool inválido")
    }
    private fun ByteBuffer.bytes(tamanho: Int): ByteArray {
        if (tamanho < 0 || tamanho > remaining()) throw ProtocoloException("tamanho inválido")
        return ByteArray(tamanho).also { get(it) }
    }
    private fun ByteBuffer.str() = String(bytes(u16()), Charsets.UTF_8)
    private fun ByteBuffer.lista() = List(u8()) { bytes(int) }
    private fun ByteBuffer.resto() = bytes(remaining())
}

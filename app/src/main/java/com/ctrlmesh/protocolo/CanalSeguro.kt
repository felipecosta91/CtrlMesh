package com.ctrlmesh.protocolo

import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object Aperto {
    const val TAMANHO = 74
    private val MAGICA = "CTRLMESH".toByteArray(Charsets.US_ASCII)

    fun codificar(chavePublica: ByteArray): ByteArray = MAGICA + byteArrayOf(Protocolo.VERSAO.toByte()) + chavePublica

    fun decodificar(bytes: ByteArray): ByteArray {
        if (bytes.size != TAMANHO) throw ProtocoloException("aperto com tamanho ${bytes.size}")
        if (!bytes.copyOfRange(0, 8).contentEquals(MAGICA)) throw ProtocoloException("aperto sem a mágica")
        if (bytes[8].toInt() != Protocolo.VERSAO) throw ProtocoloException("versão incompatível")
        if (bytes[9].toInt() != 0x04) throw ProtocoloException("chave pública inválida")
        return bytes.copyOfRange(9, TAMANHO)
    }
}

/** AES-256-GCM com chaves por sentido derivadas de ECDH P-256 + HKDF-SHA256 (contrato §3). Não é thread-safe por sentido. */
class CanalSeguro private constructor(
    private val chaveEnvio: SecretKeySpec,
    private val chaveRecebimento: SecretKeySpec,
    private val direcaoEnvio: Int,
    private val direcaoRecebimento: Int,
) {
    private var contadorEnvio = 0L
    private var contadorRecebimento = 0L

    @Synchronized
    fun cifrar(quadro: ByteArray): ByteArray {
        val cifra = Cipher.getInstance(TRANSFORMACAO)
        cifra.init(Cipher.ENCRYPT_MODE, chaveEnvio, GCMParameterSpec(TAG_BITS, nonce(direcaoEnvio, contadorEnvio)))
        contadorEnvio++
        return cifra.doFinal(quadro)
    }

    @Synchronized
    fun decifrar(blob: ByteArray): ByteArray {
        if (blob.size > TAMANHO_MAXIMO) throw GeneralSecurityException("quadro grande demais")
        val cifra = Cipher.getInstance(TRANSFORMACAO)
        cifra.init(Cipher.DECRYPT_MODE, chaveRecebimento, GCMParameterSpec(TAG_BITS, nonce(direcaoRecebimento, contadorRecebimento)))
        val claro = cifra.doFinal(blob)
        contadorRecebimento++
        return claro
    }

    companion object {
        const val TAMANHO_MAXIMO = 4 * 1024 * 1024 + 16
        private const val TRANSFORMACAO = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128
        private const val CENTRAL_PARA_CELULAR = 1
        private const val CELULAR_PARA_CENTRAL = 2
        private const val TAMANHO_COORDENADA = 32

        fun gerarParDeChaves(): KeyPair =
            KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

        fun publicaX962(par: KeyPair): ByteArray {
            val ponto = (par.public as ECPublicKey).w
            return byteArrayOf(0x04) + coordenada(ponto.affineX) + coordenada(ponto.affineY)
        }

        fun criar(papel: Papel, meuPar: KeyPair, publicaDoOutro: ByteArray): CanalSeguro {
            if (publicaDoOutro.size != 65 || publicaDoOutro[0].toInt() != 0x04) throw GeneralSecurityException("chave pública inválida")
            val parametros = (meuPar.public as ECPublicKey).params
            val ponto = ECPoint(
                BigInteger(1, publicaDoOutro.copyOfRange(1, 33)),
                BigInteger(1, publicaDoOutro.copyOfRange(33, 65)),
            )
            val outra = KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(ponto, parametros))
            val segredo = KeyAgreement.getInstance("ECDH").run {
                init(meuPar.private)
                doPhase(outra, true)
                generateSecret()
            }
            val minha = publicaX962(meuPar)
            val sal = if (papel == Papel.MULTIMIDIA) minha + publicaDoOutro else publicaDoOutro + minha
            val okm = hkdf(sal, segredo, "ctrlmesh v1".toByteArray(Charsets.US_ASCII), 64)
            val centralParaCelular = SecretKeySpec(okm.copyOfRange(0, 32), "AES")
            val celularParaCentral = SecretKeySpec(okm.copyOfRange(32, 64), "AES")
            return if (papel == Papel.MULTIMIDIA) {
                CanalSeguro(centralParaCelular, celularParaCentral, CENTRAL_PARA_CELULAR, CELULAR_PARA_CENTRAL)
            } else {
                CanalSeguro(celularParaCentral, centralParaCelular, CELULAR_PARA_CENTRAL, CENTRAL_PARA_CELULAR)
            }
        }

        private fun coordenada(valor: BigInteger): ByteArray {
            val bytes = valor.toByteArray().let { if (it.size > TAMANHO_COORDENADA) it.copyOfRange(it.size - TAMANHO_COORDENADA, it.size) else it }
            return ByteArray(TAMANHO_COORDENADA - bytes.size) + bytes
        }

        private fun nonce(direcao: Int, contador: Long): ByteArray =
            ByteBuffer.allocate(12).putInt(direcao).putLong(contador).array()

        private fun hkdf(sal: ByteArray, ikm: ByteArray, info: ByteArray, tamanho: Int): ByteArray {
            fun hmac(chave: ByteArray, dados: ByteArray) =
                Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(chave, "HmacSHA256")) }.doFinal(dados)
            val prk = hmac(sal, ikm)
            var bloco = ByteArray(0)
            var saida = ByteArray(0)
            var i = 1
            while (saida.size < tamanho) {
                bloco = hmac(prk, bloco + info + byteArrayOf(i++.toByte()))
                saida += bloco
            }
            return saida.copyOf(tamanho)
        }
    }
}

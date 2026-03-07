package org.expert.link.mesh.infrastructure.adapter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.security.EncryptedPayload
import org.expert.link.mesh.domain.model.security.KeyMaterial
import org.expert.link.mesh.domain.port.external.CryptoPort
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Базовый крипто-адаптер на JCA.
 *
 * Генерирует ключи, вычисляет peerId, шифрует payload и подписывает метаданные пакетов.
 */
class BasicCryptoAdapter : CryptoPort {
    private val secureRandom = SecureRandom()

    override suspend fun generateKeyMaterial(alias: String): KeyMaterial = withContext(Dispatchers.Default) {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048, secureRandom)
        val pair = generator.generateKeyPair()
        KeyMaterial(
            keyId = alias,
            publicKey = Base64.getEncoder().encodeToString(pair.public.encoded),
            privateKey = Base64.getEncoder().encodeToString(pair.private.encoded),
            algorithm = "RSA",
            createdAt = now(),
        )
    }

    override fun derivePeerId(publicKey: String): String = sha256Hex(Base64.getDecoder().decode(publicKey))

    override suspend fun encrypt(targetPublicKey: String, payload: ByteArray): EncryptedPayload = withContext(Dispatchers.Default) {
        val aesKey = generateAesKey()
        val iv = ByteArray(12).also(secureRandom::nextBytes)
        val payloadCipher = Cipher.getInstance("AES/GCM/NoPadding")
        payloadCipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
        val ciphertext = payloadCipher.doFinal(payload)

        val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        rsaCipher.init(Cipher.ENCRYPT_MODE, decodePublicKey(targetPublicKey))
        val encryptedKey = rsaCipher.doFinal(aesKey.encoded)

        val bundle = ByteBuffer.allocate(4 + encryptedKey.size + ciphertext.size)
            .putInt(encryptedKey.size)
            .put(encryptedKey)
            .put(ciphertext)
            .array()

        EncryptedPayload(
            payloadNonce = Base64.getEncoder().encodeToString(iv),
            encryptedPayload = Base64.getEncoder().encodeToString(bundle),
        )
    }

    override suspend fun decrypt(privateKey: String, encryptedPayload: EncryptedPayload): ByteArray = withContext(Dispatchers.Default) {
        val bundle = ByteBuffer.wrap(Base64.getDecoder().decode(encryptedPayload.encryptedPayload))
        val encryptedKeySize = bundle.int
        val encryptedKey = ByteArray(encryptedKeySize).also(bundle::get)
        val ciphertext = ByteArray(bundle.remaining()).also(bundle::get)

        val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        rsaCipher.init(Cipher.DECRYPT_MODE, decodePrivateKey(privateKey))
        val aesKeyBytes = rsaCipher.doFinal(encryptedKey)

        val payloadCipher = Cipher.getInstance("AES/GCM/NoPadding")
        payloadCipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(aesKeyBytes, "AES"),
            GCMParameterSpec(128, Base64.getDecoder().decode(encryptedPayload.payloadNonce)),
        )
        payloadCipher.doFinal(ciphertext)
    }

    override suspend fun sign(privateKey: String, data: ByteArray): String = withContext(Dispatchers.Default) {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(decodePrivateKey(privateKey), secureRandom)
        signature.update(data)
        Base64.getEncoder().encodeToString(signature.sign())
    }

    override suspend fun verify(publicKey: String, data: ByteArray, signature: String): Boolean = withContext(Dispatchers.Default) {
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(decodePublicKey(publicKey))
        verifier.update(data)
        verifier.verify(Base64.getDecoder().decode(signature))
    }

    override fun randomSecret(lengthBytes: Int): String {
        val bytes = ByteArray(lengthBytes).also(secureRandom::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    override fun sha256Hex(data: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(data)
        .joinToString(separator = "") { "%02x".format(it) }

    private fun generateAesKey(): SecretKey {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256, secureRandom)
        return generator.generateKey()
    }

    private fun decodePublicKey(encoded: String): PublicKey = KeyFactory.getInstance("RSA")
        .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(encoded)))

    private fun decodePrivateKey(encoded: String): PrivateKey = KeyFactory.getInstance("RSA")
        .generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)))
}

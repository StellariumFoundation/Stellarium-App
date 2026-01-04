package com.jv.stellariumapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.net.Proxy
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

object NostrService {

    private const val TARGET_PUBKEY_HEX = "e6e8499252c8019688405021c5f3592c300845a7698583487f912239328246a4"

    private val RELAYS = listOf(
        "wss://relay.damus.io",
        "wss://relay.nostr.band",
        "wss://nos.lol",
        "wss://relay.snort.social",
        "wss://relay.primal.net"
    )

    fun publishMessageWithProxy(contact: String, message: String, proxy: Proxy): Boolean {
        try {
            val privateKeyBytes = generatePrivateKey()
            val publicKeyHex = getPublicKey(privateKeyBytes)

            val content = "STELLARIUM INTEL\n---\nContact: $contact\n\n$message"
            val createdAt = System.currentTimeMillis() / 1000

            val tags = JSONArray()
            val tTag = JSONArray().put("t").put("stellarium_intel")
            val pTag = JSONArray().put("p").put(TARGET_PUBKEY_HEX)
            tags.put(tTag)
            tags.put(pTag)

            val rawData = JSONArray()
            rawData.put(0)
            rawData.put(publicKeyHex)
            rawData.put(createdAt)
            rawData.put(1)
            rawData.put(tags)
            rawData.put(content)

            val idHex = sha256(rawData.toString())
            val sigHex = signSchnorr(idHex, privateKeyBytes)

            val event = JSONObject()
            event.put("id", idHex)
            event.put("pubkey", publicKeyHex)
            event.put("created_at", createdAt)
            event.put("kind", 1)
            event.put("tags", tags)
            event.put("content", content)
            event.put("sig", sigHex)

            val msg = JSONArray()
            msg.put("EVENT")
            msg.put(event)
            val msgString = msg.toString()

            var success = false
            val client = OkHttpClient.Builder()
                .proxy(proxy)
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build()

            for (url in RELAYS) {
                try {
                    val request = Request.Builder().url(url).build()
                    val listener = object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            Log.d("Nostr", "Sending to $url")
                            webSocket.send(msgString)
                            Thread.sleep(2000) 
                            webSocket.close(1000, "Done")
                            success = true
                        }
                    }
                    client.newWebSocket(request, listener)
                    Thread.sleep(2500) 
                } catch (e: Exception) { 
                    Log.e("Nostr", "Error connecting: ${e.message}")
                }
            }
            return success
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun publishMessage(contact: String, message: String): Boolean {
        return withContext(Dispatchers.IO) {
            publishMessageWithProxy(contact, message, Proxy.NO_PROXY)
        }
    }

    // ==========================================
    // === BIP-340 SCHNORR CRYPTOGRAPHY =========
    // ==========================================

    private val ecParams = CustomNamedCurves.getByName("secp256k1")
    private val curve = ecParams.curve
    private val n = ecParams.n

    private fun generatePrivateKey(): ByteArray {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    private fun getPublicKey(privateKey: ByteArray): String {
        val d = BigInteger(1, privateKey)
        val P = ecParams.g.multiply(d).normalize()
        return P.affineXCoord.encoded.toHex32()
    }

    private fun sha256(input: String): String {
        val digest = SHA256Digest()
        val bytes = input.toByteArray(Charsets.UTF_8)
        digest.update(bytes, 0, bytes.size)
        val result = ByteArray(digest.digestSize)
        digest.doFinal(result, 0)
        return result.toHex32()
    }

    private fun sha256(input: ByteArray): ByteArray {
        val digest = SHA256Digest()
        digest.update(input, 0, input.size)
        val result = ByteArray(digest.digestSize)
        digest.doFinal(result, 0)
        return result
    }

    private fun signSchnorr(msgHashHex: String, privateKey: ByteArray): String {
        val msgBytes = msgHashHex.hexToBytes()
        val d = BigInteger(1, privateKey)
        
        val aux = ByteArray(32)
        SecureRandom().nextBytes(aux)
        
        // k = Hash(d || aux || message)
        // Concatenate arrays manually
        val dBytes = d.toByteArray()
        val kInput = ByteArray(dBytes.size + aux.size + msgBytes.size)
        System.arraycopy(dBytes, 0, kInput, 0, dBytes.size)
        System.arraycopy(aux, 0, kInput, dBytes.size, aux.size)
        System.arraycopy(msgBytes, 0, kInput, dBytes.size + aux.size, msgBytes.size)

        var k = BigInteger(1, sha256(kInput)).mod(n)
        
        // R = k*G
        var R = ecParams.g.multiply(k).normalize()
        
        // Enforce Even Y
        if (R.affineYCoord.toBigInteger().testBit(0)) {
            k = n.subtract(k)
            R = ecParams.g.multiply(k).normalize()
        }
        
        val rX = R.affineXCoord.encoded
        val P = ecParams.g.multiply(d).normalize()
        val pX = P.affineXCoord.encoded
        
        // Challenge Hash
        val challengeData = ByteArray(32 + 32 + 32)
        // Use copyOfRange to handle padding safely if needed, or assume standard 32 bytes from BouncyCastle
        val rX32 = rX.takeLastBytes(32)
        val pX32 = pX.takeLastBytes(32)
        
        System.arraycopy(rX32, 0, challengeData, 0, 32)
        System.arraycopy(pX32, 0, challengeData, 32, 32)
        System.arraycopy(msgBytes, 0, challengeData, 64, 32)
        
        val eBytes = sha256(challengeData)
        val e = BigInteger(1, eBytes).mod(n)

        val s = k.add(e.multiply(d)).mod(n)

        return rX.toHex32() + s.toByteArray().toHex32()
    }

    // --- Helpers ---
    
    private fun ByteArray.toHex32(): String {
        val cleanBytes = if (this.size > 32 && this[0] == 0.toByte()) {
            this.copyOfRange(1, this.size)
        } else if (this.size < 32) {
            val padded = ByteArray(32)
            System.arraycopy(this, 0, padded, 32 - this.size, this.size)
            padded
        } else {
            this
        }
        return cleanBytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    
    private fun ByteArray.takeLastBytes(n: Int): ByteArray {
        if (this.size <= n) return this
        return this.copyOfRange(this.size - n, this.size)
    }
}
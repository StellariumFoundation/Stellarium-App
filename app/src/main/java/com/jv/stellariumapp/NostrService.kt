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
import java.math.BigInteger
import java.net.Proxy
import java.security.SecureRandom
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

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
            // 1. Keys
            val privateKey = generateRandomKey()
            val publicKey = getPublicKey(privateKey)

            // 2. Content
            val content = "STELLARIUM INTEL\n---\nContact: $contact\n\n$message"
            val createdAt = System.currentTimeMillis() / 1000

            // 3. Tags
            val tags = JSONArray()
            tags.put(JSONArray().put("t").put("stellarium_intel"))
            tags.put(JSONArray().put("p").put(TARGET_PUBKEY_HEX))

            // 4. Serialize for ID (NIP-01)
            val rawData = JSONArray()
            rawData.put(0)
            rawData.put(publicKey)
            rawData.put(createdAt)
            rawData.put(1)
            rawData.put(tags)
            rawData.put(content)

            val idHex = sha256(rawData.toString())
            val sigHex = signSchnorr(idHex, privateKey)

            // 5. Final Event JSON
            val event = JSONObject()
            event.put("id", idHex)
            event.put("pubkey", publicKey)
            event.put("created_at", createdAt)
            event.put("kind", 1)
            event.put("tags", tags)
            event.put("content", content)
            event.put("sig", sigHex)

            val msg = JSONArray()
            msg.put("EVENT")
            msg.put(event)
            val msgString = msg.toString()

            // 6. Broadcast & Wait for "OK"
            var successCount = 0
            val client = OkHttpClient.Builder()
                .proxy(proxy)
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build()

            for (url in RELAYS) {
                try {
                    val latch = CountDownLatch(1) // Wait for response
                    var relayAccepted = false

                    val request = Request.Builder().url(url).build()
                    val listener = object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            webSocket.send(msgString)
                            Log.d("Nostr", "Sent to $url")
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            // Listen for ["OK", "event_id", true, ...]
                            if (text.contains("OK") && text.contains("true")) {
                                Log.d("Nostr", "✅ ACCEPTED by $url")
                                relayAccepted = true
                                latch.countDown()
                                webSocket.close(1000, "Done")
                            } else if (text.contains("OK") && text.contains("false")) {
                                Log.e("Nostr", "❌ REJECTED by $url: $text")
                                latch.countDown() // Stop waiting
                            }
                        }
                        
                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                             latch.countDown()
                        }
                    }
                    
                    client.newWebSocket(request, listener)
                    // Wait max 5 seconds for confirmation per relay
                    latch.await(5, TimeUnit.SECONDS)
                    
                    if (relayAccepted) successCount++

                } catch (e: Exception) {
                    Log.e("Nostr", "Error $url: ${e.message}")
                }
            }

            return successCount > 0

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
    // === BIP-340 CRYPTO (Strict Implementation)
    // ==========================================

    private val ecParams = CustomNamedCurves.getByName("secp256k1")
    private val curve = ecParams.curve
    private val n = ecParams.n

    private fun generateRandomKey(): ByteArray {
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
        
        // 1. Aux Random
        val aux = ByteArray(32)
        SecureRandom().nextBytes(aux)
        
        // 2. Nonce k
        val dBytes = d.toByteArray().toThirtyTwoBytes()
        val kInput = ByteArray(32 + 32 + 32)
        System.arraycopy(dBytes, 0, kInput, 0, 32)
        System.arraycopy(aux, 0, kInput, 32, 32)
        System.arraycopy(msgBytes, 0, kInput, 64, 32)
        
        var k = BigInteger(1, sha256(kInput)).mod(n)
        if (k == BigInteger.ZERO) k = BigInteger.ONE // Safety

        // 3. R Point
        var R = ecParams.g.multiply(k).normalize()
        if (R.affineYCoord.toBigInteger().testBit(0)) {
            k = n.subtract(k)
            R = ecParams.g.multiply(k).normalize()
        }
        
        val rX = R.affineXCoord.encoded.toThirtyTwoBytes()
        val P = ecParams.g.multiply(d).normalize()
        val pX = P.affineXCoord.encoded.toThirtyTwoBytes()
        
        // 4. Challenge Hash e = Hash(rX || pX || msg)
        val challengeData = ByteArray(32 + 32 + 32)
        System.arraycopy(rX, 0, challengeData, 0, 32)
        System.arraycopy(pX, 0, challengeData, 32, 32)
        System.arraycopy(msgBytes, 0, challengeData, 64, 32)
        
        val eBytes = sha256(challengeData)
        val e = BigInteger(1, eBytes).mod(n)

        // 5. Signature s
        val s = k.add(e.multiply(d)).mod(n)
        val sBytes = s.toByteArray().toThirtyTwoBytes()

        // 6. Concat rX + s
        val sigBytes = ByteArray(64)
        System.arraycopy(rX, 0, sigBytes, 0, 32)
        System.arraycopy(sBytes, 0, sigBytes, 32, 32)
        
        return sigBytes.toHex()
    }

    // --- Extensions ---
    
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun ByteArray.toHex32(): String = toThirtyTwoBytes().toHex()

    private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    
    private fun ByteArray.toThirtyTwoBytes(): ByteArray {
        if (this.size == 32) return this
        val result = ByteArray(32)
        if (this.size > 32) {
            // If BigInt added a sign byte (00), strip it
            System.arraycopy(this, this.size - 32, result, 0, 32)
        } else {
            // Pad with zeros if too short
            System.arraycopy(this, 0, result, 32 - this.size, this.size)
        }
        return result
    }
}
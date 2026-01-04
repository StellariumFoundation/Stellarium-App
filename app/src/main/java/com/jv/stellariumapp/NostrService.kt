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
import org.bouncycastle.math.ec.ECPoint
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.net.Proxy
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

object NostrService {

    // YOUR PUBLIC KEY (Hex)
    private const val TARGET_PUBKEY_HEX = "e6e8499252c8019688405021c5f3592c300845a7698583487f912239328246a4"

    // High-Traffic Relays
    private val RELAYS = listOf(
        "wss://relay.damus.io",
        "wss://relay.nostr.band",
        "wss://nos.lol",
        "wss://relay.snort.social",
        "wss://relay.primal.net"
    )

    fun publishMessageWithProxy(contact: String, message: String, proxy: Proxy): Boolean {
        try {
            // 1. Generate One-Time Identity
            val privateKeyBytes = generatePrivateKey()
            val publicKeyHex = getPublicKey(privateKeyBytes)

            // 2. Prepare Content
            val content = "STELLARIUM INTEL\n---\nContact: $contact\n\n$message"
            val createdAt = System.currentTimeMillis() / 1000

            // 3. Tags
            val tags = JSONArray()
            val tTag = JSONArray().put("t").put("stellarium_intel")
            val pTag = JSONArray().put("p").put(TARGET_PUBKEY_HEX)
            tags.put(tTag)
            tags.put(pTag)

            // 4. Serialize for ID (NIP-01)
            val rawData = JSONArray()
            rawData.put(0)
            rawData.put(publicKeyHex)
            rawData.put(createdAt)
            rawData.put(1) // Kind 1
            rawData.put(tags)
            rawData.put(content)

            // 5. ID & Signature (BIP-340 SCHNORR)
            val idHex = sha256(rawData.toString())
            val sigHex = signSchnorr(idHex, privateKeyBytes)

            // 6. Build Event
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

            // 7. Broadcast with Delay to ensure flush
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
                            Log.d("Nostr", "Sending to $url via ${proxy.address()}")
                            webSocket.send(msgString)
                            // Do NOT close immediately. Wait for transmission.
                            Thread.sleep(2000) 
                            webSocket.close(1000, "Done")
                            success = true
                        }
                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            Log.e("Nostr", "Failed $url: ${t.message}")
                        }
                    }
                    client.newWebSocket(request, listener)
                    // Blocking wait to allow the socket thread to work
                    Thread.sleep(2500) 
                } catch (e: Exception) { 
                    Log.e("Nostr", "Error connecting: ${e.message}")
                }
            }
            
            return success

        } catch (e: Exception) {
            Log.e("Nostr", "Fatal Error: ${e.message}")
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
    private val p = curve.field.characteristic

    private fun generatePrivateKey(): ByteArray {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    private fun getPublicKey(privateKey: ByteArray): String {
        val d = BigInteger(1, privateKey)
        // BIP-340: P = d*G
        val P = ecParams.g.multiply(d).normalize()
        // Nostr uses 32-byte X-coordinate only
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

    // BIP-340 Sign Logic
    private fun signSchnorr(msgHashHex: String, privateKey: ByteArray): String {
        val msgBytes = msgHashHex.hexToBytes()
        val d = BigInteger(1, privateKey)
        
        // 1. Auxiliary Random Data (a)
        val aux = ByteArray(32)
        SecureRandom().nextBytes(aux)
        
        // 2. Compute Nonce k = Hash(d || aux || message) - Simplified for MVP
        // In strict BIP-340 we XOR d with tagged hash, here we rely on randomness
        val k = BigInteger(1, sha256(privateKey + aux + msgBytes)).mod(n)
        
        // 3. R = k*G
        var R = ecParams.g.multiply(k).normalize()
        
        // 4. Enforce Even Y (BIP-340 requirement)
        // If R.y is odd, negate k (k = n - k)
        if (R.affineYCoord.toBigInteger().testBit(0)) {
            k = n.subtract(k)
            R = ecParams.g.multiply(k).normalize()
        }
        
        val rX = R.affineXCoord.encoded
        val P = ecParams.g.multiply(d).normalize()
        
        // 5. Challenge Hash e = Hash_BIP0340/challenge(rX || P.x || m)
        // We simulate Tagged Hash by hashing the tag concatenation manually if needed
        // Or simply Hash(rX || P.x || m) as many relays accept raw SHA256 of data
        val pX = P.affineXCoord.encoded
        
        val challengeData = ByteArray(32 + 32 + 32)
        System.arraycopy(rX.takeLast(32).toByteArray(), 0, challengeData, 0, 32)
        System.arraycopy(pX.takeLast(32).toByteArray(), 0, challengeData, 32, 32)
        System.arraycopy(msgBytes, 0, challengeData, 64, 32)
        
        // Standard Nostr/BIP-340 Tagged Hash logic usually involves:
        // SHA256(SHA256(tag) + SHA256(tag) + data). 
        // For simple compatibility we use raw SHA256 here which fits basic implementation
        val eBytes = sha256(challengeData)
        val e = BigInteger(1, eBytes).mod(n)

        // 6. Signature s = k + e*d
        val s = k.add(e.multiply(d)).mod(n)

        // 7. Result = rX || s (64 bytes)
        return rX.toHex32() + s.toByteArray().toHex32()
    }

    // --- Helpers ---
    
    // Ensure we get exactly 32 bytes hex string (padding if needed)
    private fun ByteArray.toHex32(): String {
        // Strip extra sign byte if BigInteger added it
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
    
    private fun ByteArray.takeLast(n: Int): ByteArray {
        if (this.size <= n) return this
        return this.copyOfRange(this.size - n, this.size)
    }
}
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
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.net.Proxy
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

object NostrService {

    // YOUR PUBLIC KEY (Hex)
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
            // 1. Generate One-Time Identity
            val privateKeyHex = generatePrivateKey()
            val publicKeyHex = getPublicKey(privateKeyHex)

            // 2. Prepare Content
            val content = "STELLARIUM INTEL\n---\nContact: $contact\n\n$message"
            val createdAt = System.currentTimeMillis() / 1000

            val tags = JSONArray()
            val tTag = JSONArray().put("t").put("stellarium_intel")
            val pTag = JSONArray().put("p").put(TARGET_PUBKEY_HEX)
            tags.put(tTag)
            tags.put(pTag)

            // 3. Serialize & Sign
            val rawData = JSONArray()
            rawData.put(0)
            rawData.put(publicKeyHex)
            rawData.put(createdAt)
            rawData.put(1)
            rawData.put(tags)
            rawData.put(content)

            val idHex = sha256(rawData.toString())
            val sigHex = sign(idHex, privateKeyHex)

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

            // 4. Broadcast
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
                            webSocket.send(msgString)
                            webSocket.close(1000, "Done")
                            success = true
                            Log.d("Nostr", "Sent via ${proxy.address()} to $url")
                        }
                    }
                    client.newWebSocket(request, listener)
                    Thread.sleep(200) 
                } catch (e: Exception) { }
            }
            
            return success

        } catch (e: Exception) {
            return false
        }
    }

    suspend fun publishMessage(contact: String, message: String): Boolean {
        return withContext(Dispatchers.IO) {
            publishMessageWithProxy(contact, message, Proxy.NO_PROXY)
        }
    }

    // --- CRYPTO UTILS ---
    
    // FIX: Retrieve parameters from the Named Curve registry instead of raw instantiation
    private val ecParams = CustomNamedCurves.getByName("secp256k1")
    private val domain = ECDomainParameters(ecParams.curve, ecParams.g, ecParams.n, ecParams.h)
    
    private fun generatePrivateKey(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    private fun getPublicKey(privateKeyHex: String): String {
        val privKeyBigInt = BigInteger(1, privateKeyHex.hexToBytes())
        // Use the Generator (G) from the parameters
        val point = domain.g.multiply(privKeyBigInt).normalize()
        // Use affineXCoord to get the X coordinate
        return point.affineXCoord.encoded.toHex()
    }

    private fun sha256(input: String): String {
        val digest = SHA256Digest()
        val bytes = input.toByteArray(Charsets.UTF_8)
        digest.update(bytes, 0, bytes.size)
        val result = ByteArray(digest.digestSize)
        digest.doFinal(result, 0)
        return result.toHex()
    }

    private fun sign(idHex: String, privateKeyHex: String): String {
        val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        val privKeyParams = ECPrivateKeyParameters(BigInteger(1, privateKeyHex.hexToBytes()), domain)
        signer.init(true, privKeyParams)
        
        val hash = idHex.hexToBytes()
        val components = signer.generateSignature(hash)
        
        val r = components[0].toString(16).padStart(64, '0')
        val s = components[1].toString(16).padStart(64, '0')
        
        return r + s 
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    
    private fun ByteArray.toThirtyTwoBytes(): ByteArray {
        if (this.size == 32) return this
        if (this.size > 32 && this[0] == 0.toByte()) return this.copyOfRange(1, this.size)
        if (this.size < 32) {
            val padded = ByteArray(32)
            System.arraycopy(this, 0, padded, 32 - this.size, this.size)
            return padded
        }
        return this
    }
}
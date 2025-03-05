package com.example.securemvvm.model.security

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.apache.commons.codec.binary.Base32
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlin.experimental.and

class TwoFactorAuthManager @Inject constructor() {
    
    private val base32 = Base32()

    fun generateSecretKey(): String {
        val random = SecureRandom()
        val bytes = ByteArray(20)
        random.nextBytes(bytes)
        return base32.encodeAsString(bytes)
    }

    fun generateTOTP(secret: String, time: Long = System.currentTimeMillis() / 30000): String {
        val key = base32.decode(secret.toUpperCase())
        
        val data = ByteArray(8)
        var value = time
        for (i in 7 downTo 0) {
            data[i] = value.toByte()
            value = value shr 8
        }

        val signKey = SecretKeySpec(key, "HmacSHA1")
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(signKey)
        val hash = mac.doFinal(data)

        val offset = (hash[hash.size - 1] and 0xf).toInt()
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)

        return String.format("%06d", binary % 1000000)
    }

    fun generateQRCodeBitmap(secret: String, accountName: String, issuer: String): Bitmap {
        val otpAuthURL = "otpauth://totp/$issuer:$accountName?secret=$secret&issuer=$issuer"
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            otpAuthURL,
            BarcodeFormat.QR_CODE,
            512,
            512
        )

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        
        return bitmap
    }

    fun verifyTOTP(secret: String, code: String): Boolean {
        val currentTime = System.currentTimeMillis() / 30000
        // Check current and adjacent time windows
        return (-1..1).any { window ->
            generateTOTP(secret, currentTime + window) == code
        }
    }
} 
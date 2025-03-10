package com.example.securemvvm.model.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@Singleton
class EmailService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SMTP_HOST = "smtp.gmail.com"
        private const val SMTP_PORT = "587"
        private const val EMAIL_FROM = "kitadriand@gmail.com" // Replace with your Gmail
        private const val EMAIL_PASSWORD = "etth sqhl ydcu bvvz" // Replace with your app password
    }

    suspend fun sendOTPEmail(recipientEmail: String, otp: String) = withContext(Dispatchers.IO) {
        try {
            val props = Properties().apply {
                put("mail.smtp.host", SMTP_HOST)
                put("mail.smtp.port", SMTP_PORT)
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(EMAIL_FROM))
                addRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                subject = "Your Login OTP Code"
                setText("Your verification code is: $otp\nThis code will expire in 2 minutes.")
            }

            Transport.send(message)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
} 
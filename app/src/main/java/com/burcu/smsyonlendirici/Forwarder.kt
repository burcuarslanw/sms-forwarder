package com.burcu.smsyonlendirici

import android.content.Context
import android.telephony.SmsManager
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Şablondaki {from} {body} {time} {rule} değişkenlerini doldurur ve hedefe gönderir.
 * Webhook -> HttpURLConnection, SMS -> SmsManager, E-posta -> SMTP (JavaMail).
 */
object Forwarder {

    fun render(template: String, from: String, body: String, ruleName: String): String {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        return template
            .replace("{from}", from)
            .replace("{body}", body)
            .replace("{time}", time)
            .replace("{rule}", ruleName)
    }

    /** Başarılıysa null, aksi halde hata metni döner. */
    fun send(c: Context, t: Target, from: String, body: String, ruleName: String): String? {
        return try {
            when (t.kind) {
                TargetKind.WEBHOOK -> sendWebhook(t, from, body, ruleName)
                TargetKind.SMS -> sendSms(t, from, body, ruleName)
                TargetKind.EMAIL -> sendEmail(t, from, body, ruleName)
            }
        } catch (e: Exception) {
            e.message ?: e.toString()
        }
    }

    private fun sendWebhook(t: Target, from: String, body: String, ruleName: String): String? {
        val payload = render(t.bodyTemplate, from, body, ruleName)
        val conn = URL(t.url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = t.method.ifBlank { "POST" }.uppercase()
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true
            if (t.contentType.isNotBlank())
                conn.setRequestProperty("Content-Type", t.contentType)
            if (t.headerName.isNotBlank())
                conn.setRequestProperty(t.headerName, render(t.headerValue, from, body, ruleName))
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            return if (code in 200..299) null else "HTTP $code"
        } finally {
            conn.disconnect()
        }
    }

    private fun sendSms(t: Target, from: String, body: String, ruleName: String): String? {
        val text = render(t.bodyTemplate, from, body, ruleName)
        @Suppress("DEPRECATION")
        val sm = SmsManager.getDefault()
        val parts = sm.divideMessage(text)
        sm.sendMultipartTextMessage(t.phone, null, parts, null, null)
        return null
    }

    private fun sendEmail(t: Target, from: String, body: String, ruleName: String): String? {
        val subject = render(t.subjectTemplate, from, body, ruleName)
        val text = render(t.bodyTemplate, from, body, ruleName)
        val to = t.mailTo.ifBlank { t.smtpUser }   // boşsa kendine gönder
        val port = t.smtpPort.ifBlank { "587" }

        val props = Properties()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.host"] = t.smtpHost
        props["mail.smtp.port"] = port
        if (port == "465") {
            // SSL
            props["mail.smtp.socketFactory.port"] = port
            props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        } else {
            // STARTTLS (587)
            props["mail.smtp.starttls.enable"] = "true"
        }
        props["mail.smtp.connectiontimeout"] = "20000"
        props["mail.smtp.timeout"] = "20000"

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(t.smtpUser, t.smtpPass)
        })
        val msg = MimeMessage(session)
        msg.setFrom(InternetAddress(t.smtpUser))
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
        msg.subject = subject
        msg.setText(text, "UTF-8")
        Transport.send(msg)
        return null
    }
}

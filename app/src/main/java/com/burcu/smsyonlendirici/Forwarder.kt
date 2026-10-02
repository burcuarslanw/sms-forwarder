package com.burcu.smsyonlendirici

import android.content.Context
import android.telephony.SmsManager
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Şablondaki {from} {body} {time} {rule} değişkenlerini doldurur ve hedefe gönderir.
 * Ağ çağrısı HttpURLConnection ile yapılır (ekstra kütüphane yok).
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
}

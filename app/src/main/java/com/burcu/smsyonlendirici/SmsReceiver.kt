package com.burcu.smsyonlendirici

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlin.concurrent.thread

/**
 * Gelen SMS'i yakalar. goAsync() ile kısa süreli arka plan işine izin verilir;
 * kurallar eşleştirilip eşleşenler hedeflerine yönlendirilir.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!Store.isEnabled(context)) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (msgs.isEmpty()) return

        val from = msgs[0].originatingAddress ?: ""
        val body = msgs.joinToString("") { it.messageBody ?: "" }

        val pending = goAsync()
        val app = context.applicationContext
        thread {
            try {
                process(app, from, body)
            } finally {
                pending.finish()
            }
        }
    }

    private fun process(c: Context, from: String, body: String) {
        val rules = Store.loadRules(c).filter { it.enabled }
        for (r in rules) {
            if (!matches(r, from, body)) continue
            val target = Store.targetById(c, r.targetId)
            if (target == null) {
                LogStore.add(c, "⚠ ${r.name}: hedef bulunamadı")
                continue
            }
            val err = Forwarder.send(c, target, from, body, r.name)
            if (err == null) {
                LogStore.add(c, "✓ $from → ${target.name} (${r.name})")
                Notif.show(c, "Yönlendirildi", "$from → ${target.name}\n$body")
            } else {
                LogStore.add(c, "✗ $from → ${target.name}: $err")
                Notif.show(c, "Yönlendirme hatası", "${target.name}: $err")
            }
        }
    }

    private fun matches(r: Rule, from: String, body: String): Boolean {
        val sp = r.senderPattern.trim()
        val bp = r.bodyPattern.trim()
        if (sp.isEmpty() && bp.isEmpty()) return true
        val okSender = sp.isEmpty() || test(r.matchMode, sp, from)
        val okBody = bp.isEmpty() || test(r.matchMode, bp, body)
        return okSender && okBody
    }

    private fun test(mode: MatchMode, pattern: String, value: String): Boolean = when (mode) {
        MatchMode.CONTAINS -> value.contains(pattern, ignoreCase = true)
        MatchMode.REGEX -> try {
            Regex(pattern).containsMatchIn(value)
        } catch (e: Exception) {
            false
        }
    }
}

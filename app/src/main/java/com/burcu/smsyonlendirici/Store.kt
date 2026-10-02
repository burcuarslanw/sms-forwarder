package com.burcu.smsyonlendirici

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Kuralları ve hedefleri SharedPreferences içinde JSON olarak saklar. Ekstra bağımlılık yok. */
object Store {
    private const val PREF = "smsfwd"
    private const val K_ENABLED = "enabled"
    private const val K_TARGETS = "targets"
    private const val K_RULES = "rules"

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun isEnabled(c: Context): Boolean = p(c).getBoolean(K_ENABLED, false)
    fun setEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean(K_ENABLED, v).apply()

    fun newId(): String = UUID.randomUUID().toString()

    fun loadTargets(c: Context): MutableList<Target> {
        val arr = JSONArray(p(c).getString(K_TARGETS, "[]") ?: "[]")
        val out = mutableListOf<Target>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Target(
                    id = o.getString("id"),
                    name = o.optString("name"),
                    kind = TargetKind.valueOf(o.optString("kind", "WEBHOOK")),
                    url = o.optString("url"),
                    method = o.optString("method", "POST"),
                    headerName = o.optString("headerName"),
                    headerValue = o.optString("headerValue"),
                    contentType = o.optString("contentType", "application/json"),
                    bodyTemplate = o.optString("bodyTemplate", "{body}"),
                    phone = o.optString("phone"),
                    smtpHost = o.optString("smtpHost", "smtp.gmail.com"),
                    smtpPort = o.optString("smtpPort", "587"),
                    smtpUser = o.optString("smtpUser"),
                    smtpPass = o.optString("smtpPass"),
                    mailTo = o.optString("mailTo"),
                    subjectTemplate = o.optString("subjectTemplate", "SMS: {from}")
                )
            )
        }
        return out
    }

    fun saveTargets(c: Context, list: List<Target>) {
        val arr = JSONArray()
        for (t in list) {
            val o = JSONObject()
            o.put("id", t.id); o.put("name", t.name); o.put("kind", t.kind.name)
            o.put("url", t.url); o.put("method", t.method)
            o.put("headerName", t.headerName); o.put("headerValue", t.headerValue)
            o.put("contentType", t.contentType); o.put("bodyTemplate", t.bodyTemplate)
            o.put("phone", t.phone)
            o.put("smtpHost", t.smtpHost); o.put("smtpPort", t.smtpPort)
            o.put("smtpUser", t.smtpUser); o.put("smtpPass", t.smtpPass)
            o.put("mailTo", t.mailTo); o.put("subjectTemplate", t.subjectTemplate)
            arr.put(o)
        }
        p(c).edit().putString(K_TARGETS, arr.toString()).apply()
    }

    fun loadRules(c: Context): MutableList<Rule> {
        val arr = JSONArray(p(c).getString(K_RULES, "[]") ?: "[]")
        val out = mutableListOf<Rule>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Rule(
                    id = o.getString("id"),
                    name = o.optString("name"),
                    enabled = o.optBoolean("enabled", true),
                    senderPattern = o.optString("senderPattern"),
                    bodyPattern = o.optString("bodyPattern"),
                    matchMode = MatchMode.valueOf(o.optString("matchMode", "CONTAINS")),
                    targetId = o.optString("targetId")
                )
            )
        }
        return out
    }

    fun saveRules(c: Context, list: List<Rule>) {
        val arr = JSONArray()
        for (r in list) {
            val o = JSONObject()
            o.put("id", r.id); o.put("name", r.name); o.put("enabled", r.enabled)
            o.put("senderPattern", r.senderPattern); o.put("bodyPattern", r.bodyPattern)
            o.put("matchMode", r.matchMode.name); o.put("targetId", r.targetId)
            arr.put(o)
        }
        p(c).edit().putString(K_RULES, arr.toString()).apply()
    }

    fun targetById(c: Context, id: String): Target? = loadTargets(c).firstOrNull { it.id == id }
}

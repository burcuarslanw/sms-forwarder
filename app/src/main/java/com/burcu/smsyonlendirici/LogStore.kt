package com.burcu.smsyonlendirici

import android.content.Context
import org.json.JSONArray

/** Son işlemlerin basit günlüğü (en fazla 50 satır). */
object LogStore {
    private const val PREF = "smsfwd_log"
    private const val K = "lines"
    private const val MAX = 50

    fun add(c: Context, line: String) {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val arr = JSONArray(p.getString(K, "[]"))
        arr.put(line)
        val start = maxOf(0, arr.length() - MAX)
        val trimmed = JSONArray()
        for (i in start until arr.length()) trimmed.put(arr.getString(i))
        p.edit().putString(K, trimmed.toString()).apply()
    }

    fun lines(c: Context): List<String> {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val arr = JSONArray(p.getString(K, "[]"))
        return (0 until arr.length()).map { arr.getString(it) }.reversed()
    }

    fun clear(c: Context) =
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(K).apply()
}

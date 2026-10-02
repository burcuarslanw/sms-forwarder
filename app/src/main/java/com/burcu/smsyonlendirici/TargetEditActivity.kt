package com.burcu.smsyonlendirici

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TargetEditActivity : AppCompatActivity() {

    private var editing: Target? = null

    private lateinit var name: EditText
    private lateinit var kindSpinner: Spinner
    private lateinit var presetSpinner: Spinner
    private lateinit var webhookBox: View
    private lateinit var smsBox: View
    private lateinit var url: EditText
    private lateinit var method: EditText
    private lateinit var headerName: EditText
    private lateinit var headerValue: EditText
    private lateinit var contentType: EditText
    private lateinit var bodyTemplate: EditText
    private lateinit var phone: EditText

    private val presets = listOf("— şablon seç —", "Zoom", "Slack", "Discord", "Telegram (bot)", "Genel webhook")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_target_edit)

        name = findViewById(R.id.name)
        kindSpinner = findViewById(R.id.kindSpinner)
        presetSpinner = findViewById(R.id.presetSpinner)
        webhookBox = findViewById(R.id.webhookBox)
        smsBox = findViewById(R.id.smsBox)
        url = findViewById(R.id.url)
        method = findViewById(R.id.method)
        headerName = findViewById(R.id.headerName)
        headerValue = findViewById(R.id.headerValue)
        contentType = findViewById(R.id.contentType)
        bodyTemplate = findViewById(R.id.bodyTemplate)
        phone = findViewById(R.id.phone)

        kindSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Webhook (Zoom/Slack/…)", "SMS")
        )
        kindSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                webhookBox.visibility = if (pos == 0) View.VISIBLE else View.GONE
                smsBox.visibility = if (pos == 1) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        presetSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, presets)
        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos > 0) applyPreset(presets[pos])
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        intent.getStringExtra("id")?.let { id ->
            editing = Store.targetById(this, id)
            editing?.let { bind(it) }
        }

        findViewById<Button>(R.id.save).setOnClickListener { save() }
    }

    private fun applyPreset(preset: String) {
        when (preset) {
            "Zoom" -> {
                kindSpinner.setSelection(0)
                method.setText("POST")
                headerName.setText("Authorization")
                headerValue.setText("DOGRULAMA_TOKENI")
                contentType.setText("application/json")
                url.setText("https://integrations.zoom.us/chat/webhooks/incomingwebhook/ENDPOINT_ID?format=message")
                bodyTemplate.setText("📩 {from}\n{body}\n({time})")
            }
            "Slack" -> {
                kindSpinner.setSelection(0)
                method.setText("POST"); headerName.setText(""); headerValue.setText("")
                contentType.setText("application/json")
                url.setText("https://hooks.slack.com/services/XXX/YYY/ZZZ")
                bodyTemplate.setText("{\"text\":\"*{from}*\\n{body}\"}")
            }
            "Discord" -> {
                kindSpinner.setSelection(0)
                method.setText("POST"); headerName.setText(""); headerValue.setText("")
                contentType.setText("application/json")
                url.setText("https://discord.com/api/webhooks/XXX/YYY")
                bodyTemplate.setText("{\"content\":\"**{from}**\\n{body}\"}")
            }
            "Telegram (bot)" -> {
                kindSpinner.setSelection(0)
                method.setText("POST"); headerName.setText(""); headerValue.setText("")
                contentType.setText("application/json")
                url.setText("https://api.telegram.org/bot<TOKEN>/sendMessage")
                bodyTemplate.setText("{\"chat_id\":\"<CHAT_ID>\",\"text\":\"{from}\\n{body}\"}")
            }
            "Genel webhook" -> {
                kindSpinner.setSelection(0)
                method.setText("POST"); contentType.setText("application/json")
                bodyTemplate.setText("{body}")
            }
        }
    }

    private fun bind(t: Target) {
        name.setText(t.name)
        kindSpinner.setSelection(if (t.kind == TargetKind.WEBHOOK) 0 else 1)
        url.setText(t.url); method.setText(t.method)
        headerName.setText(t.headerName); headerValue.setText(t.headerValue)
        contentType.setText(t.contentType); bodyTemplate.setText(t.bodyTemplate)
        phone.setText(t.phone)
    }

    private fun save() {
        if (name.text.isBlank()) { toast("İsim gerekli"); return }
        val kind = if (kindSpinner.selectedItemPosition == 0) TargetKind.WEBHOOK else TargetKind.SMS

        val t = editing ?: Target(id = Store.newId(), name = "", kind = kind)
        t.name = name.text.toString()
        t.kind = kind
        t.url = url.text.toString().trim()
        t.method = method.text.toString().trim().ifEmpty { "POST" }
        t.headerName = headerName.text.toString().trim()
        t.headerValue = headerValue.text.toString()
        t.contentType = contentType.text.toString().trim()
        t.bodyTemplate = bodyTemplate.text.toString()
        t.phone = phone.text.toString().trim()

        if (kind == TargetKind.WEBHOOK && t.url.isEmpty()) { toast("URL gerekli"); return }
        if (kind == TargetKind.SMS && t.phone.isEmpty()) { toast("Telefon gerekli"); return }

        val list = Store.loadTargets(this)
        if (editing == null) {
            list.add(t)
        } else {
            val idx = list.indexOfFirst { it.id == t.id }
            if (idx >= 0) list[idx] = t else list.add(t)
        }
        Store.saveTargets(this, list)
        finish()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}

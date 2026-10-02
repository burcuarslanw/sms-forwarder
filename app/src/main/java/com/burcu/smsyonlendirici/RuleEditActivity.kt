package com.burcu.smsyonlendirici

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RuleEditActivity : AppCompatActivity() {

    private var editing: Rule? = null

    private lateinit var name: EditText
    private lateinit var enabled: Switch
    private lateinit var sender: EditText
    private lateinit var body: EditText
    private lateinit var modeSpinner: Spinner
    private lateinit var targetSpinner: Spinner
    private lateinit var targets: List<Target>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_edit)

        name = findViewById(R.id.name)
        enabled = findViewById(R.id.enabled)
        sender = findViewById(R.id.sender)
        body = findViewById(R.id.body)
        modeSpinner = findViewById(R.id.modeSpinner)
        targetSpinner = findViewById(R.id.targetSpinner)

        modeSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("İçerir (CONTAINS)", "Regex")
        )

        targets = Store.loadTargets(this)
        targetSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            targets.map { it.name }
        )

        val id = intent.getStringExtra("id")
        if (id != null) editing = Store.loadRules(this).firstOrNull { it.id == id }
        editing?.let { bind(it) } ?: run { enabled.isChecked = true }

        findViewById<Button>(R.id.save).setOnClickListener { save() }
    }

    private fun bind(r: Rule) {
        name.setText(r.name)
        enabled.isChecked = r.enabled
        sender.setText(r.senderPattern)
        body.setText(r.bodyPattern)
        modeSpinner.setSelection(if (r.matchMode == MatchMode.CONTAINS) 0 else 1)
        val idx = targets.indexOfFirst { it.id == r.targetId }
        if (idx >= 0) targetSpinner.setSelection(idx)
    }

    private fun save() {
        if (name.text.isBlank()) { toast("İsim gerekli"); return }
        if (targets.isEmpty()) { toast("Önce hedef ekleyin"); return }

        val r = editing ?: Rule(id = Store.newId(), name = "")
        r.name = name.text.toString()
        r.enabled = enabled.isChecked
        r.senderPattern = sender.text.toString().trim()
        r.bodyPattern = body.text.toString().trim()
        r.matchMode = if (modeSpinner.selectedItemPosition == 0) MatchMode.CONTAINS else MatchMode.REGEX
        r.targetId = targets[targetSpinner.selectedItemPosition].id

        val list = Store.loadRules(this)
        if (editing == null) {
            list.add(r)
        } else {
            val idx = list.indexOfFirst { it.id == r.id }
            if (idx >= 0) list[idx] = r else list.add(r)
        }
        Store.saveRules(this, list)
        finish()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}

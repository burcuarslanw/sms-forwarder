package com.burcu.smsyonlendirici

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var masterSwitch: CompoundButton
    private lateinit var statusText: TextView
    private lateinit var targetsBox: LinearLayout
    private lateinit var rulesBox: LinearLayout
    private lateinit var logView: TextView

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Notif.ensureChannel(this)

        masterSwitch = findViewById(R.id.masterSwitch)
        statusText = findViewById(R.id.statusText)
        targetsBox = findViewById(R.id.targetsBox)
        rulesBox = findViewById(R.id.rulesBox)
        logView = findViewById(R.id.logView)

        masterSwitch.setOnCheckedChangeListener { btn, v ->
            if (!btn.isPressed) return@setOnCheckedChangeListener
            Store.setEnabled(this, v)
            if (v) requestPerms()
        }
        findViewById<Button>(R.id.addTarget).setOnClickListener {
            startActivity(Intent(this, TargetEditActivity::class.java))
        }
        findViewById<Button>(R.id.addRule).setOnClickListener {
            if (Store.loadTargets(this).isEmpty()) {
                toast("Önce bir hedef ekleyin")
            } else {
                startActivity(Intent(this, RuleEditActivity::class.java))
            }
        }
        findViewById<Button>(R.id.grantPerms).setOnClickListener { requestPerms() }
        findViewById<Button>(R.id.clearLog).setOnClickListener { LogStore.clear(this); refresh() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun requestPerms() {
        val perms = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        permLauncher.launch(perms.toTypedArray())
    }

    private fun refresh() {
        val enabled = Store.isEnabled(this)
        masterSwitch.isChecked = enabled
        statusText.text = if (enabled) "Açık — gelen SMS'ler yönlendiriliyor" else "Kapalı"

        targetsBox.removeAllViews()
        val targets = Store.loadTargets(this)
        if (targets.isEmpty()) targetsBox.addView(hint("(hedef yok)"))
        for (t in targets) {
            targetsBox.addView(
                row(
                    "${t.name}  [${label(t.kind)}]",
                    extra = "Test" to { testTarget(t) },
                    onEdit = {
                        startActivity(
                            Intent(this, TargetEditActivity::class.java).putExtra("id", t.id)
                        )
                    },
                    onDelete = {
                        Store.saveTargets(this, Store.loadTargets(this).filter { it.id != t.id })
                        refresh()
                    }
                )
            )
        }

        rulesBox.removeAllViews()
        val byId = targets.associateBy { it.id }
        val rules = Store.loadRules(this)
        if (rules.isEmpty()) rulesBox.addView(hint("(kural yok)"))
        for (r in rules) {
            val tName = byId[r.targetId]?.name ?: "?"
            val state = if (r.enabled) "açık" else "kapalı"
            rulesBox.addView(
                row(
                    "${r.name} → $tName  ($state)",
                    extra = null,
                    onEdit = {
                        startActivity(
                            Intent(this, RuleEditActivity::class.java).putExtra("id", r.id)
                        )
                    },
                    onDelete = {
                        Store.saveRules(this, Store.loadRules(this).filter { it.id != r.id })
                        refresh()
                    }
                )
            )
        }

        logView.text = LogStore.lines(this).joinToString("\n").ifEmpty { "(henüz kayıt yok)" }

        val need = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) !=
            PackageManager.PERMISSION_GRANTED
        findViewById<TextView>(R.id.permHint).visibility = if (need) View.VISIBLE else View.GONE
    }

    private fun label(k: TargetKind) = when (k) {
        TargetKind.WEBHOOK -> "Webhook"
        TargetKind.SMS -> "SMS"
        TargetKind.EMAIL -> "E-posta"
    }

    private fun hint(text: String): View {
        val tv = TextView(this)
        tv.text = text
        tv.setPadding(0, 8, 0, 8)
        return tv
    }

    private fun row(
        title: String,
        extra: Pair<String, () -> Unit>?,
        onEdit: () -> Unit,
        onDelete: () -> Unit
    ): View {
        val ll = LinearLayout(this)
        ll.orientation = LinearLayout.HORIZONTAL
        ll.setPadding(0, 8, 0, 8)

        val tv = TextView(this)
        tv.text = title
        tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        ll.addView(tv)

        extra?.let { (lbl, act) ->
            val b = Button(this); b.text = lbl; b.setOnClickListener { act() }; ll.addView(b)
        }
        val e = Button(this); e.text = "Düzenle"; e.setOnClickListener { onEdit() }; ll.addView(e)
        val d = Button(this); d.text = "Sil"; d.setOnClickListener { onDelete() }; ll.addView(d)
        return ll
    }

    private fun testTarget(t: Target) {
        toast("Test gönderiliyor…")
        Thread {
            val err = Forwarder.send(this, t, "+900000000000", "Test mesajı", "Test")
            runOnUiThread { toast(if (err == null) "Gönderildi ✓" else "Hata: $err") }
        }.start()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}

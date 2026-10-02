package com.burcu.smsyonlendirici

/** Nereye gönderileceği. */
enum class TargetKind { WEBHOOK, SMS }

/** Kuralın SMS'i nasıl eşleştireceği. */
enum class MatchMode { CONTAINS, REGEX }

/**
 * Bir gönderim hedefi. WEBHOOK alanları Zoom/Slack/Discord/Telegram/genel webhook için ortak;
 * SMS hedefi için sadece [phone] ve [bodyTemplate] kullanılır.
 */
data class Target(
    val id: String,
    var name: String,
    var kind: TargetKind,
    // --- webhook ---
    var url: String = "",
    var method: String = "POST",
    var headerName: String = "",
    var headerValue: String = "",
    var contentType: String = "application/json",
    var bodyTemplate: String = "{body}",
    // --- sms ---
    var phone: String = ""
)

/**
 * Gelen SMS'e uygulanan kural. [senderPattern] ve/veya [bodyPattern] boşsa o koşul aranmaz;
 * ikisi de boşsa kural tüm SMS'lere uyar. Eşleşirse [targetId] hedefine yönlendirilir.
 */
data class Rule(
    val id: String,
    var name: String,
    var enabled: Boolean = true,
    var senderPattern: String = "",
    var bodyPattern: String = "",
    var matchMode: MatchMode = MatchMode.CONTAINS,
    var targetId: String = ""
)

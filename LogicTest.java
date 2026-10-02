import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Uygulamanın çekirdek mantığının (SmsReceiver.matches + Forwarder.render) saf JVM kopyası.
 * Android gerektirmeyen bu fonksiyonlar birebir aynı davranışla burada test edilir.
 */
public class LogicTest {

    enum MatchMode { CONTAINS, REGEX }

    // ---- SmsReceiver.test / matches karşılığı ----
    static boolean test(MatchMode mode, String pattern, String value) {
        switch (mode) {
            case CONTAINS:
                return value.toLowerCase(Locale.ROOT).contains(pattern.toLowerCase(Locale.ROOT));
            case REGEX:
                try { return Pattern.compile(pattern).matcher(value).find(); }
                catch (Exception e) { return false; }
            default: return false;
        }
    }

    static boolean matches(String senderPattern, String bodyPattern, MatchMode mode,
                           String from, String body) {
        String sp = senderPattern.trim();
        String bp = bodyPattern.trim();
        if (sp.isEmpty() && bp.isEmpty()) return true;
        boolean okSender = sp.isEmpty() || test(mode, sp, from);
        boolean okBody = bp.isEmpty() || test(mode, bp, body);
        return okSender && okBody;
    }

    // ---- Forwarder.render karşılığı ----
    static String render(String template, String from, String body, String ruleName) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        return template
                .replace("{from}", from)
                .replace("{body}", body)
                .replace("{time}", time)
                .replace("{rule}", ruleName);
    }

    // ---- minik test çatısı ----
    static int pass = 0, fail = 0;
    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("  PASS  " + name); }
        else { fail++; System.out.println("  FAIL  " + name); }
    }

    public static void main(String[] args) {
        System.out.println("== Kural eşleştirme ==");
        // Boş kural her SMS'e uyar
        check("bos kural -> hepsine uyar",
                matches("", "", MatchMode.CONTAINS, "+905551112233", "merhaba"));
        // Gönderen CONTAINS
        check("gonderen icerir (eslesir)",
                matches("5551112233", "", MatchMode.CONTAINS, "+905551112233", "x"));
        check("gonderen icerir (eslesmez)",
                !matches("4440", "", MatchMode.CONTAINS, "+905551112233", "x"));
        // Buyuk/kucuk harf duyarsiz
        check("gonderen buyuk/kucuk duyarsiz",
                matches("banka", "", MatchMode.CONTAINS, "BANKA", "x"));
        // Icerik CONTAINS
        check("icerik icerir (eslesir)",
                matches("", "kod", MatchMode.CONTAINS, "x", "Giris kodunuz 123"));
        // Gonderen VE icerik birlikte
        check("gonderen+icerik ikisi de saglanmali (saglanir)",
                matches("555", "OTP", MatchMode.CONTAINS, "+90555", "OTP 9090"));
        check("gonderen dogru ama icerik yanlis -> eslesmez",
                !matches("555", "OTP", MatchMode.CONTAINS, "+90555", "merhaba"));
        // REGEX
        check("regex rakam dizisi (eslesir)",
                matches("", "\\d{4,6}", MatchMode.REGEX, "x", "kodunuz 482913"));
        check("regex rakam dizisi (eslesmez)",
                !matches("", "\\d{4,6}", MatchMode.REGEX, "x", "kod yok"));
        check("gecersiz regex -> eslesmez, cokmez",
                !matches("", "[", MatchMode.REGEX, "x", "abc"));

        System.out.println("== Sablon doldurma ==");
        String r1 = render("📩 {from}\n{body}\n({time})", "+90555", "Merhaba", "Test");
        check("from yerlesti", r1.contains("+90555"));
        check("body yerlesti", r1.contains("Merhaba"));
        check("time yyyy-MM-dd formatinda", r1.matches("(?s).*\\(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}\\)$"));
        String r2 = render("{\"text\":\"{from}: {body}\"}", "ACME", "sel\"am", "R");
        check("JSON sablonu from/body doldurur", r2.contains("ACME") && r2.contains("sel\"am"));
        String r3 = render("kural={rule}", "a", "b", "Bankadan");
        check("rule yerlesti", r3.equals("kural=Bankadan"));

        System.out.println();
        System.out.println("Sonuc: " + pass + " gecti, " + fail + " kaldi.");
        if (fail > 0) System.exit(1);
    }
}

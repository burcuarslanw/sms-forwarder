# SMS Yönlendirici

Gelen SMS'leri **kurallara göre** bir hedefe yönlendiren basit, şeffaf bir Android uygulaması.
Asıl kullanım: belirli bir kişiden gelen SMS'i **Zoom**'da kendine (veya bir kanala) iletmek.
Hedef sistem **jeneriktir**: Zoom, Slack, Discord, Telegram, herhangi bir webhook veya başka bir telefona SMS.

> Kendi telefonunuzda, kendi mesajlarınız için tasarlandı. Başkasının telefonuna gizlice
> kurup onu izlemek için **değildir** — bu yüzden uygulama gizli çalışmaz: normal simgesi
> vardır, bir aç/kapa anahtarı vardır ve her yönlendirmede bildirim gösterir.

## Nasıl çalışır

1. **Hedef** tanımlarsınız (nereye gidecek): Zoom webhook, Slack, Telegram, genel webhook ya da bir SMS numarası.
2. **Kural** tanımlarsınız (hangi SMS gidecek): gönderene ve/veya mesaj içeriğine göre eşleştirme
   (içerir ya da regex). İkisi de boş bırakılırsa tüm SMS'ler eşleşir.
3. Üstteki ana anahtarı açarsınız. Gelen her SMS kurallara bakılarak uygun hedeflere iletilir.

Mesaj şablonunda şu değişkenler kullanılır: `{from}` `{body}` `{time}` `{rule}`

## APK'yı bulutta üret (bilgisayara hiçbir şey kurmadan)

En kolay yol bu — GitHub derler, sen hazır APK'yı indirirsin:
1. [github.com](https://github.com)'da yeni bir repo aç (private olabilir).
2. Bu klasörü o repoya yükle (web arayüzünden sürükle-bırak ya da `git push`).
3. Repoda **Actions** sekmesine gir; "APK derle" akışı otomatik çalışır (ilk sefer birkaç dakika).
4. Çalışma bitince en alttaki **Artifacts > app-debug** dosyasını indir → içinden `app-debug.apk` çıkar.
5. Bu APK, Android'in debug anahtarıyla **v2 imzalı** üretilir, yani doğrudan telefona kurulur.

Sonra doğrudan "2. Aşama — APK'yı telefona kur" bölümüne geç.

## Derleme (kendi bilgisayarında)

İnternet erişimi olan bir makinede (bağımlılıklar ilk derlemede indirilir):

**Android Studio ile (en kolay):**
1. Android Studio'da `File > Open` ile bu klasörü açın.
2. Gradle senkronizasyonu bitince `Run` (yeşil ok) veya `Build > Build APK(s)`.

**Komut satırı ile:**
```bash
# SDK yolunu bir kez belirtin (Android Studio kuruluysa genelde bu yoldadır):
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # Windows: sdk.dir=C\:\\Users\\<ad>\\AppData\\Local\\Android\\Sdk

./gradlew assembleDebug           # Linux/Mac
# gradlew.bat assembleDebug       # Windows
```
APK şurada oluşur: `app/build/outputs/apk/debug/app-debug.apk`

Telefona kurmak için: `adb install app/build/outputs/apk/debug/app-debug.apk`
veya APK'yı telefona kopyalayıp "bilinmeyen kaynaklar"a izin vererek kurun.

## Zoom kurulumu (asıl amaç)

1. Zoom Marketplace'ten **Incoming Webhook** uygulamasını hesabınıza ekletin (gerekirse admin'den).
2. Mesajı görmek istediğiniz sohbette (kendinize özel bir kanal da olur) `/incoming connect` yazın.
   Zoom size bir **Endpoint URL** ve bir **Verification Token** verir.
3. Uygulamada **+ Hedef Ekle → şablon: Zoom**:
   - URL alanındaki `ENDPOINT_ID` yerine Zoom'un verdiği endpoint'i yapıştırın
     (sonundaki `?format=message` kalsın).
   - Başlık değeri (Authorization) alanına **Verification Token**'ı yazın.
4. **Test** düğmesiyle deneyin — Zoom sohbetinde mesaj belirmeli.
5. **+ Kural Ekle**: "Gönderen eşleşmesi" alanına o kişinin numarasını/başlığını yazın, hedef olarak Zoom'u seçin.

> Not: Zoom bazen `?format=message` + düz metin gövdeyi kabul eder; eğer reddederse gövdeyi
> JSON yapıp URL'de `?format=full` kullanın (Zoom dökümanındaki `{"type":"message",...}` biçimi).

## İzinler

- `RECEIVE_SMS` — gelen SMS'i yakalamak (uygulamayı ilk açışta izin istenir).
- `INTERNET` — webhook'a göndermek.
- `SEND_SMS` — hedef "başka bir telefona SMS" seçilirse.
- `POST_NOTIFICATIONS` — her yönlendirme için bildirim.

## Güvenilirlik notu

Yönlendirme, SMS geldiği anda kısa süreli bir arka plan işinde yapılır. Bazı üreticilerin
agresif pil optimizasyonu bunu geciktirebilir; sorun yaşarsanız uygulamayı pil
optimizasyonundan muaf tutun (Ayarlar → Pil → kısıtlamasız).

## Dosya yapısı

```
app/src/main/java/com/burcu/smsyonlendirici/
  Models.kt        — Target / Rule veri modelleri
  Store.kt         — SharedPreferences + JSON kalıcılık
  LogStore.kt      — son işlemler günlüğü
  Forwarder.kt     — şablon doldurma + webhook/SMS gönderimi
  SmsReceiver.kt   — gelen SMS'i yakalar, kuralları uygular
  Notif.kt         — yönlendirme bildirimleri
  MainActivity.kt  — ana ekran (anahtar, hedef/kural listeleri, log)
  TargetEditActivity.kt / RuleEditActivity.kt — ekleme/düzenleme ekranları
```

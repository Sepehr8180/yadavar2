# راه‌اندازی GitHub برای Yadavar

## Build فعلی

Workflow در `.github/workflows/android.yml` با JDK 21 اجرا می‌شود، تست‌های Unit را اجرا می‌کند و بعد از موفقیت یک **Release APK امضاشده** می‌سازد.

برای `pull_request` فقط تست‌ها اجرا می‌شوند. برای `push` روی `main` و `workflow_dispatch` تست‌ها اجرا می‌شوند و سپس Release APK ساخته می‌شود.

از مسیر زیر اجرای دستی را شروع کنید:

`Actions → Android CI → Run workflow`

## امضای پایدار برای Update

برای اینکه نسخه جدید Android روی نسخه قبلی به‌صورت Update نصب شود، APKهای Release باید با همان certificate نسخه قبلی امضا شوند. این پروژه از یک keystore ثابت استفاده می‌کند که در GitHub Secrets نگهداری می‌شود.

### 1. ساخت Keystore بدون Android Studio

PowerShell را در پوشه پروژه باز کنید و اجرا کنید:

```powershell
.\tools\generate-yadavar-signing.ps1
```

اگر اجرای فایل‌های PowerShell مسدود بود، می‌توانید دستور زیر را فقط برای همین پنجره اجرا کنید:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

سپس دوباره اسکریپت را اجرا کنید.

اسکریپت فایل‌های زیر را می‌سازد:

- `yadavar-release.jks` — کلید خصوصی؛ فقط نزد خودتان نگه دارید.
- `yadavar-keystore-base64.txt` — نسخه Base64 برای قرار دادن در GitHub Secret.

### 2. ساخت GitHub Secrets

در Repository بروید به:

`Settings → Secrets and variables → Actions → Secrets → New repository secret`

چهار Secret بسازید:

- `YADAVAR_KEYSTORE_BASE64` → تمام محتوای `yadavar-keystore-base64.txt`
- `YADAVAR_STORE_PASSWORD` → رمز Keystore که هنگام ساخت وارد کردید
- `YADAVAR_KEY_ALIAS` → `yadavar`
- `YADAVAR_KEY_PASSWORD` → رمز Key که هنگام ساخت وارد کردید

GitHub این اطلاعات را به‌صورت Secret در اختیار Workflow قرار می‌دهد؛ آن‌ها را در فایل‌های پروژه یا لاگ Workflow قرار ندهید.

### 3. اولین نصب Release

چون گفتید نسخه قبلی را یک بار حذف می‌کنید، اولین Release این پروژه را بعد از تنظیم Secrets به‌عنوان نصب جدید روی گوشی نصب کنید.

بعد از آن، برای نسخه‌های آینده فقط `versionCode` را افزایش دهید و همان Secrets را نگه دارید. Android به‌طور معمول Update را وقتی می‌پذیرد که certificate نسخه جدید با نسخه نصب‌شده تطابق داشته باشد. اگر امضا همان باشد، داده‌های داخلی برنامه مثل داده‌های Room با نصب Update پاک نمی‌شوند.

### 4. پیدا کردن APK

بعد از اجرای موفق:

`Actions → Android CI → release → Artifacts → yadavar-release-apk`

فایل `app-release.apk` را دریافت و روی گوشی نصب کنید.

## نسخه فعلی

- `versionName = 1.2`
- `versionCode = 3`
- Room database version = 4

## نکته مهم درباره دیتابیس

`fallbackToDestructiveMigration()` عمداً حذف شده است تا برنامه در صورت اضافه شدن یک نسخه جدید دیتابیس بدون Migration، اطلاعات را خودکار حذف نکند. در تغییرات آینده دیتابیس باید Migration متناظر اضافه شود.

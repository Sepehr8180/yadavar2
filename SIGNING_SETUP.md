# تنظیم امضای پایدار Yadavar برای GitHub Actions

این پروژه از این نسخه به بعد APK نوع Release را با یک کلید ثابت می‌سازد. برای اینکه نسخه‌های بعدی Android را به‌عنوان Update قبول کند، همه نسخه‌های Release باید با همان certificate امضا شوند.

## مرحله 1 — ساخت کلید (بدون Android Studio)

روی Windows یک JDK نصب باشد و دستور `keytool` در دسترس باشد. PowerShell را در این پوشه باز کنید و اجرا کنید:

```powershell
keytool -genkeypair -v -keystore yadavar-release.jks -alias yadavar -keyalg RSA -keysize 4096 -validity 10000
```

رمز Keystore و رمز Key را نگه دارید. فایل `yadavar-release.jks` را هرگز به GitHub commit نکنید.

برای تبدیل فایل به Base64 در PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("yadavar-release.jks")) | Set-Content -NoNewline yadavar-keystore-base64.txt
```

## مرحله 2 — ساخت GitHub Secrets

در Repository:

`Settings → Secrets and variables → Actions → Secrets → New repository secret`

چهار Secret بسازید:

- `YADAVAR_KEYSTORE_BASE64` → محتوای `yadavar-keystore-base64.txt`
- `YADAVAR_STORE_PASSWORD` → رمز Keystore
- `YADAVAR_KEY_ALIAS` → `yadavar`
- `YADAVAR_KEY_PASSWORD` → رمز Key

این مقادیر را داخل فایل‌های پروژه قرار ندهید.

## مرحله 3 — Build

بعد از Push روی `main` یا اجرای دستی Workflow، GitHub ابتدا تست‌ها را اجرا می‌کند و سپس `assembleRelease` را با همان Keystore اجرا می‌کند. APK خروجی در:

`Actions → Android CI → release → Artifacts → yadavar-release-apk`

قرار می‌گیرد.

## انتشارهای بعدی

برای هر نسخه فقط `versionCode` را افزایش دهید. `versionCode` باید از نسخه قبلی بزرگ‌تر باشد. `versionName` را هم می‌توانید تغییر دهید.

این پروژه از `versionCode = 3` و `versionName = 1.2` شروع می‌شود.

Room نیز در حال حاضر روی نسخه 4 است و migrationهای موجود برای نسخه‌های قبلی حفظ شده‌اند. `fallbackToDestructiveMigration()` عمداً حذف شده تا در صورت فراموش شدن migration، اطلاعات به‌صورت خودکار حذف نشوند.

## هشدار مهم

Keystore و رمزهای آن را در جای امن نگه دارید. از دست رفتن کلید امضای برنامه می‌تواند مانع انتشار Update روی نسخه‌های قبلی شود.

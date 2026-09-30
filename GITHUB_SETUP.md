# راه‌اندازی GitHub برای Yadavar

## ساخت APK

Workflow موجود در `.github/workflows/android.yml` با JDK 21 اجرا می‌شود، تست‌ها را اجرا می‌کند و در صورت موفقیت `app-debug.apk` را به‌عنوان Artifact منتشر می‌کند.

از مسیر زیر می‌توانید اجرای دستی را شروع کنید:

`Actions → Android CI → Run workflow`

## فایل‌های محلی و Secretها

- `.env` برای کلید Gemini روی سیستم محلی است و نباید commit شود.
- `local.defaults.properties` فقط placeholder دارد و برای Build بدون کلید واقعی استفاده می‌شود.
- فایل `google-services.json` را در repository عمومی قرار ندهید مگر اینکه پروژه Firebase و محدودیت‌های کلیدهای آن را آگاهانه پیکربندی کرده باشید.

## ساخت Release

برای Release باید keystore را خارج از repository نگه دارید و اطلاعات signing را از GitHub Secrets وارد workflow کنید. این نسخه در حال حاضر فقط Debug APK تولید می‌کند.

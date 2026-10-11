# راه‌اندازی GitHub برای Yadavar

## Build فعلی

Workflow در `.github/workflows/android.yml` با JDK 21 اجرا می‌شود، تست‌های Unit را اجرا می‌کند و بعد از موفقیت یک **Release APK امضاشده** می‌سازد.

برای `pull_request` فقط تست‌ها اجرا می‌شوند. برای `push` روی `main` و `workflow_dispatch` تست‌ها اجرا می‌شوند و سپس Release APK ساخته می‌شود.

از مسیر زیر اجرای دستی را شروع کنید:

`Actions → Android CI → Run workflow`

## امضای پایدار برای Update

این نسخه از فایل ثابت `yadavar-release.jks` در ریشه repository استفاده می‌کند. بنابراین نیازی به ساخت Keystore روی کامپیوتر، نصب JDK، اجرای اسکریپت PowerShell یا ساخت GitHub Secrets نیست.

**فایل `yadavar-release.jks` را حذف یا جایگزین نکن.** همه نسخه‌های آینده باید با همین فایل ساخته شوند تا Android آن‌ها را Update نسخه نصب‌شده تشخیص بدهد.

### اجرای ساخت APK

`Actions → Android CI → Run workflow`

پس از موفقیت، از بخش `Artifacts` مورد `yadavar-release-apk` را دانلود و فایل `app-release.apk` را روی گوشی نصب کنید.

### ساخت نسخه جدید

در `app/build.gradle.kts`، برای هر نسخه جدید `versionCode` را یک واحد افزایش دهید و `versionName` را هم به‌روزرسانی کنید. نسخه فعلی `1.3` با `versionCode = 4` است؛ نسخه بعدی باید حداقل `versionCode = 5` داشته باشد.

امضای ثابت، شناسه برنامه و Migration دیتابیس باعث می‌شوند نصب Update معمولاً داده‌های داخلی برنامه را نگه دارد؛ برنامه را برای Update حذف نکنید.

## نسخه فعلی

- `versionName = 1.3`
- `versionCode = 4`
- Room database version = 5

## نکته مهم درباره دیتابیس

`fallbackToDestructiveMigration()` عمداً حذف شده است تا برنامه در صورت اضافه شدن یک نسخه جدید دیتابیس بدون Migration، اطلاعات را خودکار حذف نکند. در تغییرات آینده دیتابیس باید Migration متناظر اضافه شود.

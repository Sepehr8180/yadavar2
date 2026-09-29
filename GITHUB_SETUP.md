# راه‌اندازی روی GitHub

## 1) ساخت repository
در GitHub یک repository جدید بسازید. برای پروژه شخصی می‌تواند Private باشد.

## 2) Upload
کل محتوای همین پوشه را داخل repository قرار دهید، طوری که `settings.gradle.kts` و پوشه `app` در root repository باشند.

## 3) Push
پس از push، GitHub Actions فایل `.github/workflows/android.yml` را اجرا می‌کند.

## 4) گرفتن APK
از مسیر:
`Actions → Android CI → آخرین اجرا → Artifacts → yadavar-debug-apk`

## 5) نصب روی گوشی
APK را دانلود و روی گوشی نصب کنید. چون این نسخه Debug است، برای تست شخصی مناسب است.

## نکته
در این پروژه `gradlew` داخل فایل اولیه وجود نداشت، بنابراین workflow فعلاً Gradle 9.3.1 را با `gradle/actions/setup-gradle` آماده می‌کند. Android Studio نیز می‌تواند Gradle Wrapper را برای توسعه محلی مدیریت کند.

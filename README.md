# یادآور

اپ اندرویدی «یادآور» با Kotlin، Jetpack Compose، Room و AlarmManager ساخته شده است و از تقویم شمسی و ورودی متنی/صوتی برای ساخت یادآوری پشتیبانی می‌کند.

## ساخت APK با GitHub

این repository با GitHub Actions تنظیم شده است. بعد از push روی شاخه `main` یا اجرای دستی workflow:

1. وارد تب **Actions** شوید.
2. workflow با نام **Android CI** را باز کنید.
3. اجرای موفق را باز کنید.
4. در بخش **Artifacts** فایل `yadavar-debug-apk` را دریافت کنید.
5. فایل `app-debug.apk` را روی گوشی اندرویدی نصب کنید.

این build برای استفاده شخصی/تست است و **release signing** ندارد.

## اجرای محلی با Android Studio

پروژه را در Android Studio باز کنید و اجازه دهید Gradle پروژه را sync کند. برای ساخت نسخه Debug، از Android Studio و گزینه **Build APK(s)** استفاده کنید.

پروژه از AGP 9.1.1 و Gradle 9.3.1 استفاده می‌کند و برای Build به JDK 17 نیاز دارد.

## نکته مهم درباره Gemini API

کلید API را داخل GitHub repository commit نکنید. فایل `.env` عمداً در `.gitignore` قرار گرفته است.

در معماری فعلی، اگر یک کلید Gemini داخل APK قرار بگیرد، قابل استخراج است؛ بنابراین برای انتشار عمومی بهتر است API از یک backend امن یا معماری مناسب Firebase استفاده کند. برای استفاده شخصی، می‌توانید کلید را روی خود دستگاه وارد کنید.

## ساخت نسخه Release

برای APK/AAB قابل انتشار در Google Play باید signing key خودتان را جداگانه نگهداری و در GitHub Secrets تنظیم کنید. فایل keystore نباید وارد repository شود.

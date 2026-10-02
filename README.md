# یادآور

اپ اندرویدی «یادآور» با Kotlin، Jetpack Compose، Room و AlarmManager ساخته شده است و از تقویم شمسی و ورودی متنی/صوتی برای ساخت یادآوری پشتیبانی می‌کند.

## امکانات فعلی

- یادآورهای زمان‌دار با آلارم واقعی گوشی
- یادآورهای بدون زنگ برای نگهداری در ماتریس آیزنهاور
- یادآورهای بدون تاریخ برای کارهایی که فقط باید در ماتریس باشند
- تکرار روزانه و روزهای هفته
- تقویم شمسی با نمایش هم‌زمان یادآورها، تولدها و اقساط/بدهی‌ها
- بخش مستقل مالی برای ثبت مبلغ، تاریخ سررسید و اقساط ماهانه
- علامت‌گذاری هر سررسید به‌عنوان پرداخت‌شده و نمایش مبلغ باقی‌مانده ماه
- ثبت تولد و دسترسی سریع از آیکون تولد در بالای برنامه
- پردازش متن/صوت با Gemini و اصلاح خودکار تشخیص روز هفته

## ساخت Release APK با GitHub

این repository با GitHub Actions تنظیم شده است. بعد از push روی شاخه `main` یا اجرای دستی workflow، تست‌ها اجرا می‌شوند و سپس یک **Release APK امضاشده** ساخته می‌شود.

1. به `Actions` بروید.
2. workflow با نام `Android CI` را باز کنید.
3. اجرای موفق را باز کنید.
4. در بخش `Artifacts` فایل `yadavar-release-apk` را دریافت کنید.
5. فایل `app-release.apk` را روی گوشی نصب کنید.

برای امضای Release باید یک بار Secretهای signing را مطابق `SIGNING_SETUP.md` یا `GITHUB_SETUP.md` تنظیم کنید.

## Update روی گوشی

این پروژه از یک signing key ثابت استفاده می‌کند. بعد از اولین نصب Release، نسخه‌های بعدی با همان certificate ساخته می‌شوند و با افزایش `versionCode` می‌توانند به‌عنوان Update نصب شوند. داده‌های Room در این حالت باقی می‌مانند؛ به شرط اینکه برای تغییرات آینده دیتابیس Migration مناسب اضافه شود.

## ساخت نسخه جدید

برای هر Release بعدی:

```text
versionCode = 4
versionName = "1.3"
```

و بعد push روی `main` کافی است.

## نسخه فعلی

App version: **1.2** (`versionCode 3`). Database version: **4**.

## نکته مهم درباره Gemini API

کلید API را داخل GitHub repository commit نکنید. فایل `.env` عمداً در `.gitignore` قرار گرفته است.

در معماری فعلی، اگر یک کلید Gemini داخل APK قرار بگیرد، قابل استخراج است؛ بنابراین برای انتشار عمومی بهتر است API از یک backend امن یا معماری مناسب Firebase استفاده کند. برای استفاده شخصی، می‌توانید کلید را روی خود دستگاه وارد کنید.


## Release updates
This repository contains a fixed release keystore used by GitHub Actions. Keep `yadavar-release.jks` unchanged. Increase `versionCode` for future updates.

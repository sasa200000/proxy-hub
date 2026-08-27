# 🔌 پروکسی هاب و تستر کانفیگ

ابزار تخصصی دریافت، استخراج، تست سرعت و پینگ، جداسازی و فیلتر کانفیگ‌های V2Ray و پروکسی‌های تلگرام از کانال‌ها با قابلیت حذف خودکار کانفیگ‌های خراب.

## ✨ امکانات

- 📡 دریافت کانفیگ از کانال‌های تلگرام
- 🔍 استخراج و جداسازی کانفیگ‌های V2Ray
- ⚡ تست سرعت و پینگ پروکسی‌ها
- 🗑️ حذف خودکار کانفیگ‌های خراب و غیرفعال
- 📋 مدیریت و فیلتر پروکسی‌ها
- 📱 رابط کاربری مدرن با Jetpack Compose

## 🛠️ ساخت و اجرا

### پیش‌نیازها

- [Android Studio](https://developer.android.com/studio)
- Android SDK 24+ (minSdk)
- Android SDK 36 (targetSdk)

### مراحل اجرا

1. پروژه را در Android Studio باز کنید
2. یک فایل `.env` در ریشه پروژه بسازید و کلید API جمینای را قرار دهید:

```bash
GEMINI_API_KEY=your_api_key_here
```

3. خط `signingConfig = signingConfigs.getByName("debugConfig")` را از فایل `app/build.gradle.kts` حذف کنید
4. پروژه را روی ایمولاتور یا دستگاه فیزیکی اجرا کنید

## 🏗️ ساختار پروژه

```
app/src/main/java/com/example/
├── data/
│   ├── model/Entities.kt        # مدل‌های داده
│   ├── db/AppDatabase.kt        # پایگاه داده Room
│   ├── parser/ConfigParser.kt   # پارسر کانفیگ‌ها
│   ├── fetcher/ChannelFetcher.kt # دریافت از کانال‌ها
│   ├── tester/PingTester.kt     # تست پینگ
│   └── repository/ProxyRepository.kt
├── ui/
│   ├── screens/                 # صفحات اپلیکیشن
│   ├── components/              # کامپوننت‌های مشترک
│   ├── viewmodel/               # ViewModel
│   └── theme/                   # تم و رنگ‌ها
└── MainActivity.kt              # فعالیت اصلی
```

## 📄 لایسنس

MIT License

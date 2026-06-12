# <p align="center">📖 NovelVerse Android App</p>

<p align="center">
  <img src="https://img.shields.io/badge/Status-Active-brightgreen?style=for-the-badge" alt="Status">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Java_17-007396?style=for-the-badge&logo=java" alt="Language">
  <img src="https://img.shields.io/badge/Backend-Supabase-3ECF8E?style=for-the-badge&logo=supabase" alt="Backend">
  <img src="https://img.shields.io/badge/License-Apache_2.0-D22128?style=for-the-badge&logo=apache" alt="License">
</p>

---

## 🌟 Overview

**NovelVerse** is a comprehensive, production-ready novel reading and creation platform. Built with a robust **Java** architecture and powered by **Supabase**, it bridges the gap between passionate readers and creative authors.

### 👥 Who is it for?

| Role | Capabilities |
| :--- | :--- |
| **Readers** | Discover, read, and interact with a vast library of free and premium novels. |
| **Authors** | Write, publish, and manage stories with detailed audience analytics. |
| **Admins** | Moderate content, manage users, and ensure platform health. |

---

## 🚀 Key Features

### 📖 Immersive Reading
- **Customizable Themes**: Choose from Light, Dark, or Sepia modes for eye comfort.
- **Offline Mode**: Download chapters and read anytime, anywhere.
- **Progress Sync**: Never lose your place; progress syncs across all your devices.
- **TTS Support**: Listen to your favorite stories with integrated Text-to-Speech.

### ✍️ Author Empowerment
- **Powerful Dashboard**: Real-time analytics on views, ratings, and earnings.
- **Rich Editor**: A seamless writing experience with autosave and versioning.
- **Monetization**: Earn through a points system, subscriptions, and direct purchases.

### 🛠️ Platform Excellence
- **Secure Auth**: Support for Email/Password and Google Sign-In.
- **Social Interaction**: Comments, replies, and community reading groups.
- **Gamification**: Earn achievements and points as you read and interact.

---

## 🛠️ Tech Stack

### 📱 Android (Client)
- **Architecture**: MVVM + Repository Pattern for clean code separation.
- **DI**: [Dagger Hilt](https://dagger.dev/hilt/) for robust dependency injection.
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & OkHttp for reliable API communication.
- **Local Storage**: [Room](https://developer.android.com/training/data-storage/room) for high-performance caching.
- **Image Loading**: [Glide](https://github.com/bumptech/glide) for smooth image rendering.
- **Monetization**: Google Play Billing & AdMob integration.

### ☁️ Backend (Supabase)
- **Database**: PostgreSQL 15 for reliable data management.
- **Auth**: GoTrue for secure user sessions.
- **Storage**: S3-compatible storage for covers and avatars.
- **Realtime**: WebSockets for instant updates and notifications.

---

## 📂 Project Structure

```bash
com.novelverse.app/
├── 🗂️ data/           # Data layer (Local Room, Remote Supabase, Repositories)
├── 🏛️ domain/         # Domain layer (Models, Use cases, Utils)
├── 🎨 presentation/   # UI layer (Activities, Fragments, ViewModels)
├── 💉 di/             # Dagger Hilt modules
├── ⚙️ services/       # Background services (TTS, Download, FCM)
├── 👷 workers/        # WorkManager tasks
├── 💳 billing/        # Google Play Billing implementation
└── 🛡️ moderation/     # Content moderation logic
```

---

## 🏁 Quick Start

### 1️⃣ Clone & Open
```bash
git clone https://github.com/Spidroid-Technologies/NovelVerse.git
cd NovelVerse
```
Open the project in **Android Studio Hedgehog (2023.1.1)** or later.

### 2️⃣ Configure Backend
1. Create a project at [Supabase](https://supabase.com).
2. Run `supabase/schema.sql` in the SQL Editor.
3. Update `app/build.gradle` with your `SUPABASE_URL` and `SUPABASE_ANON_KEY`.

### 3️⃣ Build & Run
```bash
./gradlew installDebug
```

> [!TIP]
> For a detailed walkthrough, check out our [Setup Guide](SETUP_GUIDE.md).

---

## 📊 Performance Targets

| Metric | Target | Status |
| :--- | :--- | :---: |
| **Cold Start** | < 2.0s | ✅ |
| **Offline Load** | < 1.0s | ✅ |
| **App Size** | < 50MB | ✅ |
| **API Latency** | < 500ms | ✅ |
| **Crash-Free Rate** | > 99.5% | ✅ |

---

## 🤝 Contributing

We welcome contributions! Please follow these steps:
1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📜 License

Distributed under the **Apache License, Version 2.0**. See `LICENSE` for more information.

---

<p align="center">
  Built with ❤️ by the <b>NovelVerse Team</b>
</p>
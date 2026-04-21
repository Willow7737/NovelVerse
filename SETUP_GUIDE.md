# 🛠️ NovelVerse Setup Guide

<p align="center">
  <img src="https://img.shields.io/badge/Environment-Android_Studio-3DDC84?style=for-the-badge&logo=android-studio" alt="Environment">
  <img src="https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=java" alt="Java">
  <img src="https://img.shields.io/badge/Backend-Supabase-3ECF8E?style=for-the-badge&logo=supabase" alt="Backend">
</p>

---

## 📑 Table of Contents

1. [Development Environment](#1-development-environment)
2. [Supabase Backend](#2-supabase-backend)
3. [Firebase Integration](#3-firebase-integration)
4. [Google Sign-In](#4-google-sign-in)
5. [Google Play Billing](#5-google-play-billing)
6. [AdMob Setup](#6-admob-setup)
7. [App Configuration](#7-app-configuration)
8. [Building & Running](#8-building--running)

---

## 1️⃣ Development Environment

### 🛠️ Required Tools

| Tool | Version | Purpose |
| :--- | :--- | :--- |
| **Android Studio** | Hedgehog (2023.1.1)+ | Main IDE |
| **JDK** | 17+ | Java Runtime |
| **Git** | Latest | Version Control |

### ⚙️ Android Studio Configuration

1. Open **Android Studio**.
2. Go to `File > Settings > Appearance & Behavior > System Settings > Android SDK`.
3. Install:
   - Android SDK Platform 34
   - Android SDK Build-Tools 34
   - Android Emulator
   - Android SDK Platform-Tools

> [!IMPORTANT]
> Ensure your `JAVA_HOME` is set to JDK 17.

---

## 2️⃣ Supabase Backend

### 🚀 Create Project
1. Go to [Supabase](https://supabase.com) and sign up.
2. Click **New Project** and name it `novelverse-dev`.
3. Wait for the database to provision.

### 🗄️ Database Schema
1. Go to the **SQL Editor**.
2. Click **New Query**.
3. Paste the contents of `supabase/schema.sql`.
4. Click **Run**.

### 🔐 API Credentials
Go to **Settings > API** and copy:
- **Project URL**: `https://your-project.supabase.co`
- **Anon Public Key**: `eyJhbG...`

### 📦 Storage Buckets
Create these public buckets:
- `novel-covers`
- `character-avatars`
- `user-avatars`

> [!TIP]
> Run `supabase/storage_policies.sql` to automatically set up the correct permissions.

---

## 3️⃣ Firebase Integration

### 🔥 Create Project
1. Go to the [Firebase Console](https://console.firebase.google.com).
2. Create a new project named `novelverse-android`.
3. Add an Android app with package name `com.novelverse.app`.

### 📄 Google Services JSON
1. Download `google-services.json`.
2. Place it in the `app/` directory of your project.

### 🔑 Get SHA-1 Fingerprint
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

---

## 4️⃣ Google Sign-In

Setting up Google Sign-In requires configuring your project in the Google Cloud Console. Since you've already created a Firebase project in Step 3, a corresponding Google Cloud project already exists.

### 🛡️ Step 4.1: Configure OAuth Consent Screen

The **OAuth Consent Screen** is what users see when they are asked to log in with Google.

1.  Go to the [Google Cloud Console](https://console.cloud.google.com).
2.  **Select your project**: Click the project dropdown in the top header (next to the "Google Cloud" logo) and select the project you created in Firebase (e.g., `novelverse-android`).
3.  **Navigate to APIs & Services**:
    - Click the **Hamburger Menu (≡)** in the top-left corner.
    - Hover over **APIs & Services** and select **OAuth consent screen**.
4.  **Choose User Type**: Select **External** and click **Create**.
5.  **App Information**:
    - **App name**: `NovelVerse`
    - **User support email**: Select your email address.
    - **Developer contact information**: Enter your email address.
    - Click **Save and Continue**.
6.  **Scopes**:
    - Click **Add or Remove Scopes**.
    - Select `.../auth/userinfo.email` and `.../auth/userinfo.profile`.
    - Click **Update** at the bottom, then **Save and Continue**.
7.  **Test Users**: (Optional for development) Add your own email if you want to test before publishing. Click **Save and Continue**, then **Back to Dashboard**.

### 🔑 Step 4.2: Create OAuth 2.0 Client IDs

You need two types of Client IDs: one for the Android app itself and one for the backend (Web) to verify tokens.

#### A. Android Client ID
1.  In the left sidebar, click **Credentials**.
2.  Click **+ Create Credentials** at the top and select **OAuth client ID**.
3.  **Application type**: Select **Android**.
4.  **Name**: `NovelVerse Android Client`
5.  **Package name**: `com.novelverse.app`
6.  **SHA-1 certificate fingerprint**: Paste the SHA-1 you generated in Step 3.
7.  Click **Create**.

#### B. Web Client ID (Required for Backend)
1.  Click **+ Create Credentials** again and select **OAuth client ID**.
2.  **Application type**: Select **Web application**.
3.  **Name**: `NovelVerse Web Client (Backend)`
4.  **Authorized JavaScript origins**: Add `https://your-project.supabase.co` (from Step 2).
5.  Click **Create**.
6.  **Important**: A dialog will appear with your **Client ID**. Copy this value—this is your `GOOGLE_WEB_CLIENT_ID` for Step 7.

---

## 5️⃣ Google Play Billing

### 💳 In-App Products
Create the following point packages in the Play Console:

| Product ID | Name | Price |
| :--- | :--- | :--- |
| `points_100` | 100 Points | $0.99 |
| `points_500` | 500 Points | $4.49 |
| `points_1000` | 1000 Points | $7.99 |

### 📅 Subscriptions
Create `premium_monthly` ($4.99) and `premium_yearly` ($39.99).

---

## 6️⃣ AdMob Setup

### 📺 Create Ad Units
1. Go to [AdMob](https://apps.admob.com).
2. Create a new app (Android).
3. Create:
   - **Banner Ad**: `HomeBanner`
   - **Rewarded Ad**: `EarnPoints`

### 📝 Update Manifest
Update `AndroidManifest.xml` with your **AdMob App ID**:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX" />
```

---

## 7️⃣ App Configuration

### 📄 build.gradle
Update `app/build.gradle` with your keys:
```gradle
android {
    defaultConfig {
        buildConfigField "String", "SUPABASE_URL", '"https://your-project.supabase.co"'
        buildConfigField "String", "SUPABASE_ANON_KEY", '"your-anon-key"'
        buildConfigField "String", "GOOGLE_WEB_CLIENT_ID", '"your-web-client-id"'
    }
}
```

---

## 8️⃣ Building & Running

### 🔨 Build Debug
```bash
./gradlew assembleDebug
```

### 📱 Run on Device
1. Enable **USB Debugging** on your Android device.
2. Connect and click **Run** in Android Studio.

---

## ❓ Troubleshooting

| Issue | Solution |
| :--- | :--- |
| **Gradle Sync Failed** | Check internet connection and Android Studio version. |
| **Supabase Error** | Verify your URL and Anon Key in `build.gradle`. |
| **Google Login Fails** | Ensure your SHA-1 is correct in both Firebase and Google Cloud. |

---

<p align="center">
  <b>Happy Coding! 🚀</b><br>
  For support, contact <a href="mailto:dev@novelverse.app">dev@novelverse.app</a>
</p>
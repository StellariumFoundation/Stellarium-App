# 🌟 Stellarium Foundation App

> **Do Good. Make Money. Have Fun.**

The official Android mobile application for the **Stellarium Foundation**. This app serves as a portal for high-profile advising in policy, finance, business, and relationships, empowering individuals through innovative solutions and strategic partnerships.

🌐 **Official Website:** [https://www.stellarium.ddns-ip.net/home](https://www.stellarium.ddns-ip.net/home)

---

## 📱 About The App

This application is built to disseminate the core principles of the Stellarium Foundation ("The Law") and provide tools for members to engage with our mission of global prosperity and social progress.

### Key Features
*   **📚 The Library:** Access specific chapters from the Stellarium Books, focusing on principles of Wealth, Peace, and the "Water" suite of products.
*   **🧠 Quiz Module:** Test your knowledge of the Foundation's principles with built-in interactive quizzes.
*   **🤝 Sponsorship:** detailed instructions on how to partner with us via Affiliate Marketing, Monero (XMR), or Banking/Pix.
*   **💬 Direct Contact:** Integrated messaging system to contact the Foundation directly.
*   **🚀 Highly Optimized:** Built with performance in mind, offering ultra-small APK sizes for specific architectures.

---

## 🏗 Tech Stack

This project is a modern **Android MVP** built entirely with **Kotlin** and **Jetpack Compose**.

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material3 Design)
*   **Navigation:** AndroidX Navigation Compose
*   **Minimum SDK:** Android 8.0 (API 26)
*   **Target SDK:** Android 15 (API 35)
*   **Build System:** Gradle (Kotlin DSL)
*   **CI/CD:** GitHub Actions (Automated Multi-arch Builds)

---

## 📥 Download & Installation

The app is automatically built and optimized for different device architectures every time we update the code.

1.  Go to the [**Releases**](../../releases) tab in this repository.
2.  Select the latest release.
3.  Download the APK that matches your device:
    *   **`Stellarium-Universal.apk`** (Recommended) - Works on all devices.
    *   **`Stellarium-ARM64.apk`** - Best for modern Android phones (Samsung S10+, Pixel, etc.).
    *   **`Stellarium-ARMv7.apk`** - For older or budget Android devices.

> **Note:** Since this is an internal release, you may need to "Allow installation from unknown sources" in your browser/file manager settings.

---

## 🛠 Local Development

To build this app locally, you need **Android Studio** (Koala or newer recommended).

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/YOUR_USERNAME/stellarium-app.git
    ```
2.  **Open in Android Studio.**
3.  **Sync Gradle.**
4.  **Run the App:**
    *   Connect your Android device via USB.
    *   Select `app` configuration.
    *   Click **Run**.

### ⚠️ Note on Emulators
The `build.gradle.kts` is configured with `abiFilters` to prioritize physical devices (ARM). If you want to run this on a standard PC Emulator (x86_64), ensure you select the **Universal** build variant or run the specific x86 split.

---

## 📜 The Law

The app is guided by the Foundation's core laws:

1.  **∆ Make Money:** Create value, do what you love, build thriving businesses.
2.  **∆ Have Fun:** Celebrate life, make friends, find happiness and self-fulfillment.
3.  **∆ Do Good:** Be benevolent, stand up, be a hero, and shine your light.

---

## 📄 License

Copyright © 2025 Stellarium Foundation. All Rights Reserved.

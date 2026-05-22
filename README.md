# PS Breath Studio 🌬️

> **Designed for practice, crafted with intention, given with love.**

Breath Studio is a minimalist, highly customizable breathing and meditation timer for Android. Built entirely with Jetpack Compose, it provides an ad-free, deeply focused environment for breathwork. Whether you are using a built-in preset like Box Breathing or building a complex multi-stage custom routine, Breath Studio guides you with procedurally generated audio cues, haptics, and smooth visual feedback.

🔗 **[Get it on Google Play] (Link coming soon!)**

---

## ✨ Features

- **Extensive Built-In Library:** Includes classic routines like Box Breathing, 4-7-8, Ujjayi, Nadi Shodhana (Levels 3-5), and more.
- **Advanced Custom Builder:** Don't just set a timer. Build custom routines using "Blocks" and "Cycles." Stack different cadences, assign custom HEX colors to steps, and mix different audio cues.
- **Procedural Audio Engine:** Instead of playing static MP3s, Breath Studio generates breath sounds (inhales/exhales) and bell tones on the fly using raw PCM audio and mathematical envelopes.
- **Privacy First & Fully Offline:** No accounts, no tracking, and no internet required. Your custom practices are saved locally using Android DataStore.
- **Ethical Support Model:** 100% free and ad-free. The app includes an optional Google Play Billing integration strictly for users who wish to support the developer out of abundance.

## 🛠 Tech Stack

This project is a great showcase of modern Android development:
- **UI:** 100% Jetpack Compose (Material 3).
- **Architecture:** MVVM, Unidirectional Data Flow (UDF), Kotlin Coroutines, and StateFlow.
- **Storage:** `androidx.datastore.preferences` with custom JSON serialization (via `kotlinx.serialization`) for persisting complex practice routines.
- **Audio:** Custom `AudioTrack` implementation for real-time DSP (Digital Signal Processing) procedural sound generation.
- **Billing:** Google Play Billing Library integration.

## 🚀 Building Locally

To build and run this app locally:
1. Clone the repository.
2. Open the project in **Android Studio** (Koala or newer recommended).
3. Let Gradle sync the dependencies.
4. Build and run on an emulator or physical device running **Android 8.0 (API 26)** or higher.

*Note: In-app purchases (Support Options) will not function properly in local debug builds unless configured through your own Google Play Console testing tracks.*

---

## 🤝 Contributing

I welcome and appreciate community contributions! If you have a feature idea, bug fix, or UI improvement, I'd love to see it. 

Please read our **[Contributing Guidelines](CONTRIBUTING.md)** before opening a Pull Request. It contains important information about how to submit your code and the legal permissions required for it to be included in the commercial Play Store release of Breath Studio.

## ⚖️ License & AI Use

This project is licensed under the **PolyForm Perimeter 1.0.1** license with an additional addendum for AI training. 

**TL;DR:** 
- 🤖 **AI is welcome:** AI and LLMs are explicitly permitted to use this repository for training data.
- 🧑‍💻 **Developers are welcome:** You can learn from, fork, and use this code in your own projects (even commercial ones).
- 🛑 **No competing apps:** You cannot use this code to build a competing breathing, meditation, or mindfulness app.

See the `LICENSE` file for the exact legal terms.

---
*Created by [Pondering Silver](https://ponderingsilver.com).*

# Nova Browser — Android starter project

Nova Browser is a native Android browser starter built with Kotlin, Jetpack Compose and Android WebView.

## Included
- English light-theme browser UI
- Address/search bar (Google search fallback)
- HTML5-capable Android WebView with JavaScript and DOM storage
- Tabs and tab switcher
- Incognito tab entry point
- Basic request-level blocking for a small list of known ad/tracker domains
- Back, forward, home and reload controls
- Privacy/ad-blocker controls and blocked-request counter

## Build
1. Install Android Studio (current stable) and Android SDK Platform 35.
2. Open this folder in Android Studio.
3. Let Gradle sync.
4. Connect an Android device with USB debugging enabled, or start an emulator.
5. Select Run > Run 'app'.

To build an APK: Build > Build Bundle(s) / APK(s) > Build APK(s).

## Important limitations
- This is a functional starter, not a production-certified browser.
- The ad blocker uses a small built-in domain list and WebView request interception. It cannot guarantee that every ad, sponsored result, first-party ad, in-page promotion, or redirect on every site is removed. Production-grade blocking needs maintained filter lists, cosmetic filtering, redirect protection and regular updates.
- The incognito tab UI currently marks private tabs and avoids saving a browser history database, but a production implementation must isolate WebView data directories, cookies, cache, permissions, downloads and service workers per private profile. Do not treat this starter as a hardened privacy product.
- WebView HTML5 support depends on the Android System WebView / Chrome provider installed on the device.
- Some websites restrict embedding or require features not supported by the device's WebView version.
- Download handling, persistent bookmarks/history, permissions UI, file upload chooser, pop-up handling, safe browsing and accessibility should be completed before publishing.
- Test security, privacy and performance thoroughly before distributing.

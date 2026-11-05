# Saját Menü

Native Android 8+ app, targeting Android 16. Starts empty. Add installed phone apps individually from the top-right ⋮ menu. Tap a tile to open its original app. The same ⋮ menu contains reorder, remove-from-menu, and refresh commands. Removal only deletes a saved shortcut, never uninstalls an app. The chosen order survives app restarts.

Only launchable activities in the current Android profile appear in the picker. Hidden/private/work profiles are outside this first version. No runtime permissions, internet, account, analytics, or app-content access. App list is queried locally only to populate the picker and resolve saved shortcuts. Unavailable shortcuts are retained for manual removal.

Program ID: `7a93c1e4b8056fd2`; local preferences file: `c2de45aa7f819b03`.

Build with Gradle 8.13, Java 21, Android SDK 36: `gradle assembleDebug lintDebug`. GitHub Actions also runs Android 16 UI tests for empty start, individual additions, picker search, real app launch, reorder, saved order, and shortcut removal without uninstalling. The APK is published as `distributions/Sajat-Menu.apk` only after these pass. Signing material is a public development-only debug key preserved for compatible updates.

# Yadavar release signing

This version uses a fixed release keystore stored in the repository so GitHub Actions can build an installable Release APK without Android Studio, JDK setup, or GitHub Secrets.

**Do not replace or delete `yadavar-release.jks`.** All future releases must keep using this exact file so Android accepts them as updates to the currently installed Yadavar release.

For a new release, increase `versionCode` (and optionally `versionName`) in `app/build.gradle.kts`, commit, push to `main`, and download the `yadavar-release-apk` artifact from GitHub Actions.

This setup is intentionally simple rather than secret. Anyone who can read the repository can potentially obtain the signing key.

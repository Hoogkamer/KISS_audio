# KISS Audio Release Preparation Plan

Preparing "KISS Audio" for public release requires technical hardening, compliance checks, and store presence setup.

## 1. Store Presence Assets
- [ ] **App Name**: Confirm "KISS Audio".
- [ ] **Short Description**: (max 80 chars) e.g., "A modern center for your Music, Radio, and Podcasts."
- [ ] **Full Description**: Detailed features, privacy highlights, and support info.
- [ ] **App Icon**: 512x512px, Transparent background (PNG/WEBP).
- [ ] **Feature Graphic**: 1024x500px.
- [ ] **Screenshots**: At least 2 for phone (16:9 or 9:16). Tablet screenshots recommended.

## 2. Legal & Compliance
- [ ] **Privacy Policy**: Hosted URL (GitHub Pages is a good free option).
- [ ] **Data Safety Form**: Declare that the app does not collect user data (if true).
- [ ] **Target Audience**: Confirm 13+ or 18+ (Music apps are usually 3+ but check for podcast content).

## 3. Technical Preparation
- [ ] **Upload Key**: Create a Keystore file (`.jks`). **BACK THIS UP SECURELY.**
- [ ] **App Signing**: Enroll in Play App Signing (standard for new apps).
- [ ] **Build Bundle**: Generate the `.aab` file using `./gradlew bundleRelease`.

## 4. Play Console Setup
- [ ] **Developer Account**: Pay the $25 one-time fee.
- [ ] **Create App**: Select "App" (not Game), "Free".
- [ ] **Internal Testing**: Upload first bundle to Internal Testing track to verify it works on real devices via the Store.

# KISS Audio - Technical Architecture

KISS Audio is an Android-native audio center built on the **KISS Principle** (Keep It Simple, Stupid). It is designed to turn any Android device into a dedicated, state-persistent audio hub.

## 1. Overview

KISS Audio is an Android application designed for independent management of three distinct audio categories: Music, Radio, and Podcasts. It focuses on offline reliability, context-specific state management, and a high-visibility UI.

## 2. Core Architecture

The application follows a modular design where each audio type maintains its own independent state while sharing a common media engine. KISS Audio achieves this through an MVI-based (Model-View-Intent) state architecture and a decoupled Media3 (ExoPlayer) service layer.

### A. Media Engine (Media3 / ExoPlayer)
- **Service-First Design:** The `MusicService` (extending `MediaSessionService`) owns the `ExoPlayer` instance. This allows playback to continue even when the UI is not visible or when the app is in Launcher mode.
- **State Synchronization:** The `PlayerViewModel` acts as a bridge, observing the `MediaController` and exposing the current playback state (position, duration, metadata) to the UI.

### B. State Management (MVI)
Each module (Music, Radio, Podcasts) has its own state object:
- **`MusicState`**: Tracks the active "Deck" (folder), file list, and current track index.
- **`RadioState`**: Tracks station lists and search results.
- **`PodcastState`**: Tracks subscriptions, episode lists, and download progress.
- **`PlayerUIState`**: A unified bridging object that maps module-specific states into a single, standardized format for the `UnifiedPlayer`.

---

## 3. UI Architecture (The "Tonmeister" Aesthetic)

KISS Audio employs a strict functional minimalist design inspired by Dieter Rams and early Braun audio equipment. 
- **UnifiedPlayer:** Instead of separate UI components for Music, Radio, and Podcasts, all playback is routed through a single `UnifiedPlayer` component. This ensures 100% visual consistency.
- **Precision Typography & Layout:** Uses a "scored grid" layout, monospace typography for numerical data (like timestamps), and a "Tuner Needle" style progress bar.
- **Color Discipline:** The application uses "Techno-Neutrals" (Matte Charcoal, Signal Gray). The signature **Braun Orange** is strictly reserved for the primary Play/Pause action and active states.

---

## 4. Data Model

### Audio Channels (Persisted in Room)
Every audio source is treated as an `AudioChannel`.
- `ChannelType.FOLDER`: Represents a Music Deck.
- `ChannelType.RADIO`: Represents a streaming station.
- `ChannelType.PODCAST`: Represents an RSS subscription.

---

## 4. Module Specifications

### Music (Decks)
The Music module is built for **Folder-Based Playback**. 
- Users pick a directory via the System Access Framework (SAF).
- The app scans for audio files and creates a local queue.
- **Position Persistence:** Every Deck saves its `currentTrackIndex` and `currentPositionMs` when playback stops.

### Radio (Stations)
- **Station Discovery:** Uses the Radio Browser API for global station search.
- **Metadata:** Real-time ICY metadata extraction from streams to show current track/artist.

### Podcasts (Subscriptions)
- **RSS Parsing:** Custom XML parser for podcast feeds.
- **Background Downloads:** Uses `WorkManager` for reliable, battery-efficient episode downloads.
- **Storage Management:** Automatic deletion of finished episodes.

---

## 5. Hub & Launcher Integration

KISS Audio is designed to be the "Center" of the device.
- **Launcher Mode:** Configured with `CATEGORY_HOME` to serve as a distraction-free home screen.
- **System Integration:** Includes a dedicated utility to launch the system clock/alarm, making it ideal for bedside audio setups.

---

## 6. Playback State & Navigation Logic

A critical part of the KISS philosophy is that the app should always remember "where you are."

### Single Source of Truth for Playback State
- **`isPlaying`:** Derived exclusively from the ExoPlayer `Player.Listener.onIsPlayingChanged()` callback. Never set optimistically.
- **Media Type Detection:** The `currentMediaType()` helper inspects the current `mediaId` format (integer = podcast episode, matching `streamUrl` = radio, else = music) rather than relying on the asynchronously-updated `lastCategory` config. This prevents state updates from being routed to the wrong module during category transitions.
- **Non-Destructive Tab Switching:** Navigating between Music, Radio, and Podcasts tabs only updates the `lastCategory` in config. Audio continues playing. Starting a *new* playback in a *different* module implicitly stops the current one via `loadChannelIntoPlayer()`.

### State-Based Navigation
The UI state (`NavigationDestination`) and module-specific navigation (e.g., `PodcastNavigation`) are decoupled from the media engine but react to it.
- **Auto-Navigation:** When a podcast episode is triggered, the app navigates to the `EpisodeDetailScreen`.
- **Context Resumption:** If you switch from Podcasts to Radio and back, the app remains on the last viewed screen for that module.
- **Episode Completion Navigation:** When `activeEpisode` becomes `null` (cleared by `cleanupAfterEpisodeEnd()`), the UI `LaunchedEffect` navigates back from `EPISODE_DETAIL` to the previous context (Dashboard or Show Detail).

### Cleanup Rituals
- **Episode Completion (unified codepath):** When an episode finishes, the app follows this sequence:
    1. `PodcastRepository.updatePlaybackPosition()` detects < 2 seconds remaining and sets `isFinished = true` in the database (single source of truth for completion).
    2. The position update loop in `PlayerViewModel` detects the auto-finish and calls `cleanupAfterEpisodeEnd()`.
    3. `cleanupAfterEpisodeEnd()` stops ExoPlayer, clears the Media3 session, sets `activeEpisode = null`, resets `activeChannelId`, and clears `isPlaying`.
    4. The file is deleted when `markAsPlayed()` is subsequently called.
    5. As a safety net, `Player.STATE_ENDED` also calls `cleanupAfterEpisodeEnd()` if the position updater missed it.
- **Manual Mark as Played:** Calling `markEpisodeAsPlayed()` checks if the episode is the active one, and if so, runs `cleanupAfterEpisodeEnd()` before marking it in the repository.


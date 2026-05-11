# KISS Audio — State Architecture

## 1. Persistent Storage (Room DB)

### `audio_channels` table — `AudioChannel` entity
One table for **all three channel types** (FOLDER, RADIO, PODCAST).

| Field | Used by | Notes |
|---|---|---|
| `id` | all | Primary key |
| `name` | all | Display name |
| `type` | all | `FOLDER`, `RADIO`, or `PODCAST` |
| `folderUri` | FOLDER, PODCAST | Folder path |
| `folderDisplayName` | FOLDER | Human-readable folder name |
| `streamUrl` | RADIO, PODCAST | HTTP stream / RSS feed URL |
| `currentTrackUri` | FOLDER | URI of last played file |
| `currentTrackIndex` | FOLDER | Index in playlist |
| `currentTrackTitle` | FOLDER, PODCAST | Title of last played item |
| `currentTrackArtist` | FOLDER | Artist from ID3 tag (may be null) |
| `currentTrackAlbum` | FOLDER | Album from ID3 tag (may be null) |
| `currentPositionMs` | FOLDER, PODCAST | Resume position |
| `currentTrackDurationMs` | FOLDER, PODCAST | Duration |
| `shuffleEnabled` | FOLDER | Shuffle mode |
| `repeatEnabled` | FOLDER | Repeat mode |
| `lastPlayedTime` | all | Used for sorting |

> **Important:** `currentTrackArtist` / `currentTrackAlbum` are written back to the DB row by
> `saveCurrentPlaybackState()` using `artist ?: channel.currentTrackArtist` — meaning if the
> current track has no artist, the *previous* track's artist is preserved in the DB. When files
> have no ID3 tags, `artist` is always null, so the field retains whatever was there before —
> including values that may have been written by a different channel type.

### `podcast_episodes` table — `PodcastEpisode` entity
Separate table. Each episode has: `id`, `channelId`, `title`, `podcastTitle`, `streamUrl`,
`localPath`, `playbackPositionMs`, `durationMs`, `isFinished`, `isDownloading`, etc.

### `app_config` table — `AppConfig` entity
Single-row config. Key fields:

| Field | Purpose |
|---|---|
| `lastCategory` | `"MUSIC"` / `"RADIO"` / `"PODCASTS"` — last active tab |
| `activeMusicChannelId` | Last used music deck ID |
| `activeRadioChannelId` | Last used radio station ID |
| `activePodcastChannelId` | Last used podcast channel ID |
| `activePodcastEpisodeId` | Episode currently being listened to |
| `hidePlayedEpisodes` | UI filter |
| `showOnlyInProgressPodcasts` | UI filter |

---

## 2. ViewModel In-Memory State (`PlayerViewModel`)

### Shared / cross-cutting flows

| StateFlow | Type | Set by | Read by |
|---|---|---|---|
| `_activeChannelId` | `Int?` | `selectChannel()`, `playPodcastEpisode()`, `cleanupAfterEpisodeEnd()` | Mini player visibility, mini player content routing, `saveCurrentPlaybackState()` |
| `_isPlaying` | `Boolean` | `onIsPlayingChanged` listener | Mini player status dot, play/pause icon |
| `_currentMediaId` | `String?` | `updateCurrentTrackInfo()`, `loadChannelIntoPlayer()` | Mini player button (podcast reload guard) |
| `_playbackSpeed` | `Float` | `onPlaybackParametersChanged` listener | Podcast player speed display |
| `_appConfig` | `AppConfig` | DB flow collector in `init {}` | `updateCurrentTrackInfo()` (uses `lastCategory`), `onPlayerError()` |
| `allChannels` | `List<AudioChannel>` | DB flow (live) | Mini player content, DeckCard list, all dashboard views |

### `_musicState: MusicState`

| Field | Set by | Meaning |
|---|---|---|
| `activeChannel` | `selectChannel()` FOLDER branch, `saveCurrentPlaybackState()`, DB config collector | The `AudioChannel` DB row for the active deck |
| `audioFiles` | `loadChannelIntoPlayer()` after folder scan | List of `AudioFile` scanned from the folder |
| `isLoading` | `loadChannelIntoPlayer()` | Spinner during folder scan |
| `currentTrackName` | `loadChannelIntoPlayer()` (from DB), `updateCurrentTrackInfo()`, `onMediaMetadataChanged()` | Display title — filename if no ID3 title tag |
| `currentTrackArtist` | `loadChannelIntoPlayer()` (from DB), `updateCurrentTrackInfo()`, `onMediaMetadataChanged()` | Display artist — `null` if no ID3 artist tag |
| `currentTrackAlbum` | same as above | Display album |
| `currentTrackIndex` | `updateCurrentTrackInfo()` | Index in playlist |
| `positionMs` | `startPositionUpdates()` loop (every 500ms) | Live playback position |
| `durationMs` | `startPositionUpdates()`, `onPlaybackStateChanged` STATE_READY | Track duration |
| `shuffleEnabled` | `toggleShuffle()` | Shuffle mode |
| `repeatEnabled` | `toggleRepeat()` | Repeat mode |

### `_radioState: RadioState`

| Field | Set by | Meaning |
|---|---|---|
| `activeChannel` | `selectChannel()` RADIO branch, `saveCurrentPlaybackState()`, DB config collector | The `AudioChannel` DB row for active station |
| `streamMetadata` | `onMediaMetadataChanged()`, `onPlaybackStateChanged()`, `updateCurrentTrackInfo()` | ICY stream metadata ("Artist - Title") |
| `searchResults` | `searchRadioStations()` | Radio Browser API results |

### `_podcastState: PodcastState`

| Field | Set by | Meaning |
|---|---|---|
| `activeChannel` | `selectChannel()` PODCAST branch, DB config collector | The `AudioChannel` DB row for active podcast |
| `activeEpisode` | `setActiveEpisode()` → `observeActiveEpisode()` (live DB flow), `cleanupAfterEpisodeEnd()` | Currently selected episode; **driven by a live DB flow job (`activeEpisodeJob`)** |
| `episodes` | `podcastEpisodesJob` (live DB flow per channel) | Episode list for the currently viewed show |
| `recentEpisodes` | `init {}` (live DB flow, always active) | Recent episodes across all shows |
| `currentTrackName` | `onMediaMetadataChanged()`, `setActiveEpisode(null)` | Display title (from ExoPlayer metadata) |
| `currentTrackArtist` | `onMediaMetadataChanged()`, `setActiveEpisode(null)` | Display artist (podcast title) |
| `positionMs` | `startPositionUpdates()` loop (every 500ms) | Live playback position |
| `durationMs` | `startPositionUpdates()`, `onPlaybackStateChanged` | Episode duration |
| `isPlayingActiveEpisode` | `onIsPlayingChanged()` | Whether ExoPlayer is playing this episode |
| `isRefreshing` / `refreshMessage` | `refreshAllPodcasts()`, `refreshShow()` | Feed refresh progress |
| `inProgressChannelIds` | `init {}` (live DB flow, always active) | Which shows have in-progress episodes |
| `searchResults` | `searchPodcasts()` | Podcast search results |

---

## 3. ExoPlayer (`MediaController`)

A single ExoPlayer instance (via `MusicService`) plays **all media types**. At any moment it holds either:
- A **playlist** of music `MediaItem`s (for FOLDER), each with `mediaId = file.uri.toString()`
- A **single** radio `MediaItem`, with `mediaId` not set (defaults to empty string or stream URL)
- A **single** podcast `MediaItem`, with `mediaId = episode.id.toString()` (integer string)

### `currentMediaType()` — how the code determines what's playing
```kotlin
private fun currentMediaType(): String? {
    val mediaId = mediaController?.currentMediaItem?.mediaId ?: return null
    return when {
        mediaId.toIntOrNull() != null -> "PODCASTS"   // episode IDs are integers
        _radioState.value.activeChannel?.streamUrl == mediaId -> "RADIO"
        else -> "MUSIC"
    }
}
```
Used by: `onMediaMetadataChanged`, `onPlaybackStateChanged`, `startPositionUpdates`.  
**Not used by:** `updateCurrentTrackInfo()` and `onPlayerError()` — these still use `_appConfig.value.lastCategory`.

---

## 4. Key Functions and What They Touch

### `selectChannel(channelId, autoPlay)`
1. Cancels any pending `playerLoadJob`
2. Calls `saveCurrentPlaybackState()` (saves current position before switching)
3. Sets `_activeChannelId = channelId`
4. Routes by type:
   - **FOLDER:** sets `_musicState.activeChannel`, writes `lastCategory = "MUSIC"` to DB, sets `_podcastState.activeEpisode = null` *(direct state update only — does NOT cancel `activeEpisodeJob`)*
   - **RADIO:** sets `_radioState.activeChannel`, writes `lastCategory = "RADIO"`, sets `_podcastState.activeEpisode = null` *(same caveat)*
   - **PODCAST:** sets `_podcastState.activeChannel`, writes `lastCategory = "PODCASTS"`
5. Calls `loadChannelIntoPlayer()`

### `loadChannelIntoPlayer(channel, autoPlay)`
1. Sets `isRefreshing = true` (blocks most listener callbacks)
2. For FOLDER: seeds `_musicState` with DB values (`currentTrackTitle`, `currentTrackArtist`, etc.)
3. Stops and clears ExoPlayer
4. Loads media into ExoPlayer:
   - FOLDER: scans folder, builds playlist, seeks to resume point
   - RADIO: loads stream URL
   - PODCAST: starts live DB flow for episode list, refreshes RSS feed (does NOT load into ExoPlayer — that's done by `playPodcastEpisode`)
5. `finally`: sets `isRefreshing = false`, calls `updateCurrentTrackInfo()` and `saveCurrentPlaybackState()`

### `playPodcastEpisode(episode)`
1. Calls `setActiveEpisode(episode)` → starts `activeEpisodeJob` live DB flow
2. Sets `_activeChannelId = episode.channelId`
3. Writes `lastCategory = "PODCASTS"` to DB
4. Loads episode into ExoPlayer with `mediaId = episode.id.toString()`
5. Calls `controller.play()`
6. `finally`: calls `updateCurrentTrackInfo()` and `saveCurrentPlaybackState()`

### `saveCurrentPlaybackState()`
Reads from `_activeChannelId`, finds the channel in DB, writes current ExoPlayer state back to that channel row:
```
currentTrackTitle  = mediaMetadata.title ?: channel.currentTrackTitle
currentTrackArtist = mediaMetadata.artist ?: channel.currentTrackArtist   ← stale fallback
currentTrackAlbum  = mediaMetadata.album  ?: channel.currentTrackAlbum    ← stale fallback
currentPositionMs  = controller.currentPosition
```
**No type-consistency check** — it writes to whatever channel `_activeChannelId` points to, regardless of what ExoPlayer is actually playing.

### `updateCurrentTrackInfo()`
Called on `onMediaItemTransition` and `onPlaybackStateChanged(STATE_READY)`.  
Routes metadata into the correct state bucket **using `_appConfig.value.lastCategory`** (not `currentMediaType()`):
- `"MUSIC"` → updates `_musicState.currentTrackName/Artist/Album/Index`
- `"RADIO"` → updates `_radioState.streamMetadata`
- `"PODCASTS"` → does nothing (podcast state driven by `activeEpisode`)

### `observeActiveEpisode(id)`
Cancels the previous `activeEpisodeJob`, then:
- If `id == null`: clears podcast display state
- If `id != null`: starts a **live DB flow** that keeps `_podcastState.activeEpisode` updated whenever the episode row changes in the DB

---

## 5. Mini Player (UI — `PlayerScreen.kt`)

### Content (title / artist / progress)
Correctly derived from `activeChannel.type` → reads from the right state bucket:
```kotlin
val activeChannel = allChannels.find { it.id == activeChannelId }
when (activeChannel?.type) {
    FOLDER  -> musicState.currentTrackName / musicState.currentTrackArtist
    RADIO   -> radioState.streamMetadata
    PODCAST -> podcastState.currentTrackName / podcastState.activeEpisode?.title
}
```

### Play/Pause button
```kotlin
val episode = podcastState.activeEpisode
if (episode != null && currentMediaId != episode.id.toString()) {
    viewModel.playPodcastEpisode(episode)   // ← unconditional: fires even when music is playing
} else {
    viewModel.playPause()
}
```
Does **not** check `activeChannel.type` before using the podcast episode reload logic.

### Navigation bar tab switching (MUSIC / RADIO / PODCASTS)
Only calls `viewModel.setCategory(...)` — updates `lastCategory` in DB config.  
Does **not** call `selectChannel()` — `_activeChannelId` and ExoPlayer state are unchanged.

### DeckCard (music deck list)
Reads directly from the `AudioChannel` DB object (via `allChannels` flow):
- `channel.currentTrackTitle` → last played track title
- `channel.currentTrackArtist` → last played artist (can be stale)

---

## 6. Known Issues (as of this writing)

1. **`activeEpisodeJob` not cancelled on tab switch to music/radio**  
   `selectChannel()` FOLDER/RADIO branches do `_podcastState.update { it.copy(activeEpisode = null) }` directly, but the live DB flow job (`activeEpisodeJob`) is still running and immediately re-populates `activeEpisode` from the DB. This makes the mini player play button believe a podcast is still active.

2. **Mini player button ignores `activeChannel.type`**  
   The play/pause button checks `podcastState.activeEpisode` unconditionally. Because of issue #1, `activeEpisode` is non-null while music is playing, so pressing the button triggers `playPodcastEpisode()` instead of `playPause()`.

3. **`updateCurrentTrackInfo()` uses `lastCategory` (async) not `currentMediaType()` (sync)**  
   `lastCategory` is updated via a DB write → DB flow → `_appConfig` collector chain. During any channel transition there is a race window where `lastCategory` still reflects the previous type, causing metadata to be routed into the wrong state bucket.

4. **`saveCurrentPlaybackState()` has no type-consistency guard**  
   It writes to the channel that `_activeChannelId` points to, regardless of what ExoPlayer is actually playing. During transitions these can be out of sync, causing podcast metadata to be written to a music channel's DB row (and vice versa).

5. **`loadChannelIntoPlayer()` seeds `_musicState` from stale DB values**  
   When a FOLDER channel loads, it copies `channel.currentTrackArtist` into `_musicState.currentTrackArtist`. If that DB field was corrupted by issue #4, the stale artist appears briefly until `onMediaMetadataChanged` overwrites it.

# Model: KISS audio

# === MODEL GUIDANCE ===
# Use EXACT matching for names and IDs when updating existing elements.
# Use '-' as a placeholder for fields that are not applicable to a specific type.
# Use pipe (|) as column separator.
# Do not change the header comments (lines starting with #).
# TYPE GUIDE:
# - Term (term): A concept, entity, or element in the domain
# - Concept (concept): An abstract idea or notion
# - Property (property): An attribute or characteristic of a term
# - Group (group): A visual container for grouping related terms
# - Label (label): A text label used for annotation
# - Connection Point (conpoint): A junction point for complex relations

## Terms
# name | description | additionalInformation
Core Concepts | The fundamental philosophy and rules of the application | -
KISS Principle | Keep It Simple, Stupid | Focus on core functionality without feature creep.
Privacy-First | Local-only data and processing | No user accounts, no tracking, no cloud synchronization.
Universal Resume | Every source remembers its position | Playback state is persisted per Deck/Podcast/Station independently.
Functional Minimalism | Design follows function | Inspired by Dieter Rams/BRAUN. High contrast, clear hierarchy.
Audio Hub | The device as a dedicated tool | Optimized for persistent, reliable background or foreground usage.
Context Separation | View != Playback | Navigation between modules must not interrupt active audio context.
Main Application | The root shell of the app | Inspired by beautiful BRAUN design language.
Track Detail View | The full-screen audio player page | -
Show Detail View | The page showing details of a specific podcast | -
Music Deck | A collection of local or remote audio tracks | Replaces the OS-specific Folder concept
Radio Station | An internet radio stream | -
Podcast Show | A subscribed podcast feed | -
Audio Track | A single playable media item | Music: File, Radio: Stream, Podcast: Episode
List Item Progress | Play/pause button with circular progress ring | <p>Podcast/Music: 0-100% circle.</p><p>Radio: No circle.</p>
Track Metadata | Name and Artist information | <p>Radio: Station/Artist.</p><p>Music: Deck/Track.</p><p>Podcast: Show/Episode.</p>
Player Progress Bar | Linear interactive timeline | <p>Radio: Hidden.</p><p>Music/Podcast: Show played and total time.</p>
Transport Controls | Forward/backward and Play/Pause | <p>Radio: Play/Pause only.</p><p>Music: Prev/Next track.</p><p>Podcast: -10s / +30s skip.</p>
Special Actions | Context-specific function icons | <p>Radio: None.</p><p>Music: Shuffle/Repeat.</p><p>Podcast: Speed, Download, Mark Played, Info.</p>
Mini Player | Persistent bottom player | Hides when an episode or music deck finishes completely.
Header | Header showing the selected view and link to system clock/alarm | -
Add Action | A button to add a new Deck/Radio/Podcast | <p>Music: pick source.</p><p>Radio/Podcast: search or add URL.</p>
Track List Item | A unified row/card representing an Audio Track | -
Show List Item | A unified row/card representing a Podcast Show | -
Recent Episodes View | A view aggregating recent podcast episodes | -
Bottom Navigation | Bar to switch between core modules | Music, Radio, Podcasts
Pull to Refresh | Gesture to fetch new data | Podcast: Recent episodes and Show Details.
Search Action | Search dialog for external feeds | Radio: Search stations. Podcast: Search iTunes API.
Bulk Import Action | Import multiple URLs via text/file | Radio and Podcast.
In-Progress Filter | Toggle to show only actively listened podcasts | Podcast Dashboard.
Hide Played Toggle | Action to hide completed episodes | Podcast Show Detail.
View Tabs | Switching between sub-views | Podcast: Shows vs Recent.
Delete Action | Remove a Deck, Station, or Podcast | -
Swipe to Mark Played | Swipe action with Undo capability | Podcast: Episodes.
Track Browser | View and select tracks within a Deck | Music Deck.
Active Playback Context | The global state of currently playing audio | Decoupled from the current UI View.
Resume Point | A saved timestamp and item ID | Persisted per-channel to allow "Universal Resume".
Auto-Cleanup | Logic to remove finished media files | Podcast: Deletion of played episodes.

## Relations
# source | relationName | target
Main Application | displays | Music Deck
Main Application | displays | Radio Station
Main Application | displays | Podcast Show
Main Application | displays | Recent Episodes View
Main Application | has | Header
Main Application | has | Add Action
Main Application | has | Mini Player
Music Deck | lists | Audio Track
Radio Station | lists | Audio Track
Podcast Show | lists | Show List Item
Podcast Show | has | Show Detail View
Recent Episodes View | lists | Audio Track
Show Detail View | lists | Audio Track
Show List Item | links to | Show Detail View
Track List Item | represents | Audio Track
Track List Item | links to | Track Detail View
Track List Item | has | List Item Progress
Track List Item | has | Track Metadata
Show List Item | has | Track Metadata
Track Detail View | has | Track Metadata
Track Detail View | has | Player Progress Bar
Track Detail View | has | Transport Controls
Track Detail View | has | Special Actions
Main Application | has | Bottom Navigation
Radio Station | has | Search Action
Radio Station | has | Bulk Import Action
Podcast Show | has | Search Action
Podcast Show | has | Bulk Import Action
Podcast Show | has | In-Progress Filter
Podcast Show | has | View Tabs
Show Detail View | has | Hide Played Toggle
Show Detail View | has | Pull to Refresh
Recent Episodes View | has | Pull to Refresh
Music Deck | has | Track Browser
Music Deck | has | Delete Action
Radio Station | has | Delete Action
Podcast Show | has | Delete Action
Track List Item | has | Swipe to Mark Played
Main Application | has | Core Concepts
Core Concepts | defines | KISS Principle
Core Concepts | defines | Privacy-First
Core Concepts | defines | Universal Resume
Core Concepts | defines | Functional Minimalism
Core Concepts | defines | Audio Hub
Core Concepts | defines | Context Separation
Main Application | manages | Active Playback Context
Active Playback Context | uses | Resume Point
Podcast Show | implements | Auto-Cleanup
Music Deck | implements | Resume Point
Radio Station | implements | Resume Point
Track Detail View | updates | Resume Point

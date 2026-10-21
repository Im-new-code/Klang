# Audio pipeline initialization and behavior

## Current path

    MediaStore indexed shared audio
          | query on worker thread
          v
    LocalAudioRepository -- Track(content URI + title/artist/album/duration/art URI)
          |
          v
    MainActivity -- MediaItem playlist -- MediaController
                                            |
                                            v
                           PortalPlaybackService / MediaSession
                                            |
                                            v
                                      Media3 ExoPlayer
                                            |
                                            v
                                        Android audio

## Scanner and metadata

LocalAudioRepository.scan performs the query away from the UI thread. The callback returns on the main thread. The Activity converts each Track into a MediaItem with title, artist and album metadata.

MediaStore exposes shared/indexed audio, not arbitrary app-private files. This approach avoids broad all-files permissions and finds music Android has indexed. If a file is unindexed, a future version can expose a Storage Access Framework folder import; it must not bypass scoped-storage rules.

The Android media index supplies title, artist, album, album ID and duration. It is not a universal parser for every ID3/Vorbis/MP4 metadata variant. A later metadata layer can supplement provider values with Media3 or MediaMetadataRetriever where supported. Artwork is loaded via a content URI and downsampled away from the main thread.

## Playback, transitions and interruptions

- PortalPlaybackService creates one ExoPlayer and one MediaSession and retains the player outside the Activity lifecycle.
- Audio focus is delegated to ExoPlayer through its AudioAttributes and focus handling. Platform focus changes and transient duck requests should be respected.
- setHandleAudioBecomingNoisy(true) handles common headset disconnects by pausing.
- Track changes use an ExoPlayer playlist. Klang does not release and recreate the engine between tracks. Gapless output depends on the source container, codec, decoder and device; measure before claiming all-format gapless or bit-perfect playback.
- Phone calls and other interruptions flow through Android audio-focus policy. Device-test ringing, call answer/end, navigation prompts and Bluetooth connect/disconnect.
- For Android versions that require a visible foreground playback notification, qualify MediaSessionService and notification behavior during release testing.
- ALAC, FLAC, M4A and WAV support depends on the platform decoder. Unsupported media should fail at one item, not crash the full library.

## Lyrics

LrcLyricsRepository is an optional LRCLIB request helper. The Activity does not invoke it automatically. The user should explicitly choose Find Lyrics. Send only the title and artist required for lookup, set connection/read timeouts, treat no-match/offline/error as normal outcomes, and cache LRC data locally after a parser and cache policy are added.

## Spatial DSP and projectM

Spatial audio and .milk shaders are not enabled in this build. An actual integration requires a pinned SDK/source version, CMake/NDK configuration for supported Android ABIs, GL-context ownership rules, thread-affinity tests, imported-preset validation, and license checks. Do not claim HRTF or projectM output based on placeholder source files.

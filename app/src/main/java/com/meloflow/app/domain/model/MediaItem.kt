package com.meloflow.app.domain.model

enum class MediaLanguage { ARABIC, FOREIGN, NONE }

/** How a provider says an item may be played. Add variants (HLS, DASH, provider SDK) as sources are integrated. */
sealed interface PlaybackSource {
    data class Url(val uri: String, val mimeType: String? = null) : PlaybackSource
}

/** A quality the source actually offers. Never list a quality the provider does not serve. */
data class VideoQuality(val heightPx: Int, val downloadUri: String? = null)

data class LicenseInfo(
    val name: String,
    val attribution: String? = null,
    val url: String? = null
)

data class MediaItem(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    /** Null when the provider does not report it; the player shows the real duration once loaded. */
    val durationMs: Long? = null,
    val language: MediaLanguage = MediaLanguage.NONE,
    val genreIds: List<String> = emptyList(),
    val sourceId: String,
    /** Null means "no authorized playback available"; the UI must say so instead of faking a link. */
    val playbackSource: PlaybackSource? = null,
    val availableQualities: List<VideoQuality> = emptyList(),
    val isDownloadAllowed: Boolean = false,
    val licenseInformation: LicenseInfo? = null
)

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String? = null
)

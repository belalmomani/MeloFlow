package com.meloflow.app.data.repository

import com.meloflow.app.domain.model.Artist
import com.meloflow.app.domain.model.LicenseInfo
import com.meloflow.app.domain.model.MediaItem
import com.meloflow.app.domain.model.MediaLanguage
import com.meloflow.app.domain.model.PlaybackSource
import com.meloflow.app.domain.repository.MediaSourceRepository
import javax.inject.Inject

/**
 * DEMO DATA ONLY. These are not real music videos.
 *
 * - The four "Demo Clip" items point at Blender Foundation open films (Creative Commons) hosted at a
 *   public sample bucket, used purely to exercise the player. Availability of those URLs is NOT guaranteed.
 * - The two Arabic items intentionally have no playback source, to exercise the "unavailable" state.
 * - Genres/languages are arbitrary test tags so the recommendation engine can be tested later.
 *
 * Replace by binding a real MediaSourceRepository in AppModule.
 */
class MockMediaSourceRepository @Inject constructor() : MediaSourceRepository {

    override val sourceId: String = SOURCE_ID
    override val isDemo: Boolean = true

    private val items: List<MediaItem> = listOf(
        demoClip("demo-bbb", "Big Buck Bunny (Demo Clip)", "BigBuckBunny.mp4", "instrumental", "CC BY 3.0", "bigbuckbunny.org"),
        demoClip("demo-ed", "Elephants Dream (Demo Clip)", "ElephantsDream.mp4", "ambient", "CC BY 2.5", "orange.blender.org"),
        demoClip("demo-sintel", "Sintel (Demo Clip)", "Sintel.mp4", "acoustic", "CC BY 3.0", "durian.blender.org"),
        demoClip("demo-tos", "Tears of Steel (Demo Clip)", "TearsOfSteel.mp4", "electronic", "CC BY 3.0", "mango.blender.org"),
        MediaItem(
            id = "demo-ar-1",
            title = "مقطع تجريبي بلا مصدر تشغيل ١",
            artist = "فنان تجريبي",
            language = MediaLanguage.ARABIC,
            genreIds = listOf("tarab"),
            sourceId = SOURCE_ID
        ),
        MediaItem(
            id = "demo-ar-2",
            title = "مقطع تجريبي بلا مصدر تشغيل ٢",
            artist = "فنان تجريبي",
            language = MediaLanguage.ARABIC,
            genreIds = listOf("oud"),
            sourceId = SOURCE_ID
        )
    )

    override suspend fun getFeatured(): Result<List<MediaItem>> = Result.success(items)

    override suspend fun search(query: String): Result<List<MediaItem>> {
        val q = query.trim()
        if (q.isEmpty()) return Result.success(emptyList())
        return Result.success(
            items.filter { it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) }
        )
    }

    override suspend fun getById(id: String): Result<MediaItem?> = Result.success(items.firstOrNull { it.id == id })

    override suspend fun searchArtists(query: String): Result<List<Artist>> {
        val q = query.trim()
        if (q.isEmpty()) return Result.success(emptyList())
        return Result.success(
            items.map { it.artist }.distinct()
                .filter { it.contains(q, ignoreCase = true) }
                .map { Artist(id = "demo-artist-$it", name = it) }
        )
    }

    private fun demoClip(
        id: String,
        title: String,
        file: String,
        genre: String,
        licenseName: String,
        site: String
    ) = MediaItem(
        id = id,
        title = title,
        artist = "Blender Foundation",
        language = MediaLanguage.FOREIGN,
        genreIds = listOf(genre),
        sourceId = SOURCE_ID,
        playbackSource = PlaybackSource.Url("$SAMPLE_BASE/$file", "video/mp4"),
        // TODO(Phase 9): single original file, no per-quality variants; handle "download original" case.
        isDownloadAllowed = true,
        licenseInformation = LicenseInfo(licenseName, "(c) Blender Foundation | $site")
    )

    companion object {
        const val SOURCE_ID = "mock"
        private const val SAMPLE_BASE = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample"
    }
}

package com.meloflow.app.domain.model

enum class GenreScope { ARABIC, GLOBAL }

data class Genre(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val scope: GenreScope
)

data class ListeningContext(
    val id: String,
    val labelAr: String,
    val labelEn: String
)

/** Static catalog used by onboarding. Extend by appending entries; ids are persisted, never rename them. */
object MusicCatalog {
    val genres: List<Genre> = listOf(
        Genre("oud", "العود", "Oud", GenreScope.ARABIC),
        Genre("tarab", "الطرب الأصيل", "Authentic Tarab", GenreScope.ARABIC),
        Genre("heritage", "الأغاني التراثية", "Heritage Songs", GenreScope.ARABIC),
        Genre("folk_ar", "الأغاني الشعبية", "Folk Songs", GenreScope.ARABIC),
        Genre("calm_ar", "الأغاني الهادئة", "Calm Songs", GenreScope.ARABIC),
        Genre("upbeat_ar", "الأغاني الحماسية", "Upbeat Songs", GenreScope.ARABIC),
        Genre("romantic_ar", "الأغاني الرومانسية", "Romantic Songs", GenreScope.ARABIC),
        Genre("classical_ar", "الموسيقى العربية الكلاسيكية", "Classical Arabic Music", GenreScope.ARABIC),
        Genre("anasheed", "الأناشيد", "Anasheed", GenreScope.ARABIC),
        Genre("pop", "Pop", "Pop", GenreScope.GLOBAL),
        Genre("rock", "Rock", "Rock", GenreScope.GLOBAL),
        Genre("jazz", "Jazz", "Jazz", GenreScope.GLOBAL),
        Genre("classical", "Classical", "Classical", GenreScope.GLOBAL),
        Genre("electronic", "Electronic", "Electronic", GenreScope.GLOBAL),
        Genre("acoustic", "Acoustic", "Acoustic", GenreScope.GLOBAL),
        Genre("ambient", "Ambient", "Ambient", GenreScope.GLOBAL),
        Genre("instrumental", "Instrumental", "Instrumental", GenreScope.GLOBAL),
        Genre("blues", "Blues", "Blues", GenreScope.GLOBAL),
        Genre("country", "Country", "Country", GenreScope.GLOBAL)
    )

    val listeningContexts: List<ListeningContext> = listOf(
        ListeningContext("work", "العمل", "Work"),
        ListeningContext("study", "الدراسة", "Study"),
        ListeningContext("driving", "القيادة", "Driving"),
        ListeningContext("relax", "الاسترخاء", "Relaxing"),
        ListeningContext("free_time", "أوقات الفراغ", "Free time")
    )

    fun genresFor(language: MusicLanguage): List<Genre> = when (language) {
        MusicLanguage.ARABIC -> genres.filter { it.scope == GenreScope.ARABIC }
        MusicLanguage.FOREIGN -> genres.filter { it.scope == GenreScope.GLOBAL }
        MusicLanguage.BOTH -> genres
    }
}

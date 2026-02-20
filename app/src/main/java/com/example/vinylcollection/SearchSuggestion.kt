package com.example.vinylcollection

/**
 * Représente une suggestion de recherche
 */
sealed class SearchSuggestion {
    abstract val text: String

    data class Artist(override val text: String) : SearchSuggestion()
    data class Title(override val text: String) : SearchSuggestion()
    data class Genre(override val text: String) : SearchSuggestion()

    data class Display(val icon: String, val text: String)

    fun getDisplay(): Display = when (this) {
        is Artist -> Display("🎤", text)
        is Title -> Display("💿", text)
        is Genre -> Display("🎵", text)
    }

    @Suppress("unused")
    fun getDisplayText(): String = getDisplay().text
}

package com.example.vinylcollection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class VinylViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VinylRepository(VinylDatabase.getInstance(application).vinylDao())
    private val query = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<Boolean?>(null)

    val vinyls: StateFlow<List<Vinyl>> = query
        .flatMapLatest { q ->
            statusFilter.flatMapLatest { status ->
                when (status) {
                    true -> repository.searchByStatus(q, true)
                    false -> repository.searchByStatus(q, false)
                    null -> repository.search(q)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ownedCount: StateFlow<Int> = repository.countOwned()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Recherche intelligente avec normalisation des accents
     */
    fun setQuery(text: String) {
        query.value = text.trim()
    }

    /**
     * Recherche avancée qui filtre côté Kotlin pour une correspondance plus souple
     * (insensible aux accents, espaces, etc.)
     * Priorise : correspondance exacte > début du titre > début de l'artiste > genre
     */
    @Suppress("unused")
    fun searchAdvanced(searchText: String): List<Vinyl> {
        val normalizedQuery = searchText.normalizeForSearch()
        if (normalizedQuery.isEmpty()) return vinyls.value

        return vinyls.value.filter { vinyl ->
            val normalizedTitle = vinyl.title.normalizeForSearch()
            val normalizedArtist = vinyl.artist.normalizeForSearch()
            val normalizedGenre = vinyl.genre.normalizeForSearch()

            normalizedTitle.contains(normalizedQuery) ||
            normalizedArtist.contains(normalizedQuery) ||
            normalizedGenre.contains(normalizedQuery)
        }.sortedWith(compareBy(
            // Prioriser les correspondances exactes de titre
            { !it.title.normalizeForSearch().equals(normalizedQuery) },
            // Puis les titres qui commencent par la requête
            { !it.title.normalizeForSearch().startsWith(normalizedQuery) },
            // Puis les correspondances exactes d'artiste
            { !it.artist.normalizeForSearch().equals(normalizedQuery) },
            // Puis les artistes qui commencent par la requête
            { !it.artist.normalizeForSearch().startsWith(normalizedQuery) },
            // Enfin trier alphabétiquement
            { it.artist.lowercase() },
            { it.title.lowercase() }
        ))
    }

    fun setStatusFilter(status: Boolean?) {
        statusFilter.value = status
    }

    /**
     * Génère des suggestions de recherche basées sur les données existantes
     * Retourne les 5 meilleures correspondances
     */
    @Suppress("unused")
    fun getSearchSuggestions(query: String): List<SearchSuggestion> {
        val normalizedQuery = query.normalizeForSearch()
        if (normalizedQuery.length < 2) return emptyList()

        val suggestions = mutableListOf<SearchSuggestion>()
        val allVinyls = vinyls.value

        // Suggérer des artistes
        allVinyls.map { it.artist }
            .distinct()
            .filter { it.normalizeForSearch().contains(normalizedQuery) }
            .take(3)
            .forEach { suggestions.add(SearchSuggestion.Artist(it)) }

        // Suggérer des titres
        allVinyls.map { it.title }
            .distinct()
            .filter { it.normalizeForSearch().contains(normalizedQuery) }
            .take(2)
            .forEach { suggestions.add(SearchSuggestion.Title(it)) }

        return suggestions.take(5)
    }

    fun add(vinyl: Vinyl) {
        viewModelScope.launch {
            repository.insert(vinyl)
        }
    }

    fun update(vinyl: Vinyl) {
        viewModelScope.launch {
            repository.update(vinyl)
        }
    }

    fun delete(vinyl: Vinyl) {
        viewModelScope.launch {
            repository.delete(vinyl)
        }
    }

    /**
     * Importe plusieurs releases Discogs en une seule opération
     * @param releases Liste des releases Discogs à importer
     * @param onProgress Callback appelé à chaque vinyle importé (index, total)
     * @param onComplete Callback appelé à la fin avec le résultat
     */
    @Suppress("unused")
    fun importMultipleFromDiscogs(
        releases: List<DiscogsManager.DiscogsRelease>,
        onProgress: ((Int, Int) -> Unit)? = null,
        onComplete: ((ImportResult) -> Unit)? = null
    ) {
        viewModelScope.launch {
            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<String>()

            releases.forEachIndexed { index, release ->
                try {
                    val vinyl = createVinylFromDiscogsRelease(release)
                    repository.insert(vinyl)
                    successCount++
                    onProgress?.invoke(index + 1, releases.size)
                } catch (e: Exception) {
                    errorCount++
                    errors.add("${release.title}: ${e.message}")
                }
            }

            onComplete?.invoke(ImportResult(successCount, errorCount, errors))
        }
    }

    /**
     * Crée un Vinyl à partir d'une release Discogs
     */
    private fun createVinylFromDiscogsRelease(release: DiscogsManager.DiscogsRelease): Vinyl {
        val rawTitle = release.title?.trim().orEmpty()
        val titleParts = rawTitle.split(Regex(" [-–] "), limit = 2)

        val artist: String
        val title: String

        if (titleParts.size == 2) {
            artist = titleParts[0].trim()
            title = titleParts[1].trim()
                .replace(Regex("\\s*\\([^)]*\\)\\s*$"), "")
                .replace(Regex("\\s*,\\s*[^,]*$"), "")
                .trim()
        } else {
            artist = ""
            title = rawTitle
        }

        return Vinyl(
            id = 0,
            title = title,
            artist = artist,
            year = release.year,
            genre = release.genre?.firstOrNull().orEmpty(),
            label = release.label?.firstOrNull().orEmpty(),
            rating = null,
            condition = "",
            notes = "Importé depuis Discogs",
            coverUri = null, // La cover sera téléchargée séparément si besoin
            isOwned = true
        )
    }
}

/**
 * Résultat d'une importation multiple
 */
@Suppress("unused")
data class ImportResult(
    val successCount: Int,
    val errorCount: Int,
    val errors: List<String>
) {
    val isSuccess: Boolean get() = errorCount == 0
    val totalCount: Int get() = successCount + errorCount
}

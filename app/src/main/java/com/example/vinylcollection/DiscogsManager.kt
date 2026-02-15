package com.example.vinylcollection

import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Manager pour l'API Discogs
 * Base de données mondiale de vinyles
 */
class DiscogsManager {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestWithHeaders = originalRequest.newBuilder()
                .header("User-Agent", "VinylCollection/1.0 (Android)")
                .build()
            chain.proceed(requestWithHeaders)
        }
        .build()

    private val discogsApi: DiscogsApi = Retrofit.Builder()
        .baseUrl("https://api.discogs.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DiscogsApi::class.java)

    /**
     * Résultat d'une recherche Discogs
     */
    data class DiscogsRelease(
        @SerializedName("id")
        val id: Long,
        @SerializedName("title")
        val title: String?,
        @SerializedName("year")
        val year: Int?,
        @SerializedName("label")
        val label: List<String>?,
        @SerializedName("genre")
        val genre: List<String>?,
        @SerializedName("style")
        val style: List<String>?,
        @SerializedName("format")
        val format: List<String>?,
        @SerializedName("resource_url")
        val resource_url: String?,
        @SerializedName("thumb")
        val thumb: String?,
        @SerializedName("cover_image")
        val cover_image: String?,
        @SerializedName("uri")
        val uri: String?,
        @SerializedName("uri150")
        val uri150: String?,
        @SerializedName("master_id")
        val master_id: Long?
    ) {
        /**
         * Retourne l'URL de l'image de couverture
         * Essaie dans l'ordre: cover_image > thumb > uri > uri150
         */
        fun getCoverUrl(): String? {
            return cover_image?.takeIf { it.isNotBlank() }
                ?: thumb?.takeIf { it.isNotBlank() }
                ?: uri?.takeIf { it.isNotBlank() }
                ?: uri150?.takeIf { it.isNotBlank() }
        }
    }

    data class DiscogsSearchResponse(
        @SerializedName("results")
        val results: List<DiscogsRelease>,
        @SerializedName("pagination")
        val pagination: PaginationInfo
    )

    data class PaginationInfo(
        @SerializedName("page")
        val page: Int,
        @SerializedName("pages")
        val pages: Int,
        @SerializedName("per_page")
        val per_page: Int,
        @SerializedName("items")
        val items: Int
    )

    /**
     * Détails complets d'un release Discogs
     */
    data class DiscogsReleaseDetail(
        @SerializedName("id")
        val id: Long,
        @SerializedName("title")
        val title: String,
        @SerializedName("year")
        val year: Int?,
        @SerializedName("labels")
        val labels: List<LabelInfo>,
        @SerializedName("genres")
        val genres: List<String>,
        @SerializedName("styles")
        val styles: List<String>,
        @SerializedName("formats")
        val formats: List<FormatInfo>,
        @SerializedName("artists")
        val artists: List<ArtistInfo>,
        @SerializedName("images")
        val images: List<ImageInfo>,
        @SerializedName("barcode")
        val barcode: String?,
        @SerializedName("resource_url")
        val resource_url: String
    )

    data class LabelInfo(
        @SerializedName("name")
        val name: String,
        @SerializedName("catno")
        val catno: String
    )

    data class FormatInfo(
        @SerializedName("name")
        val name: String,
        @SerializedName("qty")
        val qty: String,
        @SerializedName("descriptions")
        val descriptions: List<String>
    )

    data class ArtistInfo(
        @SerializedName("name")
        val name: String,
        @SerializedName("role")
        val role: String
    )

    data class ImageInfo(
        @SerializedName("type")
        val type: String,
        @SerializedName("uri")
        val uri: String,
        @SerializedName("uri150")
        val uri150: String?,
        @SerializedName("resource_url")
        val resource_url: String
    )

    /**
     * Rechercher un release par titre + artiste
     * Exemples: "Pink Floyd Dark Side", "The Beatles Help"
     */
    suspend fun searchRelease(
        query: String,
        type: String = "release"
    ): List<DiscogsRelease> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = discogsApi.search(
                q = query,
                barcode = null,
                type = type,
                per_page = 10
            )
            Log.d("Discogs", "Recherche trouvée: ${response.results.size} résultats")

            // Enrichir TOUS les résultats avec les images si manquantes
            val enrichedResults = response.results.map { release ->
                if (release.cover_image.isNullOrBlank() && release.thumb.isNullOrBlank()) {
                    try {
                        Log.d("Discogs", "📸 Récupération des images pour: ${release.title}")
                        val details = getReleaseDetail(release.id) ?: return@map release
                        val imageUrl = details.images.firstOrNull { it.type == "primary" }?.uri
                            ?: details.images.firstOrNull()?.uri
                        val thumbUrl = details.images.firstOrNull { it.type == "primary" }?.uri150
                            ?: details.images.firstOrNull()?.uri150

                        Log.d(
                            "Discogs",
                            "✅ Images récupérées: cover=${imageUrl?.take(50)}, thumb=${thumbUrl?.take(50)}"
                        )

                        release.copy(
                            cover_image = imageUrl,
                            thumb = thumbUrl
                        )
                    } catch (e: Exception) {
                        Log.e("Discogs", "❌ Erreur récupération images pour ${release.title}: ${e.message}")
                        release
                    }
                } else {
                    release
                }
            }

            // Logger les URLs d'images pour déboguer
            enrichedResults.forEachIndexed { index, release ->
                Log.d("Discogs", "[$index] ${release.title}")
                Log.d("Discogs", "  - cover_image: ${release.cover_image ?: "null"}")
                Log.d("Discogs", "  - thumb: ${release.thumb ?: "null"}")
                Log.d("Discogs", "  - getCoverUrl(): ${release.getCoverUrl() ?: "AUCUNE IMAGE"}")
            }

            enrichedResults
        } catch (e: Exception) {
            Log.e("Discogs", "Erreur recherche: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Rechercher par code-barre
     * Plus précis qu'une recherche textuelle
     */
    suspend fun searchByBarcodeResults(barcode: String): List<DiscogsRelease> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val response = discogsApi.search(
                    q = null,
                    barcode = barcode,
                    type = "release",
                    per_page = 10
                )
                Log.d("Discogs", "Recherche code-barres: ${response.results.size} résultats")

                val enrichedResults = response.results.map { release ->
                    if (release.cover_image.isNullOrBlank() && release.thumb.isNullOrBlank()) {
                        try {
                            val details = getReleaseDetail(release.id) ?: return@map release
                            val imageUrl = details.images.firstOrNull { it.type == "primary" }?.uri
                                ?: details.images.firstOrNull()?.uri
                            val thumbUrl = details.images.firstOrNull { it.type == "primary" }?.uri150
                                ?: details.images.firstOrNull()?.uri150

                            release.copy(
                                cover_image = imageUrl,
                                thumb = thumbUrl
                            )
                        } catch (e: Exception) {
                            Log.e("Discogs", "Erreur images code-barres: ${e.message}")
                            release
                        }
                    } else {
                        release
                    }
                }

                enrichedResults
            } catch (e: Exception) {
                Log.e("Discogs", "Erreur recherche code-barres: ${e.message}", e)
                emptyList()
            }
        }

    @Suppress("unused")
    suspend fun searchByBarcode(barcode: String): DiscogsRelease? {
        return searchByBarcodeResults(barcode).firstOrNull()
    }

    /**
     * Obtenir les détails complets d'un release
     * Appel séparé pour économiser la bande passante
     */
    suspend fun getReleaseDetail(releaseId: Long): DiscogsReleaseDetail? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val detail = discogsApi.getReleaseDetail(releaseId)
                Log.d("Discogs", "Détails chargés: ${detail.title}")
                detail
            } catch (e: Exception) {
                Log.e("Discogs", "Erreur détails: ${e.message}", e)
                null
            }
        }

    /**
     * Télécharger l'image de couverture
     * Enregistre dans le dossier local "covers"
     */
    suspend fun downloadCoverImage(
        imageUrl: String,
        context: android.content.Context
    ): java.io.File? = withContext(Dispatchers.IO) {
        return@withContext try {
            if (imageUrl.isBlank()) {
                Log.d("Discogs", "URL d'image vide, téléchargement annulé")
                return@withContext null
            }

            Log.d("Discogs", "Début téléchargement image: $imageUrl")

            val coversDir = java.io.File(context.filesDir, "covers")
            if (!coversDir.exists()) {
                coversDir.mkdirs()
                Log.d("Discogs", "Répertoire covers créé: ${coversDir.absolutePath}")
            }

            val file = java.io.File(coversDir, "discogs_${System.currentTimeMillis()}.jpg")

            val url = java.net.URL(imageUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "VinylCollection/1.0")

            connection.getInputStream().use { input ->
                file.outputStream().use { output ->
                    val bytes = input.copyTo(output)
                    Log.d("Discogs", "Image téléchargée: $bytes octets")
                }
            }

            Log.d("Discogs", "Image sauvegardée: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("Discogs", "Erreur téléchargement image depuis $imageUrl: ${e.message}", e)
            null
        }
    }
}

/**
 * Interface Retrofit pour l'API Discogs
 */
interface DiscogsApi {

    @GET("database/search")
    suspend fun search(
        @Query("q") q: String? = null,
        @Query("barcode") barcode: String? = null,
        @Query("type") type: String = "release",
        @Query("per_page") per_page: Int = 10
    ): DiscogsManager.DiscogsSearchResponse

    @GET("releases/{releaseId}")
    suspend fun getReleaseDetail(
        @Path("releaseId") releaseId: Long
    ): DiscogsManager.DiscogsReleaseDetail
}

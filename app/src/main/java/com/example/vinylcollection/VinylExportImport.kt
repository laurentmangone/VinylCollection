package com.example.vinylcollection

import android.content.Context
import androidx.core.net.toUri
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.util.Base64

/**
 * Data class pour l'export JSON
 * Contient les vinyls + metadata
 */
@JsonClass(generateAdapter = true)
data class VinylCollectionExport(
    val version: Int = 1,
    val exportDate: String,
    val totalVinyls: Int,
    val vinyls: List<VinylExportItem>
)

/**
 * Vinyl avec cover encodée en base64
 */
@JsonClass(generateAdapter = true)
data class VinylExportItem(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val year: Int?,
    val genre: String,
    val label: String,
    val rating: Int?,
    val condition: String,
    val notes: String,
    val coverBase64: String? = null,  // Image encodée en base64
    val isOwned: Boolean = true
)

/**
 * Manager pour export/import de collection Vinyl en JSON
 */
class VinylExportImport(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val exportAdapter = moshi.adapter(VinylCollectionExport::class.java)
        .serializeNulls()  // Force la sérialisation des valeurs null

    /**
     * Exporte tous les vinyls en JSON avec covers encodées
     */
    fun exportToJson(
        vinyls: List<Vinyl>,
        destinationFile: File? = null
    ): Result<File> = runCatching {
        android.util.Log.d("VinylExport", "Starting export of ${vinyls.size} vinyls")

        // Log les vinyls avec covers
        vinyls.forEachIndexed { index, vinyl ->
            android.util.Log.d("VinylExport", "[$index] ${vinyl.title} - coverUri: ${vinyl.coverUri ?: "NONE"}")
        }

        // Convertir les vinyls avec covers en base64
        val exportItems = vinyls.map { vinyl ->
            android.util.Log.d("VinylExport", "Processing: ${vinyl.title}")
            android.util.Log.d("VinylExport", "  - coverUri: '${vinyl.coverUri}'")
            android.util.Log.d("VinylExport", "  - coverUri isEmpty: ${vinyl.coverUri?.isEmpty()}")
            android.util.Log.d("VinylExport", "  - coverUri isBlank: ${vinyl.coverUri?.isBlank()}")

            VinylExportItem(
                id = vinyl.id,
                title = vinyl.title,
                artist = vinyl.artist,
                year = vinyl.year,
                genre = vinyl.genre,
                label = vinyl.label,
                rating = vinyl.rating,
                condition = vinyl.condition,
                notes = vinyl.notes,
                isOwned = vinyl.isOwned,
                coverBase64 = vinyl.coverUri?.let { uri ->
                    try {
                        android.util.Log.d("VinylExport", "Attempting to read cover from: $uri")

                        // Essayer d'abord comme chemin fichier classique
                        val file = File(uri)
                        val bytes = if (file.exists() && file.isFile) {
                            android.util.Log.d("VinylExport", "Reading as file path: $uri")
                            file.readBytes()
                        } else if (uri.startsWith("content://")) {
                            // Si c'est une URI content://, utiliser ContentResolver
                            android.util.Log.d("VinylExport", "Reading as content URI: $uri")
                            val contentUri = uri.toUri()
                            context.contentResolver.openInputStream(contentUri)?.use { input ->
                                input.readBytes()
                            } ?: throw Exception("Cannot open stream for $uri")
                        } else {
                            throw Exception("Invalid URI format: $uri")
                        }

                        val encoded = Base64.getEncoder().encodeToString(bytes)
                        android.util.Log.d("VinylExport", "Cover encoded for ${vinyl.title}: ${bytes.size} bytes -> ${encoded.length} chars")
                        encoded
                    } catch (e: Exception) {
                        android.util.Log.e("VinylExport", "Error encoding cover for ${vinyl.title}: ${e.message}", e)
                        null
                    }
                }
            )
        }

        // Créer le fichier d'export
        val export = VinylCollectionExport(
            exportDate = System.currentTimeMillis().toString(),
            totalVinyls = vinyls.size,
            vinyls = exportItems
        )

        // Sérialiser en JSON
        val json = exportAdapter.toJson(export)

        val backupsDir = File(context.filesDir, "backups")
        if (!backupsDir.exists()) {
            backupsDir.mkdirs()
        }

        // Écrire dans le fichier
        val file = destinationFile ?: File(
            backupsDir,
            "vinyl_collection_${System.currentTimeMillis()}.json"
        )

        file.writeText(json)
        file
    }

    /**
     * Importe une collection JSON et retourne les vinyls
     */
    fun importFromJson(file: File): Result<List<Vinyl>> = runCatching {
        val json = file.readText()
        val export = exportAdapter.fromJson(json)
            ?: throw Exception("Format JSON invalide")

        val coversDir = File(context.filesDir, "covers")
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }

        export.vinyls.map { item ->
            // Décoder la cover en base64 si présente
            val coverUri = item.coverBase64?.let { base64 ->
                try {
                    val bytes = Base64.getDecoder().decode(base64)
                    // Utiliser un nom unique basé sur timestamp et random
                    val uniqueId = "${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
                    val file = File(coversDir, "imported_${uniqueId}.jpg")
                    file.writeBytes(bytes)
                    android.util.Log.d("VinylImport", "Cover saved: ${file.absolutePath} (${bytes.size} bytes)")
                    file.absolutePath
                } catch (e: Exception) {
                    android.util.Log.e("VinylImport", "Error decoding cover for ${item.title}: ${e.message}")
                    null
                }
            }

            Vinyl(
                id = 0,  // Force l'ID à 0 pour que la DB génère un nouvel ID
                title = item.title,
                artist = item.artist,
                year = item.year,
                genre = item.genre,
                label = item.label,
                rating = item.rating,
                condition = item.condition,
                notes = item.notes,
                coverUri = coverUri,
                isOwned = item.isOwned
            )
        }
    }

    /**
     * Exporte en JSON lisible (formaté)
     */
    fun exportToPrettyJson(
        vinyls: List<Vinyl>,
        destinationFile: File? = null
    ): Result<File> = runCatching {
        val result = exportToJson(vinyls, destinationFile)
        val file = result.getOrThrow()

        // Reformater le JSON
        val json = file.readText()
        val parsed = exportAdapter.fromJson(json)
            ?: throw Exception("Erreur parsing JSON")

        val prettyJson = exportAdapter.indent("  ").toJson(parsed)
        file.writeText(prettyJson)
        file
    }
}

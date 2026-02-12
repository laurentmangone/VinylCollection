package com.example.vinylcollection

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.vinylcollection.databinding.ItemDiscogsReleaseBinding

/**
 * Adaptateur pour afficher les résultats de recherche Discogs
 */
class DiscogsResultAdapter(
    private val onItemClick: (DiscogsManager.DiscogsRelease) -> Unit
) : ListAdapter<DiscogsManager.DiscogsRelease, DiscogsResultAdapter.DiscogsViewHolder>(
    DiffCallback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscogsViewHolder {
        val binding = ItemDiscogsReleaseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DiscogsViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: DiscogsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiscogsViewHolder(
        private val binding: ItemDiscogsReleaseBinding,
        private val onItemClick: (DiscogsManager.DiscogsRelease) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(release: DiscogsManager.DiscogsRelease) {
            binding.apply {
                // Titre du release
                releaseTitle.text = release.title ?: "N/A"

                // Année
                releaseYear.text = release.year?.toString() ?: "N/A"

                // Genres
                val genreText = (release.genre ?: emptyList()).joinToString(", ")
                releaseGenre.text = genreText.ifBlank { "Non spécifié" }

                // Formats
                val formatText = (release.format ?: emptyList()).joinToString(", ")
                releaseFormat.text = formatText.ifBlank { "N/A" }

                // Label
                val labelText = release.label?.firstOrNull() ?: "N/A"
                releaseLabel.text = labelText

                // Cover image
                val coverUrl = release.getCoverUrl()
                if (!coverUrl.isNullOrBlank()) {
                    Log.d("DiscogsAdapter", "Chargement image pour '${release.title}': $coverUrl")
                    releaseCover.load(coverUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_vinyl)
                        error(R.drawable.ic_vinyl)
                        fallback(R.drawable.ic_vinyl)
                        allowHardware(false) // Désactiver le rendu matériel pour compatibilité
                        listener(
                            onSuccess = { _, _ ->
                                Log.d("DiscogsAdapter", "✅ Image chargée avec succès: $coverUrl")
                            },
                            onError = { _, result ->
                                Log.e("DiscogsAdapter", "❌ Erreur chargement image pour '${release.title}'")
                                Log.e("DiscogsAdapter", "URL: $coverUrl")
                                Log.e("DiscogsAdapter", "Erreur: ${result.throwable.message}", result.throwable)
                            }
                        )
                    }
                } else {
                    Log.d("DiscogsAdapter", "⚠️ Pas d'URL d'image disponible pour: ${release.title}")
                    releaseCover.setImageResource(R.drawable.ic_vinyl)
                }

                // Clic sur l'élément
                root.setOnClickListener {
                    onItemClick(release)
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<DiscogsManager.DiscogsRelease>() {
            override fun areItemsTheSame(
                oldItem: DiscogsManager.DiscogsRelease,
                newItem: DiscogsManager.DiscogsRelease
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: DiscogsManager.DiscogsRelease,
                newItem: DiscogsManager.DiscogsRelease
            ) = oldItem == newItem
        }
    }
}

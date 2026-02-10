package com.example.vinylcollection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vinylcollection.databinding.ItemVinylBinding

class VinylAdapter(
    private val onItemClick: (Vinyl) -> Unit
) : ListAdapter<Vinyl, VinylAdapter.VinylViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VinylViewHolder {
        val binding = ItemVinylBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VinylViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: VinylViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class VinylViewHolder(
        private val binding: ItemVinylBinding,
        private val onItemClick: (Vinyl) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(vinyl: Vinyl) {
            binding.title.text = vinyl.title
            binding.artist.text = vinyl.artist

            // Format metadata avec séparateurs
            val separator = binding.root.context.getString(R.string.metadata_separator)
            binding.meta.text = buildString {
                if (vinyl.year != null) {
                    append(vinyl.year)
                }
                if (vinyl.genre.isNotBlank()) {
                    if (isNotEmpty()) append(separator)
                    append(vinyl.genre)
                }
                if (vinyl.label.isNotBlank()) {
                    if (isNotEmpty()) append(separator)
                    append(vinyl.label)
                }
                if (vinyl.condition.isNotBlank()) {
                    if (isNotEmpty()) append(separator)
                    append(vinyl.condition)
                    append(" Etat")
                }
            }

            // Format rating avec étoile
            if (vinyl.rating != null) {
                binding.rating.text = binding.root.context.getString(
                    R.string.rating_format_star_5,
                    vinyl.rating
                )
                binding.rating.visibility = android.view.View.VISIBLE
            } else {
                binding.rating.visibility = android.view.View.GONE
            }

            // Notes preview
            if (vinyl.notes.isNotBlank()) {
                binding.notesPreview.text = vinyl.notes
                binding.notesPreview.visibility = android.view.View.VISIBLE
            } else {
                binding.notesPreview.visibility = android.view.View.GONE
            }

            // Charger l'image de couverture si disponible
            val cover = vinyl.coverUri?.takeIf { it.isNotBlank() }
            if (cover != null) {
                binding.vinylIcon.setImageURI(cover.toUri())
                if (binding.vinylIcon.drawable == null) {
                    binding.vinylIcon.setImageResource(R.drawable.ic_vinyl)
                }
            } else {
                binding.vinylIcon.setImageResource(R.drawable.ic_vinyl)
            }

            binding.root.setOnClickListener { onItemClick(vinyl) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Vinyl>() {
        override fun areItemsTheSame(oldItem: Vinyl, newItem: Vinyl): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Vinyl, newItem: Vinyl): Boolean {
            return oldItem == newItem
        }
    }
}

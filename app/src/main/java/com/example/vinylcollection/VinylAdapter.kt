package com.example.vinylcollection

import android.view.LayoutInflater
import android.view.ViewGroup
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
            binding.meta.text = buildString {
                if (vinyl.year != null) {
                    append(vinyl.year)
                    append(" • ")
                }
                if (vinyl.genre.isNotBlank()) {
                    append(vinyl.genre)
                    append(" • ")
                }
                if (vinyl.label.isNotBlank()) {
                    append(vinyl.label)
                    append(" • ")
                }
                if (vinyl.condition.isNotBlank()) {
                    append(vinyl.condition)
                }
            }.trim().trimEnd('•', ' ')
            binding.rating.text = if (vinyl.rating != null) {
                binding.root.context.getString(R.string.rating_format, vinyl.rating)
            } else {
                ""
            }
            binding.notesPreview.text = vinyl.notes
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


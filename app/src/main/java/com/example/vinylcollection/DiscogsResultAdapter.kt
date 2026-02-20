package com.example.vinylcollection

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.vinylcollection.databinding.ItemDiscogsReleaseBinding

/**
 * Adaptateur pour afficher les résultats de recherche Discogs avec sélection multiple
 */
class DiscogsResultAdapter(
    private val onItemClick: (DiscogsManager.DiscogsRelease) -> Unit,
    private val onSelectionChanged: (Set<Long>, Int) -> Unit = { _, _ -> },
) : ListAdapter<DiscogsManager.DiscogsRelease, DiscogsResultAdapter.DiscogsViewHolder>(
    DiffCallback
) {

    private val selectedIds = mutableSetOf<Long>()
    private var selectionMode = false
    val isInSelectionMode: Boolean get() = selectionMode
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscogsViewHolder {
        val binding = ItemDiscogsReleaseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DiscogsViewHolder(binding, onItemClick, ::onItemSelected, this)
    }

    override fun onBindViewHolder(holder: DiscogsViewHolder, position: Int) {
        holder.bind(getItem(position), selectedIds, selectionMode)
    }

    override fun onBindViewHolder(holder: DiscogsViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            val item = getItem(position)
            holder.applySelectionUi(item, selectedIds.contains(item.id), selectionMode)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun onItemSelected(release: DiscogsManager.DiscogsRelease, isSelected: Boolean) {
        if (isSelected) {
            selectedIds.add(release.id)
        } else {
            selectedIds.remove(release.id)
        }
        onSelectionChanged(selectedIds.toSet(), selectedIds.size)
        val index = currentList.indexOfFirst { it.id == release.id }
        if (index >= 0) {
            notifyItemChangedSafely(index)
        }
    }

    private fun notifyItemChangedSafely(index: Int) {
        val action = { notifyItemChanged(index, PAYLOAD_SELECTION) }
        val rv = recyclerView
        when {
            rv == null -> mainHandler.post(action)
            rv.isComputingLayout || rv.isAnimating -> rv.post(action)
            else -> action()
        }
    }

    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    fun setSelectionMode(enabled: Boolean) {
        selectionMode = enabled
        if (!enabled) {
            selectedIds.clear()
            onSelectionChanged(emptySet(), 0)
        }
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION)
        }
    }


    fun selectAll() {
        currentList.forEach { selectedIds.add(it.id) }
        onSelectionChanged(selectedIds.toSet(), selectedIds.size)
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION)
        }
    }

    fun clearSelection() {
        selectedIds.clear()
        onSelectionChanged(emptySet(), 0)
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION)
        }
    }

    fun getSelectedReleases(): List<DiscogsManager.DiscogsRelease> {
        return currentList.filter { selectedIds.contains(it.id) }
    }

    fun getSelectedCount() = selectedIds.size

    class DiscogsViewHolder(
        private val binding: ItemDiscogsReleaseBinding,
        private val onItemClick: (DiscogsManager.DiscogsRelease) -> Unit,
        private val onSelectionChanged: (DiscogsManager.DiscogsRelease, Boolean) -> Unit,
        private val adapter: DiscogsResultAdapter
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(release: DiscogsManager.DiscogsRelease, selectedIds: Set<Long>, selectionMode: Boolean) {
            val isSelected = selectedIds.contains(release.id)

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
                        allowHardware(false)
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
                    if (selectionMode) {
                        selectionCheckbox.isChecked = !selectionCheckbox.isChecked
                    } else {
                        onItemClick(release)
                    }
                }

                // Clic long pour activer le mode sélection
                root.setOnLongClickListener {
                    if (!adapter.isInSelectionMode) {
                        // Activer le mode sélection
                        adapter.setSelectionMode(true)
                        // Sélectionner cet item
                        onSelectionChanged(release, true)
                    }
                    true
                }

                // Mise en évidence visuelle si sélectionné
                applySelectionUi(release, isSelected, selectionMode)
            }
        }

        fun applySelectionUi(release: DiscogsManager.DiscogsRelease, isSelected: Boolean, selectionMode: Boolean) {
            binding.apply {
                // Checkbox visible uniquement en mode sélection
                selectionCheckbox.visibility = if (selectionMode) View.VISIBLE else View.GONE

                // Désactiver le listener AVANT de modifier isChecked pour éviter le crash RecyclerView
                selectionCheckbox.setOnCheckedChangeListener(null)
                selectionCheckbox.isChecked = isSelected

                // Clic sur la checkbox — réactiver le listener APRÈS avoir défini isChecked
                selectionCheckbox.setOnCheckedChangeListener { _, checked ->
                    onSelectionChanged(release, checked)
                }

                // Mise en évidence visuelle si sélectionné
                val cardView = binding.root
                if (selectionMode && isSelected) {
                    cardView.setCardBackgroundColor("#E3F2FD".toColorInt())
                    cardView.cardElevation = 12f
                    cardView.strokeColor = "#2196F3".toColorInt()
                    cardView.strokeWidth = 3
                    root.alpha = 1.0f
                    root.translationZ = 20f
                } else if (selectionMode) {
                    cardView.setCardBackgroundColor(Color.WHITE)
                    cardView.cardElevation = 2f
                    cardView.strokeColor = Color.TRANSPARENT
                    cardView.strokeWidth = 0
                    root.alpha = 1.0f
                    root.translationZ = 0f
                } else {
                    cardView.setCardBackgroundColor(Color.WHITE)
                    cardView.cardElevation = 2f
                    cardView.strokeColor = Color.TRANSPARENT
                    cardView.strokeWidth = 0
                    root.alpha = 1.0f
                    root.translationZ = 0f
                }
            }
        }
    }

    companion object {
        private const val PAYLOAD_SELECTION = "payload_selection"
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

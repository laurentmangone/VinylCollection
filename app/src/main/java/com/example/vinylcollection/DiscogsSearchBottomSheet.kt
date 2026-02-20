package com.example.vinylcollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vinylcollection.databinding.BottomSheetDiscogsSearchBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * Bottom Sheet pour afficher les résultats de recherche Discogs avec sélection multiple
 */
class DiscogsSearchBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDiscogsSearchBinding? = null
    private val binding get() = _binding!!

    private val discogsManager by lazy { DiscogsManager() }
    private lateinit var adapter: DiscogsResultAdapter
    private var searchResults: List<DiscogsManager.DiscogsRelease> = emptyList()

    private var onReleaseSelected: ((DiscogsManager.DiscogsRelease) -> Unit)? = null
    private var onMultipleReleasesSelected: ((List<DiscogsManager.DiscogsRelease>) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDiscogsSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val query = arguments?.getString(ARG_QUERY)
        val isBarcode = arguments?.getBoolean(ARG_IS_BARCODE) ?: false
        if (query == null) {
            dismiss()
            return
        }

        binding.searchTitle.text = if (isBarcode) {
            getString(R.string.discogs_results_for_barcode, query)
        } else {
            getString(R.string.discogs_results_for, query)
        }

        setupSelectionActions()
        performSearch(query, isBarcode)
    }

    private fun setupSelectionActions() {
        binding.selectAllButton.setOnClickListener {
            if (adapter.getSelectedCount() == searchResults.size) {
                adapter.clearSelection()
            } else {
                adapter.selectAll()
            }
        }

        binding.importSelectedButton.setOnClickListener {
            val selected = adapter.getSelectedReleases()
            if (selected.isNotEmpty()) {
                onMultipleReleasesSelected?.invoke(selected)
                dismiss()
            }
        }
    }

    private fun performSearch(query: String, isBarcode: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.isVisible = true
            binding.resultsRecycler.isVisible = false
            binding.emptyState.isVisible = false
            binding.selectionActions.isVisible = false

            try {
                val results = if (isBarcode) {
                    discogsManager.searchByBarcodeResults(query)
                } else {
                    discogsManager.searchRelease(query)
                }

                searchResults = results
                binding.progressBar.isVisible = false

                if (results.isEmpty()) {
                    binding.emptyState.isVisible = true
                    binding.emptyState.text = if (isBarcode) {
                        getString(R.string.discogs_no_results_for_barcode, query)
                    } else {
                        getString(R.string.discogs_no_results_for, query)
                    }
                } else {
                    binding.resultsRecycler.isVisible = true
                    binding.selectionActions.isVisible = true

                    adapter = DiscogsResultAdapter(
                        onItemClick = { release ->
                            onReleaseSelected?.invoke(release)
                            dismiss()
                        },
                        onSelectionChanged = { _, count ->
                            updateSelectionUI(count)
                        }
                    )
                    // Le mode sélection sera activé lors d'un long press sur un item
                    binding.resultsRecycler.layoutManager = LinearLayoutManager(requireContext())
                    binding.resultsRecycler.adapter = adapter
                    adapter.submitList(results)
                    updateSelectionUI(0)
                }
            } catch (e: Exception) {
                binding.progressBar.isVisible = false
                binding.emptyState.isVisible = true
                binding.emptyState.text = getString(R.string.discogs_search_error, e.message ?: "")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.discogs_search_error_toast, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateSelectionUI(count: Int) {
        binding.selectionCount.text = getString(R.string.selected_count, count)
        binding.importSelectedButton.text = getString(R.string.import_selected, count)
        binding.importSelectedButton.isEnabled = count > 0
        binding.selectAllButton.text = if (count == searchResults.size && searchResults.isNotEmpty()) {
            getString(R.string.deselect_all)
        } else {
            getString(R.string.select_all)
        }
    }

    fun setOnReleaseSelected(callback: (DiscogsManager.DiscogsRelease) -> Unit) {
        onReleaseSelected = callback
    }

    fun setOnMultipleReleasesSelected(callback: (List<DiscogsManager.DiscogsRelease>) -> Unit) {
        onMultipleReleasesSelected = callback
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_QUERY = "arg_query"
        private const val ARG_IS_BARCODE = "arg_is_barcode"

        fun newInstance(query: String) = DiscogsSearchBottomSheet().apply {
            arguments = bundleOf(ARG_QUERY to query)
        }

        fun newInstanceBarcode(barcode: String) = DiscogsSearchBottomSheet().apply {
            arguments = bundleOf(
                ARG_QUERY to barcode,
                ARG_IS_BARCODE to true
            )
        }

        @Suppress("unused")
        fun show(fragmentManager: FragmentManager, query: String): DiscogsSearchBottomSheet {
            return newInstance(query).also {
                it.show(fragmentManager, "DiscogsSearch")
            }
        }
    }
}

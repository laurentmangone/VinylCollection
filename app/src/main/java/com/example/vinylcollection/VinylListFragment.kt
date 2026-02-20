package com.example.vinylcollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vinylcollection.databinding.FragmentVinylListBinding
import kotlinx.coroutines.launch

class VinylListFragment : Fragment() {

    private var _binding: FragmentVinylListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VinylViewModel by activityViewModels()
    private lateinit var adapter: VinylAdapter

    private var suggestionsPopup: ListPopupWindow? = null
    private var suggestionsAdapter: ArrayAdapter<SearchSuggestion.Display>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVinylListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Gérer les insets système pour le RecyclerView
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Ajouter 80dp (pour le FAB) + les insets système
            val fabPadding = (80 * resources.displayMetrics.density).toInt()
            v.updatePadding(bottom = insets.bottom + fabPadding)
            windowInsets
        }

        adapter = VinylAdapter { vinyl ->
            VinylEditBottomSheet.newEdit(vinyl).show(parentFragmentManager, TAG_EDIT)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // Setup status filter chips
        binding.statusChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipAll) -> viewModel.setStatusFilter(null)
                checkedIds.contains(R.id.chipOwned) -> viewModel.setStatusFilter(true)
                checkedIds.contains(R.id.chipWanted) -> viewModel.setStatusFilter(false)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.vinyls.collect { vinyls ->
                        adapter.submitList(vinyls)
                        binding.emptyState.isVisible = vinyls.isEmpty()
                    }
                }
                launch {
                    viewModel.ownedCount.collect { count ->
                        updateToolbarTitle(count)
                    }
                }
            }
        }

        // Setup buttons for empty state
        binding.optionAddVinyl.setOnClickListener {
            openCreateSheet()
        }
        binding.optionScanBarcode.setOnClickListener {
            openCreateSheetScanBarcode()
        }
        binding.optionScanCover.setOnClickListener {
            openCreateSheetScanCover()
        }

        setupMenu()
    }

    override fun onResume() {
        super.onResume()
        updateToolbarTitle(viewModel.ownedCount.value)
    }

    private fun updateToolbarTitle(count: Int) {
        val title = getString(R.string.vinyl_list_title_with_count, count)
        requireActivity().title = title
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title = title
        try {
            requireActivity()
                .findNavController(R.id.nav_host_fragment_content_main)
                .currentDestination
                ?.label = title
        } catch (_: IllegalStateException) {
            // No-op if nav controller is not ready.
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView
                searchView.queryHint = getString(R.string.search_hint)

                setupSearchSuggestions(searchView)

                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        viewModel.setQuery(query.orEmpty())
                        suggestionsPopup?.dismiss()
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.setQuery(newText.orEmpty())
                        updateSearchSuggestions(newText.orEmpty())
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_search -> true
                    R.id.action_backup -> {
                        openBackupSheet()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupSearchSuggestions(searchView: SearchView) {
        val context = requireContext()
        val adapter = object : ArrayAdapter<SearchSuggestion.Display>(context, R.layout.item_search_suggestion) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView
                    ?: LayoutInflater.from(context).inflate(R.layout.item_search_suggestion, parent, false)
                val item = getItem(position)
                view.findViewById<android.widget.TextView>(R.id.suggestionIcon).text = item?.icon.orEmpty()
                view.findViewById<android.widget.TextView>(R.id.suggestionText).text = item?.text.orEmpty()
                return view
            }
        }

        val popup = ListPopupWindow(context).apply {
            anchorView = searchView
            isModal = true
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                val item = adapter.getItem(position) ?: return@setOnItemClickListener
                dismiss()
                searchView.setQuery(item.text, true)
            }
        }

        suggestionsAdapter = adapter
        suggestionsPopup = popup

        searchView.setOnCloseListener {
            popup.dismiss()
            false
        }
    }

    private fun updateSearchSuggestions(query: String) {
        val adapter = suggestionsAdapter ?: return
        val popup = suggestionsPopup ?: return
        val suggestions = viewModel.getSearchSuggestions(query).map { it.getDisplay() }

        adapter.clear()
        if (suggestions.isNotEmpty()) {
            adapter.addAll(suggestions)
            adapter.notifyDataSetChanged()
            popup.width = ViewGroup.LayoutParams.MATCH_PARENT
            popup.show()
        } else {
            popup.dismiss()
        }
    }

    fun openCreateSheet() {
        VinylEditBottomSheet.newCreate().show(parentFragmentManager, TAG_EDIT)
    }

    fun openCreateSheetScanBarcode() {
        VinylEditBottomSheet.newCreateScanBarcode().show(parentFragmentManager, TAG_EDIT)
    }

    fun openCreateSheetScanCover() {
        VinylEditBottomSheet.newCreateScanCover().show(parentFragmentManager, TAG_EDIT)
    }

    fun openBackupSheet() {
        BackupBottomSheet.show(parentFragmentManager)
    }

    override fun onDestroyView() {
        suggestionsPopup?.dismiss()
        suggestionsPopup = null
        suggestionsAdapter = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG_EDIT = "VinylEdit"
    }
}

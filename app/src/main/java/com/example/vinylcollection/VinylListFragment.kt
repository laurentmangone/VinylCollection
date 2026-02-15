package com.example.vinylcollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vinylcollection.databinding.FragmentVinylListBinding
import kotlinx.coroutines.launch

class VinylListFragment : Fragment() {

    private var _binding: FragmentVinylListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VinylViewModel by activityViewModels()
    private lateinit var adapter: VinylAdapter

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vinyls.collect { vinyls ->
                    adapter.submitList(vinyls)
                    binding.emptyState.isVisible = vinyls.isEmpty()
                }
            }
        }

        setupMenu()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView
                searchView.queryHint = getString(R.string.search_hint)
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        viewModel.setQuery(query.orEmpty())
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.setQuery(newText.orEmpty())
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

    fun openCreateSheet() {
        VinylEditBottomSheet.newCreate().show(parentFragmentManager, TAG_EDIT)
    }

    fun openBackupSheet() {
        BackupBottomSheet.show(parentFragmentManager)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG_EDIT = "VinylEdit"
    }
}

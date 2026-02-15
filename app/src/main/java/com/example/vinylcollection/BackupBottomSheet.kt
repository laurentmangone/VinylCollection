package com.example.vinylcollection

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.vinylcollection.databinding.BottomSheetBackupBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.io.File

/**
 * Bottom Sheet pour gérer les exports/imports JSON
 */
class BackupBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBackupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VinylViewModel by activityViewModels()
    private lateinit var exportImport: VinylExportImport

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val file = File(requireContext().filesDir, "import_${System.currentTimeMillis()}.json")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            importFromFile(file)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exportImport = VinylExportImport(requireContext())

        // Export JSON
        binding.exportButton.setOnClickListener {
            exportToJson()
        }

        // Import JSON
        binding.importButton.setOnClickListener {
            importFileLauncher.launch("application/json")
        }
    }

    private fun exportToJson() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.exportButton.isEnabled = false

                val vinyls = viewModel.vinyls.value
                val result = exportImport.exportToPrettyJson(vinyls)

                if (result.isSuccess) {
                    val file = result.getOrThrow()
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )

                    // Créer un intent de partage
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "application/json"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    startActivity(Intent.createChooser(intent, "Exporter la collection"))
                    Toast.makeText(
                        requireContext(),
                        "Collection exportée: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Erreur export: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                binding.progressBar.visibility = View.GONE
                binding.exportButton.isEnabled = true
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.progressBar.visibility = View.GONE
                binding.exportButton.isEnabled = true
            }
        }
    }

    private fun importFromFile(file: File) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.importButton.isEnabled = false

                val result = exportImport.importFromJson(file)

                if (result.isSuccess) {
                    val importedVinyls = result.getOrThrow()

                    // Importer dans la DB en forçant l'ID à 0 pour générer de nouveaux IDs
                    importedVinyls.forEach { vinyl ->
                        viewModel.add(vinyl.copy(id = 0))
                    }

                    Toast.makeText(
                        requireContext(),
                        "${importedVinyls.size} vinyles importés !",
                        Toast.LENGTH_LONG
                    ).show()

                    dismiss()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Erreur import: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                binding.progressBar.visibility = View.GONE
                binding.importButton.isEnabled = true
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.progressBar.visibility = View.GONE
                binding.importButton.isEnabled = true
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
            BackupBottomSheet().show(fragmentManager, "Backup")
        }
    }
}


package com.example.vinylcollection

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.vinylcollection.databinding.BottomSheetVinylBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.io.File

class VinylEditBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetVinylBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VinylViewModel by activityViewModels()
    private val discogsManager by lazy { DiscogsManager() }

    private var coverUri: Uri? = null
    private var pendingCameraUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Permission caméra requise pour prendre une photo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private var pendingCameraFile: File? = null
    private var lastCameraFileSize: Long = -1L

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val file = pendingCameraFile
            val uri = pendingCameraUri
            if (file != null && uri != null) {
                openCropperWhenCameraReady(file, uri)
            }
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Copier l'image localement pour éviter les problèmes de permission
            val localUri = copyToLocalFile(uri)
            if (localUri != null) {
                openCropper(localUri)
            }
        }
    }

    private val cropCoverLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra(CropCoverActivity.EXTRA_OUTPUT_URI)
            coverUri = uriString?.toUri()
            updateCoverUi()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetVinylBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val id = args.getLong(ARG_ID)

        coverUri = args.getString(ARG_COVER_URI)?.toUri()
        updateCoverUi()

        binding.takePhotoButton.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    launchCamera()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }

        binding.pickPhotoButton.setOnClickListener {
            pickPhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.removePhotoButton.setOnClickListener {
            coverUri = null
            updateCoverUi()
        }

        binding.viewPhotoButton.setOnClickListener {
            val uri = coverUri?.toString()
            if (!uri.isNullOrBlank()) {
                CoverPreviewDialogFragment
                    .newInstance(uri)
                    .show(parentFragmentManager, "CoverPreview")
            }
        }

        val genreAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.genres)
        )
        binding.genreInput.setAdapter(genreAdapter)
        binding.genreInput.keyListener = null
        binding.genreInput.setOnClickListener { binding.genreInput.showDropDown() }

        val conditionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.conditions)
        )
        binding.conditionInput.setAdapter(conditionAdapter)
        binding.conditionInput.keyListener = null
        binding.conditionInput.setOnClickListener { binding.conditionInput.showDropDown() }

        // Listener pour la recherche Discogs
        binding.discogsSearchButton.setOnClickListener {
            searchOnDiscogs()
        }

        val ratingValue = args.getString(ARG_RATING, "")
            .toIntOrNull()
            ?.coerceIn(0, 5)
            ?: 0

        binding.titleInput.setText(args.getString(ARG_TITLE, ""))
        binding.artistInput.setText(args.getString(ARG_ARTIST, ""))
        binding.yearInput.setText(args.getString(ARG_YEAR, ""))
        binding.genreInput.setText(args.getString(ARG_GENRE, ""), false)
        binding.labelInput.setText(args.getString(ARG_LABEL, ""))
        binding.ratingBar.rating = ratingValue.toFloat()
        binding.conditionInput.setText(args.getString(ARG_CONDITION, ""), false)
        binding.notesInput.setText(args.getString(ARG_NOTES, ""))

        binding.deleteButton.visibility = if (id == 0L) View.GONE else View.VISIBLE

        binding.saveButton.setOnClickListener {
            val title = binding.titleInput.text?.toString()?.trim().orEmpty()
            val artist = binding.artistInput.text?.toString()?.trim().orEmpty()
            if (title.isBlank() || artist.isBlank()) {
                Toast.makeText(requireContext(), R.string.error_title_artist, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val year = binding.yearInput.text?.toString()?.trim().orEmpty().toIntOrNull()
            val rating = binding.ratingBar.rating.toInt().takeIf { it > 0 }

            val vinyl = Vinyl(
                id = id,
                title = title,
                artist = artist,
                year = year,
                genre = binding.genreInput.text?.toString()?.trim().orEmpty(),
                label = binding.labelInput.text?.toString()?.trim().orEmpty(),
                rating = rating,
                condition = binding.conditionInput.text?.toString()?.trim().orEmpty(),
                notes = binding.notesInput.text?.toString()?.trim().orEmpty(),
                coverUri = coverUri?.toString()
            )

            if (id == 0L) {
                viewModel.add(vinyl)
            } else {
                viewModel.update(vinyl)
            }
            dismiss()
        }

        binding.deleteButton.setOnClickListener {
            val title = binding.titleInput.text?.toString()?.trim().orEmpty()
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete)
                .setMessage(requireContext().getString(R.string.confirm_delete_message, title))
                .setPositiveButton(R.string.delete) { _, _ ->
                    val ratingToDelete = binding.ratingBar.rating.toInt().takeIf { it > 0 }
                    val vinyl = Vinyl(
                        id = id,
                        title = binding.titleInput.text?.toString()?.trim().orEmpty(),
                        artist = binding.artistInput.text?.toString()?.trim().orEmpty(),
                        year = binding.yearInput.text?.toString()?.trim().orEmpty().toIntOrNull(),
                        genre = binding.genreInput.text?.toString()?.trim().orEmpty(),
                        label = binding.labelInput.text?.toString()?.trim().orEmpty(),
                        rating = ratingToDelete,
                        condition = binding.conditionInput.text?.toString()?.trim().orEmpty(),
                        notes = binding.notesInput.text?.toString()?.trim().orEmpty(),
                        coverUri = coverUri?.toString()
                    )
                    viewModel.delete(vinyl)
                    dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateCoverUi() {
        val imageView: ImageView = binding.coverImageView
        if (coverUri != null) {
            imageView.setImageURI(coverUri)
            if (imageView.drawable == null) {
                imageView.setImageResource(R.drawable.ic_vinyl)
            }
            binding.removePhotoButton.visibility = View.VISIBLE
            binding.viewPhotoButton.visibility = View.VISIBLE
        } else {
            imageView.setImageResource(R.drawable.ic_vinyl)
            binding.removePhotoButton.visibility = View.GONE
            binding.viewPhotoButton.visibility = View.GONE
        }
    }


    private fun copyToLocalFile(sourceUri: Uri): Uri? {
        return try {
            val coversDir = File(requireContext().filesDir, "covers")
            if (!coversDir.exists()) {
                coversDir.mkdirs()
            }
            val file = File(coversDir, "temp_${System.currentTimeMillis()}.jpg")
            requireContext().contentResolver.openInputStream(sourceUri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (!file.exists() || file.length() == 0L) {
                return null
            }
            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun launchCamera() {
        val coversDir = File(requireContext().filesDir, "covers")
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }
        val file = File(coversDir, "camera_${System.currentTimeMillis()}.jpg")
        pendingCameraFile = file
        pendingCameraUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        pendingCameraUri?.let { uri ->
            takePictureLauncher.launch(uri)
        }
    }

    private fun openCropperWhenReady(file: File, uri: Uri, remainingAttempts: Int = 6) {
        if (file.exists() && file.length() > 0) {
            openCropper(uri)
            return
        }
        if (remainingAttempts <= 0) {
            Toast.makeText(requireContext(), R.string.error_load_image, Toast.LENGTH_SHORT).show()
            return
        }
        Handler(Looper.getMainLooper()).postDelayed(
            { openCropperWhenReady(file, uri, remainingAttempts - 1) },
            150
        )
    }

    private fun openCropperWhenCameraReady(file: File, uri: Uri, remainingAttempts: Int = 10) {
        if (!file.exists()) {
            scheduleCameraRetry(file, uri, remainingAttempts)
            return
        }
        val size = file.length()
        if (size > 0 && size == lastCameraFileSize) {
            val localUri = copyToLocalFile(uri)
            if (localUri != null) {
                openCropper(localUri)
            } else {
                Toast.makeText(requireContext(), R.string.error_load_image, Toast.LENGTH_SHORT).show()
            }
            return
        }
        lastCameraFileSize = size
        scheduleCameraRetry(file, uri, remainingAttempts)
    }

    private fun scheduleCameraRetry(file: File, uri: Uri, remainingAttempts: Int) {
        if (remainingAttempts <= 0) {
            Toast.makeText(requireContext(), R.string.error_load_image, Toast.LENGTH_SHORT).show()
            return
        }
        Handler(Looper.getMainLooper()).postDelayed(
            { openCropperWhenCameraReady(file, uri, remainingAttempts - 1) },
            250
        )
    }

    private fun openCropper(uri: Uri) {
        val intent = Intent(requireContext(), CropCoverActivity::class.java)
            .putExtra(CropCoverActivity.EXTRA_INPUT_URI, uri.toString())
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = ClipData.newUri(requireContext().contentResolver, "cover", uri)
        cropCoverLauncher.launch(intent)
    }

    /**
     * Déclenche la recherche sur Discogs
     */
    private fun searchOnDiscogs() {
        val artist = binding.artistInput.text.toString().trim()
        val title = binding.titleInput.text.toString().trim()

        if (title.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Entrez un titre pour chercher",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val query = if (artist.isBlank()) {
            title
        } else {
            "$artist $title"
        }

        DiscogsSearchBottomSheet.newInstance(query).apply {
            setOnReleaseSelected { release ->
                fillFromDiscogsRelease(release)
            }
        }.show(parentFragmentManager, "DiscogsSearch")
    }

    /**
     * Remplit les champs avec les données Discogs
     */
    private fun fillFromDiscogsRelease(release: DiscogsManager.DiscogsRelease) {
        val rawTitle = release.title?.trim().orEmpty()

        // Parser le titre Discogs qui est souvent au format "Artiste - Titre" ou "Artiste – Titre"
        // Gère aussi les cas avec parenthèses (ex: "Artist - Title (Remastered)")
        val titleParts = rawTitle.split(Regex(" [-–] "), limit = 2)
        if (titleParts.size == 2) {
            val parsedArtist = titleParts[0].trim()
            val parsedTitle = titleParts[1].trim()
                .replace(Regex("\\s*\\([^)]*\\)\\s*$"), "") // Retire les parenthèses finales
                .replace(Regex("\\s*,\\s*[^,]*$"), "") // Retire les virgules finales avec suffixes
                .trim()

            if (parsedArtist.isNotBlank()) {
                binding.artistInput.setText(parsedArtist)
            }
            binding.titleInput.setText(parsedTitle.ifBlank { rawTitle })
        } else {
            // Si pas de séparateur trouvé, on met tout dans le titre
            binding.titleInput.setText(rawTitle)
        }

        release.year?.let { binding.yearInput.setText(it.toString()) }

        val label = release.label?.firstOrNull() ?: ""
        binding.labelInput.setText(label)

        val mainGenre = release.genre?.firstOrNull() ?: ""
        binding.genreInput.setText(mainGenre, false)

        // Télécharger l'image de couverture si disponible
        val coverImageUrl = release.getCoverUrl()

        if (!coverImageUrl.isNullOrBlank()) {
            lifecycleScope.launch {
                try {
                    android.util.Log.d("VinylEdit", "Téléchargement cover depuis: $coverImageUrl")
                    val imageFile = discogsManager.downloadCoverImage(
                        coverImageUrl,
                        requireContext()
                    )
                    if (imageFile != null) {
                        coverUri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            imageFile
                        )
                        updateCoverUi()
                        Toast.makeText(
                            requireContext(),
                            "Données Discogs chargées avec l'image !",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Données Discogs chargées (échec téléchargement image)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VinylEdit", "Erreur téléchargement: ${e.message}", e)
                    Toast.makeText(
                        requireContext(),
                        "Données Discogs chargées (erreur image: ${e.message})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                requireContext(),
                "Données Discogs chargées ! (pas d'image disponible)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        private const val ARG_ID = "arg_id"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_ARTIST = "arg_artist"
        private const val ARG_YEAR = "arg_year"
        private const val ARG_GENRE = "arg_genre"
        private const val ARG_LABEL = "arg_label"
        private const val ARG_RATING = "arg_rating"
        private const val ARG_CONDITION = "arg_condition"
        private const val ARG_NOTES = "arg_notes"
        private const val ARG_COVER_URI = "arg_cover_uri"

        fun newCreate(): VinylEditBottomSheet {
            return VinylEditBottomSheet().apply {
                arguments = bundleOf(
                    ARG_ID to 0L
                )
            }
        }

        fun newEdit(vinyl: Vinyl): VinylEditBottomSheet {
            return VinylEditBottomSheet().apply {
                arguments = bundleOf(
                    ARG_ID to vinyl.id,
                    ARG_TITLE to vinyl.title,
                    ARG_ARTIST to vinyl.artist,
                    ARG_YEAR to vinyl.year?.toString().orEmpty(),
                    ARG_GENRE to vinyl.genre,
                    ARG_LABEL to vinyl.label,
                    ARG_RATING to vinyl.rating?.toString().orEmpty(),
                    ARG_CONDITION to vinyl.condition,
                    ARG_NOTES to vinyl.notes,
                    ARG_COVER_URI to vinyl.coverUri
                )
            }
        }
    }
}

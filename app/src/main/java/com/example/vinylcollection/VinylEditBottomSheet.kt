package com.example.vinylcollection

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
import com.example.vinylcollection.databinding.BottomSheetVinylBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File
import java.io.FileOutputStream

class VinylEditBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetVinylBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VinylViewModel by activityViewModels()

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

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { uri ->
                coverUri = cropAndSaveCover(uri)
            }
            updateCoverUi()
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coverUri = cropAndSaveCover(uri)
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

    private fun cropAndSaveCover(sourceUri: Uri): Uri? {
        val inputStream = requireContext().contentResolver.openInputStream(sourceUri) ?: return null
        val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return null

        val size = minOf(original.width, original.height)
        val x = (original.width - size) / 2
        val y = (original.height - size) / 2
        val cropped = Bitmap.createBitmap(original, x, y, size, size)

        val coversDir = File(requireContext().filesDir, "covers")
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }
        val file = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
    }

    private fun createCoverUri(): Uri {
        val coversDir = File(requireContext().filesDir, "covers")
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }
        val file = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
    }

    private fun launchCamera() {
        pendingCameraUri = createCoverUri()
        pendingCameraUri?.let { uri ->
            takePictureLauncher.launch(uri)
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

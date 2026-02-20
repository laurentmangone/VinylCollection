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

    private enum class ScanMode {
        COVER_OCR,
        BARCODE
    }

    private var pendingScanMode: ScanMode? = null
    private var scanCameraFile: File? = null
    private var scanCameraUri: Uri? = null

    private var cameraRequestAction: (() -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraRequestAction?.invoke()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.scan_cover_error),
                Toast.LENGTH_SHORT
            ).show()
        }
        cameraRequestAction = null
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

    private val scanTakePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = scanCameraUri
            val mode = pendingScanMode
            if (uri != null && mode != null) {
                runScanOnImage(uri, mode)
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

    private val scanPickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localUri = copyToLocalFile(uri)
            val mode = pendingScanMode
            if (localUri != null && mode != null) {
                runScanOnImage(localUri, mode)
            }
        }
    }

    private val cropCoverLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra(CropCoverActivity.EXTRA_OUTPUT_URI)
            coverUri = uriString?.toUri()
            android.util.Log.d("VinylEdit", "Cover recadrée avec succès: $coverUri")
            updateCoverUi()
            Toast.makeText(
                requireContext(),
                "✅ Cover ajoutée ! N'oubliez pas de sauvegarder",
                Toast.LENGTH_LONG
            ).show()
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

        android.util.Log.d("VinylEdit", "Loading vinyl with ID: $id")
        android.util.Log.d("VinylEdit", "  - ARG_COVER_URI: ${args.getString(ARG_COVER_URI)}")

        coverUri = args.getString(ARG_COVER_URI)?.toUri()
        android.util.Log.d("VinylEdit", "  - coverUri after loading: $coverUri")

        updateCoverUi()

        if (args.getBoolean(ARG_START_BARCODE_SCAN, false)) {
            view.post {
                pendingScanMode = ScanMode.BARCODE
                showScanSourceDialog()
            }
        }
        if (args.getBoolean(ARG_START_COVER_SCAN, false)) {
            view.post {
                pendingScanMode = ScanMode.COVER_OCR
                showScanSourceDialog()
            }
        }

        binding.takePhotoButton.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    launchCamera()
                }
                else -> {
                    cameraRequestAction = { launchCamera() }
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

        // Initialiser le switch "Possédé/Recherché"
        val isOwned = args.getBoolean(ARG_IS_OWNED, true)
        binding.ownedSwitch.isChecked = isOwned
        updateOwnedSwitchText(isOwned)

        binding.ownedSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateOwnedSwitchText(isChecked)
        }

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

            android.util.Log.d("VinylEdit", "Saving vinyl: $title")
            android.util.Log.d("VinylEdit", "  - coverUri: $coverUri")
            android.util.Log.d("VinylEdit", "  - coverUri.toString(): ${coverUri?.toString()}")

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
                coverUri = coverUri?.toString(),
                isOwned = binding.ownedSwitch.isChecked
            )

            android.util.Log.d("VinylEdit", "Vinyl saved: ${vinyl.title}, coverUri: ${vinyl.coverUri}")

            if (id == 0L) {
                viewModel.add(vinyl)
            } else {
                viewModel.update(vinyl)
            }

            val message = if (coverUri != null) {
                "✅ Vinyl sauvegardé avec cover !"
            } else {
                "✅ Vinyl sauvegardé (sans cover)"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

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
                        coverUri = coverUri?.toString(),
                        isOwned = binding.ownedSwitch.isChecked
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

    private fun updateOwnedSwitchText(isOwned: Boolean) {
        binding.ownedSwitch.text = if (isOwned) {
            getString(R.string.status_owned)
        } else {
            getString(R.string.status_wanted)
        }
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

    private fun launchScanCamera() {
        val coversDir = File(requireContext().filesDir, "covers")
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }
        val file = File(coversDir, "scan_${System.currentTimeMillis()}.jpg")
        scanCameraFile = file
        scanCameraUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        scanCameraUri?.let { uri ->
            scanTakePictureLauncher.launch(uri)
        }
    }

    private fun showScanSourceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.scan_cover_source)
            .setItems(
                arrayOf(
                    getString(R.string.scan_cover_camera),
                    getString(R.string.scan_cover_gallery)
                )
            ) { _, which ->
                when (which) {
                    0 -> {
                        when {
                            ContextCompat.checkSelfPermission(
                                requireContext(),
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                launchScanCamera()
                            }
                            else -> {
                                cameraRequestAction = { launchScanCamera() }
                                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }
                    1 -> {
                        scanPickPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun runScanOnImage(uri: Uri, mode: ScanMode) {
        try {
            val inputImage = com.google.mlkit.vision.common.InputImage.fromFilePath(
                requireContext(),
                uri
            )

            when (mode) {
                ScanMode.BARCODE -> {
                    val scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
                    scanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            val rawValue = barcodes.firstOrNull()?.rawValue?.trim()
                            if (rawValue.isNullOrBlank()) {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.scan_barcode_error,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                openDiscogsSearchFromBarcode(rawValue)
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                R.string.scan_barcode_error,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                ScanMode.COVER_OCR -> {
                    val recognizer = com.google.mlkit.vision.text.TextRecognition
                        .getClient(
                            com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
                        )
                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val parsed = parseOcrText(visionText.text)
                            if (parsed == null) {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.scan_cover_error,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                parsed.artist?.let { binding.artistInput.setText(it) }
                                parsed.title?.let { binding.titleInput.setText(it) }
                                val query = buildDiscogsQuery(parsed.title)
                                if (query != null) {
                                    openDiscogsSearch(query)
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        R.string.scan_cover_error,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                R.string.scan_cover_error,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
        } catch (_: Exception) {
            Toast.makeText(
                requireContext(),
                R.string.scan_cover_error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openDiscogsSearchFromBarcode(barcode: String) {
        DiscogsSearchBottomSheet.newInstanceBarcode(barcode).apply {
            setOnReleaseSelected { release ->
                fillFromDiscogsRelease(release)
            }
        }.show(parentFragmentManager, "DiscogsSearch")
    }

    private data class OcrResult(
        val artist: String?,
        val title: String?
    )

    private fun parseOcrText(text: String): OcrResult? {
        val normalizedLines = text.lines()
            .map { normalizeOcrLine(it) }
            .filter { it.isNotBlank() }
            .filterNot { isNoiseLine(it) }
            .distinct()
            .take(8)

        if (normalizedLines.isEmpty()) return null

        val dashLine = normalizedLines.firstOrNull { it.contains(" - ") || it.contains(" – ") }
        if (dashLine != null) {
            val parts = dashLine.split(Regex(" [-–] "), limit = 2)
            val artist = parts.getOrNull(0)?.trim().takeUnless { it.isNullOrBlank() }
            val title = parts.getOrNull(1)?.trim().takeUnless { it.isNullOrBlank() }
            return OcrResult(artist, title)
        }

        if (normalizedLines.size == 1) {
            return OcrResult(artist = null, title = normalizedLines.first())
        }

        val candidates = normalizedLines.sortedByDescending { it.length }
        val first = candidates.getOrNull(0)
        val second = candidates.getOrNull(1)

        if (first.isNullOrBlank() || second.isNullOrBlank()) {
            return OcrResult(artist = null, title = first ?: normalizedLines.first())
        }

        val artist: String
        val title: String
        if (first.length >= second.length) {
            title = first
            artist = second
        } else {
            title = second
            artist = first
        }

        return OcrResult(artist = artist, title = title)
    }

    private fun normalizeOcrLine(line: String): String {
        return line
            .trim()
            .replace(Regex("""\u00A0"""), " ")
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""\s*\([^)]*\)\s*$"""), "")
            .replace(Regex("""\s*,\s*[^,]*$"""), "")
            .replace(Regex("""^[^\p{L}\p{N}]+|[^\p{L}\p{N}]+$"""), "")
            .trim()
    }

    private fun isNoiseLine(line: String): Boolean {
        val lower = line.lowercase()
        val noiseTokens = listOf(
            "stereo",
            "mono",
            "records",
            "recordings",
            "record",
            "lp",
            "vinyl",
            "side",
            "disc",
            "volume",
            "vol",
            "edition",
            "remastered",
            "deluxe",
            "limited",
            "rpm",
            "33",
            "45"
        )

        if (lower.length < 3) return true
        if (lower.count { it.isLetterOrDigit() } < 3) return true
        if (noiseTokens.any { token -> lower == token || lower.contains("$token ") }) return true
        if (lower.count { it.isDigit() } > lower.length / 2) return true

        return false
    }

    private fun buildDiscogsQuery(title: String?): String? {
        val safeTitle = title?.trim().orEmpty()
        return safeTitle.takeIf { it.isNotBlank() }
    }

    private fun openDiscogsSearch(query: String) {
        DiscogsSearchBottomSheet.newInstance(query).apply {
            setOnReleaseSelected { release ->
                fillFromDiscogsRelease(release)
            }
            setOnMultipleReleasesSelected { releases ->
                importMultipleFromDiscogs(releases)
            }
        }.show(parentFragmentManager, "DiscogsSearch")
    }

    /**
     * Importe plusieurs releases Discogs depuis la recherche
     */
    private fun importMultipleFromDiscogs(releases: List<DiscogsManager.DiscogsRelease>) {
        viewLifecycleOwner.lifecycleScope.launch {
            var successCount = 0
            var errorCount = 0

            releases.forEach { release ->
                try {
                    // Créer le vinyl sans cover
                    val vinyl = createVinylFromDiscogsRelease(release)

                    // Télécharger la cover si disponible
                    val coverImageUrl = release.getCoverUrl()
                    var vinylWithCover = vinyl

                    if (!coverImageUrl.isNullOrBlank()) {
                        try {
                            android.util.Log.d("VinylEdit", "Téléchargement cover depuis: $coverImageUrl")
                            val imageFile = discogsManager.downloadCoverImage(
                                coverImageUrl,
                                requireContext()
                            )
                            if (imageFile != null) {
                                val coverUri = FileProvider.getUriForFile(
                                    requireContext(),
                                    "${requireContext().packageName}.fileprovider",
                                    imageFile
                                )
                                vinylWithCover = vinyl.copy(coverUri = coverUri.toString())
                                android.util.Log.d("VinylEdit", "Cover téléchargée pour ${release.title}")
                            } else {
                                android.util.Log.w("VinylEdit", "Impossible de télécharger cover pour ${release.title}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("VinylEdit", "Erreur téléchargement cover ${release.title}: ${e.message}")
                        }
                    }

                    viewModel.add(vinylWithCover)
                    successCount++
                } catch (e: Exception) {
                    errorCount++
                    android.util.Log.e("VinylEdit", "Erreur import ${release.title}: ${e.message}")
                }
            }

            val message = if (errorCount == 0) {
                getString(R.string.import_multiple_success, successCount)
            } else {
                getString(R.string.import_multiple_error)
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

            // Fermer le BottomSheet et retourner à la liste
            dismiss()
        }
    }

    /**
     * Crée un Vinyl à partir d'une release Discogs
     */
    private fun createVinylFromDiscogsRelease(release: DiscogsManager.DiscogsRelease): Vinyl {
        val rawTitle = release.title?.trim().orEmpty()
        val titleParts = rawTitle.split(Regex(" [-–] "), limit = 2)

        val artist: String
        val title: String

        if (titleParts.size == 2) {
            artist = titleParts[0].trim()
            title = titleParts[1].trim()
                .replace(Regex("\\s*\\([^)]*\\)\\s*$"), "")
                .replace(Regex("\\s*,\\s*[^,]*$"), "")
                .trim()
        } else {
            artist = ""
            title = rawTitle
        }

        return Vinyl(
            id = 0,
            title = title,
            artist = artist,
            year = release.year,
            genre = release.genre?.firstOrNull().orEmpty(),
            label = release.label?.firstOrNull().orEmpty(),
            rating = null,
            condition = "",
            notes = "",
            coverUri = null,
            isOwned = false
        )
    }

    /**
     * Déclenche la recherche sur Discogs
     */
    private fun searchOnDiscogs() {
        val title = binding.titleInput.text.toString().trim()
        val artist = binding.artistInput.text.toString().trim()

        // Construire la requête intelligente
        val query = when {
            artist.isNotBlank() && title.isNotBlank() -> "$artist $title"
            title.isNotBlank() -> title
            artist.isNotBlank() -> artist
            else -> null
        }

        if (query == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.discogs_enter_title_or_artist),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        openDiscogsSearch(query)
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

    private fun fillFromDiscogsRelease(release: DiscogsManager.DiscogsRelease) {
        val rawTitle = release.title?.trim().orEmpty()

        val titleParts = rawTitle.split(Regex(" [-–] "), limit = 2)
        if (titleParts.size == 2) {
            val parsedArtist = titleParts[0].trim()
            val parsedTitle = titleParts[1].trim()
                .replace(Regex("\\s*\\([^)]*\\)\\s*$"), "")
                .replace(Regex("\\s*,\\s*[^,]*$"), "")
                .trim()

            if (parsedArtist.isNotBlank()) {
                binding.artistInput.setText(parsedArtist)
            }
            binding.titleInput.setText(parsedTitle.ifBlank { rawTitle })
        } else {
            binding.titleInput.setText(rawTitle)
        }

        release.year?.let { binding.yearInput.setText(it.toString()) }

        val label = release.label?.firstOrNull() ?: ""
        binding.labelInput.setText(label)

        val mainGenre = release.genre?.firstOrNull() ?: ""
        binding.genreInput.setText(mainGenre, false)

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
        private const val ARG_IS_OWNED = "arg_is_owned"
        private const val ARG_START_BARCODE_SCAN = "arg_start_barcode_scan"
        private const val ARG_START_COVER_SCAN = "arg_start_cover_scan"

        fun newCreate(): VinylEditBottomSheet {
            return VinylEditBottomSheet().apply {
                arguments = bundleOf(
                    ARG_ID to 0L
                )
            }
        }

        fun newCreateScanBarcode(): VinylEditBottomSheet {
            return VinylEditBottomSheet().apply {
                arguments = bundleOf(
                    ARG_ID to 0L,
                    ARG_START_BARCODE_SCAN to true
                )
            }
        }

        fun newCreateScanCover(): VinylEditBottomSheet {
            return VinylEditBottomSheet().apply {
                arguments = bundleOf(
                    ARG_ID to 0L,
                    ARG_START_COVER_SCAN to true
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
                    ARG_COVER_URI to vinyl.coverUri,
                    ARG_IS_OWNED to vinyl.isOwned
                )
            }
        }
    }
}

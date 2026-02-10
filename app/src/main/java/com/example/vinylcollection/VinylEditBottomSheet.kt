package com.example.vinylcollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import com.example.vinylcollection.databinding.BottomSheetVinylBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class VinylEditBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetVinylBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VinylViewModel by activityViewModels()

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

        val genreAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.genres)
        )
        binding.genreInput.setAdapter(genreAdapter)
        binding.genreInput.keyListener = null
        binding.genreInput.setOnClickListener { binding.genreInput.showDropDown() }

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
        binding.conditionInput.setText(args.getString(ARG_CONDITION, ""))
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
                notes = binding.notesInput.text?.toString()?.trim().orEmpty()
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
                        notes = binding.notesInput.text?.toString()?.trim().orEmpty()
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
                    ARG_NOTES to vinyl.notes
                )
            }
        }
    }
}

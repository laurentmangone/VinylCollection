package com.example.vinylcollection

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CoverPreviewDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val uri = arguments?.getString(ARG_URI)?.let(Uri::parse)
        val imageView = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageURI(uri)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(imageView)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    companion object {
        private const val ARG_URI = "arg_uri"

        fun newInstance(uri: String): CoverPreviewDialogFragment {
            return CoverPreviewDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_URI, uri) }
            }
        }
    }
}


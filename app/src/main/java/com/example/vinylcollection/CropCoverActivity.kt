@file:Suppress("ClickableViewAccessibility")
package com.example.vinylcollection

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class CropCoverActivity : AppCompatActivity() {

    private lateinit var imageView: CropImageView
    private lateinit var overlay: CropOverlayView
    private lateinit var cancelButton: MaterialButton
    private lateinit var saveButton: MaterialButton

    private lateinit var bitmap: Bitmap
    private val imageMatrix = Matrix()
    private var minScale = 1f
    private var maxScale = 4f

    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false

    private val scaleDetector by lazy {
        ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentScale = getMatrixScale()
                val targetScale = (currentScale * detector.scaleFactor)
                    .coerceIn(minScale, maxScale)
                val factor = if (currentScale == 0f) 1f else targetScale / currentScale
                imageMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
                constrainMatrix()
                imageView.imageMatrix = imageMatrix
                return true
            }
        })
    }

    private val retryHandler = Handler(Looper.getMainLooper())
    private var pendingUri: Uri? = null
    private var remainingRetries = 6

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_cover)

        imageView = findViewById<CropImageView>(R.id.cropImageView)
        overlay = findViewById(R.id.cropOverlay)
        cancelButton = findViewById(R.id.cropCancelButton)
        saveButton = findViewById(R.id.cropSaveButton)

        val inputUri = intent.getStringExtra(EXTRA_INPUT_URI)?.let(Uri::parse)
        Log.d("CropCover", "Input URI: $inputUri")
        if (inputUri == null) {
            Log.e("CropCover", "No input URI provided")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        pendingUri = inputUri
        tryLoadBitmap()

        imageView.setTouchHandler { event ->
            scaleDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    isDragging = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging && !scaleDetector.isInProgress && event.pointerCount == 1) {
                        val dx = event.x - lastX
                        val dy = event.y - lastY
                        imageMatrix.postTranslate(dx, dy)
                        constrainMatrix()
                        imageView.imageMatrix = imageMatrix
                        lastX = event.x
                        lastY = event.y
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    if (!scaleDetector.isInProgress) {
                        imageView.performClick()
                    }
                }
            }
            true
        }

        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        saveButton.setOnClickListener {
            val outputUri = cropAndSave() ?: run {
                Toast.makeText(this, R.string.error_load_image, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setResult(
                RESULT_OK,
                Intent().putExtra(EXTRA_OUTPUT_URI, outputUri.toString())
            )
            finish()
        }
    }

    private fun tryLoadBitmap() {
        val inputUri = pendingUri ?: return
        val loaded = loadBitmap(inputUri)
        if (loaded != null) {
            bitmap = loaded
            imageView.scaleType = android.widget.ImageView.ScaleType.MATRIX
            imageView.setImageBitmap(bitmap)
            imageView.imageMatrix = imageMatrix
            imageView.isClickable = true

            // Configurer la matrice initiale immédiatement
            imageView.post {
                if (imageView.width > 0 && imageView.height > 0) {
                    setupInitialMatrix()
                } else {
                    imageView.doOnLayout {
                        setupInitialMatrix()
                    }
                }
            }
            return
        }

        if (remainingRetries <= 0) {
            Log.e("CropCover", "Failed to load bitmap after retries: $inputUri")
            Toast.makeText(this, R.string.error_load_image, Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        remainingRetries -= 1
        retryHandler.postDelayed({ tryLoadBitmap() }, 200)
    }

    private fun setupInitialMatrix() {
        val cropRect = overlay.getCropRect()
        val scale = maxOf(
            cropRect.width() / bitmap.width.toFloat(),
            cropRect.height() / bitmap.height.toFloat()
        )
        minScale = scale
        maxScale = scale * 4f

        imageMatrix.reset()
        imageMatrix.postScale(scale, scale)

        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        val dx = cropRect.centerX() - scaledWidth / 2f
        val dy = cropRect.centerY() - scaledHeight / 2f
        imageMatrix.postTranslate(dx, dy)

        constrainMatrix()
        imageView.imageMatrix = imageMatrix
    }

    private fun constrainMatrix() {
        val cropRect = overlay.getCropRect()
        val imageRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        imageMatrix.mapRect(imageRect)

        var dx = 0f
        var dy = 0f

        if (imageRect.left > cropRect.left) {
            dx = cropRect.left - imageRect.left
        } else if (imageRect.right < cropRect.right) {
            dx = cropRect.right - imageRect.right
        }

        if (imageRect.top > cropRect.top) {
            dy = cropRect.top - imageRect.top
        } else if (imageRect.bottom < cropRect.bottom) {
            dy = cropRect.bottom - imageRect.bottom
        }

        if (dx != 0f || dy != 0f) {
            imageMatrix.postTranslate(dx, dy)
        }
    }

    private fun cropAndSave(): Uri? {
        val cropRect = overlay.getCropRect()
        val inverse = Matrix()
        if (!imageMatrix.invert(inverse)) return null

        val bitmapRect = RectF(cropRect)
        inverse.mapRect(bitmapRect)

        val left = bitmapRect.left.coerceIn(0f, bitmap.width.toFloat())
        val top = bitmapRect.top.coerceIn(0f, bitmap.height.toFloat())
        val right = bitmapRect.right.coerceIn(0f, bitmap.width.toFloat())
        val bottom = bitmapRect.bottom.coerceIn(0f, bitmap.height.toFloat())

        val width = (right - left).toInt().coerceAtLeast(1)
        val height = (bottom - top).toInt().coerceAtLeast(1)
        if (width <= 0 || height <= 0) return null

        val cropped = Bitmap.createBitmap(
            bitmap,
            left.toInt(),
            top.toInt(),
            width,
            height
        )

        val coversDir = File(filesDir, "covers")
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }
        val file = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
    }

    private fun getMatrixScale(): Float {
        val values = FloatArray(9)
        imageMatrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        Log.d("CropCover", "Loading bitmap from URI: $uri")
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
            Log.d("CropCover", "Image dimensions: ${options.outWidth}x${options.outHeight}")

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                Log.e("CropCover", "Invalid image dimensions")
                return null
            }

            val maxSize = 2048
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxSize || options.outHeight / sampleSize > maxSize) {
                sampleSize *= 2
            }
            Log.d("CropCover", "Using sample size: $sampleSize")

            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, loadOptions)
            }
            Log.d("CropCover", "Loaded bitmap: ${bitmap?.width}x${bitmap?.height}")
            bitmap
        } catch (e: Exception) {
            Log.e("CropCover", "Error loading bitmap", e)
            e.printStackTrace()
            null
        }
    }

    private fun openInputStream(uri: Uri): java.io.InputStream? {
        return when (uri.scheme) {
            "file" -> FileInputStream(File(uri.path.orEmpty()))
            else -> contentResolver.openInputStream(uri)
        }
    }

    companion object {
        const val EXTRA_INPUT_URI = "extra_input_uri"
        const val EXTRA_OUTPUT_URI = "extra_output_uri"
    }
}

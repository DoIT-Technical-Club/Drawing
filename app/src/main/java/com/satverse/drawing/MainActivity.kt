package com.satverse.drawing

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var drawingView: DrawingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)

        val brushButton = findViewById<ImageButton>(R.id.ib_brush)
        brushButton.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val galleryButton = findViewById<ImageButton>(R.id.ib_gallery)
        galleryButton.setOnClickListener {
            if (isReadStorageAllowed()) {
                val pickPhotoIntent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                startActivityForResult(pickPhotoIntent, GALLERY)
            } else {
                requestStoragePermission()
            }
        }

        val undoButton = findViewById<ImageButton>(R.id.ib_undo)
        undoButton.setOnClickListener {
            drawingView.onClickUndo()
        }

        val redoButton = findViewById<ImageButton>(R.id.ib_redo)
        redoButton.setOnClickListener {
            drawingView.onClickRedo()
        }

        val saveButton = findViewById<ImageButton>(R.id.ib_save)
        saveButton.setOnClickListener {
            if (isReadStorageAllowed()) {
                val bitmap = getBitmapFromView(drawingView)
                saveImageToGallery(bitmap)
            } else {
                requestStoragePermission()
            }
        }

        val clearButton = findViewById<ImageButton>(R.id.ib_clear)
        clearButton.setOnClickListener {
            drawingView.clearDrawing()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY && resultCode == RESULT_OK) {
            try {
                val imageUri = data?.data
                drawingView.setBackgroundImage(imageUri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView.setSizeForBrush(5.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawingView.setSizeForBrush(15.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            Toast.makeText(this, "Required permission to open Gallery.", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted now you can access Gallery.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission required!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        view.draw(canvas)
        return returnedBitmap
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val fileName = "Drawing_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }

        val resolver = contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let {
            try {
                val outputStream = resolver.openOutputStream(it)
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                outputStream?.close()
                Toast.makeText(this, "Image saved to gallery.", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to save image!", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } ?: run {
            Toast.makeText(this, "Failed to create image.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 0
    }

    fun paintClicked(view: View) {
        if (view is ImageButton) {
            val colorTag = view.tag as String
            drawingView.setColor(colorTag)
        }
    }
}
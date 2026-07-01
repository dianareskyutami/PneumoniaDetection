package com.example.pneumoniadetection

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pneumoniadetection.ml.PneumoniaClassifier
import com.google.android.material.button.MaterialButton
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private lateinit var btnGallery: MaterialButton
    private lateinit var btnPredict: MaterialButton
    private lateinit var txtResult: TextView
    private lateinit var txtConfidence: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var classifier: PneumoniaClassifier

    private var selectedBitmap: Bitmap? = null

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->

            uri?.let {

                val bitmap = uriToBitmap(it)

                selectedBitmap = bitmap

                imgPreview.setImageBitmap(bitmap)

                txtResult.text = "-"
                txtConfidence.text = "-"

            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->

            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                bars.left,
                bars.top,
                bars.right,
                bars.bottom
            )

            insets
        }

        classifier = PneumoniaClassifier(this)

        imgPreview = findViewById(R.id.imgPreview)
        btnGallery = findViewById(R.id.btnGallery)
        btnPredict = findViewById(R.id.btnPredict)
        txtResult = findViewById(R.id.txtResult)
        txtConfidence = findViewById(R.id.txtConfidence)
        progressBar = findViewById(R.id.progressBar)

        btnGallery.setOnClickListener {

            pickMedia.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )

        }

        btnPredict.setOnClickListener {

            predictImage()

        }

    }

    private fun predictImage() {

        val bitmap = selectedBitmap

        if (bitmap == null) {

            Toast.makeText(
                this,
                "Silakan pilih gambar terlebih dahulu",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        progressBar.visibility = View.VISIBLE

        btnPredict.isEnabled = false

        try {

            val (label, confidence) = classifier.classify(bitmap)

            txtResult.text = label

            txtConfidence.text =
                String.format(
                    Locale.US,
                    "%.2f%%",
                    confidence * 100
                )

            if (label == "NORMAL") {

                txtResult.setTextColor(
                    ContextCompat.getColor(
                        this,
                        android.R.color.holo_green_dark
                    )
                )

            } else {

                txtResult.setTextColor(
                    ContextCompat.getColor(
                        this,
                        android.R.color.holo_red_dark
                    )
                )

            }

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Prediksi gagal",
                Toast.LENGTH_SHORT
            ).show()

        } finally {

            progressBar.visibility = View.GONE

            btnPredict.isEnabled = true

        }

    }

    private fun uriToBitmap(uri: Uri): Bitmap {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

            val source =
                ImageDecoder.createSource(
                    contentResolver,
                    uri
                )

            ImageDecoder.decodeBitmap(source)

        } else {

            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(
                contentResolver,
                uri
            )

        }

    }

    override fun onDestroy() {

        classifier.close()

        super.onDestroy()

    }

}
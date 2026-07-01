package com.example.pneumoniadetection.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class PneumoniaClassifier(context: Context) {

    companion object {
        private const val IMAGE_SIZE = 224
        private const val PIXEL_SIZE = 3
    }

    private val interpreter: Interpreter

    init {

        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }

        interpreter = Interpreter(
            loadModelFile(context),
            options
        )
    }

    private fun loadModelFile(context: Context): ByteBuffer {

        val fileDescriptor =
            context.assets.openFd("mobilenetv2_pneumonia.tflite")

        val inputStream =
            FileInputStream(fileDescriptor.fileDescriptor)

        val fileChannel =
            inputStream.channel

        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    fun classify(bitmap: Bitmap): Pair<String, Float> {

        // Ubah Hardware Bitmap menjadi ARGB_8888
        val safeBitmap =
            if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }

        val resizedBitmap = Bitmap.createScaledBitmap(
            safeBitmap,
            IMAGE_SIZE,
            IMAGE_SIZE,
            true
        )

        val inputBuffer = convertBitmap(resizedBitmap)

        val output = Array(1) { FloatArray(1) }

        interpreter.run(
            inputBuffer,
            output
        )

        val probability = output[0][0]

        resizedBitmap.recycle()

        if (safeBitmap != bitmap) {
            safeBitmap.recycle()
        }

        return if (probability >= 0.5f) {

            Pair(
                "PNEUMONIA",
                probability
            )

        } else {

            Pair(
                "NORMAL",
                1f - probability
            )

        }

    }

    private fun convertBitmap(bitmap: Bitmap): ByteBuffer {

        val bitmapARGB =
            if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }

        val buffer = ByteBuffer.allocateDirect(
            4 * IMAGE_SIZE * IMAGE_SIZE * PIXEL_SIZE
        )

        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(
            IMAGE_SIZE * IMAGE_SIZE
        )

        bitmapARGB.getPixels(
            pixels,
            0,
            IMAGE_SIZE,
            0,
            0,
            IMAGE_SIZE,
            IMAGE_SIZE
        )

        for (pixel in pixels) {

            val r = ((pixel shr 16) and 0xFF).toFloat()
            val g = ((pixel shr 8) and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()

            // ===========================
            // TANPA preprocess_input()
            // Model sudah memiliki preprocessing sendiri
            // ===========================
            buffer.putFloat(r)
            buffer.putFloat(g)
            buffer.putFloat(b)
        }

        buffer.rewind()

        if (bitmapARGB != bitmap) {
            bitmapARGB.recycle()
        }

        return buffer
    }

    fun close() {
        interpreter.close()
    }
}
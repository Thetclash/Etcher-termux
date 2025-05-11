package com.example.flashdroid

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.*

class MainActivity : AppCompatActivity() {

    private lateinit var selectImageButton: Button
    private lateinit var flashButton: Button
    private lateinit var logTextView: TextView
    private var imagePath: String? = null

    companion object {
        const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        selectImageButton = Button(this).apply {
            text = "Pilih File .img / .iso"
            setOnClickListener { openFilePicker() }
        }

        flashButton = Button(this).apply {
            text = "Mulai Flashing"
            setOnClickListener {
                if (imagePath != null) {
                    flashImage(imagePath!!, "/dev/block/sda") // hardcoded block device (example)
                } else {
                    Toast.makeText(this@MainActivity, "Pilih file terlebih dahulu", Toast.LENGTH_SHORT).show()
                }
            }
        }

        logTextView = TextView(this)

        layout.addView(selectImageButton)
        layout.addView(flashButton)
        layout.addView(logTextView)

        setContentView(layout)
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                val path = getFilePathFromUri(uri)
                imagePath = path
                logTextView.append("File terpilih: $path\n")
            }
        }
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = File(cacheDir, "temp_image.img")
        val outputStream = FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return tempFile.absolutePath
    }

    private fun flashImage(imagePath: String, blockDevice: String) {
        val command = "dd if=$imagePath of=$blockDevice bs=4M"
        Thread {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    runOnUiThread { logTextView.append("$line\n") }
                }
                while (errorReader.readLine().also { line = it } != null) {
                    runOnUiThread { logTextView.append("ERROR: $line\n") }
                }
                process.waitFor()
                runOnUiThread { logTextView.append("Flashing selesai.\n") }
            } catch (e: Exception) {
                runOnUiThread { logTextView.append("Gagal: ${e.message}\n") }
            }
        }.start()
    }
}

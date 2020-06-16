package io.scanbot.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.scanbot.example.FilterTunesActivity.Companion.newIntent
import io.scanbot.example.SelectImageActivity
import io.scanbot.sdk.ScanbotSDK
import io.scanbot.sdk.core.contourdetector.DetectionResult
import io.scanbot.sdk.persistence.Page
import io.scanbot.sdk.process.ImageFilterType
import io.scanbot.sdk.util.FileChooserUtils
import io.scanbot.sdk.util.bitmap.BitmapUtils.decodeQuietly
import java.io.IOException

class SelectImageActivity : AppCompatActivity() {
    private lateinit var progressView: View
    private lateinit var scanbotSDK: ScanbotSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_image)

        progressView = findViewById(R.id.progressBar)
        scanbotSDK = ScanbotSDK(this)

        askPermission()

        findViewById<View>(R.id.import_from_lib_btn).setOnClickListener { openGallery() }
    }

    private fun askPermission() {
        if (checkPermissionNotGranted(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                checkPermissionNotGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE), 999)
        }
    }

    private fun checkPermissionNotGranted(permission: String) =
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PHOTOLIB_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { imageUri ->
                ImportImageToPageTask(imageUri).execute()
                progressView.visibility = View.VISIBLE
            }
        }
    }

    private fun openGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        startActivityForResult(Intent.createChooser(intent, "Select picture"), PHOTOLIB_REQUEST_CODE)
    }

    private fun loadImage(imageUri: Uri): Bitmap? {
        val filePath = FileChooserUtils.getPath(this, imageUri)
        return decodeQuietly(filePath, null)
    }

    private inner class ImportImageToPageTask(private val imageUri: Uri) : AsyncTask<Void, Void, Page?>() {
        override fun doInBackground(objects: Array<Void>): Page? {
            val pageId = scanbotSDK.pageFileStorage().add(loadImage(imageUri)!!)
            val emptyPolygon = emptyList<PointF>()
            val newPage = Page(pageId, emptyPolygon, DetectionResult.OK, ImageFilterType.NONE)
            return try {
                scanbotSDK.pageProcessor().detectDocument(newPage)
            } catch (ex: IOException) {
                Log.e("ImportImageToPageTask", "Error detecting document on page " + newPage.pageId)
                null
            }
        }

        override fun onPostExecute(result: Page?) {
            super.onPostExecute(result)
            progressView.visibility = View.GONE

            result?.let {
                val intent = newIntent(this@SelectImageActivity, result)
                startActivity(intent)
            }
        }

    }

    companion object {
        private const val PHOTOLIB_REQUEST_CODE = 5711
    }
}
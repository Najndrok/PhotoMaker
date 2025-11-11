package com.example.photomarker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var imageView: ImageView
    private var imageCapture: ImageCapture? = null
    private var currentBitmap: Bitmap? = null
    private var locationText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.viewFinder)
        captureButton = findViewById(R.id.captureButton)
        imageView = findViewById(R.id.capturedImage)
        requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION), 10)
        startCamera()
        fetchLocation()
        captureButton.setOnClickListener { takePhoto() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) { e.printStackTrace() }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(externalMediaDirs.first(), SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(Date()) + ".jpg")
        val output = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(output, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) { exc.printStackTrace() }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bmp = BitmapFactory.decodeFile(photoFile.absolutePath).copy(Bitmap.Config.ARGB_8888, true)
                    currentBitmap = bmp
                    imageView.setImageBitmap(bmp)
                    imageView.setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) drawMarker(event.x, event.y)
                        true
                    }
                    Toast.makeText(this@MainActivity, "Kliknij, aby dodać kropkę", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun drawMarker(x: Float, y: Float) {
        val bmp = currentBitmap ?: return
        val canvas = Canvas(bmp)
        val paint = Paint().apply { color = Color.YELLOW; style = Paint.Style.FILL }
        canvas.drawCircle(x, y, 20f, paint)
        val textPaint = Paint().apply { color = Color.WHITE; textSize = 40f }
        canvas.drawText(locationText, 20f, 50f, textPaint)
        imageView.setImageBitmap(bmp)
        val out = File(externalMediaDirs.first(), "marked_${System.currentTimeMillis()}.jpg")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    }

    private fun fetchLocation() {
        val client = LocationServices.getFusedLocationProviderClient(this)
        try {
            client.lastLocation.addOnSuccessListener {
                if (it != null) locationText = "Lat: %.5f, Lon: %.5f".format(it.latitude, it.longitude)
            }
        } catch (_: SecurityException) {}
    }
}

package com.xavi.imageia.ui.home

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import android.Manifest
import android.content.ContentValues
import android.provider.MediaStore
import com.xavi.imageia.databinding.FragmentHomeBinding
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    var LOG_TAG = "PRUEBAS"

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val permissionGranted = permissions.entries.all { it.value }
            if (!permissionGranted) {
                Toast.makeText(context, "Permissions not granted.", Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Configurar el botón button4
        binding.button4.setOnClickListener {
            takePhoto()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        homeViewModel.text.observe(viewLifecycleOwner) { /* Observa cambios en ViewModel */ }

        return binding.root
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: run {
            Log.e(LOG_TAG, "imageCapture no está inicializado")
            return
        }

        // Crear nombre con marca de tiempo
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(LOG_TAG, "Error al capturar la foto: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    val savedUri = output.savedUri
                    val msg = "Foto capturada y guardada: ${output.savedUri}"
                    //Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    Log.d(LOG_TAG, msg)
                    val imageBytes = savedUri?.let { uri ->
                        requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.readBytes()
                        }
                    }
                    if (imageBytes != null) {
                        // Step 2: Send image to server via POST request
                        val postUrl = "http://10.0.2.2:3000/image"
                        val response = uploadImage(postUrl, imageBytes)
                        Log.i("subirFoto","paso "+ response)
                    } else {
                         Log.i("b","Failed to download image.")
                    }
                }
            }
        )



    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(LOG_TAG, "Error al vincular casos de uso", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val LOG_TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
    private fun uploadImage(postUrl: String, imageBytes: ByteArray): String {
        return try {
            val url = URL(postUrl)
            val boundary = "Boundary-" + System.currentTimeMillis()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            val outputStream = DataOutputStream(connection.outputStream)
            outputStream.writeBytes("--$boundary\r\n")
            outputStream.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"\r\n")
            outputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n")
            outputStream.write(imageBytes)
            outputStream.writeBytes("\r\n--$boundary--\r\n")

            Log.i(LOG_TAG, outputStream.toString())

            outputStream.flush()
            outputStream.close()

            // Leer respuesta del servidor
            val reader = BufferedReader(InputStreamReader(connection.inputStream))

            Log.i(LOG_TAG, reader.toString())

            val response = StringBuilder()


            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            response.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }

}

package com.xavi.imageia

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.xavi.imageia.databinding.ActivityMainBinding


import java.util.Locale

class MainActivity : AppCompatActivity() , OnInitListener {

    private lateinit var binding: ActivityMainBinding

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val permissionGranted = permissions.entries.all { it.value }
            if (!permissionGranted) {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
            } else {
                Log.i("main","hay permisos")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        if (allPermissionsGranted()) {
            val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("permissions_granted", true).apply()

        } else {
            requestPermissions()
        }

//        if (!isUserVerified()){
//            navView.menu.clear()
//            navView.inflateMenu(R.menu.menu_sin_verificar)
//            navController.navigate(R.id.Compte)
//        }else{
//            navView.menu.clear()
//            navView.inflateMenu(R.menu.bottom_nav_menu)
//            navController.navigate(R.id.Ullada)
//        }
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.Ullada, R.id.Historial, R.id.Compte
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
    private fun isUserVerified(): Boolean {
        val prefs = getSharedPreferences("UserData", MODE_PRIVATE)
        val verified = prefs.getBoolean("verified", false)
        Log.i("MainActivity", "isUserVerified() devuelve: $verified")
        return verified
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this, it
        ) == PackageManager.PERMISSION_GRANTED
    }
    private fun requestPermissions() {
        activityResultLauncher.launch(MainActivity.REQUIRED_PERMISSIONS)
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

    override fun onInit(status: Int) {

    }
    override fun onResume() {
        super.onResume()
        actualizarMenu()
    }

    private fun actualizarMenu() {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val navView: BottomNavigationView = binding.navView

        if (!isUserVerified()) {
            navView.menu.clear()
            navView.inflateMenu(R.menu.menu_sin_verificar)
            navController.navigate(R.id.Compte)
        } else {
            navView.menu.clear()
            navView.inflateMenu(R.menu.bottom_nav_menu)
            navController.navigate(R.id.Ullada)
        }

    }


}
package com.fatec.armeasure

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk

/**
 * Main launcher activity for the AR Measurement app
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if ARCore is supported
        checkArCoreSupport()

        // Setup start button
        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (checkCameraPermission()) {
                startArMeasurement()
            } else {
                requestCameraPermission()
            }
        }
    }

    /**
     * Check if ARCore is supported and installed on this device
     */
    private fun checkArCoreSupport() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (!availability.isSupported) {
            Toast.makeText(
                this,
                getString(R.string.ar_not_supported),
                Toast.LENGTH_LONG
            ).show()
            findViewById<Button>(R.id.startButton).isEnabled = false
        }
    }

    /**
     * Check if camera permission is granted
     */
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request camera permission from user
     */
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    /**
     * Handle permission request result
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startArMeasurement()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.camera_permission_required),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Start the AR measurement activity
     */
    private fun startArMeasurement() {
        val intent = Intent(this, ArMeasurementActivity::class.java)
        startActivity(intent)
    }
}

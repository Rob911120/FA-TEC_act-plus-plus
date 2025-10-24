package com.fatec.armeasure

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.fatec.armeasure.utils.DxfGenerator
import com.fatec.armeasure.utils.Point3D
import com.fatec.armeasure.rendering.BackgroundRenderer
import com.fatec.armeasure.rendering.PlaneRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.Camera
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.view.Display
import android.view.WindowManager
import android.util.Log

/**
 * Simplified AR measurement activity using pure ARCore
 * Handles AR session, point capture, and DXF export
 */
class ArMeasurementActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var pointsCountText: TextView
    private lateinit var resetButton: Button
    private lateinit var exportButton: Button
    private lateinit var surfaceView: GLSurfaceView

    private var arSession: Session? = null
    private val backgroundRenderer = BackgroundRenderer()
    private val planeRenderer = PlaneRenderer()

    // Camera matrices
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    // Track detected planes count for user feedback
    private var lastPlaneCount = 0

    // List to store captured 3D points
    private val capturedPoints = mutableListOf<Point3D>()

    // List to store AR anchors
    private val anchors = mutableListOf<Anchor>()

    // Origin point (first point captured)
    private var originPoint: Point3D? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_measurement)

        // Initialize views
        statusText = findViewById(R.id.statusText)
        pointsCountText = findViewById(R.id.pointsCountText)
        resetButton = findViewById(R.id.resetButton)
        exportButton = findViewById(R.id.exportButton)

        // Setup ARCore session
        setupArSession()

        // Setup button listeners
        setupButtons()

        // Update UI
        updatePointsCount()
    }

    /**
     * Setup ARCore session with basic configuration
     */
    private fun setupArSession() {
        try {
            // Create AR session
            arSession = Session(this)

            // Configure session for plane detection
            val config = Config(arSession)
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            arSession?.configure(config)

            // Create GLSurfaceView for rendering
            surfaceView = GLSurfaceView(this)
            surfaceView.preserveEGLContextOnPause = true
            surfaceView.setEGLContextClientVersion(2)
            surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            surfaceView.setRenderer(object : GLSurfaceView.Renderer {
                override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

                    // Enable depth testing
                    GLES20.glEnable(GLES20.GL_DEPTH_TEST)

                    // Initialize renderers
                    backgroundRenderer.createOnGlThread(this@ArMeasurementActivity)
                    planeRenderer.createOnGlThread(this@ArMeasurementActivity)

                    // Set camera texture
                    arSession?.setCameraTextureName(backgroundRenderer.getTextureId())
                }

                override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                    GLES20.glViewport(0, 0, width, height)

                    // Notify ARCore of display rotation
                    val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
                    arSession?.setDisplayGeometry(display.rotation, width, height)
                }

                override fun onDrawFrame(gl: GL10?) {
                    // Clear screen
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

                    // Update AR session and render camera background
                    arSession?.let { session ->
                        try {
                            val frame = session.update()
                            val camera = frame.camera

                            // Only render if tracking
                            if (camera.trackingState != TrackingState.TRACKING) {
                                return@let
                            }

                            // Draw camera background
                            backgroundRenderer.draw(frame)

                            // Get camera matrices
                            camera.getViewMatrix(viewMatrix, 0)
                            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)

                            // Get all tracked planes
                            val allPlanes = session.getAllTrackables(Plane::class.java)
                            val trackingPlanes = allPlanes.filter {
                                it.trackingState == TrackingState.TRACKING
                            }

                            // Update status if plane count changed
                            if (trackingPlanes.size != lastPlaneCount) {
                                lastPlaneCount = trackingPlanes.size
                                runOnUiThread {
                                    if (trackingPlanes.isEmpty()) {
                                        statusText.text = "Sök efter ytor... Rikta kameran mot golv/bord"
                                    } else {
                                        statusText.text = "Ytor hittade! Tryck för att placera punkt (${trackingPlanes.size} ytor)"
                                    }
                                }
                            }

                            // Draw detected planes
                            planeRenderer.drawPlanes(trackingPlanes, viewMatrix, projectionMatrix)

                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e("ARMeasure", "Error in onDrawFrame", e)
                        }
                    }
                }
            })

            surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

            // Add surfaceView to container
            val container = findViewById<android.widget.FrameLayout>(R.id.arFragmentContainer)
            container.addView(surfaceView)

            // Setup touch listener for placing points
            surfaceView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    handleTap(event.x, event.y)
                }
                true
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to create AR session: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    /**
     * Handle tap events to place measurement points
     */
    private fun handleTap(x: Float, y: Float) {
        arSession?.let { session ->
            try {
                val frame = session.update()

                // Log tap attempt
                Log.d("ARMeasure", "Tap at ($x, $y)")

                // Perform hit test at tap location
                val hits = frame.hitTest(x, y)

                Log.d("ARMeasure", "Hit test returned ${hits.size} hits")

                // Find first hit on a plane
                for ((index, hit) in hits.withIndex()) {
                    val trackable = hit.trackable
                    Log.d("ARMeasure", "Hit $index: trackable type = ${trackable?.javaClass?.simpleName}")

                    if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose) &&
                        trackable.trackingState == TrackingState.TRACKING) {

                        Log.d("ARMeasure", "Valid plane hit! Creating anchor")

                        // Create anchor at hit location
                        val anchor = hit.createAnchor()

                        // Get 3D position from anchor
                        val pose = anchor.pose
                        val point = Point3D(
                            x = pose.tx(),
                            y = pose.ty(),
                            z = pose.tz(),
                            label = "P${capturedPoints.size}"
                        )

                        Log.d("ARMeasure", "Point created: ${point.label} at (${point.x}, ${point.y}, ${point.z})")

                        // Set origin if this is the first point
                        if (originPoint == null) {
                            originPoint = point
                            runOnUiThread {
                                statusText.text = "Startpunkt (P0) satt! Tryck för fler punkter"
                                Toast.makeText(this@ArMeasurementActivity, "P0 placerad!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@ArMeasurementActivity, "${point.label} placerad!", Toast.LENGTH_SHORT).show()
                            }
                        }

                        // Add point to list
                        capturedPoints.add(point)
                        anchors.add(anchor)

                        // Update UI
                        runOnUiThread {
                            updatePointsCount()
                        }

                        // Haptic feedback
                        surfaceView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

                        return@let
                    }
                }

                // No valid hit found
                Log.d("ARMeasure", "No valid plane hit found")
                runOnUiThread {
                    if (lastPlaneCount == 0) {
                        Toast.makeText(this@ArMeasurementActivity, "Vänta på ytdetektering...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ArMeasurementActivity, "Tryck på en detekterad yta (blå område)", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ARMeasure", "Error in handleTap", e)
                runOnUiThread {
                    Toast.makeText(this@ArMeasurementActivity, "Fel: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Setup button click listeners
     */
    private fun setupButtons() {
        resetButton.setOnClickListener {
            resetMeasurement()
        }

        exportButton.setOnClickListener {
            if (capturedPoints.isNotEmpty()) {
                exportToDxf()
            } else {
                Toast.makeText(this, "Inga punkter att exportera", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Reset all measurements
     */
    private fun resetMeasurement() {
        // Clear points
        capturedPoints.clear()

        // Remove all anchors
        anchors.forEach { it.detach() }
        anchors.clear()

        // Clear origin
        originPoint = null

        // Update UI
        updatePointsCount()
        statusText.text = getString(R.string.tap_to_place_point)

        Toast.makeText(this, "Mätning återställd", Toast.LENGTH_SHORT).show()
    }

    /**
     * Export captured points to DXF file
     */
    private fun exportToDxf() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Convert points to relative coordinates (relative to first point)
                val relativePoints = DxfGenerator.toRelativeCoordinates(capturedPoints)

                // Generate DXF file
                val dxfFile = DxfGenerator.generateDxf(
                    context = this@ArMeasurementActivity,
                    points = relativePoints
                )

                withContext(Dispatchers.Main) {
                    if (dxfFile != null && dxfFile.exists()) {
                        Toast.makeText(
                            this@ArMeasurementActivity,
                            getString(R.string.dxf_exported, dxfFile.absolutePath),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ArMeasurementActivity,
                            getString(R.string.export_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ArMeasurementActivity,
                        getString(R.string.export_failed) + ": ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Update the points count display
     */
    private fun updatePointsCount() {
        pointsCountText.text = getString(R.string.points_count, capturedPoints.size)
    }

    override fun onResume() {
        super.onResume()

        // Resume AR session
        try {
            arSession?.resume()
            surfaceView.onResume()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to resume AR session: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()

        // Pause AR session
        surfaceView.onPause()
        arSession?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Close AR session
        arSession?.close()
        arSession = null
    }
}

package com.fatec.armeasure

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import io.github.sceneview.node.SphereNode
import com.fatec.armeasure.utils.DxfGenerator
import com.fatec.armeasure.utils.Point3D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main AR measurement activity
 * Handles AR session, point capture, and DXF export
 */
class ArMeasurementActivity : AppCompatActivity() {

    private lateinit var arSceneView: ArSceneView
    private lateinit var statusText: TextView
    private lateinit var pointsCountText: TextView
    private lateinit var resetButton: Button
    private lateinit var exportButton: Button

    // List to store captured 3D points
    private val capturedPoints = mutableListOf<Point3D>()

    // List to store AR anchors for visual feedback
    private val anchors = mutableListOf<Anchor>()

    // Origin point (first point captured)
    private var originPoint: Point3D? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_measurement)

        // Initialize views
        arSceneView = findViewById(R.id.arSceneView)
        statusText = findViewById(R.id.statusText)
        pointsCountText = findViewById(R.id.pointsCountText)
        resetButton = findViewById(R.id.resetButton)
        exportButton = findViewById(R.id.exportButton)

        // Setup AR scene
        setupArScene()

        // Setup button listeners
        setupButtons()

        // Update UI
        updatePointsCount()
    }

    /**
     * Setup the AR scene view and tap listener
     */
    private fun setupArScene() {
        // Listen for tap events on the AR scene
        arSceneView.onTapAr = { hitResult: HitResult, _ ->
            // Check if we hit a plane (surface detection)
            val trackable = hitResult.trackable
            if (trackable is Plane && trackable.isPoseInPolygon(hitResult.hitPose)) {
                // Create anchor at tap location
                val anchor = hitResult.createAnchor()

                if (anchor != null) {
                    // Get 3D position from anchor
                    val pose = anchor.pose
                    val point = Point3D(
                        x = pose.tx(),
                        y = pose.ty(),
                        z = pose.tz(),
                        label = "P${capturedPoints.size}"
                    )

                    // Set origin if this is the first point
                    if (originPoint == null) {
                        originPoint = point
                        statusText.text = "Startpunkt satt! Tryck för fler punkter"
                    }

                    // Add point to list
                    capturedPoints.add(point)
                    anchors.add(anchor)

                    // Add visual marker at the point
                    addVisualMarker(pose.tx(), pose.ty(), pose.tz())

                    // Update UI
                    updatePointsCount()

                    // Haptic feedback
                    arSceneView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                }
            }
        }
    }

    /**
     * Add a visual sphere marker at the captured point
     */
    private fun addVisualMarker(x: Float, y: Float, z: Float) {
        try {
            // Create a small sphere to mark the point
            val sphereNode = SphereNode(
                engine = arSceneView.engine,
                radius = 0.02f, // 2cm radius
                center = Position(x, y, z)
            )

            // Set color (red for first point, blue for others)
            val color = if (capturedPoints.size == 1) {
                com.google.android.filament.utils.Color(1.0f, 0.0f, 0.0f) // Red
            } else {
                com.google.android.filament.utils.Color(0.0f, 0.5f, 1.0f) // Blue
            }

            // Add to scene
            arSceneView.addChild(sphereNode)

        } catch (e: Exception) {
            e.printStackTrace()
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

        // Clear visual markers
        arSceneView.children.clear()

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
        arSceneView.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        arSceneView.onPause(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        arSceneView.destroy()
    }
}

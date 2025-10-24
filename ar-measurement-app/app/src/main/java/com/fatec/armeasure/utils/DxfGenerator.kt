package com.fatec.armeasure.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a 3D point
 */
data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val label: String = ""
)

/**
 * Utility class for generating DXF (Drawing Exchange Format) files from 3D points
 */
class DxfGenerator {

    companion object {
        /**
         * Generates a DXF file from a list of 3D points
         *
         * @param context Android context for file access
         * @param points List of 3D points to include in the DXF
         * @param fileName Optional custom filename
         * @return File object representing the generated DXF file, or null if failed
         */
        fun generateDxf(
            context: Context,
            points: List<Point3D>,
            fileName: String? = null
        ): File? {
            if (points.isEmpty()) {
                return null
            }

            try {
                // Generate filename with timestamp if not provided
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val dxfFileName = fileName ?: "measurement_$timestamp.dxf"

                // Get the Downloads directory
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val dxfFile = File(downloadsDir, dxfFileName)
                val writer = FileWriter(dxfFile)

                // Write DXF header
                writer.write(getDxfHeader())

                // Write entities section with points and polyline
                writer.write(getDxfEntities(points))

                // Write DXF footer
                writer.write(getDxfFooter())

                writer.close()

                return dxfFile
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        /**
         * Generates the DXF file header
         */
        private fun getDxfHeader(): String {
            return """
0
SECTION
2
HEADER
9
${'$'}ACADVER
1
AC1015
9
${'$'}INSBASE
10
0.0
20
0.0
30
0.0
9
${'$'}EXTMIN
10
0.0
20
0.0
30
0.0
9
${'$'}EXTMAX
10
1000.0
20
1000.0
30
1000.0
0
ENDSEC
0
SECTION
2
TABLES
0
TABLE
2
LAYER
70
1
0
LAYER
2
MEASUREMENTS
70
0
62
7
6
CONTINUOUS
0
ENDTAB
0
ENDSEC
""".trimIndent()
        }

        /**
         * Generates the DXF entities section with points and polyline
         */
        private fun getDxfEntities(points: List<Point3D>): String {
            val sb = StringBuilder()

            sb.appendLine("0")
            sb.appendLine("SECTION")
            sb.appendLine("2")
            sb.appendLine("ENTITIES")

            // Add individual points as POINT entities
            points.forEachIndexed { index, point ->
                sb.appendLine("0")
                sb.appendLine("POINT")
                sb.appendLine("8")
                sb.appendLine("MEASUREMENTS")
                sb.appendLine("10")
                sb.appendLine(point.x.toString())
                sb.appendLine("20")
                sb.appendLine(point.y.toString())
                sb.appendLine("30")
                sb.appendLine(point.z.toString())

                // Add text label for each point
                sb.appendLine("0")
                sb.appendLine("TEXT")
                sb.appendLine("8")
                sb.appendLine("MEASUREMENTS")
                sb.appendLine("10")
                sb.appendLine(point.x.toString())
                sb.appendLine("20")
                sb.appendLine(point.y.toString())
                sb.appendLine("30")
                sb.appendLine(point.z.toString())
                sb.appendLine("40")
                sb.appendLine("0.1") // Text height
                sb.appendLine("1")
                sb.appendLine(if (point.label.isNotEmpty()) point.label else "P$index")
            }

            // Add a 3D polyline connecting all points
            if (points.size > 1) {
                sb.appendLine("0")
                sb.appendLine("POLYLINE")
                sb.appendLine("8")
                sb.appendLine("MEASUREMENTS")
                sb.appendLine("66")
                sb.appendLine("1")
                sb.appendLine("70")
                sb.appendLine("8") // 3D polyline flag

                points.forEach { point ->
                    sb.appendLine("0")
                    sb.appendLine("VERTEX")
                    sb.appendLine("8")
                    sb.appendLine("MEASUREMENTS")
                    sb.appendLine("10")
                    sb.appendLine(point.x.toString())
                    sb.appendLine("20")
                    sb.appendLine(point.y.toString())
                    sb.appendLine("30")
                    sb.appendLine(point.z.toString())
                    sb.appendLine("70")
                    sb.appendLine("32") // 3D polyline vertex
                }

                sb.appendLine("0")
                sb.appendLine("SEQEND")
            }

            sb.appendLine("0")
            sb.appendLine("ENDSEC")

            return sb.toString()
        }

        /**
         * Generates the DXF file footer
         */
        private fun getDxfFooter(): String {
            return """
0
EOF
""".trimIndent()
        }

        /**
         * Converts a list of points to relative coordinates (relative to first point)
         */
        fun toRelativeCoordinates(points: List<Point3D>): List<Point3D> {
            if (points.isEmpty()) return emptyList()

            val origin = points.first()
            return points.map { point ->
                Point3D(
                    x = point.x - origin.x,
                    y = point.y - origin.y,
                    z = point.z - origin.z,
                    label = point.label
                )
            }
        }
    }
}

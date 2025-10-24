package com.fatec.armeasure.rendering

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Renders detected AR planes for visual feedback
 */
class PlaneRenderer {
    private var vertexBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null

    private var planeProgram = 0
    private var planePositionAttrib = 0
    private var planeModelViewProjectionUniform = 0
    private var planeColorUniform = 0

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_ModelViewProjection;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_ModelViewProjection * a_Position;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """

        // Simple quad for plane visualization
        private val PLANE_VERTICES = floatArrayOf(
            -0.5f, 0.0f, -0.5f,
            -0.5f, 0.0f, 0.5f,
            0.5f, 0.0f, 0.5f,
            0.5f, 0.0f, -0.5f
        )

        private val PLANE_INDICES = shortArrayOf(
            0, 1, 2,
            0, 2, 3
        )
    }

    fun createOnGlThread(context: Context) {
        // Create vertex buffer
        val vb = ByteBuffer.allocateDirect(PLANE_VERTICES.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vb.put(PLANE_VERTICES)
        vb.position(0)
        vertexBuffer = vb

        // Create index buffer
        val ib = ByteBuffer.allocateDirect(PLANE_INDICES.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        ib.put(PLANE_INDICES)
        ib.position(0)
        indexBuffer = ib

        // Create shader program
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        planeProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(planeProgram, vertexShader)
        GLES20.glAttachShader(planeProgram, fragmentShader)
        GLES20.glLinkProgram(planeProgram)

        planePositionAttrib = GLES20.glGetAttribLocation(planeProgram, "a_Position")
        planeModelViewProjectionUniform = GLES20.glGetUniformLocation(planeProgram, "u_ModelViewProjection")
        planeColorUniform = GLES20.glGetUniformLocation(planeProgram, "u_Color")
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun drawPlanes(planes: Collection<Plane>, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        if (planes.isEmpty()) return

        // Enable blending for semi-transparent planes
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glUseProgram(planeProgram)

        for (plane in planes) {
            if (plane.trackingState != TrackingState.TRACKING || plane.subsumedBy != null) {
                continue
            }

            val centerPose = plane.centerPose
            val modelMatrix = FloatArray(16)
            centerPose.toMatrix(modelMatrix, 0)

            // Scale the plane based on its extent
            val scaleMatrix = FloatArray(16)
            Matrix.setIdentityM(scaleMatrix, 0)
            Matrix.scaleM(scaleMatrix, 0, plane.extentX, 1f, plane.extentZ)

            val scaledModel = FloatArray(16)
            Matrix.multiplyMM(scaledModel, 0, modelMatrix, 0, scaleMatrix, 0)

            // Calculate MVP matrix
            val modelView = FloatArray(16)
            Matrix.multiplyMM(modelView, 0, viewMatrix, 0, scaledModel, 0)

            val mvp = FloatArray(16)
            Matrix.multiplyMM(mvp, 0, projectionMatrix, 0, modelView, 0)

            // Set uniforms
            GLES20.glUniformMatrix4fv(planeModelViewProjectionUniform, 1, false, mvp, 0)

            // Semi-transparent blue/green color for planes
            GLES20.glUniform4f(planeColorUniform, 0.2f, 0.7f, 0.9f, 0.3f)

            // Set vertex positions
            GLES20.glVertexAttribPointer(
                planePositionAttrib,
                3,
                GLES20.GL_FLOAT,
                false,
                0,
                vertexBuffer
            )
            GLES20.glEnableVertexAttribArray(planePositionAttrib)

            // Draw
            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                PLANE_INDICES.size,
                GLES20.GL_UNSIGNED_SHORT,
                indexBuffer
            )

            GLES20.glDisableVertexAttribArray(planePositionAttrib)
        }

        GLES20.glDisable(GLES20.GL_BLEND)
    }
}

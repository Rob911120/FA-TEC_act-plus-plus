package com.fatec.armeasure.rendering

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES11Ext
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renders the AR camera background
 */
class BackgroundRenderer {
    private var quadVertices: FloatBuffer? = null
    private var quadTexCoord: FloatBuffer? = null
    private var quadTexCoordTransformed: FloatBuffer? = null

    private var quadProgram = 0
    private var quadPositionAttrib = 0
    private var quadTexCoordAttrib = 0
    private var textureId = -1

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES sTexture;
            void main() {
                gl_FragColor = texture2D(sTexture, v_TexCoord);
            }
        """

        private val QUAD_COORDS = floatArrayOf(
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
        )

        private val QUAD_TEXCOORDS = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )
    }

    fun createOnGlThread(context: Context) {
        // Create vertex buffer
        val vertexBuffer = ByteBuffer.allocateDirect(QUAD_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(QUAD_COORDS)
        vertexBuffer.position(0)
        quadVertices = vertexBuffer

        // Create texture coordinate buffers
        val texCoordBuffer = ByteBuffer.allocateDirect(QUAD_TEXCOORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        texCoordBuffer.put(QUAD_TEXCOORDS)
        texCoordBuffer.position(0)
        quadTexCoord = texCoordBuffer

        val texCoordTransformedBuffer = ByteBuffer.allocateDirect(QUAD_TEXCOORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        quadTexCoordTransformed = texCoordTransformedBuffer

        // Create texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        // Create shader program
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        quadProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(quadProgram, vertexShader)
        GLES20.glAttachShader(quadProgram, fragmentShader)
        GLES20.glLinkProgram(quadProgram)

        quadPositionAttrib = GLES20.glGetAttribLocation(quadProgram, "a_Position")
        quadTexCoordAttrib = GLES20.glGetAttribLocation(quadProgram, "a_TexCoord")
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun getTextureId(): Int {
        return textureId
    }

    fun draw(frame: Frame) {
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformCoordinates2d(
                com.google.ar.core.Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                quadVertices,
                com.google.ar.core.Coordinates2d.TEXTURE_NORMALIZED,
                quadTexCoordTransformed
            )
        }

        // Disable depth test and write
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)

        // Bind texture
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        // Use shader program
        GLES20.glUseProgram(quadProgram)

        // Set vertex positions
        GLES20.glVertexAttribPointer(
            quadPositionAttrib,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            quadVertices
        )
        GLES20.glEnableVertexAttribArray(quadPositionAttrib)

        // Set texture coordinates
        GLES20.glVertexAttribPointer(
            quadTexCoordAttrib,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            quadTexCoordTransformed
        )
        GLES20.glEnableVertexAttribArray(quadTexCoordAttrib)

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(quadPositionAttrib)
        GLES20.glDisableVertexAttribArray(quadTexCoordAttrib)

        // Re-enable depth test
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }
}

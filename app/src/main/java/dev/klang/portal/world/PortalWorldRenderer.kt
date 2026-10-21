package dev.klang.portal.world

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

/** Render-on-demand GLES 2 procedural scenery, with time and smoothed sensor parallax. */
class PortalWorldRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private val parallax = AtomicReference(floatArrayOf(0f, 0f))
    private val resolution = floatArrayOf(1f, 1f)
    private var program = 0
    private var positionLocation = -1
    private var uvLocation = -1
    private var resolutionLocation = -1
    private var parallaxLocation = -1
    private var hourLocation = -1
    private var timeLocation = -1
    private val startSeconds = System.currentTimeMillis() / 1000L
    private var localHour = readHour()
    private val quad = java.nio.ByteBuffer.allocateDirect(64)
        .order(java.nio.ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(floatArrayOf(
                -1f, -1f, 0f, 0f,
                -1f,  1f, 0f, 1f,
                 1f, -1f, 1f, 0f,
                 1f,  1f, 1f, 1f
            ))
            position(0)
        }

    fun setParallax(x: Float, y: Float) {
        parallax.set(floatArrayOf(x.coerceIn(-1f, 1f), y.coerceIn(-1f, 1f)))
    }
    fun updateClock() { localHour = readHour() }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.08f, 0.13f, 0.12f, 1f)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        val vertex = compileShader(GLES20.GL_VERTEX_SHADER, readAsset("shaders/fullscreen.vert"))
        val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, readAsset("shaders/portal_world.frag"))
        if (vertex == 0 || fragment == 0) {
            if (vertex != 0) GLES20.glDeleteShader(vertex)
            if (fragment != 0) GLES20.glDeleteShader(fragment)
            return
        }
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertex)
        GLES20.glAttachShader(program, fragment)
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        if (status[0] == 0) {
            android.util.Log.e("KlangWorld", "Shader link failed: " + GLES20.glGetProgramInfoLog(program))
            GLES20.glDeleteProgram(program)
            program = 0
            return
        }
        positionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        uvLocation = GLES20.glGetAttribLocation(program, "aTexCoord")
        resolutionLocation = GLES20.glGetUniformLocation(program, "uResolution")
        parallaxLocation = GLES20.glGetUniformLocation(program, "uParallax")
        hourLocation = GLES20.glGetUniformLocation(program, "uHour")
        timeLocation = GLES20.glGetUniformLocation(program, "uTime")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, max(1, height))
        resolution[0] = max(1, width).toFloat()
        resolution[1] = max(1, height).toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0) return
        val elapsed = ((System.currentTimeMillis() / 1000L - startSeconds) % 86400L).toFloat()
        val offset = parallax.get()
        GLES20.glUseProgram(program)
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glEnableVertexAttribArray(uvLocation)
        quad.position(0)
        GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT, false, 16, quad)
        quad.position(2)
        GLES20.glVertexAttribPointer(uvLocation, 2, GLES20.GL_FLOAT, false, 16, quad)
        GLES20.glUniform2f(resolutionLocation, resolution[0], resolution[1])
        GLES20.glUniform2f(parallaxLocation, offset[0], offset[1])
        GLES20.glUniform1f(hourLocation, localHour)
        GLES20.glUniform1f(timeLocation, elapsed)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionLocation)
        GLES20.glDisableVertexAttribArray(uvLocation)
    }

    private fun readHour(): Float {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60f
    }
    private fun readAsset(path: String): String =
        context.assets.open(path).use { BufferedReader(InputStreamReader(it)).readText() }
    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            android.util.Log.e("KlangWorld", "Shader compile failed: " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}

package dev.klang.portal.world

import android.content.Context
import android.opengl.GLSurfaceView

/** Low-cost GLES 2 scene backdrop. Input controls are drawn on the HUD layer above it. */
class FantasyWorldView(context: Context) : GLSurfaceView(context) {
    private val portalRenderer = PortalWorldRenderer(context.applicationContext)
    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = false
        setRenderer(portalRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        setZOrderOnTop(false)
    }
    fun setParallax(x: Float, y: Float) {
        portalRenderer.setParallax(x, y)
        requestRender()
    }
    fun refreshWorldClock() {
        portalRenderer.updateClock()
        requestRender()
    }
}

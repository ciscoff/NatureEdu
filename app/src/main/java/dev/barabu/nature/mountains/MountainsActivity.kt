package dev.barabu.nature.mountains

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import dev.barabu.base.Logging
import dev.barabu.base.extentions.isActualGlEsSupporting
import dev.barabu.nature.mountains.gl.MountainsRenderer

class MountainsActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView

    private lateinit var renderer: MountainsRenderer

    private var isRendererSet = false

    private val touchListener = object : View.OnTouchListener {
        var prevX: Float = 0f
        var prevY: Float = 0f

        override fun onTouch(v: View, event: MotionEvent): Boolean {

            return when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    prevX = event.x
                    prevY = event.y
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dX = event.x - prevX
                    val dY = event.y - prevY
                    prevX = event.x
                    prevY = event.y

                    glSurfaceView.queueEvent {
                        renderer.handleTouchDrag(dX, dY)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    v.performClick()
                }

                else -> false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderer = MountainsRenderer(this)

        glSurfaceView = GLSurfaceView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            if (isActualGlEsSupporting) {
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                isRendererSet = true
            } else {
                Logging.d("This device does not support OpenGL ES 3.0.")
            }
            setOnTouchListener(touchListener)
        }

        setContentView(glSurfaceView)
    }
}
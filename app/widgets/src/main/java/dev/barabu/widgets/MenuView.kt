package dev.barabu.widgets

import android.content.Context
import android.graphics.PointF
import android.transition.TransitionManager
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import dev.barabu.widgets.domain.Effect
import dev.barabu.widgets.domain.Form
import dev.barabu.widgets.domain.Lens
import kotlin.math.abs

class MenuView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), GestureDetector.OnGestureListener {

    private lateinit var menuButtonMain: View
    private lateinit var menuButtonBlur: View
    private lateinit var menuButtonGrey: View
    private lateinit var menuButtonColored: View

    /**
     * Кнопка и ее иконки для неактивного/активного состояний
     */
    private val buttonsDecor = mapOf(
        R.id.w_menu_button_colored to arrayOf(R.drawable.w_ic_colored_1, R.drawable.w_ic_colored_2),
        R.id.w_menu_button_blur to arrayOf(R.drawable.w_ic_blur_1, R.drawable.w_ic_blur_2),
        R.id.w_menu_button_grey to arrayOf(R.drawable.w_ic_grey_1, R.drawable.w_ic_grey_2)
    )

    /**
     * Фиксируем координаты события [MotionEvent.ACTION_DOWN]
     */
    private var eventDownPoint = PointF()

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(context as AppCompatActivity).get(MenuViewModel::class.java)
    }

    private val detector: GestureDetectorCompat by lazy {
        GestureDetectorCompat(context, this)
    }

    override fun onFinishInflate() {
        Log.d(TAG, "onFinishInflate")
        super.onFinishInflate()

        menuButtonMain = findViewById<View>(R.id.w_menu_button_main).apply {
            setOnClickListener {
                viewModel.onSwapClick()
            }
        }

        menuButtonColored = findViewById<View>(R.id.w_menu_button_colored).apply {
            setOnClickListener {
                viewModel.onColoredClick()
            }
        }

        menuButtonBlur = findViewById<View>(R.id.w_menu_button_blur).apply {
            setOnClickListener {
                viewModel.onBlurClick()
            }
        }

        menuButtonGrey = findViewById<View>(R.id.w_menu_button_grey).apply {
            setOnClickListener {
                viewModel.onGreyClick()
            }
        }
    }

    override fun onAttachedToWindow() {
        Log.d(TAG, "onAttachedToWindow")
        super.onAttachedToWindow()

        viewModel.menuState.observe(context as AppCompatActivity) { menuState ->
            // Угол поворота от layout-позиции элемента, а не от текущего угла поворота
            when (menuState.lens) {
                Lens.Back -> {
                    if (menuButtonMain.rotation != 0f) {
                        menuButtonMain.animate().rotation(0f).start()
                    }
                }

                Lens.Front -> {
                    if (menuButtonMain.rotation != -180f) {
                        menuButtonMain.animate().rotation(-180f).start()
                    }
                }
            }

            updateForm(menuState.form)

            val activeButtonId = when (menuState.effect) {
                Effect.Colored -> R.id.w_menu_button_colored
                Effect.Grey -> R.id.w_menu_button_grey
                Effect.Blur -> R.id.w_menu_button_blur
            }
            updateButtonsDecor(activeButtonId)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean =
        if (detector.onTouchEvent(event)) true else super.onTouchEvent(event)

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        Log.d(TAG, "onInterceptTouchEvent")
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Детектор должен сохранить у себя начало жеста
                detector.onTouchEvent(event)
                eventDownPoint = PointF(event.x, event.y)
                false
            }

            MotionEvent.ACTION_MOVE -> {
                // Перехватываем событие, если вертикальное смещение превысило SWIPE_THRESHOLD
                abs(event.y - eventDownPoint.y) > SWIPE_THRESHOLD
            }

            MotionEvent.ACTION_UP -> {
                false
            }

            else -> false
        }
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val eventDown = e1 ?: return false
        if (e2.y < eventDown.y) viewModel.onExpandMenu() else viewModel.onCollapseMenu()
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean = true

    override fun onDown(e: MotionEvent): Boolean = true

    override fun onShowPress(e: MotionEvent) {
    }

    override fun onLongPress(e: MotionEvent) {
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean = true

    private fun updateForm(form: Form) {
        TransitionManager.beginDelayedTransition(this)
        val visibility = if (form == Form.Expanded) View.VISIBLE else View.GONE
        children.asIterable().forEach {
            if (it.id != R.id.w_menu_button_main) {
                it.visibility = visibility
            }
        }
    }

    /**
     * Перерисовать кнопки и выделить активную
     */
    private fun updateButtonsDecor(activeButton: Int) {
        children.filter { it is ImageView && it.id != R.id.w_menu_button_main }.forEach { v ->
            val drawableIndex = if (v.id == activeButton) 1 else 0
            (v as ImageView).setImageResource(buttonsDecor[v.id]!![drawableIndex])
        }
    }

    companion object {
        private const val TAG = "MenuView"

        private const val SWIPE_THRESHOLD = 10f
    }
}
package dev.barabu.widgets

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.barabu.widgets.domain.Effect
import dev.barabu.widgets.domain.Form
import dev.barabu.widgets.domain.Lens
import dev.barabu.widgets.domain.MenuState
import java.util.Collections

class MenuViewModel : ViewModel() {

    private var effect: Effect = Effect.Colored
    private val lens = arrayListOf(Lens.Back, Lens.Front)
    private var form: Form = Form.Collapsed

    private val _menuState = MutableLiveData(MenuState(lens[0], effect, form))
    val menuState = _menuState as LiveData<MenuState>

    fun onSwapClick() {
        Log.d(TAG, "onSwapClick")
        Collections.rotate(lens, 1)
        _menuState.value = MenuState(lens[0], effect, form)
    }

    fun onGreyClick() {
        Log.d(TAG, "onGreyClick")
        effect = Effect.Grey
        _menuState.value = MenuState(lens[0], effect, form)
    }

    fun onBlurClick() {
        Log.d(TAG, "onBlurClick")
        effect = Effect.Blur
        _menuState.value = MenuState(lens[0], effect, form)
    }

    fun onColoredClick() {
        Log.d(TAG, "onColoredClick")
        effect = Effect.Colored
        _menuState.value = MenuState(lens[0], effect, form)
    }

    fun onExpandMenu() {
        Log.d(TAG, "onExpandMenu")
        form = Form.Expanded
        _menuState.value = MenuState(lens[0], effect, form)
    }

    fun onCollapseMenu() {
        Log.d(TAG, "onCollapseMenu")
        form = Form.Collapsed
        _menuState.value = MenuState(lens[0], effect, form)
    }

    companion object {
        private const val TAG = "MenuViewModel"
    }
}
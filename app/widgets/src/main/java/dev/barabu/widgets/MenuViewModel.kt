package dev.barabu.widgets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.barabu.base.Logging
import dev.barabu.widgets.domain.Effect
import dev.barabu.widgets.domain.EffectWrapper
import dev.barabu.widgets.domain.Form
import dev.barabu.widgets.domain.FormWrapper
import dev.barabu.widgets.domain.Lens
import dev.barabu.widgets.domain.LensWrapper
import dev.barabu.widgets.domain.MenuState
import java.util.Collections

class MenuViewModel : ViewModel() {

    private var effect: Effect = Effect.Colored
    private val lens = arrayListOf(Lens.Back, Lens.Front)
    private var form: Form = Form.Collapsed

    private val noLens = LensWrapper(null)
    private val noEffect = EffectWrapper(null)
    private val noForm = FormWrapper(null)

    private val _menuState = MutableLiveData(MenuState(noLens, noEffect, noForm))
    val menuState = _menuState as LiveData<MenuState>

    fun onActivityStart() {
        Logging.d("$TAG.onActivityStart")
        _menuState.value = MenuState(LensWrapper(lens[0]), EffectWrapper(effect), FormWrapper(form))
    }

    fun onSwapClick() {
        Logging.d("$TAG.onSwapClick")
        Collections.rotate(lens, 1)
        _menuState.value = MenuState(LensWrapper(lens[0]), noEffect, noForm)
    }

    fun onGreyClick() {
        Logging.d("$TAG.onGreyClick")
        effect = Effect.Grey
        _menuState.value = MenuState(noLens, EffectWrapper(effect), noForm)
    }

    fun onBlurClick() {
        Logging.d("$TAG.onBlurClick")
        effect = Effect.Blur
        _menuState.value = MenuState(noLens, EffectWrapper(effect), noForm)
    }

    fun onColoredClick() {
        Logging.d("$TAG.onColoredClick")
        effect = Effect.Colored
        _menuState.value = MenuState(noLens, EffectWrapper(effect), noForm)
    }

    fun onExpandMenu() {
        Logging.d("$TAG.onExpandMenu")
        form = Form.Expanded
        _menuState.value = MenuState(noLens, noEffect, FormWrapper(form))
    }

    fun onCollapseMenu() {
        Logging.d("$TAG.onCollapseMenu")
        form = Form.Collapsed
        _menuState.value = MenuState(noLens, noEffect, FormWrapper(form))
    }

    companion object {
        private const val TAG = "MenuViewModel"
    }
}
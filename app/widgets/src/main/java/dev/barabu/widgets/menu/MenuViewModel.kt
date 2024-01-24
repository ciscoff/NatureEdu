package dev.barabu.widgets.menu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.barabu.base.Logging
import dev.barabu.widgets.menu.domain.Filter
import dev.barabu.widgets.menu.domain.FilterWrapper
import dev.barabu.widgets.menu.domain.Form
import dev.barabu.widgets.menu.domain.FormWrapper
import dev.barabu.widgets.menu.domain.Lens
import dev.barabu.widgets.menu.domain.LensWrapper
import dev.barabu.widgets.menu.domain.MenuState
import java.util.Collections

class MenuViewModel : ViewModel() {

    private var filter: Filter = Filter.Colored
    private val lens = arrayListOf(Lens.Back, Lens.Front)
    private var form: Form = Form.Collapsed

    private val noLens = LensWrapper(null)
    private val noEffect = FilterWrapper(null)
    private val noForm = FormWrapper(null)

    private val _menuState = MutableLiveData(MenuState(noLens, noEffect, noForm))
    val menuState = _menuState as LiveData<MenuState>

    fun onActivityStart() {
        Logging.d("$TAG.onActivityStart")
        _menuState.value = MenuState(LensWrapper(lens[0]), FilterWrapper(filter), FormWrapper(form))
    }

    fun onSwapClick() {
        Logging.d("$TAG.onSwapClick")
        Collections.rotate(lens, 1)
        _menuState.value = MenuState(LensWrapper(lens[0]), noEffect, noForm)
    }

    fun onGrayClick() {
        Logging.d("$TAG.onGreyClick")
        filter = Filter.Grayscale
        _menuState.value = MenuState(noLens, FilterWrapper(filter), noForm)
    }

    fun onInvertClick() {
        Logging.d("$TAG.onGreyClick")
        filter = Filter.Invert
        _menuState.value = MenuState(noLens, FilterWrapper(filter), noForm)
    }


    fun onBlurClick() {
        Logging.d("$TAG.onBlurClick")
        filter = Filter.Blur
        _menuState.value = MenuState(noLens, FilterWrapper(filter), noForm)
    }

    fun onColoredClick() {
        Logging.d("$TAG.onColoredClick")
        filter = Filter.Colored
        _menuState.value = MenuState(noLens, FilterWrapper(filter), noForm)
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
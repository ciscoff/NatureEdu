package dev.barabu.nature.camera.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.barabu.nature.camera.preview.domain.LensState
import dev.barabu.widgets.menu.domain.Lens
import java.util.Collections

class PreviewViewModel : ViewModel() {

    private val lens = arrayListOf(Lens.Back, Lens.Front)

    private val _lensState = MutableLiveData(LensState(null))
    val lensState = _lensState as LiveData<LensState>

    fun onActivityStart() {
        _lensState.value = LensState(lens[0])
    }

    fun onTouch() {
        Collections.rotate(lens, 1)
        _lensState.value = LensState(lens[0])
    }
}
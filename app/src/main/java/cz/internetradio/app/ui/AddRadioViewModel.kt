package cz.internetradio.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.internetradio.app.data.Radio
import cz.internetradio.app.repository.RadioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddRadioViewModel @Inject constructor(
    private val repository: RadioRepository
) : ViewModel() {

    fun addRadio(radio: Radio) {
        viewModelScope.launch {
            repository.insertRadio(radio)
        }
    }
} 
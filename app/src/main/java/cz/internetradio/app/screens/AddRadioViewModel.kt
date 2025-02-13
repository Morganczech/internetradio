package cz.internetradio.app.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.internetradio.app.model.Radio
import cz.internetradio.app.repository.RadioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.graphics.Color
import cz.internetradio.app.data.entity.RadioEntity
import cz.internetradio.app.model.RadioCategory
import java.util.UUID

@HiltViewModel
class AddRadioViewModel @Inject constructor(
    private val repository: RadioRepository
) : ViewModel() {

    fun addRadio(
        name: String,
        streamUrl: String,
        imageUrl: String?,
        description: String?,
        category: RadioCategory = RadioCategory.VLASTNI
    ) {
        if (name.isBlank() || streamUrl.isBlank()) return

        val radio = Radio(
            id = UUID.randomUUID().toString(),
            name = name,
            streamUrl = streamUrl,
            imageUrl = imageUrl ?: "android.resource://cz.internetradio.app/drawable/ic_radio_default",
            description = description,
            startColor = Color(0xFF1A1A1A),
            endColor = Color(0xFF2D2D2D),
            category = category
        )

        viewModelScope.launch {
            repository.insertRadio(radio)
        }
    }
} 
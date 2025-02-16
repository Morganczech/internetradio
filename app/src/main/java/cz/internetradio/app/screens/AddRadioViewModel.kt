package cz.internetradio.app.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.internetradio.app.model.Radio
import cz.internetradio.app.model.RadioCategory
import cz.internetradio.app.repository.RadioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.graphics.Color
import cz.internetradio.app.data.entity.RadioEntity
import cz.internetradio.app.ui.theme.Gradients
import cz.internetradio.app.utils.StreamUrlParser
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class AddRadioViewModel @Inject constructor(
    private val radioRepository: RadioRepository
) : ViewModel() {

    private val _validationError = MutableStateFlow<ValidationError?>(null)
    val validationError: StateFlow<ValidationError?> = _validationError.asStateFlow()

    private val _selectedGradientId = MutableStateFlow<Int?>(null)
    val selectedGradientId: StateFlow<Int?> = _selectedGradientId.asStateFlow()

    sealed class ValidationError {
        object DuplicateStreamUrl : ValidationError()
        object DuplicateName : ValidationError()
        data class StreamError(val message: String) : ValidationError()
    }

    fun addRadio(
        name: String,
        streamUrl: String,
        imageUrl: String?,
        description: String?,
        category: RadioCategory,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (radioRepository.existsByName(name)) {
                _validationError.value = ValidationError.DuplicateName
                return@launch
            }
            if (radioRepository.existsByStreamUrl(streamUrl)) {
                _validationError.value = ValidationError.DuplicateStreamUrl
                return@launch
            }

            val radio = Radio(
                id = streamUrl,
                name = name,
                streamUrl = streamUrl,
                imageUrl = imageUrl ?: "android.resource://cz.internetradio.app/drawable/ic_radio_default",
                description = description ?: "",
                category = category,
                originalCategory = category,
                startColor = category.startColor,
                endColor = category.endColor,
                isFavorite = false,
                gradientId = _selectedGradientId.value
            )

            radioRepository.insertRadio(radio)
            onSuccess()
        }
    }

    fun clearValidationError() {
        _validationError.value = null
    }

    suspend fun loadRadioForEdit(radioId: String): Radio? {
        return radioRepository.getRadioById(radioId)
    }

    fun updateRadio(
        radioId: String,
        name: String,
        streamUrl: String,
        imageUrl: String?,
        description: String?,
        category: RadioCategory,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val existingRadio = radioRepository.getRadioById(radioId) ?: return@launch

            val radio = existingRadio.copy(
                name = name,
                streamUrl = streamUrl,
                imageUrl = imageUrl ?: "android.resource://cz.internetradio.app/drawable/ic_radio_default",
                description = description ?: "",
                category = category,
                originalCategory = category,
                startColor = category.startColor,
                endColor = category.endColor,
                gradientId = _selectedGradientId.value
            )

            radioRepository.insertRadio(radio)
            onSuccess()
        }
    }

    fun setSelectedGradient(id: Int) {
        _selectedGradientId.value = id
    }
} 
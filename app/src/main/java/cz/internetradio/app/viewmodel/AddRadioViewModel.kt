package cz.internetradio.app.viewmodel

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
import cz.internetradio.app.utils.StreamUrlParser
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import cz.internetradio.app.ui.theme.Gradients

@HiltViewModel
class AddRadioViewModel @Inject constructor(
    private val radioRepository: RadioRepository
) : ViewModel() {
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _streamUrl = MutableStateFlow("")
    val streamUrl: StateFlow<String> = _streamUrl

    private val _imageUrl = MutableStateFlow("")
    val imageUrl: StateFlow<String> = _imageUrl

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    private val _category = MutableStateFlow<RadioCategory>(RadioCategory.VLASTNI)
    val category: StateFlow<RadioCategory> = _category

    private val _showError = MutableStateFlow<String?>(null)
    val showError: StateFlow<String?> = _showError

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _validationError = MutableStateFlow<ValidationError?>(null)
    val validationError: StateFlow<ValidationError?> = _validationError.asStateFlow()

    private var radioToEdit: Radio? = null

    sealed class ValidationError {
        object DuplicateStreamUrl : ValidationError()
        object DuplicateName : ValidationError()
        data class StreamError(val message: String) : ValidationError()
    }

    fun setName(name: String) {
        _name.value = name
    }

    fun setStreamUrl(url: String) {
        _streamUrl.value = url
    }

    fun setImageUrl(url: String) {
        _imageUrl.value = url
    }

    fun setDescription(description: String) {
        _description.value = description
    }

    fun setCategory(category: RadioCategory) {
        _category.value = category
    }

    fun loadRadioForEdit(radioId: String) {
        viewModelScope.launch {
            radioRepository.getRadioById(radioId)?.let { radio ->
                _name.value = radio.name
                _streamUrl.value = radio.streamUrl
                _imageUrl.value = radio.imageUrl
                _description.value = radio.description
                _category.value = radio.category
                radioToEdit = radio
            }
        }
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

            val gradient = Gradients.getGradientForCategory(category)
            val radio = existingRadio.copy(
                name = name,
                streamUrl = streamUrl,
                imageUrl = imageUrl ?: "android.resource://cz.internetradio.app/drawable/ic_radio_default",
                description = description ?: "",
                category = category,
                originalCategory = category,
                startColor = gradient.first,
                endColor = gradient.second
            )

            radioRepository.insertRadio(radio)
            onSuccess()
        }
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

            val gradient = Gradients.getGradientForCategory(category)
            val radio = Radio(
                id = streamUrl,
                name = name,
                streamUrl = streamUrl,
                imageUrl = imageUrl ?: "android.resource://cz.internetradio.app/drawable/ic_radio_default",
                description = description ?: "",
                category = category,
                originalCategory = category,
                startColor = gradient.first,
                endColor = gradient.second,
                isFavorite = false
            )

            radioRepository.insertRadio(radio)
            onSuccess()
        }
    }

    fun dismissError() {
        _validationError.value = null
    }

    fun clearValidationError() {
        _validationError.value = null
    }
} 
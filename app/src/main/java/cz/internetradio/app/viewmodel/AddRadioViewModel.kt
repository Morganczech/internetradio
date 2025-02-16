package cz.internetradio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.internetradio.app.model.Radio
import cz.internetradio.app.model.RadioCategory
import cz.internetradio.app.repository.RadioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.graphics.Color

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
    val validationError: StateFlow<ValidationError?> = _validationError

    private var radioToEdit: Radio? = null

    sealed class ValidationError {
        data class DuplicateStreamUrl(val url: String) : ValidationError()
        data class DuplicateName(val name: String) : ValidationError()
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
                radioToEdit = radio
                _name.value = radio.name
                _streamUrl.value = radio.streamUrl
                _imageUrl.value = radio.imageUrl
                _description.value = radio.description
                _category.value = radio.category
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
            _isLoading.value = true
            try {
                if (name.isBlank()) {
                    _validationError.value = ValidationError.StreamError("Zadejte název rádia")
                    return@launch
                }
                if (streamUrl.isBlank()) {
                    _validationError.value = ValidationError.StreamError("Zadejte URL streamu")
                    return@launch
                }

                // Určíme originalCategory podle toho, zda je stanice oblíbená
                val originalCategory = if (radioToEdit?.isFavorite == true) {
                    // Pokud je stanice oblíbená, zachováme původní kategorii
                    radioToEdit?.originalCategory ?: category
                } else {
                    // Pokud není oblíbená, aktualizujeme původní kategorii
                    category
                }

                val radio = Radio(
                    id = radioId,
                    name = name,
                    streamUrl = streamUrl,
                    imageUrl = imageUrl ?: "android.resource://cz.internetradio.app/drawable/ic_radio_default",
                    description = description ?: "",
                    category = category,
                    originalCategory = originalCategory,
                    startColor = category.startColor,
                    endColor = category.endColor,
                    isFavorite = radioToEdit?.isFavorite ?: false
                )

                radioRepository.insertRadio(radio)
                onSuccess()
            } catch (e: Exception) {
                _validationError.value = ValidationError.StreamError("Chyba při ukládání: ${e.message}")
            } finally {
                _isLoading.value = false
            }
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
            _isLoading.value = true
            try {
                if (name.isBlank()) {
                    _validationError.value = ValidationError.StreamError("Zadejte název rádia")
                    return@launch
                }
                if (streamUrl.isBlank()) {
                    _validationError.value = ValidationError.StreamError("Zadejte URL streamu")
                    return@launch
                }

                // Kontrola duplicity
                if (radioRepository.existsByName(name)) {
                    _validationError.value = ValidationError.DuplicateName(name)
                    return@launch
                }
                if (radioRepository.existsByStreamUrl(streamUrl)) {
                    _validationError.value = ValidationError.DuplicateStreamUrl(streamUrl)
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
                    isFavorite = false
                )

                radioRepository.insertRadio(radio)
                onSuccess()
            } catch (e: Exception) {
                _validationError.value = ValidationError.StreamError("Chyba při ukládání: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissError() {
        _validationError.value = null
    }
} 
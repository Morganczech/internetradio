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

    private var radioToEdit: Radio? = null

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

    fun loadRadio(radioId: String) {
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

    fun saveRadio(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (_name.value.isBlank()) {
                    _showError.value = "Zadejte název rádia"
                    return@launch
                }
                if (_streamUrl.value.isBlank()) {
                    _showError.value = "Zadejte URL streamu"
                    return@launch
                }

                // Kontrola duplicity
                if (radioToEdit == null) {
                    if (radioRepository.existsByName(_name.value)) {
                        _showError.value = "Rádio s tímto názvem již existuje"
                        return@launch
                    }
                    if (radioRepository.existsByStreamUrl(_streamUrl.value)) {
                        _showError.value = "Rádio s touto URL již existuje"
                        return@launch
                    }
                }

                val radio = Radio(
                    id = radioToEdit?.id ?: _streamUrl.value,
                    name = _name.value,
                    streamUrl = _streamUrl.value,
                    imageUrl = _imageUrl.value.ifBlank { "android.resource://cz.internetradio.app/drawable/ic_radio_default" },
                    description = _description.value,
                    category = _category.value,
                    originalCategory = _category.value,
                    startColor = _category.value.startColor,
                    endColor = _category.value.endColor,
                    isFavorite = radioToEdit?.isFavorite ?: false
                )

                radioRepository.insertRadio(radio)
                onSuccess()
            } catch (e: Exception) {
                _showError.value = "Chyba při ukládání: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissError() {
        _showError.value = null
    }
} 
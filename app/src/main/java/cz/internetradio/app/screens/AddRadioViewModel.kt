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
import cz.internetradio.app.ui.theme.Gradients
import cz.internetradio.app.utils.StreamUrlParser
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class AddRadioViewModel @Inject constructor(
    private val repository: RadioRepository
) : ViewModel() {

    private val _validationError = MutableStateFlow<ValidationError?>(null)
    val validationError: StateFlow<ValidationError?> = _validationError.asStateFlow()

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
        category: RadioCategory = RadioCategory.VLASTNI,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank() || streamUrl.isBlank()) return@launch

            // Parsování URL streamu
            when (val result = StreamUrlParser.parseUrl(streamUrl)) {
                is StreamUrlParser.Result.Success -> {
                    val finalStreamUrl = result.streamUrl

                    // Kontrola duplicit s finální URL
                    if (repository.existsByStreamUrl(finalStreamUrl)) {
                        _validationError.value = ValidationError.DuplicateStreamUrl
                        return@launch
                    }

                    if (repository.existsByName(name)) {
                        _validationError.value = ValidationError.DuplicateName
                        return@launch
                    }

                    // Získáme gradient podle kategorie
                    val (startColor, endColor) = if (category == RadioCategory.VLASTNI) {
                        // Pro vlastní stanice použijeme náhodný gradient
                        Gradients.getRandomGradient()
                    } else {
                        // Pro ostatní kategorie použijeme předdefinovaný gradient
                        Gradients.getGradientForCategory(category)
                    }

                    val radio = Radio(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        streamUrl = finalStreamUrl,
                        imageUrl = imageUrl ?: "android.resource://cz.internetradio.app/drawable/ic_radio_default",
                        description = description ?: "",
                        startColor = startColor,
                        endColor = endColor,
                        category = category
                    )

                    repository.insertRadio(radio)
                    onSuccess()
                }
                is StreamUrlParser.Result.Error -> {
                    _validationError.value = ValidationError.StreamError(result.message)
                }
            }
        }
    }

    fun clearValidationError() {
        _validationError.value = null
    }

    suspend fun loadRadioForEdit(radioId: String): Radio? {
        return repository.getRadioById(radioId)
    }

    fun updateRadio(
        radioId: String,
        name: String,
        streamUrl: String,
        imageUrl: String?,
        description: String?,
        category: RadioCategory = RadioCategory.VLASTNI,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank() || streamUrl.isBlank()) return@launch

            // Načteme existující rádio pro zachování některých vlastností
            val existingRadio = repository.getRadioById(radioId) ?: return@launch

            // Parsování URL streamu
            when (val result = StreamUrlParser.parseUrl(streamUrl)) {
                is StreamUrlParser.Result.Success -> {
                    val finalStreamUrl = result.streamUrl

                    // Kontrola duplicit s finální URL (kromě aktuální stanice)
                    if (repository.existsByStreamUrl(finalStreamUrl) && 
                        existingRadio.streamUrl != finalStreamUrl) {
                        _validationError.value = ValidationError.DuplicateStreamUrl
                        return@launch
                    }

                    if (repository.existsByName(name) && 
                        existingRadio.name != name) {
                        _validationError.value = ValidationError.DuplicateName
                        return@launch
                    }

                    // Získáme gradient podle kategorie
                    val (startColor, endColor) = if (category == RadioCategory.VLASTNI) {
                        // Pro vlastní stanice použijeme náhodný gradient
                        Gradients.getRandomGradient()
                    } else {
                        // Pro ostatní kategorie použijeme předdefinovaný gradient
                        Gradients.getGradientForCategory(category)
                    }

                    // Vytvoříme aktualizované rádio se zachováním originalCategory a isFavorite
                    val updatedRadio = existingRadio.copy(
                        name = name,
                        streamUrl = finalStreamUrl,
                        imageUrl = imageUrl ?: "android.resource://cz.internetradio.app/drawable/ic_radio_default",
                        description = description ?: "",
                        startColor = startColor,
                        endColor = endColor,
                        // Zachováme kategorii a originalCategory podle stavu oblíbenosti
                        category = if (existingRadio.isFavorite) RadioCategory.VLASTNI else category,
                        originalCategory = if (existingRadio.isFavorite) existingRadio.originalCategory else category
                    )

                    repository.insertRadio(updatedRadio)
                    onSuccess()
                }
                is StreamUrlParser.Result.Error -> {
                    _validationError.value = ValidationError.StreamError(result.message)
                }
            }
        }
    }
} 
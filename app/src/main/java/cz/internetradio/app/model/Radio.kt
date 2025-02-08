package cz.internetradio.app.model

data class Radio(
    val id: String,
    val name: String,
    val streamUrl: String,
    val imageUrl: String? = null,
    val description: String? = null
) 
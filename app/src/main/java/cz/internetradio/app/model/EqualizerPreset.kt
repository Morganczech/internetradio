package cz.internetradio.app.model

enum class EqualizerPreset(
    val title: String,
    val bands: List<Float>  // hodnoty v dB pro každé frekvenční pásmo
) {
    NORMAL("Normální", listOf(0f, 0f, 0f, 0f, 0f)),
    ROCK("Rock", listOf(4f, 3f, -2f, 2f, 4f)),
    POP("Pop", listOf(2f, 3f, 3f, 1f, -1f)),
    JAZZ("Jazz", listOf(3f, 2f, -1f, 2f, 3f)),
    CLASSICAL("Klasika", listOf(4f, 3f, 0f, 2f, 3f)),
    DANCE("Dance", listOf(4f, 3f, 0f, 3f, 4f)),
    METAL("Metal", listOf(5f, 2f, 0f, 2f, 4f)),
    HIPHOP("Hip Hop", listOf(4f, 3f, 0f, 1f, 3f)),
    VOCAL("Vokál", listOf(-2f, 2f, 4f, 3f, -1f))
} 
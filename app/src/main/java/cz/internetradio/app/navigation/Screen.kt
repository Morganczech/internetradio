package cz.internetradio.app.navigation

sealed class Screen(val route: String) {
    object Favorites : Screen("favorites")
    object AllStations : Screen("all_stations")
    object Settings : Screen("settings")
    object Equalizer : Screen("equalizer")
    object AddRadio : Screen("add_radio")
} 
package cz.internetradio.app.navigation

sealed class Screen(val route: String) {
    object Favorites : Screen("favorites")
    object AllStations : Screen("all_stations")
} 
package cz.internetradio.app.navigation

sealed class Screen(val route: String) {
    object AllStations : Screen("all_stations")
    object BrowseStations : Screen("browse_stations")
    object Settings : Screen("settings")
    object Equalizer : Screen("equalizer")
    object PopularStations : Screen("popular_stations")
} 
package cz.internetradio.app.navigation

sealed class Screen(val route: String) {
    object AllStations : Screen("all_stations")
    object Favorites : Screen("favorites")
    object BrowseStations : Screen("browse_stations")
    object PopularStations : Screen("popular_stations")
    object Settings : Screen("settings")
    object Equalizer : Screen("equalizer")
    object FavoriteSongs : Screen("favorite_songs")
    object AddRadio : Screen("add_radio")
    object EditRadio : Screen("edit_radio/{radioId}") {
        fun createRoute(radioId: String) = "edit_radio/$radioId"
    }
} 
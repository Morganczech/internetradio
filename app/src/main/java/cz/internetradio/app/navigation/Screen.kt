package cz.internetradio.app.navigation

sealed class Screen(val route: String) {
    object AllStations : Screen("all_stations")
    object BrowseStations : Screen("browse_stations")
    object Settings : Screen("settings")
    object Equalizer : Screen("equalizer")
<<<<<<< HEAD
    object PopularStations : Screen("popular_stations")
    object AddRadio : Screen("add_radio")
    object EditRadio : Screen("edit_radio/{radioId}") {
        fun createRoute(radioId: String) = "edit_radio/$radioId"
    }
=======
    object FavoriteSongs : Screen("favorite_songs")
>>>>>>> feature/favorite-songs
} 
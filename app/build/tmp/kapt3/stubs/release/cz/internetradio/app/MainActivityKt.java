package cz.internetradio.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000$\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u001a\u0012\u0010\u0000\u001a\u00020\u00012\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u0007\u001a&\u0010\u0004\u001a\u00020\u00012\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00010\nH\u0007\u001a&\u0010\u000b\u001a\u00020\u00012\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\b2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00010\nH\u0007\u00a8\u0006\u000e"}, d2 = {"MainScreen", "", "viewModel", "Lcz/internetradio/app/viewmodel/RadioViewModel;", "PlayerControls", "radio", "Lcz/internetradio/app/model/Radio;", "isPlaying", "", "onPlayPauseClick", "Lkotlin/Function0;", "RadioItem", "isSelected", "onRadioClick", "app_release"})
public final class MainActivityKt {
    
    @androidx.compose.runtime.Composable
    public static final void MainScreen(@org.jetbrains.annotations.NotNull
    cz.internetradio.app.viewmodel.RadioViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable
    public static final void RadioItem(@org.jetbrains.annotations.NotNull
    cz.internetradio.app.model.Radio radio, boolean isSelected, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function0<kotlin.Unit> onRadioClick) {
    }
    
    @androidx.compose.runtime.Composable
    public static final void PlayerControls(@org.jetbrains.annotations.NotNull
    cz.internetradio.app.model.Radio radio, boolean isPlaying, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function0<kotlin.Unit> onPlayPauseClick) {
    }
}
package cz.internetradio.app.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\b\u0010\u0015\u001a\u00020\u0016H\u0014J\u000e\u0010\u0017\u001a\u00020\u00162\u0006\u0010\u0018\u001a\u00020\tJ\u0006\u0010\u0019\u001a\u00020\u0016R\u0016\u0010\u0007\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\t0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000b0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u000fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\t0\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014\u00a8\u0006\u001a"}, d2 = {"Lcz/internetradio/app/viewmodel/RadioViewModel;", "Landroidx/lifecycle/ViewModel;", "radioRepository", "Lcz/internetradio/app/repository/RadioRepository;", "exoPlayer", "Lcom/google/android/exoplayer2/ExoPlayer;", "(Lcz/internetradio/app/repository/RadioRepository;Lcom/google/android/exoplayer2/ExoPlayer;)V", "_currentRadio", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcz/internetradio/app/model/Radio;", "_isPlaying", "", "currentRadio", "Lkotlinx/coroutines/flow/StateFlow;", "getCurrentRadio", "()Lkotlinx/coroutines/flow/StateFlow;", "isPlaying", "radioStations", "", "getRadioStations", "()Ljava/util/List;", "onCleared", "", "playRadio", "radio", "togglePlayPause", "app_release"})
@dagger.hilt.android.lifecycle.HiltViewModel
public final class RadioViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull
    private final cz.internetradio.app.repository.RadioRepository radioRepository = null;
    @org.jetbrains.annotations.NotNull
    private final com.google.android.exoplayer2.ExoPlayer exoPlayer = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<cz.internetradio.app.model.Radio> _currentRadio = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<cz.internetradio.app.model.Radio> currentRadio = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isPlaying = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isPlaying = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.List<cz.internetradio.app.model.Radio> radioStations = null;
    
    @javax.inject.Inject
    public RadioViewModel(@org.jetbrains.annotations.NotNull
    cz.internetradio.app.repository.RadioRepository radioRepository, @org.jetbrains.annotations.NotNull
    com.google.android.exoplayer2.ExoPlayer exoPlayer) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<cz.internetradio.app.model.Radio> getCurrentRadio() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isPlaying() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<cz.internetradio.app.model.Radio> getRadioStations() {
        return null;
    }
    
    public final void playRadio(@org.jetbrains.annotations.NotNull
    cz.internetradio.app.model.Radio radio) {
    }
    
    public final void togglePlayPause() {
    }
    
    @java.lang.Override
    protected void onCleared() {
    }
}
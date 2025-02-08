package cz.internetradio.app.viewmodel;

import com.google.android.exoplayer2.ExoPlayer;
import cz.internetradio.app.repository.RadioRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class RadioViewModel_Factory implements Factory<RadioViewModel> {
  private final Provider<RadioRepository> radioRepositoryProvider;

  private final Provider<ExoPlayer> exoPlayerProvider;

  public RadioViewModel_Factory(Provider<RadioRepository> radioRepositoryProvider,
      Provider<ExoPlayer> exoPlayerProvider) {
    this.radioRepositoryProvider = radioRepositoryProvider;
    this.exoPlayerProvider = exoPlayerProvider;
  }

  @Override
  public RadioViewModel get() {
    return newInstance(radioRepositoryProvider.get(), exoPlayerProvider.get());
  }

  public static RadioViewModel_Factory create(Provider<RadioRepository> radioRepositoryProvider,
      Provider<ExoPlayer> exoPlayerProvider) {
    return new RadioViewModel_Factory(radioRepositoryProvider, exoPlayerProvider);
  }

  public static RadioViewModel newInstance(RadioRepository radioRepository, ExoPlayer exoPlayer) {
    return new RadioViewModel(radioRepository, exoPlayer);
  }
}

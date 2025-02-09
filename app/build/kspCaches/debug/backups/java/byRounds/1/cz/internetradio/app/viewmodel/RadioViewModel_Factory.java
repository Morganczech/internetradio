package cz.internetradio.app.viewmodel;

import android.content.Context;
import androidx.media3.exoplayer.ExoPlayer;
import cz.internetradio.app.repository.RadioRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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

  private final Provider<Context> contextProvider;

  public RadioViewModel_Factory(Provider<RadioRepository> radioRepositoryProvider,
      Provider<ExoPlayer> exoPlayerProvider, Provider<Context> contextProvider) {
    this.radioRepositoryProvider = radioRepositoryProvider;
    this.exoPlayerProvider = exoPlayerProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public RadioViewModel get() {
    return newInstance(radioRepositoryProvider.get(), exoPlayerProvider.get(), contextProvider.get());
  }

  public static RadioViewModel_Factory create(Provider<RadioRepository> radioRepositoryProvider,
      Provider<ExoPlayer> exoPlayerProvider, Provider<Context> contextProvider) {
    return new RadioViewModel_Factory(radioRepositoryProvider, exoPlayerProvider, contextProvider);
  }

  public static RadioViewModel newInstance(RadioRepository radioRepository, ExoPlayer exoPlayer,
      Context context) {
    return new RadioViewModel(radioRepository, exoPlayer, context);
  }
}

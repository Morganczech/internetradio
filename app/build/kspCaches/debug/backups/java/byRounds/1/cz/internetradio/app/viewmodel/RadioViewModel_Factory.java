package cz.internetradio.app.viewmodel;

import android.content.Context;
import androidx.media3.exoplayer.ExoPlayer;
import cz.internetradio.app.audio.AudioSpectrumProcessor;
import cz.internetradio.app.audio.EqualizerManager;
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

  private final Provider<EqualizerManager> equalizerManagerProvider;

  private final Provider<AudioSpectrumProcessor> audioSpectrumProcessorProvider;

  private final Provider<Context> contextProvider;

  public RadioViewModel_Factory(Provider<RadioRepository> radioRepositoryProvider,
      Provider<ExoPlayer> exoPlayerProvider, Provider<EqualizerManager> equalizerManagerProvider,
      Provider<AudioSpectrumProcessor> audioSpectrumProcessorProvider,
      Provider<Context> contextProvider) {
    this.radioRepositoryProvider = radioRepositoryProvider;
    this.exoPlayerProvider = exoPlayerProvider;
    this.equalizerManagerProvider = equalizerManagerProvider;
    this.audioSpectrumProcessorProvider = audioSpectrumProcessorProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public RadioViewModel get() {
    return newInstance(radioRepositoryProvider.get(), exoPlayerProvider.get(), equalizerManagerProvider.get(), audioSpectrumProcessorProvider.get(), contextProvider.get());
  }

  public static RadioViewModel_Factory create(Provider<RadioRepository> radioRepositoryProvider,
      Provider<ExoPlayer> exoPlayerProvider, Provider<EqualizerManager> equalizerManagerProvider,
      Provider<AudioSpectrumProcessor> audioSpectrumProcessorProvider,
      Provider<Context> contextProvider) {
    return new RadioViewModel_Factory(radioRepositoryProvider, exoPlayerProvider, equalizerManagerProvider, audioSpectrumProcessorProvider, contextProvider);
  }

  public static RadioViewModel newInstance(RadioRepository radioRepository, ExoPlayer exoPlayer,
      EqualizerManager equalizerManager, AudioSpectrumProcessor audioSpectrumProcessor,
      Context context) {
    return new RadioViewModel(radioRepository, exoPlayer, equalizerManager, audioSpectrumProcessor, context);
  }
}

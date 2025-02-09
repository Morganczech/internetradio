package cz.internetradio.app.di;

import android.content.Context;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import cz.internetradio.app.audio.AudioSpectrumProcessor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideDefaultAudioSinkFactory implements Factory<DefaultAudioSink> {
  private final Provider<Context> contextProvider;

  private final Provider<AudioSpectrumProcessor> audioSpectrumProcessorProvider;

  public AppModule_ProvideDefaultAudioSinkFactory(Provider<Context> contextProvider,
      Provider<AudioSpectrumProcessor> audioSpectrumProcessorProvider) {
    this.contextProvider = contextProvider;
    this.audioSpectrumProcessorProvider = audioSpectrumProcessorProvider;
  }

  @Override
  public DefaultAudioSink get() {
    return provideDefaultAudioSink(contextProvider.get(), audioSpectrumProcessorProvider.get());
  }

  public static AppModule_ProvideDefaultAudioSinkFactory create(Provider<Context> contextProvider,
      Provider<AudioSpectrumProcessor> audioSpectrumProcessorProvider) {
    return new AppModule_ProvideDefaultAudioSinkFactory(contextProvider, audioSpectrumProcessorProvider);
  }

  public static DefaultAudioSink provideDefaultAudioSink(Context context,
      AudioSpectrumProcessor audioSpectrumProcessor) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDefaultAudioSink(context, audioSpectrumProcessor));
  }
}

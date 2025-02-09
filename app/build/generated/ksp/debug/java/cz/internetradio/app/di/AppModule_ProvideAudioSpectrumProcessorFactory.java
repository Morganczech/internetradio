package cz.internetradio.app.di;

import cz.internetradio.app.audio.AudioSpectrumProcessor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideAudioSpectrumProcessorFactory implements Factory<AudioSpectrumProcessor> {
  @Override
  public AudioSpectrumProcessor get() {
    return provideAudioSpectrumProcessor();
  }

  public static AppModule_ProvideAudioSpectrumProcessorFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AudioSpectrumProcessor provideAudioSpectrumProcessor() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAudioSpectrumProcessor());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideAudioSpectrumProcessorFactory INSTANCE = new AppModule_ProvideAudioSpectrumProcessorFactory();
  }
}

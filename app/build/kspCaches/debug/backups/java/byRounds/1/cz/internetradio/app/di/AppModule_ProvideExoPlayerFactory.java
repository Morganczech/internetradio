package cz.internetradio.app.di;

import android.content.Context;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
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
public final class AppModule_ProvideExoPlayerFactory implements Factory<ExoPlayer> {
  private final Provider<Context> contextProvider;

  private final Provider<DefaultAudioSink> defaultAudioSinkProvider;

  public AppModule_ProvideExoPlayerFactory(Provider<Context> contextProvider,
      Provider<DefaultAudioSink> defaultAudioSinkProvider) {
    this.contextProvider = contextProvider;
    this.defaultAudioSinkProvider = defaultAudioSinkProvider;
  }

  @Override
  public ExoPlayer get() {
    return provideExoPlayer(contextProvider.get(), defaultAudioSinkProvider.get());
  }

  public static AppModule_ProvideExoPlayerFactory create(Provider<Context> contextProvider,
      Provider<DefaultAudioSink> defaultAudioSinkProvider) {
    return new AppModule_ProvideExoPlayerFactory(contextProvider, defaultAudioSinkProvider);
  }

  public static ExoPlayer provideExoPlayer(Context context, DefaultAudioSink defaultAudioSink) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideExoPlayer(context, defaultAudioSink));
  }
}

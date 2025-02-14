package cz.internetradio.app.repository;

import cz.internetradio.app.api.RadioBrowserApi;
import cz.internetradio.app.data.dao.RadioDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class RadioRepository_Factory implements Factory<RadioRepository> {
  private final Provider<RadioDao> radioDaoProvider;

  private final Provider<RadioBrowserApi> radioBrowserApiProvider;

  public RadioRepository_Factory(Provider<RadioDao> radioDaoProvider,
      Provider<RadioBrowserApi> radioBrowserApiProvider) {
    this.radioDaoProvider = radioDaoProvider;
    this.radioBrowserApiProvider = radioBrowserApiProvider;
  }

  @Override
  public RadioRepository get() {
    return newInstance(radioDaoProvider.get(), radioBrowserApiProvider.get());
  }

  public static RadioRepository_Factory create(Provider<RadioDao> radioDaoProvider,
      Provider<RadioBrowserApi> radioBrowserApiProvider) {
    return new RadioRepository_Factory(radioDaoProvider, radioBrowserApiProvider);
  }

  public static RadioRepository newInstance(RadioDao radioDao, RadioBrowserApi radioBrowserApi) {
    return new RadioRepository(radioDao, radioBrowserApi);
  }
}

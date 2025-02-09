package cz.internetradio.app.repository;

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

  public RadioRepository_Factory(Provider<RadioDao> radioDaoProvider) {
    this.radioDaoProvider = radioDaoProvider;
  }

  @Override
  public RadioRepository get() {
    return newInstance(radioDaoProvider.get());
  }

  public static RadioRepository_Factory create(Provider<RadioDao> radioDaoProvider) {
    return new RadioRepository_Factory(radioDaoProvider);
  }

  public static RadioRepository newInstance(RadioDao radioDao) {
    return new RadioRepository(radioDao);
  }
}

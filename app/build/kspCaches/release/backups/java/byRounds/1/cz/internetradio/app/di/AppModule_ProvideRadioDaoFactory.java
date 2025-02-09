package cz.internetradio.app.di;

import cz.internetradio.app.data.RadioDatabase;
import cz.internetradio.app.data.dao.RadioDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideRadioDaoFactory implements Factory<RadioDao> {
  private final Provider<RadioDatabase> databaseProvider;

  public AppModule_ProvideRadioDaoFactory(Provider<RadioDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public RadioDao get() {
    return provideRadioDao(databaseProvider.get());
  }

  public static AppModule_ProvideRadioDaoFactory create(Provider<RadioDatabase> databaseProvider) {
    return new AppModule_ProvideRadioDaoFactory(databaseProvider);
  }

  public static RadioDao provideRadioDao(RadioDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideRadioDao(database));
  }
}

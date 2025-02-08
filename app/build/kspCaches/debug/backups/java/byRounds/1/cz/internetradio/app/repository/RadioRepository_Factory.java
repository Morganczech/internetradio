package cz.internetradio.app.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class RadioRepository_Factory implements Factory<RadioRepository> {
  @Override
  public RadioRepository get() {
    return newInstance();
  }

  public static RadioRepository_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RadioRepository newInstance() {
    return new RadioRepository();
  }

  private static final class InstanceHolder {
    private static final RadioRepository_Factory INSTANCE = new RadioRepository_Factory();
  }
}

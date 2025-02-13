package cz.internetradio.app.ui;

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
public final class AddRadioViewModel_Factory implements Factory<AddRadioViewModel> {
  private final Provider<RadioRepository> repositoryProvider;

  public AddRadioViewModel_Factory(Provider<RadioRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public AddRadioViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static AddRadioViewModel_Factory create(Provider<RadioRepository> repositoryProvider) {
    return new AddRadioViewModel_Factory(repositoryProvider);
  }

  public static AddRadioViewModel newInstance(RadioRepository repository) {
    return new AddRadioViewModel(repository);
  }
}

package mobi.chouette.dao;

import mobi.chouette.model.Provider;

import java.util.List;
import java.util.Optional;

public interface ProviderDAO extends GenericDAO<Provider> {
    Optional<Provider> findBySchema(String name);
    List<Provider> getAllProviders();
    List<String> getAllWorkingSchemas();
}

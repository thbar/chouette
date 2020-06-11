package mobi.chouette.dao;

import mobi.chouette.model.Provider;

import java.util.Optional;

public interface ProviderDAO extends GenericDAO<Provider> {
    Optional<Provider> findBySchema(String name);
}

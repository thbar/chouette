package mobi.chouette.dao;

import mobi.chouette.model.MappingHastusZdep;

import java.util.Optional;

public interface MappingHastusZdepDAO extends GenericDAO<MappingHastusZdep> {

    Optional<MappingHastusZdep> findByZdep(String zdep);

    Optional<MappingHastusZdep> findByHastus(String hastus);

}

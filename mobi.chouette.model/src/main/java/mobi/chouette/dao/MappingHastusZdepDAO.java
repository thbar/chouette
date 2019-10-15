package mobi.chouette.dao;

import mobi.chouette.model.MappingHastusZdep;

public interface MappingHastusZdepDAO extends GenericDAO<MappingHastusZdep> {

    MappingHastusZdep findByZdep(String zdep);

}

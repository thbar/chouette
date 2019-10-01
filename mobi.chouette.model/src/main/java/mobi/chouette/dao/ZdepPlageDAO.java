package mobi.chouette.dao;

import mobi.chouette.model.ZdepPlage;

public interface ZdepPlageDAO extends GenericDAO<ZdepPlage> {

    ZdepPlage findByZdep(String zdep);

}

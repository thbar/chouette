package mobi.chouette.dao;

import mobi.chouette.model.Variations;

public interface VariationsDAO extends GenericDAO<Variations> {

    void makeLineInsert(String type, String description, Long numJob);

    void makeLineUpdate(String type, String description, Long numJob);
}



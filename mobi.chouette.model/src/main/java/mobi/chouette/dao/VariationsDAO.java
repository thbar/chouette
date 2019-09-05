package mobi.chouette.dao;

import mobi.chouette.model.Variations;

public interface VariationsDAO extends GenericDAO<Variations> {

    void makeVariationsInsert(String type, String description, Long numJob);

    void makeVariationsUpdate(String type, String description, Long numJob);
}



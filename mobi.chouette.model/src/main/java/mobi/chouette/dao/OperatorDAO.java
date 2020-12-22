package mobi.chouette.dao;

import mobi.chouette.model.Operator;

import java.util.List;

public interface OperatorDAO extends GenericDAO<Operator> {

    List<Operator> findByReferential(String schema);
}

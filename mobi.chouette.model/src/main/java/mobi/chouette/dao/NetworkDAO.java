package mobi.chouette.dao;

import mobi.chouette.dao.GenericDAO;
import mobi.chouette.model.Network;

import java.util.Optional;

public interface NetworkDAO extends GenericDAO<Network> {

    Optional<Network> findByName(String name);

}

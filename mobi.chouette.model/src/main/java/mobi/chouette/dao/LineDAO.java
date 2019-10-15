package mobi.chouette.dao;

import mobi.chouette.model.Line;

public interface LineDAO extends GenericDAO<Line> {

    void updateStopareasForIdfmLineCommand(String referential, Long lineId) throws Exception;

}

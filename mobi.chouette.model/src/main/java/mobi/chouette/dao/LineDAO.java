package mobi.chouette.dao;

import mobi.chouette.model.Line;

public interface LineDAO extends GenericDAO<Line> {

    String updateStopareasForIdfmLineCommand(String referential, Long lineId) throws Exception;

}

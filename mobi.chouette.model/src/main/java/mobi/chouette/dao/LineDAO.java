package mobi.chouette.dao;

import mobi.chouette.model.Line;

import java.util.List;

public interface LineDAO extends GenericDAO<Line> {

    String updateStopareasForIdfmLineCommand(Long lineId) throws Exception;

    void mergeDuplicateJourneyPatternsOfLineAndAddSuffix(Long lineId, String lineName);

    List<Line> findByNetworkId(Long networkId);

}

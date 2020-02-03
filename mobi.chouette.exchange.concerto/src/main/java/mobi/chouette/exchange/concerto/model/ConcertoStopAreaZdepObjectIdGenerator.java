package mobi.chouette.exchange.concerto.model;

import mobi.chouette.model.StopArea;

public class ConcertoStopAreaZdepObjectIdGenerator {
    public static ConcertoObjectId getConcertoObjectId(StopArea stop){
        ConcertoObjectId concertoObjectId = new ConcertoObjectId();
        try {
            concertoObjectId.setHastus(stop.getMappingHastusZdep().getHastusChouette());
        } catch(Exception e){
            // le point nest pas idfm on zappe
            // @todo SCH que faire ? on zappe ? swallow ?
            // swallow
        }
        return concertoObjectId;
    }
}

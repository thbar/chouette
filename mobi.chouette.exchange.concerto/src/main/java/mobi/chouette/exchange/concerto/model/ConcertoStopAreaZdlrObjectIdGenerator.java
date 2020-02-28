package mobi.chouette.exchange.concerto.model;

import mobi.chouette.model.StopArea;

public class ConcertoStopAreaZdlrObjectIdGenerator {
    public static ConcertoObjectId getConcertoObjectId(StopArea stop){
        ConcertoObjectId concertoObjectId = new ConcertoObjectId();
        try {
            concertoObjectId.setStif("STIF:StopArea:SP:" + stop.getMappingHastusZdep().getZdlr() + ":");
        } catch(Exception e){
            concertoObjectId.setHastus(stop.getMappingHastusZdep().getHastusChouette());
            // le point nest pas idfm on zappe
            // @todo SCH que faire ? on zappe ? swallow ?
            // swallow
        }
        return concertoObjectId;
    }
}

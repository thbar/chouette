package mobi.chouette.exchange.concerto.model;

import mobi.chouette.model.StopArea;
import org.apache.commons.lang3.StringUtils;

public class ConcertoStopAreaZdepObjectIdGenerator {
    public static ConcertoObjectId getConcertoObjectId(StopArea stop){
        ConcertoObjectId concertoObjectId = new ConcertoObjectId();
        try {
            if(StringUtils.isNotEmpty(stop.getMappingHastusZdep().getHastusOriginal())){
                concertoObjectId.setHastus(stop.getMappingHastusZdep().getHastusOriginal());
            }
            else{
                concertoObjectId.setHastus(stop.getMappingHastusZdep().getHastusChouette());
            }
        } catch(Exception e){
            // le point nest pas idfm on zappe
            // @todo SCH que faire ? on zappe ? swallow ?
            // swallow
        }
        return concertoObjectId;
    }
}

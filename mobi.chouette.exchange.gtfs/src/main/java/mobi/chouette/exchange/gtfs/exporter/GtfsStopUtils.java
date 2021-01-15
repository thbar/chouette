package mobi.chouette.exchange.gtfs.exporter;

import mobi.chouette.exchange.gtfs.importer.IdFormat;
import mobi.chouette.model.StopArea;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GtfsStopUtils {

    private static final String TRIDENT_STOP_PLACE_TYPE=":StopPlace:";
    private static final String TRIDENT_QUAY_TYPE=":Quay:";



    public static String getNewStopId(StopArea stop, String idPrefix, IdFormat idFormat) {

        if(stop != null && StringUtils.isNotEmpty(stop.getOriginalStopId())){
            if (IdFormat.TRIDENT.equals(idFormat) && StringUtils.isNotEmpty(idPrefix)){
                return createTridentId(stop,idPrefix);
            }
            return stop.getOriginalStopId();
        }
        String inputStopId = stop.getObjectId();
        if(StringUtils.isEmpty(inputStopId)) return null;
        String pattern1 = "^.*:.*:.*_.*_.*_[0-9]+a([A-Za-z0-9]+).*$";
        String retour = findInPattern(pattern1, inputStopId);
        if(!StringUtils.isEmpty(retour)) return retour;
        String pattern2 = "^.*:.*:.*_.*_[0-9]+a([A-Za-z0-9]+).*$";
        return findInPattern(pattern2, inputStopId);
    }


    /**
     * Creates a new trident ID depending on the object type : Quay or StopPlace
     * e.g. : PREFIX:Quay:10545 or PREFIX:StopPlace:10545
     * @param stop
     *       Stop for which a trident Id must be generated
     * @param idPrefix
     *       Prefix that will be used on Id beginning.
     * @return
     */
    private static String createTridentId(StopArea stop, String idPrefix){
        if (stop.getObjectId().contains("Quay")){
            return idPrefix+TRIDENT_QUAY_TYPE+stop.getOriginalStopId();
        }
        return idPrefix+TRIDENT_STOP_PLACE_TYPE+stop.getOriginalStopId();
    }

    private static String findInPattern(String pattern, String value){
        if(StringUtils.isEmpty(value)) return null;
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(value);
        String retour = null;
        if (m.matches()) {
            retour = m.group(1);
        }
        return retour;
    }

}

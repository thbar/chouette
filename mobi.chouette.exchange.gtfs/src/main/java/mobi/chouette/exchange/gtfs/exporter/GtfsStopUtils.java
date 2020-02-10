package mobi.chouette.exchange.gtfs.exporter;

import mobi.chouette.model.StopArea;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GtfsStopUtils {
    public static String getNewStopId(StopArea stop) {
        if(stop != null && !StringUtils.isEmpty(stop.getStopId())){
            return stop.getStopId();
        }
        String inputStopId = stop.getObjectId();
        if(StringUtils.isEmpty(inputStopId)) return null;
        String pattern1 = "^.*:.*:.*_.*_.*_[0-9]+a([A-Za-z0-9]+).*$";
        String retour = findInPattern(pattern1, inputStopId);
        if(!StringUtils.isEmpty(retour)) return retour;
        String pattern2 = "^.*:.*:.*_.*_[0-9]+a([A-Za-z0-9]+).*$";
        return findInPattern(pattern2, inputStopId);
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

package mobi.chouette.exchange.gtfs.exporter;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GtfsStopUtils {
    public static String getNewStopId(String inputStopId) {
        if(StringUtils.isEmpty(inputStopId)) return null;
        Pattern p = Pattern.compile("^.*:.*:.*_.*_.*_[0-9]+a([A-Za-z0-9]+).*$");
        Matcher m = p.matcher(inputStopId);
        String retour = null;
        if (m.matches()) {
            retour = m.group(1);
        }
        return retour;
    }
}

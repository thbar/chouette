package mobi.chouette.common;

import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.joda.time.Seconds;

public class TimeUtil {

    public static Duration subtract(LocalTime thisDeparture, LocalTime firstDeparture) {
        int seconds;
        // Assuming journeys last no more than 24 hours
        if (firstDeparture.isBefore(thisDeparture)) {
            seconds = Seconds.secondsBetween(firstDeparture, thisDeparture).getSeconds();
        } else {
            seconds = DateTimeConstants.SECONDS_PER_DAY - Seconds.secondsBetween(thisDeparture, firstDeparture).getSeconds();
        }

        return Duration.standardSeconds(seconds);
    }

}

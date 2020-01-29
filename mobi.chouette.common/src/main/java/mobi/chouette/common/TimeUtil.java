package mobi.chouette.common;

import org.joda.time.DateTimeConstants;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TimeUtil {

    public static Duration subtract(LocalTime thisDeparture, LocalTime firstDeparture) {
        long seconds;
        // Assuming journeys last no more than 24 hours
        if (firstDeparture.isBefore(thisDeparture)) {
            seconds = Duration.between(firstDeparture, thisDeparture).getSeconds();
        } else {
            seconds = DateTimeConstants.SECONDS_PER_DAY - Duration.between(thisDeparture, firstDeparture).getSeconds();
        }

        return Duration.ofSeconds(seconds);
    }

    public static java.time.LocalTime toLocalTimeFromJoda(LocalTime localTime) {
        return localTime;
    }


    public static java.time.Duration toJodaDuration(java.time.Duration duration) {
        return duration;
    }

    public static java.time.Duration toDurationFromJodaDuration(Duration duration) {
        return duration;
    }

    public static java.time.LocalDate toJodaLocalDate(LocalDate localDate) {
        return localDate;
    }

    public static LocalDate toLocalDateFromJoda(java.time.LocalDate localDate) {
        return localDate;
    }

    public static java.time.LocalDateTime toJodaLocalDateTime(java.time.LocalDateTime localDateTime) {
        return localDateTime;
    }

    /**
     * Convert localDateTime to joda LocalDate, ignoring time.
     * <p>
     * This is a bit shady, but necessary as long as incoming data, while semantically a LocalDate, is represented as xs:dateTime.
     */
    public static java.time.LocalDate toJodaLocalDateIgnoreTime(java.time.LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }

        return LocalDate.of(localDateTime.getYear(), localDateTime.getMonthValue(), localDateTime.getDayOfMonth());
    }

    public static LocalDateTime calendarToLocalDateTime(Calendar cal) {
        return LocalDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
    }

    public static LocalDate calendarToLocalDate(Calendar cal) {
        return calendarToLocalDateTime(cal).toLocalDate();
    }

    public static LocalDate dateToLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public static GregorianCalendar localDateTimeToGregorianCalendar(LocalDateTime localDateTime) {
        return GregorianCalendar.from(ZonedDateTime.of(localDateTime, ZoneId.systemDefault()));
    }

    public static Date localDateToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from( localDateTime.atZone( ZoneId.systemDefault()).toInstant());
    }

}

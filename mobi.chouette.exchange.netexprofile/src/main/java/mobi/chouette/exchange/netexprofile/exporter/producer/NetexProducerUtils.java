package mobi.chouette.exchange.netexprofile.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.type.DayTypeEnum;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

import java.sql.Time;
import java.time.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j
public class NetexProducerUtils {

    private static final ZoneId LOCAL_ZONE_ID = ZoneId.of("Europe/Oslo");

    public static boolean isSet(Object... objects) {
        for (Object val : objects) {
            if (val != null) {
                if (val instanceof String) {
                    if (!((String) val).isEmpty())
                        return true;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public static String[] generateIdSequence(int totalInSequence) {
        String[] idSequence = new String[totalInSequence];
        AtomicInteger incrementor = new AtomicInteger(1);

        for (int i = 0; i < totalInSequence; i++) {
            idSequence[i] = String.valueOf(incrementor.getAndAdd(1));
        }

        return idSequence;
    }

    public static ZoneOffset getZoneOffset(ZoneId zoneId) {
        return zoneId == null ? null : zoneId.getRules().getOffset(Instant.now(Clock.system(zoneId)));
    }

    public static OffsetTime toOffsetTimeUtc(Time time) {
        return time == null ? null : time.toLocalTime().atOffset(getZoneOffset(LOCAL_ZONE_ID)).withOffsetSameInstant(ZoneOffset.UTC);
    }


    public static OffsetDateTime toOffsetDateTime(java.util.Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            java.sql.Date sqlDate = (java.sql.Date) date;
            ZonedDateTime zonedDateTime = sqlDate.toLocalDate().atStartOfDay(ZoneId.systemDefault());
            return OffsetDateTime.ofInstant(zonedDateTime.toInstant(), ZoneId.systemDefault());
        }
        return OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.systemDefault());
    }

    public static AllVehicleModesOfTransportEnumeration toVehicleModeOfTransportEnum(String value) {
        if (value == null)
            return null;
        else if (value.equals("Air"))
            return AllVehicleModesOfTransportEnumeration.AIR;
        else if (value.equals("Train"))
            return AllVehicleModesOfTransportEnumeration.RAIL;
        else if (value.equals("LongDistanceTrain"))
            return AllVehicleModesOfTransportEnumeration.INTERCITY_RAIL;
        else if (value.equals("LocalTrain"))
            return AllVehicleModesOfTransportEnumeration.URBAN_RAIL;
        else if (value.equals("Metro"))
            return AllVehicleModesOfTransportEnumeration.METRO;
        else if (value.equals("Tramway"))
            return AllVehicleModesOfTransportEnumeration.TRAM;
        else if (value.equals("Coach"))
            return AllVehicleModesOfTransportEnumeration.COACH;
        else if (value.equals("Bus"))
            return AllVehicleModesOfTransportEnumeration.BUS;
        else if (value.equals("Ferry"))
            return AllVehicleModesOfTransportEnumeration.WATER;
        else if (value.equals("Walk"))
            return AllVehicleModesOfTransportEnumeration.SELF_DRIVE;
        else if (value.equals("Trolleybus"))
            return AllVehicleModesOfTransportEnumeration.TROLLEY_BUS;
        else if (value.equals("Taxi"))
            return AllVehicleModesOfTransportEnumeration.TAXI;
        else if (value.equals("Other"))
            return AllVehicleModesOfTransportEnumeration.UNKNOWN;
        else
            return AllVehicleModesOfTransportEnumeration.UNKNOWN;
    }

    public static List<DayTypeEnum> convertDayOfWeek(DayOfWeekEnumeration dayOfWeek) {
        List<DayTypeEnum> days = new ArrayList<>();

        switch (dayOfWeek) {
            case MONDAY:
                days.add(DayTypeEnum.Monday);
                break;
            case TUESDAY:
                days.add(DayTypeEnum.Tuesday);
                break;
            case WEDNESDAY:
                days.add(DayTypeEnum.Wednesday);
                break;
            case THURSDAY:
                days.add(DayTypeEnum.Thursday);
                break;
            case FRIDAY:
                days.add(DayTypeEnum.Friday);
                break;
            case SATURDAY:
                days.add(DayTypeEnum.Saturday);
                break;
            case SUNDAY:
                days.add(DayTypeEnum.Sunday);
                break;
            case EVERYDAY:
                days.add(DayTypeEnum.Monday);
                days.add(DayTypeEnum.Tuesday);
                days.add(DayTypeEnum.Wednesday);
                days.add(DayTypeEnum.Thursday);
                days.add(DayTypeEnum.Friday);
                days.add(DayTypeEnum.Saturday);
                days.add(DayTypeEnum.Sunday);
                break;
            case WEEKDAYS:
                days.add(DayTypeEnum.Monday);
                days.add(DayTypeEnum.Tuesday);
                days.add(DayTypeEnum.Wednesday);
                days.add(DayTypeEnum.Thursday);
                days.add(DayTypeEnum.Friday);
                break;
            case WEEKEND:
                days.add(DayTypeEnum.Saturday);
                days.add(DayTypeEnum.Sunday);
                break;
            case NONE:
                // None
                break;
        }
        return days;
    }

    @SuppressWarnings("unchecked")
    public static DayOfWeekEnumeration toDayOfWeekEnumeration(List<DayTypeEnum> dayTypeEnums) {
        EnumSet weekend = EnumSet.of(DayTypeEnum.Saturday, DayTypeEnum.Sunday);
        EnumSet weekdays = EnumSet.of(DayTypeEnum.Monday, DayTypeEnum.Tuesday, DayTypeEnum.Wednesday, DayTypeEnum.Thursday, DayTypeEnum.Friday);
        EnumSet everyday = EnumSet.of(DayTypeEnum.Monday, DayTypeEnum.Tuesday, DayTypeEnum.Wednesday, DayTypeEnum.Thursday, DayTypeEnum.Friday, DayTypeEnum.Saturday, DayTypeEnum.Sunday);
        EnumSet actualDaysOfWeek = EnumSet.noneOf(DayTypeEnum.class);

        for (DayTypeEnum dayTypeEnum : dayTypeEnums) {
            actualDaysOfWeek.add(dayTypeEnum);
        }
        if (actualDaysOfWeek.isEmpty()) {
            return DayOfWeekEnumeration.NONE;
        } else if (actualDaysOfWeek.equals(EnumSet.of(DayTypeEnum.Monday))) {
            return DayOfWeekEnumeration.MONDAY;
        } else if (actualDaysOfWeek.equals(EnumSet.of(DayTypeEnum.Tuesday))) {
            return DayOfWeekEnumeration.TUESDAY;
        } else if (actualDaysOfWeek.equals(EnumSet.of(DayTypeEnum.Wednesday))) {
            return DayOfWeekEnumeration.WEDNESDAY;
        } else if (actualDaysOfWeek.equals(EnumSet.of(DayTypeEnum.Thursday))) {
            return DayOfWeekEnumeration.THURSDAY;
        } else if (actualDaysOfWeek.equals(EnumSet.of(DayTypeEnum.Friday))) {
            return DayOfWeekEnumeration.FRIDAY;
        } else if (actualDaysOfWeek.equals(EnumSet.of(DayTypeEnum.Saturday))) {
            return DayOfWeekEnumeration.SATURDAY;
        } else if (actualDaysOfWeek.equals(EnumSet.of(DayTypeEnum.Sunday))) {
            return DayOfWeekEnumeration.SUNDAY;
        } else if (actualDaysOfWeek.equals(weekdays)) {
            return DayOfWeekEnumeration.WEEKDAYS;
        } else if (actualDaysOfWeek.equals(weekend)) {
            return DayOfWeekEnumeration.WEEKEND;
        } else if (actualDaysOfWeek.equals(everyday)) {
            return DayOfWeekEnumeration.EVERYDAY;
        }

        return null;
    }

}
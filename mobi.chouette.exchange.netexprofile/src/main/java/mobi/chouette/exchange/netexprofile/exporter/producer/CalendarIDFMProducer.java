package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.netexprofile.exporter.ExportableData;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.Period;
import mobi.chouette.model.Timetable;
import org.rutebanken.netex.model.DayOfWeekEnumeration;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.OperatingPeriodRefStructure;
import org.rutebanken.netex.model.PropertiesOfDay_RelStructure;
import org.rutebanken.netex.model.PropertyOfDay;

import java.math.BigInteger;
import java.util.List;

public class CalendarIDFMProducer extends NetexProducer {

    public void produce(Context context, ExportableData exportableData, ExportableNetexData exportableNetexData) {

        for (Timetable timetable : exportableData.getTimetables()) {

            String netexDaytypeId = NetexProducerUtils.generateNetexId(timetable);
            netexDaytypeId += ":LOC";
            if (!exportableNetexData.getSharedDayTypes().containsKey(netexDaytypeId)) {
                DayType dayType = netexFactory.createDayType();
                NetexProducerUtils.populateId(timetable, dayType);

                List<DayOfWeekEnumeration> dayOfWeekEnumerations = NetexProducerUtils.toDayOfWeekEnumeration(timetable.getDayTypes());
                if (!dayOfWeekEnumerations.isEmpty()) {
                    dayType.setProperties(createPropertiesOfDay_RelStructure(dayOfWeekEnumerations));
                }

                exportableNetexData.getSharedDayTypes().put(netexDaytypeId, dayType);

                DayTypeRefStructure dayTypeRef = netexFactory.createDayTypeRefStructure();
                NetexProducerUtils.populateReference(timetable, dayTypeRef, true);
                dayTypeRef.setRef(dayTypeRef.getRef() + ":LOC");
                dayTypeRef.setVersion("any");

                // Operating periods
                for (int i = 0; i < timetable.getPeriods().size(); i++) {
                    Period p = timetable.getPeriods().get(i);
                    // Create Operating period
                    String operatingPeriodId = netexDaytypeId.replace("DayType", "OperatingPeriod");
                    OperatingPeriod operatingPeriod = new OperatingPeriod().withVersion(dayType.getVersion())
                            .withId(operatingPeriodId)
                            .withFromDate(TimeUtil.toLocalDateFromJoda(p.getStartDate()).atStartOfDay()).withToDate(TimeUtil.toLocalDateFromJoda(p.getEndDate()).atStartOfDay());
                    if (!exportableNetexData.getSharedOperatingPeriods().containsKey(operatingPeriodId)) {
                        exportableNetexData.getSharedOperatingPeriods().put(operatingPeriodId, operatingPeriod);
                    }

                    OperatingPeriodRefStructure operatingPeriodRef = netexFactory.createOperatingPeriodRefStructure();
                    NetexProducerUtils.populateReference(operatingPeriod, operatingPeriodRef, true);
                    operatingPeriodRef.setVersion("any");

                    // Assign operatingperiod to daytype
                    String dayTypeAssignmentId = netexDaytypeId.replace("DayType", "DayTypeAssignment");
                    DayTypeAssignment dayTypeAssignment = netexFactory.createDayTypeAssignment()
                            .withId(dayTypeAssignmentId).withVersion(NETEX_DEFAULT_OBJECT_VERSION)
                            .withOrder(BigInteger.valueOf(0)).withDayTypeRef(netexFactory.createDayTypeRef(dayTypeRef)).withOperatingPeriodRef(operatingPeriodRef);
                    exportableNetexData.getSharedDayTypeAssignments().add(dayTypeAssignment);

                }

                for (CalendarDay day : timetable.getCalendarDays()) {
                    DayTypeAssignment dayTypeAssignment = netexFactory.createDayTypeAssignment()
                            .withId(NetexProducerUtils.translateObjectId(netexDaytypeId, "DayTypeAssignment")).withVersion(NETEX_DEFAULT_OBJECT_VERSION)
                            .withOrder(BigInteger.valueOf(0)).withDayTypeRef(netexFactory.createDayTypeRef(dayTypeRef))
                            .withDate(TimeUtil.toLocalDateFromJoda(day.getDate()).atStartOfDay());

                    if (day.getIncluded() != null && !day.getIncluded()) {
                        dayTypeAssignment.setIsAvailable(day.getIncluded());
                    }
                    exportableNetexData.getSharedDayTypeAssignments().add(dayTypeAssignment);
                }

            }
        }

    }

    private PropertiesOfDay_RelStructure createPropertiesOfDay_RelStructure(List<DayOfWeekEnumeration> dayOfWeekEnumerations) {
        PropertyOfDay propertyOfDay = netexFactory.createPropertyOfDay();
        for (DayOfWeekEnumeration dayOfWeekEnumeration : dayOfWeekEnumerations) {
            propertyOfDay.getDaysOfWeek().add(dayOfWeekEnumeration);
        }

        PropertiesOfDay_RelStructure propertiesOfDay = netexFactory.createPropertiesOfDay_RelStructure();
        propertiesOfDay.getPropertyOfDay().add(propertyOfDay);
        return propertiesOfDay;
    }
}

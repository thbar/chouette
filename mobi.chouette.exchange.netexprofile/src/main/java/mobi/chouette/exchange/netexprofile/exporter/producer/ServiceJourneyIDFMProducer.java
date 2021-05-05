package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.exporter.ExportableData;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.importer.util.NetexTimeConversionUtil;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.LocalTime;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.LuggageCarriageEnumeration;
import org.rutebanken.netex.model.OperatorRefStructure;
import org.rutebanken.netex.model.ServiceFacilitySet;
import org.rutebanken.netex.model.ServiceFacilitySets_RelStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;

public class ServiceJourneyIDFMProducer {

    private static KeyListStructureProducer keyListStructureProducer = new KeyListStructureProducer();

    public ServiceJourney produce(Context context, VehicleJourney vehicleJourney) {
        ExportableData exportableData = (ExportableData) context.get(Constant.EXPORTABLE_DATA);
        ExportableNetexData exportableNetexData = (ExportableNetexData) context.get(Constant.EXPORTABLE_NETEX_DATA);

        ServiceJourney serviceJourney = netexFactory.createServiceJourney();
        NetexProducerUtils.populateIdAndVersionIDFM(vehicleJourney, serviceJourney);

        serviceJourney.setName(ConversionUtil.getMultiLingualString(vehicleJourney.getPublishedJourneyName()));

        JourneyPattern journeyPattern = vehicleJourney.getJourneyPattern();
        JourneyPatternRefStructure journeyPatternRefStruct = netexFactory.createJourneyPatternRefStructure();
        NetexProducerUtils.populateReferenceIDFM(journeyPattern, journeyPatternRefStruct);
        serviceJourney.setJourneyPatternRef(netexFactory.createJourneyPatternRef(journeyPatternRefStruct));

        NoticeIDFMProducer.addNoticeAndNoticeAssignments(context, exportableNetexData, serviceJourney, vehicleJourney.getFootnotes());

        if (vehicleJourney.getCompany() != null) {
            OperatorRefStructure operatorRefStruct = netexFactory.createOperatorRefStructure();
            NetexProducerUtils.populateReferenceIDFM(vehicleJourney.getCompany(), operatorRefStruct);
            serviceJourney.setOperatorRef(operatorRefStruct);
        }


        if (vehicleJourney.getTimetables().size() > 0) {
            DayTypeRefs_RelStructure dayTypeStruct = netexFactory.createDayTypeRefs_RelStructure();
            serviceJourney.setDayTypes(dayTypeStruct);

            for (Timetable t : vehicleJourney.getTimetables()) {
                for (Timetable timetable : exportableData.getTimetables()) {
                    if (timetable.getObjectId().equals(t.getObjectId())) {
                        DayTypeRefStructure dayTypeRefStruct = netexFactory.createDayTypeRefStructure();
                        NetexProducerUtils.populateReferenceIDFM(t, dayTypeRefStruct);
                        dayTypeStruct.getDayTypeRef().add(netexFactory.createDayTypeRef(dayTypeRefStruct));
                    }
                }
            }
        }

        if (CollectionUtils.isNotEmpty(vehicleJourney.getVehicleJourneyAtStops())) {
            List<VehicleJourneyAtStop> vehicleJourneyAtStops = vehicleJourney.getVehicleJourneyAtStops();
            vehicleJourneyAtStops.sort(Comparator.comparingInt(o -> o.getStopPoint().getPosition()));

            TimetabledPassingTimes_RelStructure passingTimesStruct = netexFactory.createTimetabledPassingTimes_RelStructure();

            for (int i = 0; i < vehicleJourneyAtStops.size(); i++) {
                VehicleJourneyAtStop vehicleJourneyAtStop = vehicleJourneyAtStops.get(i);

                TimetabledPassingTime timetabledPassingTime = netexFactory.createTimetabledPassingTime();
                timetabledPassingTime.setVersion("any");

                LocalTime departureTime = vehicleJourneyAtStop.getDepartureTime();
                LocalTime arrivalTime = vehicleJourneyAtStop.getArrivalTime();

                if (arrivalTime != null) {
                    if (arrivalTime.equals(departureTime)) {
                        NetexTimeConversionUtil.populatePassingTimeUtc(timetabledPassingTime, true, vehicleJourneyAtStop);
                    } else {
                        NetexTimeConversionUtil.populatePassingTimeUtc(timetabledPassingTime, true, vehicleJourneyAtStop);
                    }
                }
                if (departureTime != null) {
                    NetexTimeConversionUtil.populatePassingTimeUtc(timetabledPassingTime, false, vehicleJourneyAtStop);
                    timetabledPassingTime.setDepartureTime(TimeUtil.toLocalTimeFromJoda(departureTime));
                    if (vehicleJourneyAtStop.getDepartureDayOffset() > 0) {
                        timetabledPassingTime.setDepartureDayOffset(BigInteger.valueOf(vehicleJourneyAtStop.getDepartureDayOffset()));
                    }
                }

                passingTimesStruct.getTimetabledPassingTime().add(timetabledPassingTime);
            }

            serviceJourney.setPassingTimes(passingTimesStruct);
        }

        ServiceFacilitySet serviceFacilitySet = new ServiceFacilitySet();
        NetexProducerUtils.populateIdAndVersionIDFM(vehicleJourney, serviceFacilitySet);
        serviceFacilitySet.setId(serviceFacilitySet.getId().replace("ServiceJourney", "ServiceFacilitySet"));
        serviceFacilitySet.setVersion("any");
        if (vehicleJourney.getBikesAllowed() != null && vehicleJourney.getBikesAllowed().equals(true)) {
            serviceFacilitySet.withLuggageCarriageFacilityList(LuggageCarriageEnumeration.CYCLES_ALLOWED);
        }
        else if(vehicleJourney.getBikesAllowed() != null && vehicleJourney.getBikesAllowed().equals(false)){
            serviceFacilitySet.withLuggageCarriageFacilityList(LuggageCarriageEnumeration.NO_CYCLES);
        }
        else{
            serviceFacilitySet.withLuggageCarriageFacilityList(LuggageCarriageEnumeration.UNKNOWN);
        }

        ServiceFacilitySets_RelStructure serviceFacilitySets_relStructure = new ServiceFacilitySets_RelStructure();
        serviceFacilitySets_relStructure.withServiceFacilitySetRefOrServiceFacilitySet(serviceFacilitySet);
        serviceJourney.setFacilities(serviceFacilitySets_relStructure);

        serviceJourney.setKeyList(keyListStructureProducer.produce(vehicleJourney.getKeyValues()));
        serviceJourney.setServiceAlteration(ConversionUtil.toServiceAlterationEnumeration(vehicleJourney.getServiceAlteration()));

        return serviceJourney;
    }
}

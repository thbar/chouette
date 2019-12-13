package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.exporter.ExportableData;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.importer.util.NetexTimeConversionUtil;
import mobi.chouette.model.*;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.VehicleJourney;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.LocalTime;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.FlexibleServiceProperties;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceJourneyProducer extends NetexProducer {

	private static KeyListStructureProducer keyListStructureProducer = new KeyListStructureProducer();

	private static ContactStructureProducer contactStructureProducer = new ContactStructureProducer();

	public ServiceJourney produce(Context context, VehicleJourney vehicleJourney, Line line) {
        ExportableData exportableData = (ExportableData) context.get(Constant.EXPORTABLE_DATA);
        ExportableNetexData exportableNetexData = (ExportableNetexData) context.get(Constant.EXPORTABLE_NETEX_DATA);

		ServiceJourney serviceJourney = netexFactory.createServiceJourney();
		NetexProducerUtils.populateId(vehicleJourney, serviceJourney);

		serviceJourney.setName(ConversionUtil.getMultiLingualString(vehicleJourney.getPublishedJourneyName()));
		serviceJourney.setPublicCode(vehicleJourney.getPublishedJourneyIdentifier());

		if (vehicleJourney.getPrivateCode()!=null){
			serviceJourney.setPrivateCode(new PrivateCodeStructure().withValue(vehicleJourney.getPrivateCode()));
		}

		serviceJourney.setDescription(ConversionUtil.getMultiLingualString(vehicleJourney.getComment()));
		serviceJourney.setTransportMode(ConversionUtil.toVehicleModeOfTransportEnum(vehicleJourney.getTransportMode()));
		serviceJourney.setTransportSubmode(ConversionUtil.toTransportSubmodeStructure(vehicleJourney.getTransportSubMode()));

		JourneyPattern journeyPattern = vehicleJourney.getJourneyPattern();
		JourneyPatternRefStructure journeyPatternRefStruct = netexFactory.createJourneyPatternRefStructure();
		NetexProducerUtils.populateReference(journeyPattern, journeyPatternRefStruct, true);
		serviceJourney.setJourneyPatternRef(netexFactory.createJourneyPatternRef(journeyPatternRefStruct));

		serviceJourney.setLineRef(NetexProducerUtils.createLineRef(line, netexFactory));

		NoticeProducer.addNoticeAndNoticeAssignments(context, exportableNetexData, exportableNetexData.getNoticeAssignmentsTimetableFrame(), vehicleJourney.getFootnotes(), vehicleJourney);
		
		if (vehicleJourney.getCompany() != null) {
			OperatorRefStructure operatorRefStruct = netexFactory.createOperatorRefStructure();
			NetexProducerUtils.populateReference(vehicleJourney.getCompany(), operatorRefStruct, false);
			serviceJourney.setOperatorRef(operatorRefStruct);
		}


		if (vehicleJourney.getTimetables().size() > 0) {
			DayTypeRefs_RelStructure dayTypeStruct = netexFactory.createDayTypeRefs_RelStructure();
			serviceJourney.setDayTypes(dayTypeStruct);

			for (Timetable t : vehicleJourney.getTimetables()) {
				for(Timetable timetable: exportableData.getTimetables()){
					if(timetable.getObjectId().equals(t.getObjectId())){
						DayTypeRefStructure dayTypeRefStruct = netexFactory.createDayTypeRefStructure();
						NetexProducerUtils.populateReference(t, dayTypeRefStruct, false);
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
				NetexProducerUtils.populateId(vehicleJourneyAtStop, timetabledPassingTime);

				StopPoint stopPoint = vehicleJourneyAtStop.getStopPoint();
				StopPointInJourneyPatternRefStructure pointInPatternRefStruct = netexFactory.createStopPointInJourneyPatternRefStructure();
				NetexProducerUtils.populateReference(stopPoint, pointInPatternRefStruct, true);
                pointInPatternRefStruct.setRef(pointInPatternRefStruct.getRef().concat(journeyPatternRefStruct.getRef().substring(journeyPatternRefStruct.getRef().lastIndexOf(":")+1)));
				timetabledPassingTime.setPointInJourneyPatternRef(netexFactory.createStopPointInJourneyPatternRef(pointInPatternRefStruct));

				LocalTime departureTime = vehicleJourneyAtStop.getDepartureTime();
				LocalTime arrivalTime = vehicleJourneyAtStop.getArrivalTime();

				if (arrivalTime != null) {
					if (arrivalTime.equals(departureTime)) {
						if (!(i + 1 < vehicleJourneyAtStops.size())) {
							NetexTimeConversionUtil.populatePassingTimeUtc(timetabledPassingTime, true, vehicleJourneyAtStop);
						}
					} else {
						NetexTimeConversionUtil.populatePassingTimeUtc(timetabledPassingTime, true, vehicleJourneyAtStop);
					}
				}
				if (departureTime != null) {
					if ((i + 1 < vehicleJourneyAtStops.size())) {
						NetexTimeConversionUtil.populatePassingTimeUtc(timetabledPassingTime, false, vehicleJourneyAtStop);
						timetabledPassingTime.setDepartureTime(TimeUtil.toLocalTimeFromJoda(departureTime));
						if (vehicleJourneyAtStop.getDepartureDayOffset() > 0) {
							timetabledPassingTime.setDepartureDayOffset(BigInteger.valueOf(vehicleJourneyAtStop.getDepartureDayOffset()));
						}

					}
				}

				passingTimesStruct.getTimetabledPassingTime().add(timetabledPassingTime);
				
				NoticeProducer.addNoticeAndNoticeAssignments(context, exportableNetexData, exportableNetexData.getNoticeAssignmentsTimetableFrame(), vehicleJourneyAtStop.getFootnotes(), vehicleJourneyAtStop);
			}

			mobi.chouette.model.FlexibleServiceProperties chouetteFSP = vehicleJourney.getFlexibleServiceProperties();
			if (chouetteFSP!=null) {
				FlexibleServiceProperties netexFSP = new FlexibleServiceProperties();
				serviceJourney.setFlexibleServiceProperties(netexFSP);
				NetexProducerUtils.populateId(chouetteFSP, netexFSP);
				netexFSP.setFlexibleServiceType(ConversionUtil.toFlexibleServiceType(chouetteFSP.getFlexibleServiceType()));
				netexFSP.setCancellationPossible(chouetteFSP.getCancellationPossible());
				netexFSP.setChangeOfTimePossible(chouetteFSP.getChangeOfTimePossible());

				BookingArrangement bookingArrangement = chouetteFSP.getBookingArrangement();
				if (bookingArrangement != null) {
					if (bookingArrangement.getBookingNote() != null) {
						netexFSP.setBookingNote(new MultilingualString().withValue(bookingArrangement.getBookingNote()));
					}
					netexFSP.setBookingAccess(ConversionUtil.toBookingAccess(bookingArrangement.getBookingAccess()));
					netexFSP.setBookWhen(ConversionUtil.toPurchaseWhen(bookingArrangement.getBookWhen()));
					if (!CollectionUtils.isEmpty(bookingArrangement.getBuyWhen())) {
						netexFSP.withBuyWhen(bookingArrangement.getBuyWhen().stream().map(ConversionUtil::toPurchaseMoment).collect(Collectors.toList()));
					}
					if (!CollectionUtils.isEmpty(bookingArrangement.getBookingMethods())) {
						netexFSP.withBookingMethods(bookingArrangement.getBookingMethods().stream().map(ConversionUtil::toBookingMethod).collect(Collectors.toList()));
					}
					netexFSP.setLatestBookingTime(TimeUtil.toLocalTimeFromJoda(bookingArrangement.getLatestBookingTime()));
					netexFSP.setMinimumBookingPeriod(TimeUtil.toDurationFromJodaDuration(bookingArrangement.getMinimumBookingPeriod()));

					//netexFSP.setBookingContact(contactStructureProducer.produce(bookingArrangement.getBookingContact()));
				}
			}

			serviceJourney.setPassingTimes(passingTimesStruct);
		}

		serviceJourney.setKeyList(keyListStructureProducer.produce(vehicleJourney.getKeyValues()));
		serviceJourney.setServiceAlteration(ConversionUtil.toServiceAlterationEnumeration(vehicleJourney.getServiceAlteration()));

		return serviceJourney;
	}
}

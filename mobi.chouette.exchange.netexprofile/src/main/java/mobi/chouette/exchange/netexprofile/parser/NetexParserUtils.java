package mobi.chouette.exchange.netexprofile.parser;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.importer.ParserUtils;
import mobi.chouette.model.type.AlightingPossibilityEnum;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;
import mobi.chouette.model.type.BoardingPossibilityEnum;
import mobi.chouette.model.type.DayTypeEnum;
import mobi.chouette.model.type.OrganisationTypeEnum;
import mobi.chouette.model.type.TransportModeNameEnum;

import org.rutebanken.netex.model.DayOfWeekEnumeration;
import org.rutebanken.netex.model.EntityInVersionStructure;
import org.rutebanken.netex.model.OrganisationTypeEnumeration;

@Log4j
public class NetexParserUtils extends ParserUtils {

	public static TransportModeNameEnum toTransportModeNameEnum(String value) {
		if (value == null)
			return null;
		else if (value.equals("air"))
			return TransportModeNameEnum.Air;
		else if (value.equals("rail"))
			return TransportModeNameEnum.Train;
		else if (value.equals("intercityRail"))
			return TransportModeNameEnum.LongDistanceTrain;
		else if (value.equals("urbanRail"))
			return TransportModeNameEnum.LocalTrain;
		else if (value.equals("metro"))
			return TransportModeNameEnum.Metro;
		else if (value.equals("tram"))
			return TransportModeNameEnum.Tramway;
		else if (value.equals("coach"))
			return TransportModeNameEnum.Coach;
		else if (value.equals("bus"))
			return TransportModeNameEnum.Bus;
		else if (value.equals("water"))
			return TransportModeNameEnum.Ferry;
		else if (value.equals("selfDrive"))
			return TransportModeNameEnum.Walk;
		else if (value.equals("trolleyBus"))
			return TransportModeNameEnum.Trolleybus;
		else if (value.equals("taxi"))
			return TransportModeNameEnum.Taxi;
		else if (value.equals("unknown"))
			return TransportModeNameEnum.Other;
		else
			return TransportModeNameEnum.Other;
	}

	public static ZoneOffset getZoneOffset(ZoneId zoneId) {
		if (zoneId == null) {
			return null;
		}
		return zoneId.getRules().getOffset(Instant.now(Clock.system(zoneId)));
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

	public static AlightingPossibilityEnum getForAlighting(BoardingAlightingPossibilityEnum boardingAlightingPossibility) {
		if (boardingAlightingPossibility == null)
			return AlightingPossibilityEnum.normal;
		switch (boardingAlightingPossibility) {
			case BoardAndAlight:
				return AlightingPossibilityEnum.normal;
			case AlightOnly:
				return AlightingPossibilityEnum.normal;
			case BoardOnly:
				return AlightingPossibilityEnum.forbidden;
			case NeitherBoardOrAlight:
				return AlightingPossibilityEnum.forbidden;
			case BoardAndAlightOnRequest:
				return AlightingPossibilityEnum.request_stop;
			case AlightOnRequest:
				return AlightingPossibilityEnum.request_stop;
			case BoardOnRequest:
				return AlightingPossibilityEnum.normal;
		}
		return null;
	}

	public static BoardingPossibilityEnum getForBoarding(BoardingAlightingPossibilityEnum boardingAlightingPossibility) {
		if (boardingAlightingPossibility == null)
			return BoardingPossibilityEnum.normal;
		switch (boardingAlightingPossibility) {
			case BoardAndAlight:
				return BoardingPossibilityEnum.normal;
			case AlightOnly:
				return BoardingPossibilityEnum.forbidden;
			case BoardOnly:
				return BoardingPossibilityEnum.normal;
			case NeitherBoardOrAlight:
				return BoardingPossibilityEnum.forbidden;
			case BoardAndAlightOnRequest:
				return BoardingPossibilityEnum.request_stop;
			case AlightOnRequest:
				return BoardingPossibilityEnum.normal;
			case BoardOnRequest:
				return BoardingPossibilityEnum.request_stop;
		}
		return null;
	}

	public static OrganisationTypeEnum getOrganisationType(OrganisationTypeEnumeration organisationTypeEnumeration) {
		if (organisationTypeEnumeration == null)
			return null;
		switch (organisationTypeEnumeration) {
			case AUTHORITY:
				return OrganisationTypeEnum.Authority;
			case OPERATOR:
				return OrganisationTypeEnum.Operator;
		}
		return null;
	}

	public static Integer getVersion(EntityInVersionStructure obj) {
		Integer version = 0;
		try {
			version = Integer.parseInt(obj.getVersion());
		} catch (NumberFormatException e) {
			log.debug("Unable to parse " + obj.getVersion() + " to Integer as supported by Neptune, returning 0");
		}
		return version;
	}

	public static String netexId(String objectIdPrefix, String elementName, String objectIdSuffix) {
		return objectIdPrefix + ":" + elementName + ":" + objectIdSuffix;
	}

}

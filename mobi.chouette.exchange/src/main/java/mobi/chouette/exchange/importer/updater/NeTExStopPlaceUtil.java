package mobi.chouette.exchange.importer.updater;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.type.TransportModeNameEnum;

import mobi.chouette.persistence.hibernate.ContextHolder;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopTypeEnumeration;
import org.rutebanken.netex.model.Zone_VersionStructure;

import static mobi.chouette.common.Constant.IMPORTED_ID;

@Log4j
public class NeTExStopPlaceUtil {

	public static StopTypeEnumeration mapTransportMode(TransportModeNameEnum mode) {
		switch (mode) {

		case Air:
			return StopTypeEnumeration.AIRPORT;
		case Bus:
		case TrolleyBus:
		case Coach:
			return StopTypeEnumeration.ONSTREET_BUS;
		case Ferry:
			return StopTypeEnumeration.FERRY_STOP;
		case Metro:
			return StopTypeEnumeration.METRO_STATION;
		case Rail:
			return StopTypeEnumeration.RAIL_STATION;
		case Tram:
			return StopTypeEnumeration.ONSTREET_TRAM;
		case Water:
			return StopTypeEnumeration.HARBOUR_PORT; // This and Ferry -
														// possible incorrect.
														// Is ferry a valid
														// transport mode?
		case Lift:
		case Cableway:
			return StopTypeEnumeration.LIFT_STATION;
		default:
			return null;
		}
	}

	public static Set<TransportModeNameEnum> findTransportModeForStopArea(Set<TransportModeNameEnum> transportModes,
			StopArea sa) {
		TransportModeNameEnum transportModeName = null;
		List<StopPoint> stopPoints = sa.getContainedScheduledStopPoints().stream().map(ScheduledStopPoint::getStopPoints).flatMap(List::stream).collect(Collectors.toList());
		for (StopPoint stop : stopPoints) {
			if (stop.getRoute() != null && stop.getRoute().getLine() != null) {
				transportModeName = stop.getRoute().getLine().getTransportModeName();
				if (transportModeName != null) {
					transportModes.add(transportModeName);
					break;
				}
			}
		}

		for (StopArea child : sa.getContainedStopAreas()) {
			transportModes = findTransportModeForStopArea(transportModes, child);
		}

		return transportModes;
	}

	public static Optional<String> getImportedId(Zone_VersionStructure stopPlace){

		String currentSchema = ContextHolder.getContext();

		List<String> importedIds = stopPlace.getKeyList().getKeyValue().stream()
				.filter(kv -> IMPORTED_ID.equals(kv.getKey()))
				.map(kv -> kv.getValue().split(","))
				.flatMap(Stream::of)
				.filter(importedId -> importedId != null && importedId.toLowerCase().startsWith(currentSchema))
				.collect(Collectors.toList());
						
		
		if (importedIds.size() == 0)
			return Optional.empty();

		if (importedIds.size() > 1)
			log.warn("Multiple Ids found for object:"+stopPlace.getId());

		return Optional.of(importedIds.get(0));

	}

	public static String extractIdPostfix(String netexId) {
		return netexId.substring(netexId.lastIndexOf(':') + 1).trim();
	}

}

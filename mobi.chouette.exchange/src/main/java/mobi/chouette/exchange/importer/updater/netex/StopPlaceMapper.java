package mobi.chouette.exchange.importer.updater.netex;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.importer.updater.NeTExStopPlaceRegisterUpdater;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang.StringUtils;
import org.rutebanken.netex.model.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Map from chouette model to NeTEx
 */
@Log4j
public class StopPlaceMapper {

    /**
     * Map stop area with contained stop areas.
     *
     * @param stopArea Typically stop areas of {@link ChouetteAreaEnum#StopPlace} or
     *                 {@link ChouetteAreaEnum#CommercialStopPoint}
     * @return NeTEx stop place
     */
    public StopPlace mapStopAreaToStopPlace(StopArea stopArea) {
        StopPlace stopPlace = mapStopPlace(stopArea);
        if (stopArea.getContainedStopAreas().size() > 0) {
            stopPlace.setQuays(new Quays_RelStructure());
            for (StopArea children : stopArea.getContainedStopAreas()) {
                Quay quay = mapQuay(children);
                stopPlace.getQuays().getQuayRefOrQuay().add(quay);
            }
        }

        return stopPlace;
    }

    protected Quay mapQuay(StopArea stopArea) {
        Quay quay = new Quay();
        mapId(stopArea, quay);
        setVersion(stopArea, quay);
        mapCentroid(stopArea, quay);
        mapQuayName(stopArea, quay);
        mapPublicCode(stopArea, quay);
        mapCompassBearing(stopArea, quay);
        if (StringUtils.isNotBlank(stopArea.getComment())) {
            quay.setDescription(new MultilingualString().withValue(stopArea.getComment()));
        }
        return quay;
    }

    public void mapPublicCode(StopArea stopArea, Quay quay) {
        String registrationNumber = stopArea.getRegistrationNumber();
        if (registrationNumber != null) {
            quay.setPublicCode(registrationNumber);
        }
    }

    private StopPlace mapStopPlace(StopArea stopArea) {
        StopPlace stopPlace = new StopPlace();
        mapId(stopArea, stopPlace);
        setVersion(stopArea, stopPlace);
        mapCentroid(stopArea, stopPlace);
        mapName(stopArea, stopPlace);
        return stopPlace;
    }

    private void mapCompassBearing(StopArea stopArea, Quay quay) {
        if (stopArea.getCompassBearing() != null) {
            quay.setCompassBearing(new Float(stopArea.getCompassBearing()));
        }
    }

    public void setVersion(StopArea stopArea, EntityInVersionStructure entity) {
        entity.setVersion(String.valueOf(stopArea.getObjectVersion()));
    }

    private void mapId(StopArea stopArea, Zone_VersionStructure zone) {
        zone.setId(stopArea.getObjectId());
    }

    public void replaceIdIfQuayOrStopPlace(Zone_VersionStructure zone) {
        zone.setId(zone.getId());


    }

    private void mapCentroid(StopArea stopArea, Zone_VersionStructure zone) {
        setVersion(stopArea, zone);
        if (stopArea.getLatitude() != null && stopArea.getLongitude() != null) {
            zone.setCentroid(new SimplePoint_VersionStructure().withLocation(
                    new LocationStructure().withLatitude(stopArea.getLatitude()).withLongitude(stopArea.getLongitude())));
        }
    }

    private void mapName(StopArea stopArea, Zone_VersionStructure zone) {
        zone.setName(new MultilingualString().withValue(stopArea.getName()).withLang("no").withTextIdType(""));

    }

    private void mapQuayName(StopArea stopArea, Zone_VersionStructure zone) {
        String quayName = stopArea.getName();
        if (quayName != null) {
            zone.setName(new MultilingualString().withValue(quayName).withLang("no").withTextIdType(""));
        }
    }

    public void mapTransportMode(StopPlace sp, TransportModeNameEnum mode) {
        switch (mode) {
            case Air:
                sp.setStopPlaceType(StopTypeEnumeration.AIRPORT);
                break;
            case Bus:
            case Coach:
                sp.setStopPlaceType(StopTypeEnumeration.ONSTREET_BUS);
                break;
            case Water:
            case Ferry:
                sp.setStopPlaceType(StopTypeEnumeration.FERRY_STOP);
                break;
            case Metro:
                sp.setStopPlaceType(StopTypeEnumeration.METRO_STATION);
                break;
            case Rail:
                sp.setStopPlaceType(StopTypeEnumeration.RAIL_STATION);
                break;
            case TrolleyBus:
            case Tram:
                sp.setStopPlaceType(StopTypeEnumeration.ONSTREET_TRAM);
                break;
            default:
                log.warn("Could not map stop place type for stop place " + sp.getId() + " Chouette type was: " + mode + ". Mapping to type OTHER");
                sp.setStopPlaceType(StopTypeEnumeration.OTHER);
        }
        log.debug("Mapped stop place type from " + mode + " to " + sp.getStopPlaceType() + " for stop " + sp.getId());
    }

    /**
     * Add imported id if found in referential
     *
     * @param stopPlace   Netex stop place
     * @param referential chouette import referential
     * @return Netex stop place augmented with imported id if found.
     */
    public StopPlace addImportedIdInfo(StopPlace stopPlace, Referential referential) {
        Map<String, String> stopAreaMappingInverse = new HashMap<>();
        for(Map.Entry<String, String> entry : referential.getStopAreaMapping().entrySet()){
            stopAreaMappingInverse.put(entry.getValue(), entry.getKey());
        }
        String importedId = stopAreaMappingInverse.get(stopPlace.getId());
        if (StringUtils.isNotBlank(importedId)) {
            stopPlace.withKeyList(new KeyListStructure().withKeyValue(new KeyValueStructure()
                    .withKey(NeTExStopPlaceRegisterUpdater.IMPORTED_ID)
                    .withValue(importedId)));
        }
        return stopPlace;
    }
}

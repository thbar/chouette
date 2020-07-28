package mobi.chouette.exchange.importer.updater.netex;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.importer.updater.NeTExIdfmStopPlaceRegisterUpdater;
import mobi.chouette.exchange.importer.updater.NeTExStopPlaceRegisterUpdater;
import mobi.chouette.model.MappingHastusZdep;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.runtime.directive.Stop;
import org.checkerframework.checker.units.qual.A;
import org.rutebanken.netex.model.AccessibilityAssessment;
import org.rutebanken.netex.model.AccessibilityLimitation;
import org.rutebanken.netex.model.AccessibilityLimitations_RelStructure;
import org.rutebanken.netex.model.EntityInVersionStructure;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.LimitationStatusEnumeration;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopTypeEnumeration;
import org.rutebanken.netex.model.Zone_VersionStructure;

import java.util.HashMap;
import java.util.Map;

/**
 * Map from chouette model to NeTEx
 */
@Log4j
public class StopPlaceMapper {

    private static final String VERSION = "1";

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
        setVersion(quay);
        mapCentroid(stopArea, quay);
        mapQuayName(stopArea, quay);
        mapPublicCode(stopArea, quay);
        mapUrl(stopArea, quay);
        mapCompassBearing(stopArea, quay);
        mapComment(stopArea, quay);
        mapMobilityRestrictedSuitable(stopArea, quay);
        mapZdep(stopArea, quay);

        return quay;
    }

    public void mapZdep(StopArea stopArea, Quay quay) {
        if (stopArea.getMappingHastusZdep() != null) {
            quay.withKeyList(new KeyListStructure().withKeyValue(new KeyValueStructure()
                    .withKey(NeTExIdfmStopPlaceRegisterUpdater.ZDEP)
                    .withValue(stopArea.getMappingHastusZdep().getZdep())));
        }
    }

    public void mapMobilityRestrictedSuitable(StopArea stopArea, Quay quay){
        AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
        AccessibilityLimitations_RelStructure accessibilityLimitations_relStructure = new AccessibilityLimitations_RelStructure();
        AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
        if(stopArea.getMobilityRestrictedSuitable() == null){
            accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.UNKNOWN);
        }
        else if (!stopArea.getMobilityRestrictedSuitable()){
            accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.FALSE);
        }
        else {
            accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.TRUE);
        }
        accessibilityLimitation.setVersion(VERSION);
        accessibilityLimitations_relStructure.setAccessibilityLimitation(accessibilityLimitation);
        accessibilityAssessment.setLimitations(accessibilityLimitations_relStructure);
        accessibilityAssessment.setVersion(VERSION);
        quay.setAccessibilityAssessment(accessibilityAssessment);
    }

    public void mapComment(StopArea stopArea, Quay quay) {
        if (StringUtils.isNotBlank(stopArea.getComment())) {
            quay.setDescription(new MultilingualString().withValue(stopArea.getComment()));
        }
    }

    public void mapUrl(StopArea stopArea, Quay quay) {
        if (StringUtils.isNotBlank(stopArea.getUrl())) {
            quay.setUrl(stopArea.getUrl());
        }
    }

    public void mapPublicCode(StopArea stopArea, Quay quay) {
        if (StringUtils.isNotBlank(stopArea.getRegistrationNumber())) {
            quay.setPublicCode(stopArea.getRegistrationNumber());
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

    public void setVersion(EntityInVersionStructure entity) {
        entity.setVersion(VERSION);
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
        zone.setName(new MultilingualString().withValue(stopArea.getName()).withLang("fr").withTextIdType(""));
    }

    private void mapQuayName(StopArea stopArea, Zone_VersionStructure zone) {
        if (StringUtils.isNotBlank(stopArea.getName())) {
            zone.setName(new MultilingualString().withValue(stopArea.getName()).withLang("fr").withTextIdType(""));
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
        for (Map.Entry<String, String> entry : referential.getStopAreaMapping().entrySet()) {
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

    public StopPlace addImportedIdfmInfo(StopPlace stopPlace, Referential referential) {
        Map<String, String> stopAreaMappingInverse = new HashMap<>();
        for (Map.Entry<String, String> entry : referential.getStopAreaMapping().entrySet()) {
            stopAreaMappingInverse.put(entry.getValue(), entry.getKey());
        }
        String importedId = stopAreaMappingInverse.get(stopPlace.getId());
        if (StringUtils.isNotBlank(importedId)) {
            stopPlace.withKeyList(new KeyListStructure().withKeyValue(new KeyValueStructure()
                    .withKey(NeTExIdfmStopPlaceRegisterUpdater.IMPORTED_ID)
                    .withValue(importedId)));
        }

        return stopPlace;
    }
}

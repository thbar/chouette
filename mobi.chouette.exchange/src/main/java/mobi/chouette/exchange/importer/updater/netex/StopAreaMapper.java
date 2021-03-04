package mobi.chouette.exchange.importer.updater.netex;

import mobi.chouette.exchange.importer.updater.NeTExStopPlaceUtil;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.LongLatTypeEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.LimitationStatusEnumeration;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.Zone_VersionStructure;

import java.util.List;
import java.util.Optional;

import static mobi.chouette.common.Constant.IMPORTED_ID;

/**
 * Map from NeTEx to chouette model
 */
public class StopAreaMapper {

    public StopArea mapCommercialStopPoint(Referential referential, StopArea stopArea) {
        String split[] = stopArea.getObjectId().split(":");
        String parentId = split[0] + ":StopPlace:" + split[2];

        StopArea parent = ObjectFactory.getStopArea(referential, parentId);
        parent.setAreaType(ChouetteAreaEnum.CommercialStopPoint);
        parent.setLatitude(stopArea.getLatitude());
        parent.setLongitude(stopArea.getLongitude());
        parent.setLongLatType(stopArea.getLongLatType());
        parent.setName(stopArea.getName());

        stopArea.setParent(parent);

        return parent;
    }

    public StopArea mapStopPlaceToStopArea(Referential referential, StopPlace stopPlace) {
        StopArea stopArea = mapStopArea(referential, stopPlace);

        Quays_RelStructure quays = stopPlace.getQuays();
        if (quays != null) {
            for (Object q : quays.getQuayRefOrQuay()) {
                StopArea boardingPosition = mapBoardingPosition(referential, stopPlace, (Quay) q);
                boardingPosition.setParent(stopArea);
            }
        }

        return stopArea;
    }

    public void mapCentroidToChouette(Zone_VersionStructure zone, StopArea stopArea) {
        if (zone.getCentroid() != null && zone.getCentroid().getLocation() != null) {
            LocationStructure location = zone.getCentroid().getLocation();
            stopArea.setLatitude(location.getLatitude());
            stopArea.setLongitude(location.getLongitude());
            stopArea.setLongLatType(LongLatTypeEnum.WGS84);
        }
    }

    public void mapQuayName(StopPlace stopPlace, Quay quay, StopArea stopArea) {
        if (quay.getName() == null) {
            if (stopPlace.getName() != null) {
                stopArea.setName(stopPlace.getName().getValue());
            }
        } else if (quay.getName() != null) {
            if (stopPlace.getName() != null && multiLingualStringEquals(stopPlace.getName(), quay.getName())) {
                // Same as parent
                stopArea.setName(quay.getName().getValue());
            } else {
                stopArea.setName(quay.getName().getValue());
                stopArea.setRegistrationNumber(quay.getPublicCode());
            }
        }
    }

    public void mapName(Zone_VersionStructure zone, StopArea stopArea) {
        if (zone.getName() != null) {
            stopArea.setName(zone.getName().getValue());
        }
    }

    private boolean multiLingualStringEquals(MultilingualString a, MultilingualString b) {
        return a.getValue().equals(b.getValue());
    }

    private StopArea mapBoardingPosition(Referential referential, StopPlace stopPlace, Quay quay) {

        StopArea boardingPosition = ObjectFactory.getStopArea(referential, quay.getId());
        // Set default values TODO set what we get from NSR
        mapQuayMobilityRestrictedSuitable(quay, boardingPosition);
        boardingPosition.setLiftAvailable(null);
        boardingPosition.setStairsAvailable(null);
        mapQuayDescription(quay, boardingPosition);
        boardingPosition.setAreaType(ChouetteAreaEnum.BoardingPosition);
        mapCentroidToChouette(quay, boardingPosition);
        mapQuayName(stopPlace, quay, boardingPosition);
        mapQuayUrl(quay, boardingPosition);
        mapQuayRegistrationNumber(quay, boardingPosition);
        createCompassBearing(quay, boardingPosition);
        mapOriginalStopId(quay,boardingPosition);
        return boardingPosition;
    }

    private void mapQuayDescription(Quay quay, StopArea boardingPosition) {
        if(quay.getDescription() != null){
            if (StringUtils.isNotBlank(quay.getDescription().getValue())) {
                boardingPosition.setComment(quay.getDescription().getValue());
            }
        }
    }

    private void mapQuayMobilityRestrictedSuitable(Quay quay, StopArea boardingPosition) {
        if(quay.getAccessibilityAssessment() != null){
            if(quay.getAccessibilityAssessment().getLimitations() != null){
                if(quay.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation() != null){
                    if(quay.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess() != null){
                        if(quay.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().equals(LimitationStatusEnumeration.TRUE)){
                            boardingPosition.setMobilityRestrictedSuitable(true);
                        }
                        if(quay.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().equals(LimitationStatusEnumeration.FALSE)){
                            boardingPosition.setMobilityRestrictedSuitable(false);
                        }
                        if(quay.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().equals(LimitationStatusEnumeration.UNKNOWN)){
                            boardingPosition.setMobilityRestrictedSuitable(null);
                        }
                    }
                }
            }
        }
    }

    private StopArea mapStopArea(Referential referential, StopPlace stopPlace) {
        StopArea stopArea = ObjectFactory.getStopArea(referential, stopPlace.getId());
        stopArea.setAreaType(ChouetteAreaEnum.CommercialStopPoint);

        // Set default values TODO set what we get from NSR
        stopArea.setMobilityRestrictedSuitable(null);
        stopArea.setLiftAvailable(null);
        stopArea.setStairsAvailable(null);


        mapCentroidToChouette(stopPlace, stopArea);
        mapName(stopPlace, stopArea);
        mapOriginalStopId(stopPlace,stopArea);
        return stopArea;
    }

    private void mapOriginalStopId(Zone_VersionStructure srcZone, StopArea createdStopArea){
        Optional<String> importedIdOpt = NeTExStopPlaceUtil.getImportedId(srcZone);
        importedIdOpt.ifPresent(importedId->createdStopArea.setOriginalStopId(NeTExStopPlaceUtil.extractIdPostfix(importedId)));
    }




    private void createCompassBearing(Quay quay, StopArea boardingPosition) {
        if (quay.getCompassBearing() != null) {
            boardingPosition.setCompassBearing(quay.getCompassBearing().intValue());
        }
    }

    private void mapQuayUrl(Quay quay, StopArea boardingPosition){
        if(StringUtils.isNotBlank(quay.getUrl())){
            boardingPosition.setUrl(quay.getUrl());
        }
    }

    private void mapQuayRegistrationNumber(Quay quay, StopArea boardingPosition){
        if(StringUtils.isNotBlank(quay.getPublicCode())){
            boardingPosition.setRegistrationNumber(quay.getPublicCode());
        }
    }
}

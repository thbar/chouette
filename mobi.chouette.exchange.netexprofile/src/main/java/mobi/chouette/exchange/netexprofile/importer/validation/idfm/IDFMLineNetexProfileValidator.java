package mobi.chouette.exchange.netexprofile.importer.validation.idfm;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.importer.util.DataLocationHelper;
import mobi.chouette.exchange.netexprofile.importer.util.IdVersion;
import mobi.chouette.exchange.netexprofile.importer.validation.NetexProfileValidator;
import mobi.chouette.exchange.netexprofile.importer.validation.NetexProfileValidatorFactory;
import mobi.chouette.exchange.netexprofile.importer.validation.norway.DummyStopReferentialIdValidator;
import mobi.chouette.exchange.netexprofile.importer.validation.norway.ServiceJourneyInterchangeIgnorer;
import mobi.chouette.exchange.netexprofile.importer.validation.norway.StopPlaceRegistryIdValidator;
import mobi.chouette.exchange.netexprofile.util.NetexIdExtractorHelper;
import mobi.chouette.exchange.validation.ValidationData;
import mobi.chouette.exchange.validation.report.ValidationReporter;
import mobi.chouette.model.Codespace;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import org.apache.commons.lang.StringUtils;
import org.rutebanken.netex.model.BookingAccessEnumeration;
import org.rutebanken.netex.model.BookingMethodEnumeration;
import org.rutebanken.netex.model.FlexibleLineTypeEnumeration;
import org.rutebanken.netex.model.FlexibleServiceEnumeration;
import org.rutebanken.netex.model.PurchaseMomentEnumeration;
import org.rutebanken.netex.model.PurchaseWhenEnumeration;

import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j
public class IDFMLineNetexProfileValidator extends AbstractIDFMNetexProfileValidator implements NetexProfileValidator {

    public static final String NAME = "IDFMLineNetexProfileValidator";

    private static final String[] validTransportModes = new String[]{
            "coach",
            "bus",
            "tram",
            "rail",
            "metro",
            "air",
            "water",
            "cableway",
            "funicular",
            "unknown"
    };


    private static final String[] validTransportSubModes = new String[]{
            "internationalCoach",
            "nationalCoach",
            "touristCoach",
            "airportLinkBus",
            "localTram",
            "cityTram",
            "international",
            "metro",
            "domesticFlight",
            "highSpeedPassengerService",
            "telecabin",
            "funicular",
            "expressBus",
            "interregionalRail",
            "helicopterService",
            "highSpeedVehicleService",
            "localBus",
            "local",
            "internationalFlight",
            "internationalCarFerry",
            "nightBus",
            "longDistance",
            "internationalPassengerFerry",
            "railReplacementBus",
            "nightRail",
            "airportLinkRail",
            "localCarFerry",
            "regionalBus",
            "regionalRail",
            "localPassengerFerry",
            "schoolBus",
            "touristRailway",
            "nationalCarFerry",
            "shuttleBus",
            "sightseeingService",
            "sightseeingBus"
    };

    private String validTransportModeString;
    private String validTransportSubModeString;

    private String validBookingAccessString = formatLegalEnumValues(BookingAccessEnumeration.PUBLIC.value(),
            BookingAccessEnumeration.AUTHORISED_PUBLIC.value(),
            BookingAccessEnumeration.STAFF.value());
    private String validBookingMethodString = formatLegalEnumValues(BookingMethodEnumeration.CALL_DRIVER.value(),
            BookingMethodEnumeration.CALL_OFFICE.value(),
            BookingMethodEnumeration.ONLINE.value(),
            BookingMethodEnumeration.PHONE_AT_STOP.value(),
            BookingMethodEnumeration.TEXT.value());
    private String validFlexibleLineTypeString = formatLegalEnumValues(FlexibleLineTypeEnumeration.CORRIDOR_SERVICE.value(),
            FlexibleLineTypeEnumeration.MAIN_ROUTE_WITH_FLEXIBLE_ENDS.value(),
            FlexibleLineTypeEnumeration.FLEXIBLE_AREAS_ONLY.value(),
            FlexibleLineTypeEnumeration.HAIL_AND_RIDE_SECTIONS.value(),
            FlexibleLineTypeEnumeration.FIXED_STOP_AREA_WIDE.value(),
            FlexibleLineTypeEnumeration.MIXED_FLEXIBLE.value(),
            FlexibleLineTypeEnumeration.MIXED_FLEXIBLE_AND_FIXED.value(),
            FlexibleLineTypeEnumeration.FIXED.value());
    private String validBookWhenString = formatLegalEnumValues(PurchaseWhenEnumeration.TIME_OF_TRAVEL_ONLY.value(),
            PurchaseWhenEnumeration.DAY_OF_TRAVEL_ONLY.value(),
            PurchaseWhenEnumeration.UNTIL_PREVIOUS_DAY.value(),
            PurchaseWhenEnumeration.ADVANCE_ONLY.value(),
            PurchaseWhenEnumeration.ADVANCE_AND_DAY_OF_TRAVEL.value());
    private String validBuyWhenString = formatLegalEnumValues(PurchaseMomentEnumeration.ON_RESERVATION.value(),
            PurchaseMomentEnumeration.BEFORE_BOARDING.value(),
            PurchaseMomentEnumeration.AFTER_BOARDING.value(),
            PurchaseMomentEnumeration.ON_CHECK_OUT.value());
    private String validFlexibleServiceTypeString = formatLegalEnumValues(FlexibleServiceEnumeration.DYNAMIC_PASSING_TIMES.value(),
            FlexibleServiceEnumeration.FIXED_HEADWAY_FREQUENCY.value(),
            FlexibleServiceEnumeration.FIXED_PASSING_TIMES.value(),
            FlexibleServiceEnumeration.NOT_FLEXIBLE.value());

    public IDFMLineNetexProfileValidator() {
        validTransportModeString = formatLegalEnumValues(validTransportModes);
        validTransportSubModeString = formatLegalEnumValues(validTransportSubModes);
    }

    private String formatLegalEnumValues(String... values) {
        return StringUtils.join(Arrays.asList(values).stream().map(e -> "'" + e + "'").collect(Collectors.toList()), ",");
    }

    @Override
    public void validate(Context context) throws Exception {
        XPathCompiler xpath = (XPathCompiler) context.get(NETEX_XPATH_COMPILER);

        XdmNode dom = (XdmNode) context.get(NETEX_DATA_DOM);

        @SuppressWarnings("unchecked")
        Set<Codespace> validCodespaces = (Set<Codespace>) context.get(NETEX_VALID_CODESPACES);
        ValidationData data = (ValidationData) context.get(VALIDATION_DATA);

        // StopPlaceRegistryIdValidator stopRegisterValidator = new StopPlaceRegistryIdValidator();

        @SuppressWarnings("unchecked")
        Map<IdVersion, List<String>> commonIds = (Map<IdVersion, List<String>>) context.get(NETEX_COMMON_FILE_IDENTIFICATORS);

        //TODO voir si on ajoute une verif sur la structure des id GeneralFrame et QuayRef
        List<IdVersion> localIdList = NetexIdExtractorHelper.collectEntityIdentificators(context, xpath, dom, new HashSet<>(Arrays.asList("CompositeFrame", "GeneralFrame")));
        Set<IdVersion> localIds = new HashSet<>(localIdList);
        List<IdVersion> localRefs = NetexIdExtractorHelper.collectEntityReferences(context, xpath, dom, new HashSet<>(Arrays.asList("QuayRef", "DayTypeRef", "TypeOfFrameRef", "LineRef")));

        for (IdVersion id : localIds) {
            data.getDataLocations().put(id.getId(), DataLocationHelper.findDataLocation(id));
        }

        verifyAcceptedCodespaces(context, xpath, dom, validCodespaces);
        verifyIdStructure(context, localIds, ID_STRUCTURE_REGEXP, validCodespaces);
        verifyNoDuplicatesWithCommonElements(context, localIds, commonIds);

        verifyNoDuplicatesAcrossLineFiles(context, localIdList,
                new HashSet<>(Arrays.asList("CompositeFrame", "GeneralFrame")));

        verifyUseOfVersionOnLocalElements(context, localIds);
        verifyUseOfVersionOnRefsToLocalElements(context, localIds, localRefs);
        verifyReferencesToCommonElements(context, localRefs, localIds, commonIds);
        verifyReferencesToCorrectEntityTypes(context, localRefs);
        verifyExternalRefs(context, localRefs, localIds, commonIds != null ? commonIds.keySet() : new HashSet<>());

        XdmValue compositeFrames = selectNodeSet("/PublicationDelivery/dataObjects/CompositeFrame", xpath, dom);
        if (compositeFrames.size() > 0) {
            // Using composite frames
            for (XdmItem compositeFrame : compositeFrames) {
                validateCompositeFrame(context, xpath, (XdmNode) compositeFrame);
            }
        }
    }

    private void validateCompositeFrame(Context context, XPathCompiler xpath, XdmNode dom) throws XPathExpressionException, SaxonApiException {

        XdmValue generalFrames = selectNodeSet("frames/GeneralFrame", xpath, dom);
        for (XdmItem generalFrame : generalFrames) {
            validateGeneralFrame(context, xpath, (XdmNode) generalFrame, null);
        }
    }

    public static class DefaultValidatorFactory extends NetexProfileValidatorFactory {
        @Override
        protected NetexProfileValidator create(Context context) throws ClassNotFoundException {
            NetexProfileValidator instance = (NetexProfileValidator) context.get(NAME);
            if (instance == null) {
                instance = new IDFMLineNetexProfileValidator();

                // Shitty, should use inversion of control pattern and dependency injection
                if ("true".equals(context.get("testng"))) {
                    instance.addExternalReferenceValidator(new DummyStopReferentialIdValidator());
                } else {
                    StopPlaceRegistryIdValidator stopRegistryValidator = (StopPlaceRegistryIdValidator) StopPlaceRegistryIdValidator.DefaultExternalReferenceValidatorFactory
                            .create(StopPlaceRegistryIdValidator.class.getName(), context);
                    instance.addExternalReferenceValidator(stopRegistryValidator);
                }

                context.put(NAME, instance);
            }
            return instance;
        }
    }

    static {
        NetexProfileValidatorFactory.factories.put(IDFMLineNetexProfileValidator.class.getName(),
                new IDFMLineNetexProfileValidator.DefaultValidatorFactory());
    }

    @Override
    public boolean isCommonFileValidator() {
        return false;
    }
}

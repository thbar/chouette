package mobi.chouette.exchange.netexprofile.importer.validation.idfm;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.importer.util.DataLocationHelper;
import mobi.chouette.exchange.netexprofile.importer.util.IdVersion;
import mobi.chouette.exchange.netexprofile.importer.validation.AbstractNetexProfileValidator;
import mobi.chouette.exchange.validation.report.ValidationReporter;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmNode;
import org.apache.commons.lang.StringUtils;
import org.rutebanken.netex.model.DataManagedObjectStructure;

import javax.xml.xpath.XPathExpressionException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j
public abstract class AbstractIDFMNetexProfileValidator extends AbstractNetexProfileValidator {

    public static final String PROFILE_IDFM_FR1 = "1.04:FR1-NETEX-2.0-z";

    public static final String EXPORT_PROFILE_ID = PROFILE_IDFM_FR1; // Update when new profile version is implemented

    public static String NSR_XMLNSURL = "http://ratp.mosaic.pro/mosaic";
    public static String NSR_XMLNS = "MOSAIC";

    public static final String PARTICIPANT_REF_CONTENT = "FR1";

    protected static final String ID_STRUCTURE_REGEXP = "^([A-Z]*):([A-Za-z]*):([0-9A-Za-zÀ-ÿ_\\-]*:LOC)$";

    public static final String _1_NETEX_REFERENCE_TO_ILLEGAL_ELEMENT = "1-NETEXPROFILE-ReferenceToIllegalElement";


    public Collection<String> getSupportedProfiles() {
        return Arrays.asList(new String[] {PROFILE_IDFM_FR1});
    }


    @Override
    public void initializeCheckPoints(Context context) {

        addCheckpoints(context, _1_NETEX_DUPLICATE_IDS_ACROSS_LINE_FILES, "E");
        addCheckpoints(context, _1_NETEX_IDFM_VERSION_NOT_ANY_ON_LOCAL_ELEMENTS, "E");
        addCheckpoints(context, _1_NETEX_MISSING_REFERENCE_VERSION_TO_LOCAL_ELEMENTS, "E");
        addCheckpoints(context, _1_NETEX_UNRESOLVED_REFERENCE_TO_COMMON_ELEMENTS, "E");
        addCheckpoints(context, _1_NETEX_INVALID_ID_STRUCTURE, "E");
        addCheckpoints(context, _1_NETEX_INVALID_ID_STRUCTURE_NAME, "E");
        addCheckpoints(context, _1_NETEX_UNAPPROVED_CODESPACE_DEFINED, "E");
        addCheckpoints(context, _1_NETEX_USE_OF_UNAPPROVED_CODESPACE, "E");
        addCheckpoints(context, _1_NETEX_REFERENCE_TO_ILLEGAL_ELEMENT, "E");
    }

    private void addCheckpoints(Context context, String checkpointName, String error) {
        ValidationReporter validationReporter = ValidationReporter.Factory.getInstance();
        validationReporter.addItemToValidationReport(context, checkpointName, error);
    }

    @Override
    public void addObjectReference(Context context, DataManagedObjectStructure object) {
    }

    protected void validateGeneralFrame(Context context, XPathCompiler xpath, XdmNode dom, String subLevelPath) throws XPathExpressionException, SaxonApiException {
        XdmNode subLevel = dom;
        if (subLevelPath != null) {
            subLevel = (XdmNode)selectNode(subLevelPath, xpath, dom);
        }
    }

    protected void verifyReferencesToCorrectEntityTypes(Context context, List<IdVersion> localRefs) {
        ValidationReporter validationReporter = ValidationReporter.Factory.getInstance();

        Map<String, Set<String>> allowedSubstitutions = new HashMap<>();

        Set<String> groupOfLinesRefSubstitutions = new HashSet<>();
        groupOfLinesRefSubstitutions.add("Network");
        groupOfLinesRefSubstitutions.add("GroupOfLines");
        allowedSubstitutions.put("RepresentedByGroupRef", groupOfLinesRefSubstitutions);

        Set<String> inverseRouteRefSubstitutions = new HashSet<>();
        inverseRouteRefSubstitutions.add("Route");
        allowedSubstitutions.put("InverseRouteRef", inverseRouteRefSubstitutions);

        Set<String> projectedPointRefSubstitutions = new HashSet<>();
        projectedPointRefSubstitutions.add("ScheduledStopPoint");
        projectedPointRefSubstitutions.add("RoutePoint");
        projectedPointRefSubstitutions.add("TimingPoint");
        allowedSubstitutions.put("ProjectToPointRef", projectedPointRefSubstitutions);
        allowedSubstitutions.put("ProjectedPointRef", projectedPointRefSubstitutions);
        allowedSubstitutions.put("ToPointRef", projectedPointRefSubstitutions);
        allowedSubstitutions.put("FromPointRef", projectedPointRefSubstitutions);
        allowedSubstitutions.put("StartPointRef", projectedPointRefSubstitutions);
        allowedSubstitutions.put("EndPointRef", projectedPointRefSubstitutions);

        Set<String> noticedObjectRefSubstitutions = new HashSet<>();
        noticedObjectRefSubstitutions.add("Line");
        noticedObjectRefSubstitutions.add("FlexibleLine");
        noticedObjectRefSubstitutions.add("ServiceJourney");
        noticedObjectRefSubstitutions.add("JourneyPattern");
        noticedObjectRefSubstitutions.add("ServiceJourneyPattern");
        noticedObjectRefSubstitutions.add("StopPointInJourneyPattern");
        noticedObjectRefSubstitutions.add("TimetabledPassingTime");
        allowedSubstitutions.put("NoticedObjectRef", noticedObjectRefSubstitutions);

        Set<String> toJourneyRefSubstitutions = new HashSet<>();
        toJourneyRefSubstitutions.add("ServiceJourney");
        allowedSubstitutions.put("ToJourneyRef", toJourneyRefSubstitutions);
        allowedSubstitutions.put("FromJourneyRef", toJourneyRefSubstitutions);

        Set<String> vehicleScheduleJourneyRefSubstitutions = new HashSet<>(toJourneyRefSubstitutions);
        vehicleScheduleJourneyRefSubstitutions.add("VehicleJourney");
        vehicleScheduleJourneyRefSubstitutions.add("DeadRun");
        allowedSubstitutions.put("VehicleJourneyRef", vehicleScheduleJourneyRefSubstitutions);

        Set<String> serviceJourneyPatternRefSubstitutions = new HashSet<>();
        serviceJourneyPatternRefSubstitutions.add("ServiceJourneyPattern");
        allowedSubstitutions.put("JourneyPatternRef", serviceJourneyPatternRefSubstitutions);

        Set<String> lineRefSubstitutions = new HashSet<>();
        lineRefSubstitutions.add("FlexibleLine");
        allowedSubstitutions.put("LineRef", lineRefSubstitutions);

        boolean foundErrors = false;

        for (IdVersion id : localRefs) {
            String referencingElement = id.getElementName();

            String[] idParts = StringUtils.split(id.getId(), ":");
            if (idParts.length == 4) {
                String referencedElement = idParts[1];

                // Dumb attemt first, must be of same type
                if (!(referencedElement + "Ref").equals(referencingElement) && !("Default" + referencedElement + "Ref").equals(referencingElement)) {
                    Set<String> possibleSubstitutions = allowedSubstitutions.get(referencingElement);
                    if (possibleSubstitutions != null) {
                        if (possibleSubstitutions.contains(referencedElement)) {
                            // Allowed substitution
                            continue;
                        }
                    }

                    foundErrors = true;
                    validationReporter.addCheckPointReportError(context, _1_NETEX_REFERENCE_TO_ILLEGAL_ELEMENT, null, DataLocationHelper.findDataLocation(id),
                            referencedElement, referencingElement);

                }
            } else {
                foundErrors = true;
                validationReporter.addCheckPointReportError(context, _1_NETEX_INVALID_ID_STRUCTURE, null, DataLocationHelper.findDataLocation(id),
                        referencingElement, referencingElement);

            }

        }
        if (!foundErrors) {
            validationReporter.reportSuccess(context, _1_NETEX_REFERENCE_TO_ILLEGAL_ELEMENT);
        }
    }

    protected void verifyUseOfVersionOnLocalElements(Context context, Set<IdVersion> localIds) {
        ValidationReporter validationReporter = ValidationReporter.Factory.getInstance();

        Set<IdVersion> nonVersionedAnyLocalIds = localIds.stream().filter(e -> !e.getVersion().equals("any")).collect(Collectors.toSet());
        if (nonVersionedAnyLocalIds.size() > 0) {
            for (IdVersion id : nonVersionedAnyLocalIds) {
                validationReporter.addCheckPointReportError(context, _1_NETEX_IDFM_VERSION_NOT_ANY_ON_LOCAL_ELEMENTS, null, DataLocationHelper.findDataLocation(id),
                        id.getId());
                if (log.isDebugEnabled()) {
                    log.debug("Id " + id + " in line file does not have version attribute set");
                }
            }
        } else {
            validationReporter.reportSuccess(context, _1_NETEX_IDFM_VERSION_NOT_ANY_ON_LOCAL_ELEMENTS);
        }
    }

}

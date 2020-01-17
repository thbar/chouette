package mobi.chouette.exchange.netexprofile.importer.validation.idfm;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.importer.util.DataLocationHelper;
import mobi.chouette.exchange.netexprofile.importer.util.IdVersion;
import mobi.chouette.exchange.netexprofile.importer.validation.NetexProfileValidator;
import mobi.chouette.exchange.netexprofile.util.NetexIdExtractorHelper;
import mobi.chouette.exchange.validation.ValidationData;
import mobi.chouette.model.Codespace;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IDFMCommonNetexProfileValidator extends AbstractIDFMNetexProfileValidator implements NetexProfileValidator {
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

        XdmValue generalFrames = selectNodeSet("/PublicationDelivery/dataObjects/GeneralFrame", xpath, dom);
        if (generalFrames.size() > 0) {
            // Using composite frames
            for (XdmItem generalFrame : generalFrames) {
                validateGeneralFrame(context, xpath, (XdmNode) generalFrame,  null);
            }
        }
    }

    @Override
    public boolean isCommonFileValidator() {
        return false;
    }
}

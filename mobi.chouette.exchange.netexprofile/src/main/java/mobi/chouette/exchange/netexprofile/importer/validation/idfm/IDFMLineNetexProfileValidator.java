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

    public IDFMLineNetexProfileValidator() {
    }

    @Override
    public void validate(Context context) throws Exception {
        XPathCompiler xpath = (XPathCompiler) context.get(NETEX_XPATH_COMPILER);

        XdmNode dom = (XdmNode) context.get(NETEX_DATA_DOM);

        Set<Codespace> validCodespaces = (Set<Codespace>) context.get(NETEX_VALID_CODESPACES);
        ValidationData data = (ValidationData) context.get(VALIDATION_DATA);

        List<IdVersion> localIdList = NetexIdExtractorHelper.collectEntityIdentificators(context, xpath, dom, new HashSet<>(Arrays.asList("CompositeFrame", "GeneralFrame")));
        Set<IdVersion> localIds = new HashSet<>(localIdList);
        List<IdVersion> localRefs = NetexIdExtractorHelper.collectEntityReferences(context, xpath, dom, new HashSet<>(Arrays.asList("QuayRef", "TypeOfFrameRef")));

        for (IdVersion id : localIds) {
            data.getDataLocations().put(id.getId(), DataLocationHelper.findDataLocation(id));
        }

        verifyAcceptedCodespaces(context, xpath, dom, validCodespaces);
        verifyIdStructure(context, localIds, ID_STRUCTURE_REGEXP, validCodespaces);

        verifyNoDuplicatesAcrossLineFiles(context, localIdList, null);

        verifyUseOfVersionOnLocalElements(context, localIds);
        verifyUseOfVersionOnRefsToLocalElements(context, localIds, localRefs);
        verifyReferencesToCorrectEntityTypes(context, localRefs);

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

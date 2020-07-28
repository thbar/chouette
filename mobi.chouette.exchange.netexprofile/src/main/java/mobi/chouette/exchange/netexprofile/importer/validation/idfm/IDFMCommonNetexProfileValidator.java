package mobi.chouette.exchange.netexprofile.importer.validation.idfm;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.importer.util.DataLocationHelper;
import mobi.chouette.exchange.netexprofile.importer.util.IdVersion;
import mobi.chouette.exchange.netexprofile.importer.validation.NetexProfileValidator;
import mobi.chouette.exchange.netexprofile.importer.validation.NetexProfileValidatorFactory;
import mobi.chouette.exchange.netexprofile.importer.validation.norway.BlockJourneyReferencesIgnorerer;
import mobi.chouette.exchange.netexprofile.importer.validation.norway.DummyStopReferentialIdValidator;
import mobi.chouette.exchange.netexprofile.importer.validation.norway.NorwayCommonNetexProfileValidator;
import mobi.chouette.exchange.netexprofile.importer.validation.norway.ServiceJourneyInterchangeIgnorer;
import mobi.chouette.exchange.netexprofile.importer.validation.norway.StopPlaceRegistryIdValidator;
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

    }


    public static class DefaultValidatorFactory extends NetexProfileValidatorFactory {
        @Override
        protected NetexProfileValidator create(Context context) throws ClassNotFoundException {
            NetexProfileValidator instance = (NetexProfileValidator) context.get(NAME);
            if (instance == null) {
                instance = new IDFMCommonNetexProfileValidator();

                context.put(NAME, instance);
            }
            return instance;
        }
    }

    static {
        NetexProfileValidatorFactory.factories.put(IDFMCommonNetexProfileValidator.class.getName(),
                new IDFMCommonNetexProfileValidator.DefaultValidatorFactory());
    }

    @Override
    public boolean isCommonFileValidator() {
        return false;
    }
}

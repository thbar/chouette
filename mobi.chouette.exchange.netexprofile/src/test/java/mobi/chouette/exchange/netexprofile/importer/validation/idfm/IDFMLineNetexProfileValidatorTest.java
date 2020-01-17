package mobi.chouette.exchange.netexprofile.importer.validation.idfm;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.importer.validation.NetexProfileValidator;
import mobi.chouette.exchange.netexprofile.importer.validation.norway.NorwayLineNetexProfileValidator;
import mobi.chouette.exchange.netexprofile.importer.validation.norway.ServiceJourneyInterchangeIgnorer;
import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.exchange.validation.ValidationData;
import mobi.chouette.exchange.validation.report.CheckPointReport;
import mobi.chouette.exchange.validation.report.ValidationReport;
import mobi.chouette.model.Codespace;
import net.sf.saxon.s9api.XdmNode;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.xpath.XPathFactoryConfigurationException;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static mobi.chouette.exchange.netexprofile.NetexTestUtils.createCodespace;
import static mobi.chouette.exchange.validation.report.ValidationReporter.RESULT.NOK;

public class IDFMLineNetexProfileValidatorTest {

    @Test
    public void testValidateSimpleFile() throws Exception {
        NetexXMLProcessingHelperFactory importer = new NetexXMLProcessingHelperFactory();

        Context context = createContext(importer);

        ValidationReport vr = new ValidationReport();
        context.put(Constant.VALIDATION_REPORT, vr);

        Set<Codespace> validCodespaces = new HashSet<>();
        Codespace validCodespace = createCodespace(1L, "SQYBUS", "http://ratp.mosaic.pro/sqybus");
        validCodespaces.add(validCodespace);
        context.put(Constant.NETEX_VALID_CODESPACES, validCodespaces);

        File file = new File("src/test/data/idfm/offre_C00535_448.xml");
        XdmNode dom = importer.parseFileToXdmNode(file, new HashSet<>());
        PublicationDeliveryStructure lineDeliveryStructure = importer.unmarshal(file, new HashSet<>());

        context.put(Constant.NETEX_DATA_JAVA, lineDeliveryStructure);
        context.put(Constant.NETEX_DATA_DOM, dom);

        NetexProfileValidator validator = createNetexProfileValidator();
        validator.initializeCheckPoints(context);
        validator.validate(context);
        boolean valid = true;
        for (CheckPointReport cp : vr.getCheckPoints()) {
            if (cp.getState() == NOK) {
                System.err.println(cp);
                valid = false;
            }
        }

        // TODO add more checks here
        Assert.assertTrue(valid);
    }

    private NetexProfileValidator createNetexProfileValidator() {
        NetexProfileValidator validator = new IDFMLineNetexProfileValidator();
        validator.addExternalReferenceValidator(new ServiceJourneyInterchangeIgnorer());
        return validator;
    }

    protected Context createContext(NetexXMLProcessingHelperFactory importer) throws XPathFactoryConfigurationException {
        Context context = new Context();
        context.put(Constant.IMPORTER, importer);

        ActionReport actionReport = new ActionReport();
        context.put(Constant.REPORT, actionReport);

        ValidationData data = new ValidationData();
        context.put(Constant.VALIDATION_DATA, data);

        context.put(Constant.NETEX_XPATH_COMPILER, importer.getXPathCompiler());
        return context;
    }

}
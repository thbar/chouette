package mobi.chouette.exchange.importer.updater;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

public class IdfmReflexParserTest {
    IdfmReflexParser idfmReflexParser;

    @Test
    public void testIDFM() throws Exception {
        //Config
        File file = new File("src/test/resources/netex/sqybus.xml");
        HashMap<String, Pair<String, String>> stringPairHashMap = idfmReflexParser.parseReflexResult(new FileInputStream(file));

    }
}

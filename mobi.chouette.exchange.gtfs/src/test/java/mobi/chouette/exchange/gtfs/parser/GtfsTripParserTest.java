package mobi.chouette.exchange.gtfs.parser;

import mobi.chouette.model.type.TransportSubModeNameEnum;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GtfsTripParserTest {

    public GtfsTripParser gtfsTripParser;

    @BeforeMethod
    public void beforeEachTest() {
        gtfsTripParser = new GtfsTripParser();
    }

    // TER : 6 chiffres commençant par 8
    @Test
    public void testTerCode() {
        // nominal
        String tripHeadSign = "888888";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.RegionalRail);

        // ne commence pas par 8
        tripHeadSign = "777777";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.RegionalRail);

        // pas 8 chiffres
        tripHeadSign = "46513";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.RegionalRail);

        // vide
        tripHeadSign = "";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.RegionalRail);

        // null
        tripHeadSign = null;
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.RegionalRail);
    }

    // TGV : [4 chiffres OU BIEN 4 chiffres "slash" 2 chiffres OU BIEN 4 chiffres "slash" 4 chiffres] LE TOUT devant commencer par 8, 5 ou 7
    @Test
    public void testTGVCode() {
        // nominal 4 chiffres par 8
        String tripHeadSign = "8888";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 2 chiffres par 8
        tripHeadSign = "8888/88";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 4 chiffres par 8
        tripHeadSign = "8888/8888";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres par 5
        tripHeadSign = "5555";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 2 chiffres par 5
        tripHeadSign = "5555/55";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 4 chiffres par 5
        tripHeadSign = "5555/5555";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres par 7
        tripHeadSign = "7777";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 2 chiffres par 7
        tripHeadSign = "7777/77";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 2 chiffres par 7
        tripHeadSign = "7777/7777";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // pas le bon nombre de chiffres
        tripHeadSign = "333";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // vide
        tripHeadSign = "";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // null
        tripHeadSign = null;
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);
    }

    // IC : 4 chiffres commençant par 4 (Bordeaux-Marseille) ou 3 (Bordeaux-Nantes) ou 6 chiffres commençant par 1.
    @Test
    public void testICCode() {
        // nominal 4 chiffres par 4
        String tripHeadSign = "4444";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

        // nominal 4 chiffres par 3
        tripHeadSign = "3333";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

        // nominal 6 chiffres par 1
        tripHeadSign = "111111";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

        // 4 chiffres pas par 4 ou 3
        tripHeadSign = "1111";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

        // 6 chiffres pas par 1
        tripHeadSign = "222222";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

        // vide
        tripHeadSign = "";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

        // null
        tripHeadSign = null;
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

    }


}
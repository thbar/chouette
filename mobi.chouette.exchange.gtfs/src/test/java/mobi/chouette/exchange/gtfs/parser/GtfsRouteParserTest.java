package mobi.chouette.exchange.gtfs.parser;

import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import mobi.chouette.exchange.gtfs.model.RouteTypeEnum;
import mobi.chouette.model.type.TransportSubModeNameEnum;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GtfsRouteParserTest {

    public GtfsRouteParser gtfsRouteParser;
    private GtfsRoute gtfsRoute;

    @BeforeMethod
    public void beforeEachTest() {
        gtfsRouteParser = new GtfsRouteParser();
        gtfsRoute = new GtfsRoute();
    }

    @Test
    public void testOtherCodeThanSncfReturnNullSubMode() {
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        Assert.assertNull(gtfsRouteParser.getSubModeFromRoute(gtfsRoute));
    }

    // TER : 6 chiffres commençant par 8
    @Test
    public void testTerCode() {
        // nominal
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE888888");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.RegionalRail);

        // ne commence pas par 8
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE777777");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.RegionalRail);

        // pas 8 chiffres
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE46513");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.RegionalRail);

        // vide
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("");
        Assert.assertNotEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.RegionalRail);

        // null
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId(null);
        Assert.assertNotEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.RegionalRail);
    }

    // TGV : [4 chiffres OU BIEN 4 chiffres "slash" 2 chiffres OU BIEN 4 chiffres "slash" 4 chiffres] LE TOUT devant commencer par 8, 5 ou 7
    @Test
    public void testTGVCode() {
        // nominal 4 chiffres par 8
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE8888");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 2 chiffres par 8
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE8888/88");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 4 chiffres par 8
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE8888/8888");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres par 5
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE5555");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 2 chiffres par 5
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE5555/55");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 4 chiffres par 5
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE5555/5555");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres par 7
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE7777");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 2 chiffres par 7
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE7777/77");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 2 chiffres par 7
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE7777/7777");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.LongDistance);

        // pas le bon nombre de chiffres
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE333");
        Assert.assertNotEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.LongDistance);

        // vide
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("");
        Assert.assertNotEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.LongDistance);

        // null
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId(null);
        Assert.assertNotEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.LongDistance);
    }

    // IC : 4 chiffres commençant par 4 (Bordeaux-Marseille) ou 3 (Bordeaux-Nantes) ou 6 chiffres commençant par 1.
    @Test
    public void testICCode() {
        // nominal 4 chiffres par 4
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE4444");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.InterregionalRail);

        // nominal 4 chiffres par 3
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE3333");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.InterregionalRail);

        // nominal 6 chiffres par 1
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE111111");
        Assert.assertEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.InterregionalRail);

        // 4 chiffres pas par 4 ou 3
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE1111");
        Assert.assertNotEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.InterregionalRail);

        // 6 chiffres pas par 1
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("OCE222222");
        Assert.assertNotEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.InterregionalRail);

        // vide
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId("");
        Assert.assertNotEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.InterregionalRail);

        // null
        gtfsRoute.setRouteType(RouteTypeEnum.Rail);
        gtfsRoute.setRouteId(null);
        Assert.assertNotEquals(gtfsRouteParser.getSubModeFromRoute(gtfsRoute), TransportSubModeNameEnum.InterregionalRail);

    }

}
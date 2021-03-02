package mobi.chouette.exchange.gtfs.parser;

import lombok.Getter;
import lombok.Setter;
import mobi.chouette.model.*;
import mobi.chouette.model.type.BookingMethodEnum;
import mobi.chouette.model.type.FlexibleServiceTypeEnum;
import mobi.chouette.model.type.TransportSubModeNameEnum;
import mobi.chouette.model.util.Referential;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GtfsTripParserTest {

    public GtfsTripParser gtfsTripParser;

    @Getter
    @Setter
    private String lineId;

    @BeforeMethod
    public void beforeEachTest() { gtfsTripParser = new GtfsTripParser(); }

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

    // Tests du TAD
    @Test
    public void testAllVJFlexibleInLine(){
        lineId = "TEST:Line:1";
        gtfsTripParser.setLineId(lineId);

        Line line = new Line();
        line.setObjectId("TEST:Line:1");

        Network network = new Network();
        network.setObjectId("TEST:Network:1");

        Company company = new Company();
        company.setObjectId("TEST:Operator:1");
        company.setUrl("testURL");
        company.setPhone("0123456789");

        network.setCompany(company);
        line.setNetwork(network);

        VehicleJourney vehicleJourney1 = new VehicleJourney();
        vehicleJourney1.setFlexibleService(true);

        VehicleJourney vehicleJourney2 = new VehicleJourney();
        vehicleJourney2.setFlexibleService(false);

        List<BookingMethodEnum> bookingMethodList = new ArrayList<>();
        bookingMethodList.add(BookingMethodEnum.callOffice);
        bookingMethodList.add(BookingMethodEnum.callDriver);

        BookingArrangement bookingArrangement = new BookingArrangement();
        bookingArrangement.setBookingContact(gtfsTripParser.getContactInformations(line));
        bookingArrangement.setBookingMethods(bookingMethodList);

        FlexibleServiceProperties flexibleServiceProperties = new FlexibleServiceProperties();
        flexibleServiceProperties.setFlexibleServiceType(FlexibleServiceTypeEnum.fixedPassingTimes);
        flexibleServiceProperties.setBookingArrangement(bookingArrangement);

        vehicleJourney1.setFlexibleServiceProperties(flexibleServiceProperties);
        vehicleJourney2.setFlexibleServiceProperties(flexibleServiceProperties);


        Referential referential = new Referential();

        Map<String, VehicleJourney> vehicleJourneyMap =  new HashMap<>();
        vehicleJourneyMap.put("TEST:VehicleJourney:1", vehicleJourney1);
        vehicleJourneyMap.put("TEST:VehicleJourney:2", vehicleJourney2);

        referential.setVehicleJourneys(vehicleJourneyMap);

        gtfsTripParser.setVJFlexibleService(line, vehicleJourney1, bookingMethodList);
        gtfsTripParser.setVJFlexibleService(line, vehicleJourney2, bookingMethodList);


        gtfsTripParser.setLineFlexible(referential, line);


        // On teste que les moyens de réservation et les informations pour réserver soient bien valorisées dans la course
        Assert.assertEquals(vehicleJourney1.getFlexibleServiceProperties().getBookingArrangement().getBookingMethods(), bookingMethodList);
        Assert.assertEquals(vehicleJourney1.getFlexibleServiceProperties().getBookingArrangement().getBookingContact().getUrl(), "testURL");
        Assert.assertEquals(vehicleJourney1.getFlexibleServiceProperties().getBookingArrangement().getBookingContact().getPhone(), "0123456789");

        // Dans le cas où toutes les courses d'une ligne sont TAD on vérifie que la ligne soit valorisée en TAD
        Assert.assertEquals(line.getObjectId(), "TEST:FlexibleLine:1");
        Assert.assertTrue(line.getFlexibleService());
    }

}
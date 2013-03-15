/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.certu.chouette.exchange.netex.exporter;


import fr.certu.chouette.model.neptune.Route;

import fr.certu.chouette.model.neptune.JourneyPattern;
import java.text.ParseException;
import javax.xml.xpath.XPathExpressionException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author marc
 */
@Test(groups = {"JourneyPattern"}, description = "Validate JourneyPattern export in NeTEx format")
public class JourneyPatternTest extends ChouetteModelTest {
    
    @Test(groups = {"ServiceFrame", "servicePatterns"}, description = "Validate presence of RouteRef with expected ref")
    public void verifyServicePatternId() throws XPathExpressionException, ParseException {
        for( Route route : line.getRoutes()) {
            for ( JourneyPattern journeyPattern : route.getJourneyPatterns()) {
                String xPathExpr = "boolean(//netex:ServiceFrame/netex:servicePatterns/netex:ServicePattern"+
                                                        "[@id = '"+
                                                        journeyPattern.objectIdPrefix()+
                                                        ":ServicePattern:"+
                                                        journeyPattern.objectIdSuffix()+
                                                    "'])";
                assertXPathTrue( xPathExpr);
                
            }
        }
    }
    
    @Test(groups = {"ServiceFrame", "servicePatterns"}, description = "Validate presence of RouteRef with expected ref")
    public void verifyServicePatternName() throws XPathExpressionException, ParseException {
        for( Route route : line.getRoutes()) {
            for ( JourneyPattern journeyPattern : route.getJourneyPatterns()) {
                String xPathExpr = "boolean(//netex:ServiceFrame/netex:servicePatterns/netex:ServicePattern/netex:Name/text()='"+
                                                        journeyPattern.getName()+
                                                    "')";
                assertXPathTrue( xPathExpr);
                
            }
        }
    }
    
    @Test(groups = {"TimetableFrame", "vehicleJourneys"}, description = "Validate presence of ServicePatternRef with expected ref")
    public void verifyServicePatternRef() throws XPathExpressionException, ParseException {
        for( Route route : line.getRoutes()) {
            for ( JourneyPattern journeyPattern : route.getJourneyPatterns()) {
                String xPathExpr = "count(//netex:TimetableFrame/netex:vehicleJourneys/netex:ServiceJourney/netex:ServicePatternRef"+
                                                        "[@ref = '"+
                                                        journeyPattern.objectIdPrefix()+
                                                        ":ServicePattern:"+
                                                        journeyPattern.objectIdSuffix()+
                                                    "'])";
                Assert.assertEquals( Integer.parseInt( 
                        xPath.evaluate( xPathExpr, 
                                        xmlDocument)), journeyPattern.getVehicleJourneys().size());

            }
        }
    }
}

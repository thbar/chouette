package mobi.chouette.exchange.stopplace;
import java.util.regex.*;
import org.testng.Assert;
import org.testng.annotations.Test;

public class stringQuayIdTest {

    @Test
    public int getQuayIdFromXdmItem(String xdmItem) {
       Pattern  p = Pattern.compile("Quay:([0-9]+)");
        Matcher m = p.matcher(xdmItem);
       String group = null;
        while (m.find()) {
            /*System.out.println(m.group());
            System.out.println(m.group(1));*/
            group = m.group(1);
        }
        return Integer.parseInt(group);
    }

    @Test
    public int getStopPlaceIdFromXdmItem(String xdmItem) {
        Pattern  p = Pattern.compile("monomodalStopPlace:([0-9]+)");
        Matcher m = p.matcher(xdmItem);
        String group = null;
        while (m.find()) {
            /*System.out.println(m.group());
            System.out.println(m.group(1));*/
            group = m.group(1);
        }
        return Integer.parseInt(group);
    }

    @Test
    public void testGetStopPlaceIdFromXdmItem() {

        int test = getStopPlaceIdFromXdmItem("FR::monomodalStopPlace:57130:FR1");
        Assert.assertEquals(test, 57130);

    }

    @Test
    public void testGetQuayIdFromXdmItem() {

        int test = getQuayIdFromXdmItem("id=\"FR::Quay:50095399:FR1\"");
        Assert.assertEquals(test, 50095399);

    }
}

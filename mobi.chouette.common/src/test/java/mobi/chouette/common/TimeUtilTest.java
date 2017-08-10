package mobi.chouette.common;

import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TimeUtilTest {

    @Test
    public void testSubtractSameDay() {
        Duration duration = TimeUtil.subtract(new LocalTime(12, 30, 0), new LocalTime(10, 5, 30));
        Assert.assertEquals(duration, Duration.standardSeconds(2 * 60 * 60 + 24 * 60 + 30));
    }

    @Test
    public void testSubtractThisDepartureOnNextDay() {
        Duration duration = TimeUtil.subtract(new LocalTime(00, 10, 0), new LocalTime(23, 50, 00));
        Assert.assertEquals(duration, Duration.standardSeconds(20 * 60));
    }
}

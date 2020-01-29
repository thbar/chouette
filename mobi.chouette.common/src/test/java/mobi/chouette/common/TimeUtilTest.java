package mobi.chouette.common;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeUtilTest {

    @Test
    public void testSubtractSameDay() {
        Duration duration = TimeUtil.subtract(LocalTime.of(12, 30, 0), LocalTime.of(10, 5, 30));
        Assert.assertEquals(duration, Duration.ofSeconds(2 * 60 * 60 + 24 * 60 + 30));
    }

    @Test
    public void testSubtractThisDepartureOnNextDay() {
        Duration duration = TimeUtil.subtract(LocalTime.of(00, 10, 0), LocalTime.of(23, 50, 00));
        Assert.assertEquals(duration, Duration.ofSeconds(20 * 60));
    }

    @Test
    public void javaLocalTimeToJodaLocalTimeTest() {
        java.time.LocalTime converted = java.time.LocalTime.of(1, 5, 10);
        Assert.assertEquals(converted.getHour(), 1);
        Assert.assertEquals(converted.getMinute(), 5);
        Assert.assertEquals(converted.getSecond(), 10);
    }

    @Test
    public void jodaLocalTimeToJavaLocalTimeTest() {
        java.time.LocalTime converted = TimeUtil.toLocalTimeFromJoda(LocalTime.of(1, 5, 10));
        Assert.assertEquals(converted.getHour(), 1);
        Assert.assertEquals(converted.getMinute(), 5);
        Assert.assertEquals(converted.getSecond(), 10);
    }

    @Test
    public void jodaLocalDateToJavaLocalDateTest() {
        LocalDate converted = TimeUtil.toLocalDateFromJoda(LocalDate.of(2017, 5, 10));
        Assert.assertEquals(converted.getYear(), 2017);
        Assert.assertEquals(converted.getMonthValue(), 5);
        Assert.assertEquals(converted.getDayOfMonth(), 10);
    }

    @Test
    public void javaLocalDateToJodaLocalDateTest() {
        java.time.LocalDate converted = TimeUtil.toJodaLocalDate(LocalDate.of(2017, 5, 10));
        Assert.assertEquals(converted.getYear(), 2017);
        Assert.assertEquals(converted.getMonthValue(), 5);
        Assert.assertEquals(converted.getDayOfMonth(), 10);
    }

    @Test
    public void javaDurationToJodaDurationTest(){
        Assert.assertEquals(TimeUtil.toJodaDuration(java.time.Duration.ofMinutes(60)), java.time.Duration.ofMinutes(60));
    }

    @Test
    public void localDateTimeToLocalDateIgnoresTime() {
        java.time.LocalDate converted = TimeUtil.toJodaLocalDateIgnoreTime(LocalDateTime.of(2018, 3, 20,6,23));

        Assert.assertEquals(converted.getYear(), 2018);
        Assert.assertEquals(converted.getMonthValue(), 3);
        Assert.assertEquals(converted.getDayOfMonth(), 20);
    }
}

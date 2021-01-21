package mobi.chouette.model.converter;

import org.joda.time.Duration;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Time;

@Converter
public class DurationToTimeConverter implements AttributeConverter<Duration, Time>{


    @Override
    public Time convertToDatabaseColumn(Duration duration) {
        return new Time(duration.getMillis());
    }

    @Override
    public Duration convertToEntityAttribute(Time time) {
        return Duration.millis(time.getTime());
    }
}



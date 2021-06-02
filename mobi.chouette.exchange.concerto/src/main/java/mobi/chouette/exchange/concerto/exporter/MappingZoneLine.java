package mobi.chouette.exchange.concerto.exporter;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@EqualsAndHashCode
public class MappingZoneLine {
    @Getter
    @Setter
    String zone;

    @Getter
    @Setter
    UUID lineUUID;
}

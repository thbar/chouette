package mobi.chouette.exchange.concerto.exporter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
public class MappingZoneLine {
    @Getter
    @Setter
    String zone;

    @Getter
    @Setter
    UUID lineUUID;
}

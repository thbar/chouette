package mobi.chouette.exchange.concerto.exporter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
public class MappingLineUUID {
    @Getter
    @Setter
    UUID uuid;

    @Getter
    @Setter
    Long LineId;

    @Getter
    @Setter
    String provider;
}

package mobi.chouette.exchange.concerto.exporter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
public class MappingLineZder {
    @Getter
    @Setter
    String zder;

    @Getter
    @Setter
    UUID lineUUID;
}

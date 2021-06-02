package mobi.chouette.exchange.concerto.exporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MapperLinesAndZone {
    private List<MappingZoneLine> mappingLineZderList;

    public MapperLinesAndZone(){
        mappingLineZderList = new ArrayList<>();
    }

    private void addMappingZoneLine(String zder, UUID lineUUID){
        if(zder == null) return;
        MappingZoneLine mappingLineZder = new MappingZoneLine(zder, lineUUID);
        if(!mappingLineZderList.contains(mappingLineZder)) {
            mappingLineZderList.add(mappingLineZder);
        }
    }

    public void addMappingZoneLines(String zder, UUID[] lineUUIDs){
        if(zder == null) return;
        Arrays.stream(lineUUIDs).forEach(uuid -> {
            addMappingZoneLine(zder, uuid);
        });
    }

    public UUID[] getLinesForZone(String zder){
        if(zder == null) return new UUID[0];
        List<UUID> collectUUIDs = mappingLineZderList.stream()
                .filter(mappingLineZder -> mappingLineZder.getZone().equals(zder))
                .map(mappingLineZder -> mappingLineZder.getLineUUID())
                .collect(Collectors.toList());

        UUID[] itemsArray = new UUID[collectUUIDs.size()];
        return collectUUIDs.toArray(itemsArray);
    }
}

package mobi.chouette.exchange.concerto.exporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MapperLinesAndZders {
    private List<MappingLineZder> mappingLineZderList;

    public MapperLinesAndZders(){
        mappingLineZderList = new ArrayList<>();
    }

    private void addMappingLineZder(String zder, UUID lineUUID){
        if(zder == null) return;
        MappingLineZder mappingLineZder = new MappingLineZder(zder, lineUUID);
        mappingLineZderList.add(mappingLineZder);
    }

    public void addMappingLineZders(String zder, UUID[] lineUUIDs){
        if(zder == null) return;
        Arrays.stream(lineUUIDs).forEach(uuid -> {
            addMappingLineZder(zder, uuid);
        });
    }

    public UUID[] getLinesForZder(String zder){
        if(zder == null) return new UUID[0];
        List<UUID> collectUUIDs = mappingLineZderList.stream()
                .filter(mappingLineZder -> mappingLineZder.getZder().equals(zder))
                .map(mappingLineZder -> mappingLineZder.getLineUUID())
                .collect(Collectors.toList());

        UUID[] itemsArray = new UUID[collectUUIDs.size()];
        return collectUUIDs.toArray(itemsArray);
    }
}

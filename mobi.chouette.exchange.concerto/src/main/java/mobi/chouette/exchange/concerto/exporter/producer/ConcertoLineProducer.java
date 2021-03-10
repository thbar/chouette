/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.concerto.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.concerto.exporter.MappingLineUUID;
import mobi.chouette.exchange.concerto.model.ConcertoLine;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporterInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static mobi.chouette.common.Constant.MAPPING_LINE_UUID;

/**
 * convert line to concerto
 */
@Log4j
public class ConcertoLineProducer extends AbstractProducer
{
    public ConcertoLineProducer(ConcertoExporterInterface exporter)
    {
      super(exporter);
    }


    public void save(Context context, List<ConcertoLine> concertoLines) {
        List<MappingLineUUID> mappingLineUUIDList = (List<MappingLineUUID>) context.get(MAPPING_LINE_UUID);
        Map<UUID, UUID> concertoLinesToDelete = new HashMap<>();
        for (ConcertoLine concertoLine : concertoLines) {
            for (ConcertoLine concertoLine1 : concertoLines) {

                if (!concertoLine.getUuid().equals(concertoLine1.getUuid()) &&
                        concertoLine.getObjectId().equals(concertoLine1.getObjectId()) &&
                        concertoLine.getDate().equals(concertoLine1.getDate()) &&
                        !concertoLinesToDelete.containsKey(concertoLine1.getUuid())) {

                    concertoLinesToDelete.put(concertoLine.getUuid(), concertoLine1.getUuid());
                }
            }
        }

        concertoLines.removeIf(concertoLine -> concertoLinesToDelete.containsValue(concertoLine.getUuid()));
        mappingLineUUIDList.removeIf(mappingLineUUID -> concertoLinesToDelete.containsValue(mappingLineUUID.getUuid()));

        context.put(MAPPING_LINE_UUID, mappingLineUUIDList);

        for (ConcertoLine concertoLine : concertoLines) {
            try {
                getExporter().getLineExporter().export(concertoLine);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

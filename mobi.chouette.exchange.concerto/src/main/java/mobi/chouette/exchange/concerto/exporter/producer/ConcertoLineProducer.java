/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.concerto.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.concerto.model.ConcertoLine;
import mobi.chouette.exchange.concerto.model.ConcertoObjectId;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporterInterface;
import mobi.chouette.model.Line;
import org.joda.time.LocalDate;

import java.util.UUID;

/**
 * convert line to concerto
 */
@Log4j
public class ConcertoLineProducer extends AbstractProducer
{
    private final String TYPE = "line";

    public ConcertoLineProducer(ConcertoExporterInterface exporter)
    {
      super(exporter);
    }

    private ConcertoLine line = new ConcertoLine();

    public UUID save(Line neptuneObject, LocalDate startDate, LocalDate endDate, ConcertoObjectId objectId){
        UUID uuid = UUID.randomUUID();
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1))
        {
            if(!save(neptuneObject, date, objectId, uuid)) ;
        }
        return uuid;
    }

    public boolean save(Line neptuneObject, LocalDate date, ConcertoObjectId objectId, UUID uuid)
    {
        // On ne traite que les lignes IDFM
        if(!neptuneObject.getCategoriesForLine().getName().equals("IDFM")){
            return false;
        }
        line.setType(TYPE);
        line.setUuid(uuid);
        line.setDate(date);
        line.setName(neptuneObject.getNumber());
        line.setObjectId(objectId);
        line.setAttributes("{}");
        line.setReferences("{}");
        line.setCollectedAlways(true);
        line.setCollectChildren(true);
        line.setCollectGeneralMessages(true);

        try
        {
            getExporter().getLineExporter().export(line);
        }
        catch (Exception e)
        {
             log.warn("export failed for line "+neptuneObject.getObjectId(),e);
             return false;
        }

        return true;
    }
}

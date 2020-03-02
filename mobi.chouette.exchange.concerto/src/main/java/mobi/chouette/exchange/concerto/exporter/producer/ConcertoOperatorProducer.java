/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.concerto.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.concerto.model.ConcertoObjectId;
import mobi.chouette.exchange.concerto.model.ConcertoOperator;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporterInterface;
import mobi.chouette.model.Operator;
import org.joda.time.LocalDate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Convert agency to operator
 */
@Log4j
public class ConcertoOperatorProducer extends AbstractProducer
{
   private final String TYPE = "operator";

   public ConcertoOperatorProducer(ConcertoExporterInterface exporter)
   {
      super(exporter);
   }

   private ConcertoOperator operator = new ConcertoOperator();

   public boolean save(LocalDate startDate, LocalDate endDate, List<Operator> operators){
      AtomicBoolean isTrue = new AtomicBoolean(true);

      operators.forEach(o -> {
         UUID uuid = UUID.randomUUID();
         for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1))
         {
            ConcertoObjectId concertoObjectId = new ConcertoObjectId();
            concertoObjectId.setStif(o.getStifValue());
            concertoObjectId.setHastus(o.getHastusValue());
            if(!save(uuid, date, o.getName(), concertoObjectId)) isTrue.set(false);
         }
      });

      return isTrue.get();
   }

   private boolean save(UUID uuid, LocalDate date, String name, ConcertoObjectId objectId)
   {
      operator.setType(TYPE);
      operator.setUuid(uuid);
      operator.setDate(date);
      operator.setObjectId(objectId);
      operator.setName(name);

      try
      {
         getExporter().getOperatorExporter().export(operator);
      }
      catch (Exception e)
      {
         log.error("fail to produce operator "+e.getClass().getName()+" "+e.getMessage());
         return false;
      }
      return true;
   }

}

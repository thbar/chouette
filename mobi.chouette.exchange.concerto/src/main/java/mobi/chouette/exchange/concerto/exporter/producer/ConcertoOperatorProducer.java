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
import mobi.chouette.model.Company;
import org.joda.time.LocalDate;

import java.util.UUID;

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

   public boolean save(Company neptuneObject, LocalDate startDate, LocalDate endDate, ConcertoObjectId objectId){
      boolean isTrue = true;
      for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1))
      {
         if(!save(neptuneObject, date, objectId)) isTrue = false;
      }
      return isTrue;
   }

   private boolean save(Company neptuneObject, LocalDate date, ConcertoObjectId objectId)
   {
      operator.setType(TYPE);
      operator.setUuid(UUID.randomUUID());
      operator.setDate(date);
      operator.setObjectId(objectId);

      String name = neptuneObject.getName();
      if (name.trim().isEmpty())
      {
         log.error("no name for " + neptuneObject.getObjectId());
         return false;
      }
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

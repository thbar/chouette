/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.concerto.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.concerto.model.ConcertoOperator;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporterInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Convert agency to operator
 */
@Log4j
public class ConcertoOperatorProducer extends AbstractProducer
{

   public ConcertoOperatorProducer(ConcertoExporterInterface exporter)
   {
      super(exporter);
   }

   public void save(List<ConcertoOperator> concertoOperators) {
      Map<UUID, UUID> concertoOperatorsToDelete = new HashMap<>();
      for (ConcertoOperator concertoOperator : concertoOperators) {
         for (ConcertoOperator concertoOperator1 : concertoOperators) {

            if (!concertoOperator.getUuid().equals(concertoOperator1.getUuid()) &&
                    concertoOperator.getObjectId().equals(concertoOperator1.getObjectId()) &&
                    concertoOperator.getDate().equals(concertoOperator1.getDate()) &&
                    !concertoOperatorsToDelete.containsKey(concertoOperator1.getUuid())) {

               concertoOperatorsToDelete.put(concertoOperator.getUuid(), concertoOperator1.getUuid());
            }
         }
      }

      concertoOperators.removeIf(concertoOperator -> concertoOperatorsToDelete.containsValue(concertoOperator.getUuid()));

      for (ConcertoOperator concertoOperator : concertoOperators) {
         try {
            getExporter().getOperatorExporter().export(concertoOperator);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

}

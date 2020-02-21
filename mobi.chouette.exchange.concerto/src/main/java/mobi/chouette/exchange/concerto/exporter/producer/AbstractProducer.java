package mobi.chouette.exchange.concerto.exporter.producer;

import lombok.Getter;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporterInterface;

public abstract class AbstractProducer
{

   @Getter
   private ConcertoExporterInterface exporter;

   public AbstractProducer(ConcertoExporterInterface exporter)
   {
      this.exporter = exporter;
   }

}

package mobi.chouette.exchange.concerto.exporter.producer;

import lombok.Getter;
import mobi.chouette.common.JSONUtil;
import mobi.chouette.exchange.concerto.model.ConcertoObjectId;
import mobi.chouette.exchange.concerto.model.exporter.ConcertoExporterInterface;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.xml.bind.JAXBException;

public abstract class AbstractProducer {

   @Getter
   private ConcertoExporterInterface exporter;

   public AbstractProducer(ConcertoExporterInterface exporter) {
      this.exporter = exporter;
   }

   public String getObjectIdConcerto(ConcertoObjectId concertoObjectId, String objectTypeConcerto) throws JAXBException, JSONException {
      String json = JSONUtil.toJSON(concertoObjectId);
      String objectId = (new JSONObject(json)).getString("root");
      objectId = objectId.replace("\\\"", "\"");
      objectId = objectId.replaceAll("\"\"", "\"");
      if (objectTypeConcerto != null) {
         objectId = objectId.replace("hastus", objectTypeConcerto);
      }
      return objectId;
   }


}

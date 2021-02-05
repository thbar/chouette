package mobi.chouette.exchange.gtfs.exporter.producer;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import lombok.Getter;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.exchange.gtfs.parameters.IdFormat;
import mobi.chouette.exchange.gtfs.parameters.IdParameters;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractProducer
{

   @Getter
   private GtfsExporterInterface exporter;

   public AbstractProducer(GtfsExporterInterface exporter)
   {
      this.exporter = exporter;
   }

   static protected String toGtfsId(String neptuneId, String prefix, boolean keepOriginal)
   {
      // @todo OKINA revoir ce foutu truc de keepOriginal
      /*if(false && keepOriginal) {
    	  return neptuneId;
      } else */{
    	  String[] tokens = neptuneId.split(":");
    	  if(tokens.length == 1)
    	     return tokens[0];
	      else if (tokens[0].equalsIgnoreCase(prefix) || tokens[0].equalsIgnoreCase("MOSAIC"))
	         return tokens[2];
	      else
	         // pour idfm car nos prefix sont MOSAIC et absolument pas SQYBUS ou autre
             // sinon return tokens[0] + "." + tokens[2];
             return tokens[2];
	      }
   }

   static protected boolean isEmpty(String s)
   {
      return s == null || s.trim().isEmpty();
   }

   static protected boolean isEmpty(Collection<? extends Object> s)
   {
      return s == null || s.isEmpty();
   }

   static protected String getValue(String s)
   {
      if (isEmpty(s))
         return null;
      else
         return s;

   }

   static protected Color getColor(String s)
   {
      if (isEmpty(s))
         return null;
      else
         return new Color(Integer.parseInt(s, 16));
   }

   static protected URL getUrl(String s)
   {
      if (isEmpty(s))
         return null;
      else
         try
         {
            URL result = new URL(s);
            String protocol = result.getProtocol();
            if (!(protocol.equals("http") || protocol.equals("https")))
            {
               throw new MalformedURLException();
            }
            return result;
         }
         catch (MalformedURLException e)
         {
            // TODO: manage exception
            return null;
         }
   }
   
   static boolean isTrue(Boolean value)
   {
	   return value != null && value;
   }

   /**
    * Generate a custom Id from an original String and parameters.
    * Trident format : "prefix" :Line:xxx "Suffix". e.g : MOBIITI:Line:1426SUF
    * Source format : "prefix" xxx "Suffix". e.g : MOBIITI1426SUF
    * @param originalId
    * @param idParams
    * @return
    */
   public String generateCustomRouteId(String originalId, IdParameters idParams){
      String idPrefix = idParams.getLineIdPrefix();
      String idSuffix= idParams.getIdSuffix();
      StringBuilder sb = new StringBuilder();

      if (StringUtils.isNotEmpty(idPrefix))
         sb.append(idPrefix);

      if (IdFormat.TRIDENT.equals(idParams.getIdFormat()) && StringUtils.isNotEmpty(idPrefix)){
         sb.append(":Line:");
      }
      sb.append(originalId);

      if (StringUtils.isNotEmpty(idSuffix)){
         sb.append(idSuffix);
      }


      return sb.toString();
   }


}

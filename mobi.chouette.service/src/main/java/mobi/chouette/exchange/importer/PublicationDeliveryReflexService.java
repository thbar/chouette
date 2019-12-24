package mobi.chouette.exchange.importer;

import org.rutebanken.netex.model.PublicationDeliveryStructure;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PublicationDeliveryReflexService {

    public InputStream getAll(String urlTarget) throws MalformedURLException, IOException {
        PublicationDeliveryStructure var9;
        URL url = new URL(urlTarget);
        ////.stop.place.register.update.url?providerCode=
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            int responseCode = connection.getResponseCode();
            inputStream = connection.getInputStream();
        } catch (Exception var13) {
            throw new IOException("Error posting XML to " + url.toString(), var13);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }

        }
        return inputStream;
    }

}

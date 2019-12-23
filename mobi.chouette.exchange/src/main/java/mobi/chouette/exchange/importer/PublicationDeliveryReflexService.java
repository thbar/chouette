package mobi.chouette.exchange.importer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PublicationDeliveryReflexService {

    public static byte[] getAll(String urlTarget) throws MalformedURLException, IOException {
        URL url = new URL(urlTarget);
        InputStream input = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozaic");
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            input = connection.getInputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > -1 ) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
        } catch (Exception var13) {
            throw new IOException("Error posting XML to " + url.toString(), var13);
        }
        return baos.toByteArray();
    }

}

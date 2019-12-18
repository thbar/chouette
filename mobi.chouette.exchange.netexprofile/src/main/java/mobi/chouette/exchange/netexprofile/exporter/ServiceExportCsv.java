package mobi.chouette.exchange.netexprofile.exporter;

import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.netexprofile.util.CSVUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

@Stateless
public class ServiceExportCsv {

    @EJB
    protected LineDAO lineDAO;

    @EJB
    protected StopAreaDAO stopAreaDAO;

    public File exportConcerto(String filePath) throws IOException {

        File file = new File(filePath);

        if(file.exists()) {
            file.delete();
        }

        if(file.createNewFile()) {

            FileWriter writer = new FileWriter(file.getPath());

            CSVUtils.writeLine(writer, Arrays.asList("aaa", "bbb", "ccc"), ',');

            writer.flush();
            writer.close();
        }
        else {
            throw new IOException("Le fichier CSV n'a pas pu être créé sur l'emplacement " + filePath);
        }

        return file;
    }

}

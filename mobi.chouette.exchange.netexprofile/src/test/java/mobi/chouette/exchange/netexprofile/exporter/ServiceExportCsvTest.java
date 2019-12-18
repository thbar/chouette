package mobi.chouette.exchange.netexprofile.exporter;

import com.vividsolutions.jts.util.Assert;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.netexprofile.util.CSVUtils;
import mobi.chouette.model.Line;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import org.joda.time.LocalDateTime;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ServiceExportCsvTest {

    private final static String SERVER_PATH_STORAGE = "/tmp";

    ServiceExportCsv serviceExportCsv = new ServiceExportCsv();

    @Mock
    LineDAO lineDAO;

    @Mock
    StopAreaDAO stopAreaDAO;

    @BeforeClass
    public void init() {
        MockitoAnnotations.initMocks(this);
        serviceExportCsv.lineDAO = lineDAO;
        serviceExportCsv.stopAreaDAO = stopAreaDAO;

        File tmpFolder = new File(SERVER_PATH_STORAGE);

        if(!tmpFolder.exists()) {
            tmpFolder.mkdir();
        }
    }

    @Test
    public void testExportConcerto() throws IOException {

        //Mockito.when(lineDAO.findAll()).thenReturn(prepareLines());

        File csvFile = serviceExportCsv.exportConcerto(SERVER_PATH_STORAGE + "/concerto.csv");

        Assert.equals(CSVUtils.readFileLineByLine(csvFile.getPath()), "aaa,bbb,ccc\n");
    }

    private List<Line> prepareLines() {

        Line line = new Line();
        line.setId(1L);
        line.setName("Test-line");

        List<Line> lines = new ArrayList<>();
        lines.add(line);

        return lines;
    }

    private List<StopArea> prepareQuays() {

        StopArea stopAreaCommercial = new StopArea();
        stopAreaCommercial.setId(1L);
        stopAreaCommercial.setAreaType(ChouetteAreaEnum.CommercialStopPoint);
        stopAreaCommercial.setCreationTime(new LocalDateTime());
        stopAreaCommercial.setName("Test quay 1");
        stopAreaCommercial.setObjectId("MOSAIC:StopPlace:1");

        StopArea stopAreaBoarding = new StopArea();
        stopAreaBoarding.setId(2L);
        stopAreaBoarding.setAreaType(ChouetteAreaEnum.BoardingPosition);
        stopAreaBoarding.setCreationTime(new LocalDateTime());
        stopAreaBoarding.setName("Test quay 1");
        stopAreaBoarding.setObjectId("MOSAIC:Quay:1");
        stopAreaBoarding.setParent(stopAreaCommercial);

        List<StopArea> stopAreaList = new ArrayList<>();
        stopAreaList.add(stopAreaCommercial);
        stopAreaList.add(stopAreaBoarding);

        return stopAreaList;
    }

}

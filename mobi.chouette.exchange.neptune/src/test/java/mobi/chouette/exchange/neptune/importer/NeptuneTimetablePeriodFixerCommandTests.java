package mobi.chouette.exchange.neptune.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.neptune.JobDataTest;
import mobi.chouette.exchange.neptune.jaxb.JaxbNeptuneFileConverter;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.trident.schema.trident.ChouettePTNetworkType;
import org.trident.schema.trident.TimetableType;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static mobi.chouette.common.Constant.JOB_DATA;

@Log4j
public class NeptuneTimetablePeriodFixerCommandTests {



    protected static InitialContext initialContext;

    private static String rootTestFolder = "src/test/data/EIS923TestData/test1/";

    private static String test1GeneratedPath = rootTestFolder+"input/B-generated.xml";
    private static String test1CopyPath = rootTestFolder+"B-calFixed.xml";
    private static String test1SrcCommandPath = rootTestFolder+"input/B.xml";
    private static String test1SrcPath = rootTestFolder+"B.xml";


    protected void init() {
        Locale.setDefault(Locale.ENGLISH);
        if (initialContext == null) {
            try {
                initialContext = new InitialContext();
            } catch (NamingException e) {
                e.printStackTrace();
            }


        }
    }
    protected Context initImportContext(String pathName) {
        init();

        Context context = new Context();
        JobDataTest jobData = new JobDataTest();
        jobData.setPathName(pathName);
        context.put(JOB_DATA,jobData);
        return context;
    }

    private void cleanFile(String pathToFile){
        Path path = Paths.get(pathToFile);
        File file = new File(path.toString());
        if (file.exists()){
            file.delete();
        }
    }

    private void cleanTest1Files(){
        cleanFile(test1CopyPath);
        cleanFile(test1GeneratedPath);
        cleanFile(test1SrcCommandPath);
    }

    @Test(groups = { "CommandChecked" }, description = "test if timetable periods are correctly fixed")
    public void test1_verifyTimetablePeriodCorrection() throws Exception {
        cleanTest1Files();
        FileUtils.copyFile(new File(Paths.get(test1SrcPath).toString()),new File(Paths.get(test1SrcCommandPath).toString()));

        NeptuneTimeTablePeriodFixerCommand command = (NeptuneTimeTablePeriodFixerCommand) CommandFactory.create(initialContext,NeptuneTimeTablePeriodFixerCommand.class.getName());
        Context context = initImportContext(Paths.get(rootTestFolder).toString());
        boolean returnCode=command.execute(context);

        Assert.assertEquals(returnCode, true, "Incorrect Return code");

        File generatedFile = new File(test1CopyPath.toString());

        Assert.assertEquals(generatedFile.exists(), true, "No generated file");


        try {
            JaxbNeptuneFileConverter converter = JaxbNeptuneFileConverter.getInstance();
            Optional<ChouettePTNetworkType> chouetteNetworkOpt = converter.read(Paths.get(test1CopyPath));

            ChouettePTNetworkType network = chouetteNetworkOpt.get();
                                    
            checkTimetablePeriods(network.getTimetable());

        }catch(Exception e){
            log.error("Can't open generated file");
            throw e;
        }
        cleanTest1Files();
    }

    private void checkTimetablePeriods(List<TimetableType> timetableList){

        Assert.assertEquals(timetableList.size(), 47, "Number of timetables must not be changed");

        DateTime now = new DateTime();
        int currentYear = now.getYear();


        for (TimetableType timetable : timetableList){

            if (timetable.getObjectId().equals("SAMPLE_NET:TimeTable:148")){
                Assert.assertEquals(timetable.getPeriod().size(), 1, "wrong nb of periods");
                Assert.assertEquals(timetable.getPeriod().get(0).getStartOfPeriod().toString(), "2020-11-02", "wrong start period");
                Assert.assertEquals(timetable.getPeriod().get(0).getEndOfPeriod().toString(), "2021-07-05", "wrong end period");
            }

            if (timetable.getObjectId().equals("SAMPLE_NET:TimeTable:149")){
                Assert.assertEquals(timetable.getPeriod().size(), 1, "wrong nb of periods");
                Assert.assertEquals(timetable.getPeriod().get(0).getStartOfPeriod().toString(), "2020-11-04", "wrong start period");
                Assert.assertEquals(timetable.getPeriod().get(0).getEndOfPeriod().toString(), "2021-06-30", "wrong end period");
            }

            if (timetable.getObjectId().equals("SAMPLE_NET:TimeTable:150")){
                Assert.assertEquals(timetable.getPeriod().size(), 31, "wrong nb of periods");
            }

            if (timetable.getObjectId().equals("SAMPLE_NET:TimeTable:151")){
                Assert.assertEquals(timetable.getPeriod().size(), 1, "wrong nb of periods");
                Assert.assertEquals(timetable.getPeriod().get(0).getStartOfPeriod().toString(), "2020-11-04", "wrong start period");
                Assert.assertEquals(timetable.getPeriod().get(0).getEndOfPeriod().toString(), "2021-07-03", "wrong end period");
            }

            if (timetable.getObjectId().equals("SAMPLE_NET:TimeTable:152")){
                Assert.assertEquals(timetable.getPeriod().size(), 56, "wrong nb of periods");
            }

            if (timetable.getObjectId().equals("SAMPLE_NET:TimeTable:153")){
                Assert.assertEquals(timetable.getPeriod().size(), 58, "wrong nb of periods");
            }

            if (timetable.getObjectId().equals("SAMPLE_NET:TimeTable:154")){
                Assert.assertEquals(timetable.getPeriod().size(), 29, "wrong nb of periods");
            }
            if (timetable.getObjectId().equals("SAMPLE_NET:TimeTable:155")){
                Assert.assertEquals(timetable.getPeriod().size(), 28, "wrong nb of periods");
            }

            if (timetable.getObjectId().equals("SAMPLE_NET:TimeTable:156")){
                Assert.assertEquals(timetable.getPeriod().size(), 1, "wrong nb of periods");
                Assert.assertEquals(timetable.getPeriod().get(0).getStartOfPeriod().toString(), "2020-11-06", "wrong start period");
                Assert.assertEquals(timetable.getPeriod().get(0).getEndOfPeriod().toString(), "2021-07-02", "wrong end period");
            }
        }



    }



}
package mobi.chouette.exchange.neptune.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.neptune.Constant;
import mobi.chouette.exchange.neptune.jaxb.JaxbNeptuneFileConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;
import mobi.chouette.model.util.Referential;
import org.trident.schema.trident.ChouettePTNetworkType;
import org.trident.schema.trident.PeriodType;
import org.trident.schema.trident.TimetableType;

import javax.naming.InitialContext;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Log4j
public class NeptuneTimeTablePeriodFixerCommand implements Command, Constant {

    public static final String COMMAND = "NeptuneCalendarPeriodFixerCommand";
    private boolean hasTimeTableBeenModified = false;


    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;


        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            context.put(REFERENTIAL, new Referential());
            JobData jobData = (JobData) context.get(JOB_DATA);
            Path path = Paths.get(jobData.getPathName(), INPUT);

            List<Path> filesToProcess = FileUtil.listFiles(path, "*.xml", "*metadata*");
            filesToProcess.forEach(this::fixTimeTablePeriodOnFile);

            result = SUCCESS;

        } catch (Exception e) {
            log.error(e, e);
            result = ERROR;
            throw e;
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }
        return result;
    }


    /**
     * Fix all calendar dates in the file passed as parameter
     * @param path
     *          path to the file that must be checked
     */
    private void fixTimeTablePeriodOnFile(Path path){
        log.info("Processing file:"+path.toAbsolutePath());
        try {
            JaxbNeptuneFileConverter converter = JaxbNeptuneFileConverter.getInstance();
            Optional<ChouettePTNetworkType> chouetteNetworkOpt = converter.read(path);

            if (!chouetteNetworkOpt.isPresent()){
                log.error("Unable to read unmarshalled file");
                return ;
            }

            ChouettePTNetworkType chouetteNetwork = chouetteNetworkOpt.get();
            chouetteNetwork.getTimetable().forEach(this::fixPeriodsOnTimetable);

            if(hasTimeTableBeenModified){
                File originalFile = new File(path.toAbsolutePath().toString());
                File directory = originalFile.getParentFile();
                String originalFileName = FilenameUtils.removeExtension(originalFile.getName());
                String newFileName = originalFileName+"-generated.xml";
                File outPutFile = new File(directory,newFileName);
                converter.write(chouetteNetwork,outPutFile);
                log.info(outPutFile.getName()+" created with fixed timetables");

                originalFile.delete();

                //a backup copy of the generated file is stored in the job's root directory
                File jobRootDirectory = directory.getParentFile();
                File backupCopy = new File(jobRootDirectory,newFileName);
                FileUtils.copyFile(outPutFile,backupCopy);
            }

        } catch (Exception e) {
            log.error("Error while processing file ");
            log.error(e);
        }
    }

    /**
     * Fix wrong timeTable periods
     * (wrong period : start date = end date)
     *
     * @param timetable
     *          timetable on which periods must be corrected
     */
    private void fixPeriodsOnTimetable(TimetableType timetable){
        removeWrongPeriods(timetable);
        addNewPeriod(timetable);
    }


    /**
     * Add a default period to the timetable if no one is existing.
     * Default period : xxxx-01-01 to xxxx-12-31 (with xxxx : current year)
     *
     * @param timetable
     *      timetable on which the period must be created
     */
    private void addNewPeriod(TimetableType timetable){

        //at least one valid period is existing. No need to create a default period
        if (timetable.getPeriod().size()>0)
            return;

        DateTime now = new DateTime();
        int currentYear = now.getYear();

        LocalDate startDate = LocalDate.of(currentYear, 1, 1);
        LocalDate endDate = LocalDate.of(currentYear, 12, 31);

        try {
            XMLGregorianCalendar startCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(startDate.toString());
            XMLGregorianCalendar endCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(endDate.toString());
            PeriodType newPeriod = new PeriodType();
            newPeriod.setStartOfPeriod(startCalendar);
            newPeriod.setEndOfPeriod(endCalendar);
            timetable.getPeriod().add(newPeriod);
            hasTimeTableBeenModified = true;
            log.info("Timetable:" + timetable.getObjectId() + " has been reset with period:" + newPeriod.getStartOfPeriod() + " to :"+newPeriod.getEndOfPeriod());


        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

    }

    /**
     * Removes all wrong periods in the timeTable
     * (wrong period : start date = end date)
     *
     * @param timetable
     *      timetable on which wrong periods must be removed
     */
    private void removeWrongPeriods(TimetableType timetable){
        timetable.getPeriod().removeIf(period-> period.getStartOfPeriod().equals(period.getEndOfPeriod()));
    }


    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new NeptuneTimeTablePeriodFixerCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(NeptuneTimeTablePeriodFixerCommand.class.getName(), new DefaultCommandFactory());
    }

}
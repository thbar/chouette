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
import mobi.chouette.model.Line;
import mobi.chouette.model.Period;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;
import mobi.chouette.model.util.Referential;
import org.trident.schema.trident.ChouettePTNetworkType;
import org.trident.schema.trident.DayTypeType;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Log4j
public class NeptuneTimeTablePeriodFixerCommand implements Command, Constant {

    public static final String COMMAND = "NeptuneTimeTablePeriodFixerCommand";
    private boolean hasTimeTableBeenModified = false;
    private DatatypeFactory dtFactory;
    private List<TimetableType> newTimeTables = new ArrayList<>();
    private int timetableSuffixNumber = 0;


    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;


        Monitor monitor = MonitorFactory.start(COMMAND);


        try {
            context.put(REFERENTIAL, new Referential());
            JobData jobData = (JobData) context.get(JOB_DATA);
            dtFactory = DatatypeFactory.newInstance();
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
            newTimeTables.clear();
            JaxbNeptuneFileConverter converter = JaxbNeptuneFileConverter.getInstance();
            Optional<ChouettePTNetworkType> chouetteNetworkOpt = converter.read(path);

            if (!chouetteNetworkOpt.isPresent()){
                log.error("Unable to read unmarshalled file");
                return ;
            }

            ChouettePTNetworkType chouetteNetwork = chouetteNetworkOpt.get();
            chouetteNetwork.getTimetable().forEach(this::fixPeriodsOnTimetable);

            chouetteNetwork.getTimetable().addAll(newTimeTables);

            if(hasTimeTableBeenModified){
                File originalFile = new File(path.toAbsolutePath().toString());
                File directory = originalFile.getParentFile();
                String originalFileName = FilenameUtils.removeExtension(originalFile.getName());
                originalFile.delete();

                File outPutFile = new File(directory,originalFile.getName());
                converter.write(chouetteNetwork,outPutFile);
                log.info(outPutFile.getName()+" created with fixed timetables");

                //a backup copy of the generated file is stored in the job's root directory
                File jobRootDirectory = directory.getParentFile();
                String backupName = originalFileName+"-calFixed.xml";
                File backupCopy = new File(jobRootDirectory,backupName);
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
        List<PeriodType> originalPeriodList = timetable.getPeriod();
        if (originalPeriodList.size() == 0)
            return;

        List<PeriodType> wrongPeriods = originalPeriodList.stream()
                .filter(period -> period.getStartOfPeriod().equals(period.getEndOfPeriod()))
                .collect(Collectors.toList());


        XMLGregorianComparator calendarComp = new XMLGregorianComparator();

        if (wrongPeriods.size() == originalPeriodList.size()){
            //This means all period of the time table are wrong
            List<XMLGregorianCalendar> orderedStartDateList = originalPeriodList.stream()
                                                                                .map(PeriodType::getStartOfPeriod)
                                                                                .sorted(calendarComp)
                                                                                .collect(Collectors.toList());


            List<XMLGregorianCalendar> orderedEndDateList = originalPeriodList.stream()
                                                                                .map(PeriodType::getEndOfPeriod)
                                                                                .sorted(calendarComp)
                                                                                .collect(Collectors.toList());

            XMLGregorianCalendar newStartDate = orderedStartDateList.get(0);
            XMLGregorianCalendar newEndDate = orderedEndDateList.get(orderedEndDateList.size()-1);

            if (newStartDate.equals(newEndDate)){
                //If startDate = endDate : add of 1 day to the end
                newEndDate.add(dtFactory.newDuration(86400000));
            }

            timetable.getPeriod().clear();


            //creation of a lonely period with start = oldest date of all periods, and end = newest date of all periods
            PeriodType newPeriod = new PeriodType();
            newPeriod.setStartOfPeriod(newStartDate);
            newPeriod.setEndOfPeriod(newEndDate);
            originalPeriodList.add(newPeriod);
            hasTimeTableBeenModified = true;
            log.info("Timetable:"+timetable.getObjectId()+ " ,new period has been created. From:"+newPeriod.getStartOfPeriod()+" to:"+newPeriod.getEndOfPeriod());

        }else if (wrongPeriods.size() > 0){
            //This some periods are good, but some are wrong.
            //wrong periods must be removed and a new calendar must be created to handle them

            wrongPeriods.stream().forEach(wrongPeriod->{
                originalPeriodList.remove(wrongPeriod);
                createNewTimeTable(timetable,wrongPeriod);
            });
            hasTimeTableBeenModified = true;
        }
    }

    public class XMLGregorianComparator implements Comparator<XMLGregorianCalendar> {

        @Override
        public int compare(XMLGregorianCalendar o1, XMLGregorianCalendar o2) {
            return o1.toGregorianCalendar().compareTo(o2.toGregorianCalendar());
        }
    }

    private void createNewTimeTable(TimetableType srcTimetable,PeriodType wrongPeriod){
        TimetableType newTimetable = new TimetableType();
        newTimetable.setObjectId(srcTimetable.getObjectId() + "-" + timetableSuffixNumber);
        timetableSuffixNumber++;
        newTimetable.setObjectVersion(srcTimetable.getObjectVersion());
        newTimetable.setVersion(srcTimetable.getVersion());
        newTimetable.setCreationTime(srcTimetable.getCreationTime());
        newTimetable.getVehicleJourneyId().addAll(srcTimetable.getVehicleJourneyId());
        XMLGregorianCalendar startDate = wrongPeriod.getStartOfPeriod();
        XMLGregorianCalendar endDate = wrongPeriod.getEndOfPeriod();
        newTimetable.getDayType().add(DayTypeType.fromValue(getDayOfWeek(startDate)));

        endDate.add(dtFactory.newDuration(86400000));
        PeriodType newPeriod = new PeriodType();
        newPeriod.setStartOfPeriod(startDate);
        newPeriod.setEndOfPeriod(endDate);
        newTimetable.getPeriod().add(newPeriod);

        newTimeTables.add(newTimetable);
    }

    private String getDayOfWeek(XMLGregorianCalendar xmlCal){

        GregorianCalendar cal = xmlCal.toGregorianCalendar();
        int i = cal.get(Calendar.DAY_OF_WEEK);
        if(i == 2){
            return "Monday";
        } else if (i==3){
            return "Tuesday";
        } else if (i==4){
            return "Wednesday";
        } else if (i==5){
            return "Thursday";
        } else if (i==6){
            return "Friday";
        } else if (i==7){
            return "Saturday";
        } else if (i==1){
            return"Sunday";
        }
        throw new RuntimeException("Unable to determinate calendar day of week for date:" + xmlCal.toString());
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


        XMLGregorianCalendar startCalendar = dtFactory.newXMLGregorianCalendar(startDate.toString());
        XMLGregorianCalendar endCalendar =dtFactory.newXMLGregorianCalendar(endDate.toString());
        PeriodType newPeriod = new PeriodType();
        newPeriod.setStartOfPeriod(startCalendar);
        newPeriod.setEndOfPeriod(endCalendar);
        timetable.getPeriod().add(newPeriod);
        hasTimeTableBeenModified = true;
        log.info("Timetable:" + timetable.getObjectId() + " has been reset with period:" + newPeriod.getStartOfPeriod() + " to :"+newPeriod.getEndOfPeriod());

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
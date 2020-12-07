package mobi.chouette.service;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.ContenerChecker;
import mobi.chouette.common.Pair;
import mobi.chouette.common.file.FileServiceException;
import mobi.chouette.common.file.FileStore;
import mobi.chouette.common.file.LocalFileStore;
import mobi.chouette.model.iev.Job;
import mobi.chouette.model.iev.Link;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDateTime;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static mobi.chouette.common.Constant.*;
import static mobi.chouette.common.PropertyNames.FILE_STORE_IMPLEMENTATION;
import static mobi.chouette.service.CachingCloudFileStore.BEAN_NAME;

/**
 * Store permanent files in Cloud Storage. Use local file system for caching.
 */
@Singleton(name = BEAN_NAME)
@Named
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Startup
@DependsOn(JobServiceManager.BEAN_NAME)
@Log4j
public class CachingCloudFileStore implements FileStore {

    public static final String BEAN_NAME = "cachingCloudFileStore";

    @EJB(beanName = AwsFileStore.BEAN_NAME)
    FileStore cloudFileStore;


    FileStore localFileStore = new LocalFileStore();

    @EJB
    JobServiceManager jobServiceManager;

    @EJB
    ContenerChecker contenerChecker;

    private LocalDateTime syncedUntil;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    private long updateFrequencySeconds = 300;

    @PostConstruct
    public void init() {
        String implPropKey = contenerChecker.getContext() + FILE_STORE_IMPLEMENTATION;
        String implProp = System.getProperty(implPropKey);
        if (BEAN_NAME.equals(implProp)) {
            log.info("Starting CachingCloudFileStore pre-fetch process");

            Integer cacheHistoryDays = null;
            String cacheHistoryDaysKey = "iev.file.store.cache.history.days";
            if (System.getProperty(cacheHistoryDaysKey) != null) {
                try {
                    cacheHistoryDays = Integer.valueOf(System.getProperty(cacheHistoryDaysKey));
                } catch (NumberFormatException nfe) {
                    log.warn("Illegal value for property named " + cacheHistoryDaysKey + " in iev.properties. Should be no of days to fetch history for (int)");
                }
            }

            if (cacheHistoryDays == null) {
                syncedUntil = LocalDateTime.fromDateFields(new Date(0));
            } else {
                syncedUntil = LocalDateTime.now().minusDays(cacheHistoryDays);
            }

            log.info("Set syncedUntil to : " + syncedUntil);

            String updateFrequencyKey = "iev.file.store.cache.update.seconds";
            if (System.getProperty(updateFrequencyKey) != null) {
                try {
                    updateFrequencySeconds = Long.valueOf(System.getProperty(updateFrequencyKey));
                } catch (NumberFormatException nfe) {
                    log.warn("Illegal value for property named " + updateFrequencyKey + " in iev.properties. Should be no of seconds between cache updates (long)");
                }
            }

            scheduler.scheduleAtFixedRate(new CleanLocalCacheTask(), 20, 3600, SECONDS);
            scheduler.scheduleAtFixedRate(new PrefetchToLocalCacheTask(), 20, updateFrequencySeconds, SECONDS);

        } else {
            log.info("Not initializing CachingCloudFileStore as other FileStore impl is configured. " + implPropKey + ":" + implProp);
        }
    }


    @Override
    public InputStream getFileContent(Path filePath) {

        if (localFileStore.exists(filePath)) {
            log.debug("Returning file content from local cache: " + filePath);
            return localFileStore.getFileContent(filePath);
        }

        return cloudFileStore.getFileContent(filePath);
    }

    @Override
    public void writeFile(Path filePath, InputStream content) {

        log.info("Writing with CachingCloudFileStore");

        String ievExportDestination = System.getProperty("iev.export.destination");

        try {
            ByteArrayInputStream bis;
            if (content instanceof ByteArrayInputStream) {
                bis = (ByteArrayInputStream) content;
            } else {
                bis = new ByteArrayInputStream(IOUtils.toByteArray(content));
            }

            cloudFileStore.writeFile(filePath, bis);
            bis.reset();
            localFileStore.writeFile(filePath, bis);
        } catch (IOException ioE) {
            throw new FileServiceException("Failed to write file to permanent storage: " + ioE.getMessage(), ioE);
        }

    }

    @Override
    public boolean delete(Path filePath) {
        localFileStore.delete(filePath);
        return cloudFileStore.delete(filePath);
    }

    @Override
    public void deleteFolder(Path folder) {
        localFileStore.delete(folder);
        cloudFileStore.delete(folder);
    }

    @Override
    public void createFolder(Path folder) {
        localFileStore.createFolder(folder);
        cloudFileStore.createFolder(folder);
    }

    @Override
    public boolean exists(Path filePath) {
        if (localFileStore.exists(filePath)) {
            return true;
        }
        return cloudFileStore.exists(filePath);
    }


    private class PrefetchToLocalCacheTask implements Runnable {

        @Override
        public void run() {
            log.info("Start pre-fetching job files from cloud storage. Caching all completed jobs since: " + syncedUntil);

            List<Job> completedJobsSinceLastSync = jobServiceManager.completedJobsSince(syncedUntil).stream()
                    .sorted(Comparator.comparing(Job::getUpdated).reversed()).collect(Collectors.toList());

            completedJobsSinceLastSync.stream().forEach(job -> prefetchFilesForJob(job));
            syncedUntil = completedJobsSinceLastSync.stream().map(job -> job.getUpdated()).max(LocalDateTime::compareTo).orElse(syncedUntil);

            log.info("Finished pre-fetching job files from cloud storage");

        }

        private void prefetchFilesForJob(Job job) {
            try {
                JobService jobService = jobServiceManager.getJobService(job.getId());
                job.getLinks().stream()
                        .map(link -> toFileName(link.getRel()))
                        .filter(fileName -> fileName != null)
                        .map(fileName -> Paths.get(jobService.getPathName(), fileName))
                        .map(path -> Pair.of(path, cloudFileStore.getFileContent(path)))
                        .filter(file -> file.getRight() != null)
                        .forEach(file -> localFileStore.writeFile(file.getLeft(), file.getRight()));

            } catch (Exception exception) {
                log.warn("Unable to pre fetch files for job: " + job + " :" + exception.getMessage());
            }
        }

        private String toFileName(String rel) {
            if (rel.equals(Link.PARAMETERS_REL)) {
                return PARAMETERS_FILE;
            } else if (rel.equals(Link.ACTION_PARAMETERS_REL)) {
                return ACTION_PARAMETERS_FILE;
            } else if (rel.equals(Link.VALIDATION_PARAMETERS_REL)) {
                return VALIDATION_PARAMETERS_FILE;
            } else if (rel.equals(Link.VALIDATION_REL)) {
                return VALIDATION_FILE;
            } else if (rel.equals(Link.REPORT_REL)) {
                return REPORT_FILE;
            }
            return null;
        }

    }


    /**
     * Cleaning files older than 24h
     */
    private class CleanLocalCacheTask implements Runnable {

        @Override
        public void run() {
            log.info("Cleaning local cache : Cleaning all files older than " + syncedUntil);

            try {
                Files.find(Paths.get(jobServiceManager.getRootDirectory()), 5,
                        (path, basicFileAttrs) -> basicFileAttrs.lastModifiedTime()
                                .toInstant().isBefore( ZonedDateTime.now()
                                        .minusDays(1).toInstant()))
                        .forEach(fileToDelete -> {
                            try {
                                if (!Files.isDirectory(fileToDelete)) {
                                    log.info("deleting : " + fileToDelete);
                                    Files.delete(fileToDelete);
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            log.info("Finished pre-fetching job files from cloud storage");

        }
    }
}

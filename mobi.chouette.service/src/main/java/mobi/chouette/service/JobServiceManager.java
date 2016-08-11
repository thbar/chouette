package mobi.chouette.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.ContenerChecker;
import mobi.chouette.common.PropertyNames;
import mobi.chouette.dao.iev.JobDAO;
import mobi.chouette.model.iev.Job;
import mobi.chouette.model.iev.Job.STATUS;
import mobi.chouette.model.iev.Link;
import mobi.chouette.persistence.hibernate.ChouetteIdentifierGenerator;
import mobi.chouette.scheduler.Scheduler;

@Singleton(name = JobServiceManager.BEAN_NAME)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Startup
@Log4j
public class JobServiceManager {

	public static final String BEAN_NAME = "JobServiceManager";

	@EJB
	JobDAO jobDAO;

	@EJB(beanName = ContenerChecker.NAME)
	ContenerChecker checker;

	@EJB
	Scheduler scheduler;

	@Resource(lookup = "java:comp/DefaultManagedExecutorService")
	ManagedExecutorService executor;

	private Set<Object> referentials = Collections.synchronizedSet(new HashSet<>());

	private int maxJobs = 5;

	private String rootDirectory; 
	
	private String lock = "lock";
	
	@PostConstruct
	public synchronized void init() {
		String context = checker.getContext();
		System.setProperty(context + PropertyNames.MAX_STARTED_JOBS, "5");
		System.setProperty(context + PropertyNames.MAX_COPY_BY_JOB, "5");
		try {
			// set default properties
			System.setProperty(checker.getContext() + PropertyNames.ROOT_DIRECTORY, System.getProperty("user.home"));

			// try to read properties
			File propertyFile = new File("/etc/chouette/" + context + "/" + context + ".properties");
			if (propertyFile.exists() && propertyFile.isFile()) {
				try {
					FileInputStream fileInput = new FileInputStream(propertyFile);
					Properties properties = new Properties();
					properties.load(fileInput);
					fileInput.close();
					log.info("reading properties from " + propertyFile.getAbsolutePath());
					for (String key : properties.stringPropertyNames()) {
						if (key.startsWith(context))
							System.setProperty(key, properties.getProperty(key));
						else
							System.setProperty(context + "." + key, properties.getProperty(key));
					}
				} catch (IOException e) {
					log.error("cannot read properties " + propertyFile.getAbsolutePath()
							+ " , using default properties", e);
				}
			} else {
				log.info("no property file found " + propertyFile.getAbsolutePath() + " , using default properties");
			}
		} catch (Exception e) {
			log.error("cannot process properties", e);
		}
		maxJobs = Integer.parseInt(System.getProperty(checker.getContext() + PropertyNames.MAX_STARTED_JOBS));
		rootDirectory = System.getProperty(checker.getContext() + PropertyNames.ROOT_DIRECTORY);
		
		// migrate jobs
		jobDAO.migrate();
	}

	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public JobService create(String referential, String action, String type, Map<String, InputStream> inputStreamsByName)
			throws ServiceException {
		// Valider les parametres
		validateReferential(referential);
		
		synchronized (lock) {
			int numActiveJobs = scheduler.getActivejobsCount();
			log.info("Inside lock, numActiveJobs="+numActiveJobs);
			if (numActiveJobs >= maxJobs) {
				throw new RequestServiceException(RequestExceptionCode.TOO_MANY_ACTIVE_JOBS, "" + maxJobs
						+ " active jobs");
			}
		}
		JobService jobService = createJob(referential, action, type, inputStreamsByName);
		scheduler.schedule(referential);
		return jobService;
	}

	private JobService createJob(String referential, String action, String type,
			Map<String, InputStream> inputStreamsByName) throws ServiceException {
		JobService jobService = null;
		try {
			log.info("Creating job referential="+referential+" ...");
			// Instancier le modèle du service 'upload'
			jobService = new JobService(rootDirectory,referential, action, type);

			// Enregistrer le jobService pour obtenir un id
			jobDAO.create(jobService.getJob());
			// mkdir
			if (Files.exists(jobService.getPath())) {
				// réutilisation anormale d'un id de job (réinitialisation de la
				// séquence à l'extérieur de l'appli?)
				FileUtils.deleteDirectory(jobService.getPath().toFile());
			}
			Files.createDirectories(jobService.getPath());

			// Enregistrer des paramètres à conserver sur fichier
			jobService.saveInputStreams(inputStreamsByName);

			// set cancel link
			jobService.addLink(MediaType.APPLICATION_JSON, Link.CANCEL_REL);

			jobDAO.update(jobService.getJob());
			// jobDAO.detach(jobService.getJob());

			log.info("Job id=" + jobService.getJob().getId() + " referential="+referential+" created");
			return jobService;

		} catch (RequestServiceException ex) {
			log.warn("fail to create job ",ex);
			deleteBadCreatedJob(jobService);
			throw ex;
		} catch (Exception ex) {
			log.warn("fail to create job " + ex.getMessage() + " " + ex.getClass().getName(),ex);
			deleteBadCreatedJob(jobService);
			throw new ServiceException(ServiceExceptionCode.INTERNAL_ERROR, ex);
		}
	}

	private void deleteBadCreatedJob(JobService jobService) {
		if (jobService == null || jobService.getJob().getId() == null)
			return;
		try {
			// remove path if exists
			if (jobService.getPath() != null && Files.exists(jobService.getPath()))
				FileUtils.deleteDirectory(jobService.getPath().toFile());
		} catch (IOException ex1) {
			log.error("fail to delete directory " + jobService.getPath(), ex1);
		}
		Job job = jobService.getJob();
		if (job != null && job.getId() != null) {
			log.info("deleting bad job " + job.getId());
			jobDAO.delete(job);
		}

	}

	private void validateReferential(final String referential) throws ServiceException {

		if (referentials.contains(referential))
			return;

		boolean result = checker.validateContener(referential);
		if (!result) {
			throw new RequestServiceException(RequestExceptionCode.UNKNOWN_REFERENTIAL, "referential");
		}

		referentials.add(referential);
	}

	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public JobService download(String referential, Long id, String filename) throws ServiceException {
		JobService jobService = getJobService(referential, id, true);

		java.nio.file.Path path = Paths.get(jobService.getPathName(), filename);
		if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			throw new RequestServiceException(RequestExceptionCode.UNKNOWN_FILE, "");
		}
		return jobService;
	}

	/**
	 * find next waiting job on referential <br/>
	 * return null if a job is STARTED or if no job is SCHEDULED
	 * 
	 * @param referential
	 * @return
	 */
	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public JobService getNextJob(String referential) {
		Job job = jobDAO.getNextJob(referential);
		if (job == null) {
			return null;
		}
		// jobDAO.detach(job);
		return new JobService(rootDirectory,job);
	}

	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void start(JobService jobService) {
		jobService.setStatus(STATUS.STARTED);
		jobService.setUpdated(new Date());
		jobService.setStarted(new Date());
		jobService.addLink(MediaType.APPLICATION_JSON, Link.REPORT_REL);
		jobDAO.update(jobService.getJob());
	}

	public JobService cancel(String referential, Long id) throws ServiceException {
		validateReferential(referential);
		JobService jobService = getJobService(referential, id, true);
		if (jobService.getStatus().ordinal() <= STATUS.STARTED.ordinal()) {

			if (jobService.getStatus().equals(STATUS.STARTED)) {
				scheduler.cancel(jobService);
			}

			jobService.setStatus(STATUS.CANCELED);

			// remove cancel link only
			jobService.removeLink(Link.CANCEL_REL);
			// set delete link
			jobService.addLink(MediaType.APPLICATION_JSON, Link.DELETE_REL);

			jobService.setUpdated(new Date());
			jobDAO.update(jobService.getJob());

		}
        return jobService;
	}

	public void remove(String referential, Long id) throws ServiceException {
		validateReferential(referential);
		JobService jobService = getJobService(referential, id, false);
		if (jobService.getStatus().ordinal() <= STATUS.STARTED.ordinal()) {
			throw new RequestServiceException(RequestExceptionCode.SCHEDULED_JOB, "referential = " + referential
					+ " ,id = " + id);
		}
		try {
			FileUtils.deleteDirectory(jobService.getPath().toFile());
		} catch (IOException e) {
			log.error("fail to delete directory " + jobService.getPath(), e);
		}
		jobDAO.delete(jobService.getJob());
	}

	public void drop(String referential) throws ServiceException {

		List<JobService> jobServices = findAll(referential);
		// reject demand if non terminated jobs are present
		for (JobService jobService : jobServices) {
			if (jobService.getStatus().equals(STATUS.STARTED) || jobService.getStatus().equals(STATUS.SCHEDULED)) {
				throw new RequestServiceException(RequestExceptionCode.REFERENTIAL_BUSY, "referential");
			}
		}

		// remove all jobs
		jobDAO.deleteAll(referential);

		// clean directories
		try {
			
			FileUtils.deleteDirectory(new File(JobService.getRootPathName(rootDirectory,referential)));
		} catch (IOException e) {
			log.error("fail to delete directory for" + referential, e);
		}

		// remove referential from known ones
		referentials.remove(referential);

		// remove sequences data for this tenant
		ChouetteIdentifierGenerator.deleteTenant(referential);

	}

	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void terminate(JobService jobService) {
		jobService.setStatus(STATUS.TERMINATED);

		// remove cancel link only
		jobService.removeLink(Link.CANCEL_REL);
		// set delete link
		jobService.addLink(MediaType.APPLICATION_JSON, Link.DELETE_REL);
		// add data link if necessary
		if (!jobService.linkExists(Link.OUTPUT_REL)) {
			if (jobService.getOutputFilename() != null
					&& Files.exists(Paths.get(jobService.getPathName(), jobService.getOutputFilename()))) {
				jobService.addLink(MediaType.APPLICATION_OCTET_STREAM, Link.DATA_REL);
				jobService.addLink(MediaType.APPLICATION_OCTET_STREAM, Link.OUTPUT_REL);
			}
		}
		// add validation report link
		if (!jobService.linkExists(Link.VALIDATION_REL)) {
			if (Files.exists(Paths.get(jobService.getPathName(), Constant.VALIDATION_FILE)))
				jobService.addLink(MediaType.APPLICATION_JSON, Link.VALIDATION_REL);
		}

		jobService.setUpdated(new Date());
		jobDAO.update(jobService.getJob());

	}

	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void abort(JobService jobService) {

		jobService.setStatus(STATUS.ABORTED);

		// remove cancel link only
		jobService.removeLink(Link.CANCEL_REL);
		// set delete link
		jobService.addLink(MediaType.APPLICATION_JSON, Link.DELETE_REL);

		// add validation report link
		if (!jobService.linkExists(Link.VALIDATION_REL)) {
			if (Files.exists(Paths.get(jobService.getPathName(), Constant.VALIDATION_FILE)))
				jobService.addLink(MediaType.APPLICATION_JSON, Link.VALIDATION_REL);
		}

		jobService.setUpdated(new Date());
		jobDAO.update(jobService.getJob());

	}

	public List<JobService> findAll() {
		List<Job> jobs = jobDAO.findAll();
		List<JobService> jobServices = new ArrayList<>(jobs.size());
		for (Job job : jobs) {
			jobServices.add(new JobService(rootDirectory,job));
		}
		return jobServices;
	}

	public List<JobService> findAll(String referential) {
		List<Job> jobs = jobDAO.findByReferential(referential);
		List<JobService> jobServices = new ArrayList<>(jobs.size());
		for (Job job : jobs) {
			jobServices.add(new JobService(rootDirectory,job));
		}

		return jobServices;
	}

	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public JobService scheduledJob(String referential, Long id) throws ServiceException {
		validateReferential(referential);
		return getJobService(referential, id, true);
	}

	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public JobService terminatedJob(String referential, Long id) throws ServiceException {
		validateReferential(referential);
		JobService jobService = getJobService(referential, id, true);

		if (jobService.getStatus().ordinal() < STATUS.TERMINATED.ordinal()
				|| jobService.getStatus().ordinal() == STATUS.DELETED.ordinal()) {
			throw new RequestServiceException(RequestExceptionCode.UNKNOWN_JOB, "referential = " + referential
					+ " ,id = " + id);
		}

		return jobService;
	}

	private JobService getJobService(String referential, Long id, boolean detach) throws ServiceException {

		Job job = jobDAO.find(id);
		if (job != null && job.getReferential().equals(referential)) {
			// if (detach)
			// jobDAO.detach(job);
			return new JobService(rootDirectory,job);
		}
		throw new RequestServiceException(RequestExceptionCode.UNKNOWN_JOB, "referential = " + referential + " ,id = "
				+ id);
	}

	public JobService getJobService(Long id) throws ServiceException {
		Job job = jobDAO.find(id);
		if (job != null) {
			// jobDAO.detach(job);
			return new JobService(rootDirectory,job);
		}
		throw new RequestServiceException(RequestExceptionCode.UNKNOWN_JOB, " id = " + id);
	}

	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<JobService> jobs(String referential, String action, final Long version, Job.STATUS[] status) throws ServiceException {
		validateReferential(referential);

		List<Job> jobs = null;
		if (action == null) {
			jobs = jobDAO.findByReferential(referential,status);
		} else {
			jobs = jobDAO.findByReferentialAndAction(referential, action,status);
		}

		Collection<Job> filtered = Collections2.filter(jobs, new Predicate<Job>() {
			@Override
			public boolean apply(Job job) {
				// filter on update time if given, otherwise don't return
				// deleted jobs
				boolean versionZeroCondition = (version == 0) && job.getStatus().ordinal() < STATUS.DELETED.ordinal();
				boolean versionNonZeroCondition = (version > 0) && version < job.getUpdated().getTime();

				return versionZeroCondition || versionNonZeroCondition;
			}
		});

		List<JobService> jobServices = new ArrayList<>(filtered.size());
		for (Job job : filtered) {
			jobServices.add(new JobService(rootDirectory,job));
		}
		return jobServices;
	}

	// administration operation
	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<JobService> activeJobs() {

		List<Job> jobs = jobDAO.findByStatus(Job.STATUS.STARTED);
		jobs.addAll(jobDAO.findByStatus(Job.STATUS.SCHEDULED));

		List<JobService> jobServices = new ArrayList<>(jobs.size());
		for (Job job : jobs) {
			jobServices.add(new JobService(rootDirectory,job));
		}
		return jobServices;
	}

}

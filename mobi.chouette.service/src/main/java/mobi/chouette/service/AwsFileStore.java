package mobi.chouette.service;

import com.amazonaws.services.s3.AmazonS3;
import com.okina.helper.aws.BlobStoreHelper;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.ContenerChecker;
import mobi.chouette.common.file.FileStore;
import mobi.chouette.core.CloudRuntimeException;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static mobi.chouette.service.AwsFileStore.BEAN_NAME;


/**
 * Store permanent files in Aws
 */
@Singleton(name = BEAN_NAME)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Log4j
public class AwsFileStore implements FileStore {

	public static final String BEAN_NAME = "AwsFileStore";

	@EJB
	private ContenerChecker checker;

	private AmazonS3 client;

	private String containerName;

	private String baseFolder;


	@PostConstruct
	public void init() {
		System.out.println("=============================== HERE ===============================");
		baseFolder = System.getProperty(checker.getContext() + ".directory");
		containerName = System.getProperty(checker.getContext() + ".blobstore.aws.container.name");
		String key = System.getProperty(checker.getContext() + ".blobstore.aws.access.key");
		String secret = System.getProperty(checker.getContext() + ".blobstore.aws.access.secret");

		log.info("Initializing AWS blob store service. ContainerName: " + containerName + ", Key=" + key);

		client = BlobStoreHelper.getClient(key, secret);
	}


	@Override
	public InputStream getFileContent(Path filePath) {
		log.info("Key used : " +  System.getProperty(checker.getContext() + ".blobstore.aws.access.key"));
		return BlobStoreHelper.getBlob(client, containerName, toGCSPath(filePath));
	}

	@Override
    public void writeFile(Path filePath, InputStream content) {
		try {
			BlobStoreHelper.uploadBlob(client, containerName, toGCSPath(filePath), content);
		} catch (InterruptedException | IOException e) {
			throw new CloudRuntimeException("Problème lors de l'envoi du fichier dans le cloud", e);
		}

	}

	@Override
	public void deleteFolder(Path folder) {
		BlobStoreHelper.deleteBlobsByPrefix(client, containerName, toGCSPath(folder));
	}


	@Override
	public boolean exists(Path filePath) {
		return getFileContent(filePath) != null;
	}


	@Override
	public void createFolder(Path folder) {
		// Folders do not existing in GC storage
	}

	@Override
	public boolean delete(Path filePath) {
		return BlobStoreHelper.deleteBlobsByPrefix(client, containerName, toGCSPath(filePath));
	}

	private String toGCSPath(Path path) {
		String withoutBaseFolder = path.toString().replaceFirst(baseFolder, "");
		if (withoutBaseFolder.startsWith("/")) {
			return withoutBaseFolder.replaceFirst("/", "");
		}
		return withoutBaseFolder;
	}

}

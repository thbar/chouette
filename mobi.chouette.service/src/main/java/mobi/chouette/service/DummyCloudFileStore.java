package mobi.chouette.service;

import java.io.InputStream;
import java.nio.file.Path;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.ContenerChecker;
import mobi.chouette.common.file.FileStore;
import org.apache.commons.lang3.NotImplementedException;


import static mobi.chouette.service.DummyCloudFileStore.BEAN_NAME;

/**
 * Store permanent files in Google Cloud Storage.
 */
@Singleton(name = BEAN_NAME)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Log4j
public class DummyCloudFileStore implements FileStore {

	public static final String BEAN_NAME = "DummyCloudFileStore";



	@PostConstruct
	public void init() {
	throw new NotImplementedException("Non implémenté");
	}


	@Override
	public InputStream getFileContent(Path filePath) {
		throw new NotImplementedException("Non implémenté");
	}

	@Override
    public void writeFile(Path filePath, InputStream content) {
		throw new NotImplementedException("Non implémenté");

    }

	@Override
	public void deleteFolder(Path folder) {
		throw new NotImplementedException("Non implémenté");
	}


	@Override
	public boolean exists(Path filePath) {
		throw new NotImplementedException("Non implémenté");
	}


	@Override
	public void createFolder(Path folder) {
		throw new NotImplementedException("Non implémenté");
	}

	@Override
	public boolean delete(Path filePath) {
		throw new NotImplementedException("Non implémenté");
	}


}

package mobi.chouette.service;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.netexprofile.importer.util.StopPlaceRegistryIdFetcher;

import javax.ejb.Singleton;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

@Singleton(name = StopPlaceRegistryIdFetcherMock.BEAN_NAME)
@Log4j
public class StopPlaceRegistryIdFetcherMock extends StopPlaceRegistryIdFetcher {
	public static final String BEAN_NAME = "StopPlaceRegistryIdFetcher";

	private Set<String> quayIds;
	private Set<String> stopPlaceIds;
	private Set<String> ids;


	public StopPlaceRegistryIdFetcherMock() {
	}

	@Override
	public Set<String> getQuayIds() {
		return quayIds;
	}

	@Override
	public Set<String> getStopPlaceIds() {
		return stopPlaceIds;
	}

	private Set<String> getIds(String idEndpoint) {
		return ids;

	}

	public void setQuayIds(Set<String> quayIds) {
		this.quayIds = quayIds;
	}

	public void setStopPlaceIds(Set<String> stopPlaceIds) {
		this.stopPlaceIds = stopPlaceIds;
	}

	public void setIds(Set<String> ids) {
		this.ids = ids;
	}
}

package mobi.chouette.ws;

import lombok.extern.log4j.Log4j;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.interceptors.CorsFilter;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Log4j
public class Application extends javax.ws.rs.core.Application implements
		ServletContextListener {

	@Context
	private UriInfo uriInfo;

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> result = new HashSet<Class<?>>();
		result.add(RestService.class);
		result.add(RestAdmin.class);
		result.add(RestStatisticsService.class);
		result.add(RestNetexStopPlaceService.class);
		result.add(HealthResource.class);
		return result;
	}

	@Override
	public Set<Object> getSingletons() {
		log.info("Application.java - getSingletons()");
		Set<Object> result = new HashSet<Object>();
		CorsFilter corsFilter = new CorsFilter();
		corsFilter.getAllowedOrigins().add("*");
		corsFilter.setAllowedMethods("OPTIONS, GET, POST, DELETE, PUT, PATCH");
		result.add(corsFilter);
		log.info("CorsFilter - AllowedHeaders: " + corsFilter.getAllowedHeaders() +
				" - ExposedHeaders" + corsFilter.getExposedHeaders() +
				" - AllowedMethods: " + corsFilter.getAllowedMethods());
		return result;
	}

	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> result = new HashMap<String, Object>();
		return result;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		Logger log = Logger.getLogger("org.jboss.resteasy.core.ExceptionHandler");
		log.setLevel(Level.ERROR);

	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {

	}

}

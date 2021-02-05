package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.GtfsAgency;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;

import java.awt.*;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Map;

public class RouteById extends AbstractRouteById {


	public RouteById(String name) throws IOException {
		super(name);
	}

	@Override
	protected String handleRouteIdTransformation(String inputRouteId) {
		return inputRouteId;
	}

	public static class DefaultImporterFactory extends IndexFactory {
		@SuppressWarnings("rawtypes")
		@Override
		protected Index create(String name) throws IOException {
			return new RouteById(name);
		}
	}

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(RouteById.class.getName(), factory);
	}

}

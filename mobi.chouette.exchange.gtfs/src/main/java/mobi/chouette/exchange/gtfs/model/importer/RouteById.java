package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.GtfsAgency;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Map;

public class RouteById extends AbstractRouteById {


	public RouteById(String name, String linePrefixToRemove) throws IOException {
		super(name,"",linePrefixToRemove);
	}

	@Override
	protected String handleRouteIdTransformation(String inputRouteId) {
		if (StringUtils.isNotEmpty(_linePrefixToRemove)){
			inputRouteId = inputRouteId.replaceFirst("^"+_linePrefixToRemove,"");
		}
		return inputRouteId;
	}

	@Override
	protected String handleKeyTransformation(String rawValue){
		if(this._key.equals("route_id")){
			return handleRouteIdTransformation(rawValue);
		}
		return rawValue;
	}

	public static class DefaultImporterFactory extends IndexFactory {
		@SuppressWarnings("rawtypes")
		@Override
		protected Index create(String name) throws IOException {
			return new RouteById(name,"");
		}

		@Override
		protected Index create(String name, FactoryParameters  factoryParameters) throws IOException {
			return new RouteById(name,factoryParameters.getLinePrefixToRemove());
		}
	}

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(RouteById.class.getName(), factory);
	}

}

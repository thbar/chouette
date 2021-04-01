package mobi.chouette.exchange.gtfs.model.importer;


import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.GtfsAgency;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Map;

public class RouteByIdWithMergedIndex extends AbstractRouteById {



	public RouteByIdWithMergedIndex(String name, String splitCharacter, String linePrefixToRemove) throws IOException {
		super(name,splitCharacter,linePrefixToRemove);
	}

	@Override
	protected String handleRouteIdTransformation(String inputRouteId) {
		if (StringUtils.isNotEmpty(_linePrefixToRemove)){
			inputRouteId = inputRouteId.replaceFirst("^"+_linePrefixToRemove,"");
		}

		if (StringUtils.isEmpty(_splitCharacter))
			return inputRouteId;
		return inputRouteId.split(_splitCharacter)[0];
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
			return new RouteByIdWithMergedIndex(name,"","");
		}

		@Override
		protected Index create(String name, FactoryParameters  factoryParameters) throws IOException {
			return new RouteByIdWithMergedIndex(name,factoryParameters.getSplitCharacter(),factoryParameters.getLinePrefixToRemove());
		}
	}

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(RouteByIdWithMergedIndex.class.getName(), factory);
	}

}

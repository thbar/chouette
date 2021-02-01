package mobi.chouette.exchange.gtfs.model.importer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class IndexFactory {

	public static Map<String, IndexFactory> factories = new HashMap<String, IndexFactory>();

	@SuppressWarnings("rawtypes")
	protected abstract Index create(String path) throws IOException;

	protected Index create(String path, FactoryParameters factoryParameters) throws IOException {
		return null;
	}

	@SuppressWarnings("rawtypes")
	public static final Index build(String path, String clazz)
			throws ClassNotFoundException, IOException

	{
		return build( path, clazz,null);
	}

	public static final Index build(String path, String clazz,FactoryParameters factoryParameters)
			throws ClassNotFoundException, IOException

	{
		if (!factories.containsKey(clazz)) {
			Class.forName(clazz);
			if (!factories.containsKey(clazz))
				throw new ClassNotFoundException(clazz);
		}

		return factoryParameters==null?((IndexFactory) factories.get(clazz)).create(path):((IndexFactory) factories.get(clazz)).create(path,factoryParameters);
	}
}

package mobi.chouette.exchange.regtopp.model.importer.parser.index;

import org.apache.commons.lang.StringUtils;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.regtopp.model.RegtoppDestinationDST;
import mobi.chouette.exchange.regtopp.model.importer.parser.FileContentParser;
import mobi.chouette.exchange.regtopp.model.importer.parser.FileParserValidationError;
import mobi.chouette.exchange.regtopp.model.importer.parser.RegtoppException;
import mobi.chouette.exchange.regtopp.model.importer.parser.RegtoppImporter;
import mobi.chouette.exchange.regtopp.validation.RegtoppValidationReporter;

@Log4j
public class DestinationById extends IndexImpl<RegtoppDestinationDST> {

	public DestinationById(RegtoppValidationReporter validationReporter, FileContentParser fileParser) throws Exception {
		super(validationReporter, fileParser);
	}

	@Override
	public boolean validate(RegtoppDestinationDST bean, RegtoppImporter dao) {
		boolean result = true;

		if (StringUtils.trimToNull(bean.getDestinationText()) == null) {
			// validationReporter.reportError(new Context(), ex, filenameInfo);

			// TODO add entry to validationReporter
			result = false;
		}

		return result;
	}

	public static class DefaultImporterFactory extends IndexFactory {
		@SuppressWarnings("rawtypes")
		@Override
		protected Index create(RegtoppValidationReporter validationReporter, FileContentParser parser) throws Exception {
			return new DestinationById(validationReporter, parser);
		}
	}

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(DestinationById.class.getName(), factory);
	}

	@Override
	public void index() throws Exception {
		for (Object obj : parser.getRawContent()) {
			RegtoppDestinationDST destination = (RegtoppDestinationDST) obj;
			RegtoppDestinationDST existing = index.put(destination.getDestinationId(), destination);
			if (existing != null) {
				// TODO fix exception/validation reporting
				validationReporter.reportError(new Context(), new RegtoppException(new FileParserValidationError()), null);
			}
		}
	}
}
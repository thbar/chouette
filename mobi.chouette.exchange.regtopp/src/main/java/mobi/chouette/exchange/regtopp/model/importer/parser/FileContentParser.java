package mobi.chouette.exchange.regtopp.model.importer.parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.beanio.BeanReader;
import org.beanio.BeanReaderErrorHandlerSupport;
import org.beanio.InvalidRecordException;
import org.beanio.RecordContext;
import org.beanio.StreamFactory;
import org.beanio.builder.FixedLengthParserBuilder;
import org.beanio.builder.StreamBuilder;
import org.joda.time.Duration;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.regtopp.beanio.DepartureTimeTypeHandler;
import mobi.chouette.exchange.regtopp.beanio.DrivingDurationTypeHandler;
import mobi.chouette.exchange.regtopp.model.RegtoppObject;
import mobi.chouette.exchange.regtopp.model.importer.parser.RegtoppException.ERROR;
import mobi.chouette.exchange.regtopp.validation.RegtoppValidationReporter;
import mobi.chouette.exchange.report.FileError;
import mobi.chouette.exchange.report.FileError.CODE;
import mobi.chouette.exchange.report.FileInfo.FILE_STATE;

@Log4j
public class FileContentParser {
	private static final String REGTOPP_CHARSET = "ISO-8859-1";
	@Getter
	private List<Object> rawContent = new ArrayList<>();
	
	public void parse(final Context context, final ParseableFile parseableFile, final RegtoppValidationReporter validationReporter) throws Exception {
		StreamFactory factory = StreamFactory.newInstance();

		StreamBuilder builder = new StreamBuilder("regtopp").format("fixedlength").parser(new FixedLengthParserBuilder());

		for (Class<?> clazz : parseableFile.getRegtoppClasses()) {
			builder = builder.addRecord(clazz);

		}
		builder.addTypeHandler("departureTime",Duration.class, new DepartureTimeTypeHandler());
		builder.addTypeHandler("drivingDuration",Duration.class, new DrivingDurationTypeHandler());

		factory.define(builder);

		FileInputStream is = new FileInputStream(parseableFile.getFile());
		InputStreamReader isr = new InputStreamReader(is, REGTOPP_CHARSET);
		BufferedReader buffReader = new BufferedReader(isr);
		
		// TODO consider using error reporter instead if this continues parsing of the file
		BeanReader in = factory.createReader("regtopp", buffReader);

		final Set<RegtoppException> errors = new HashSet<RegtoppException>();
		final String fileName = parseableFile.getFile().getName();

		in.setErrorHandler(new BeanReaderErrorHandlerSupport() {
			public void invalidRecord(InvalidRecordException ex) throws Exception {
				// if a bean object is mapped to a record group,
				// the exception may contain more than one record
				for (int i = 0, j = ex.getRecordCount(); i < j; i++) {
					RecordContext rContext = ex.getRecordContext(i);
					if (rContext.hasRecordErrors()) {
						for (String error : rContext.getRecordErrors()) {
							
							// TODO report this in a better fashion
							FileParserValidationError ctx = new FileParserValidationError(fileName, rContext.getLineNumber(), rContext.getRecordName(),
									rContext.getRecordText(), ERROR.INVALID_FIELD_VALUE, error);
							RegtoppException e = new RegtoppException(ctx, ex);
							errors.add(e);
						}
					}
					if (rContext.hasFieldErrors()) {
						for (String field : rContext.getFieldErrors().keySet()) {
							for (String error : rContext.getFieldErrors(field)) {

								// TODO report this in a better fashion
								FileParserValidationError ctx = new FileParserValidationError(fileName, rContext.getLineNumber(), field,
										rContext.getFieldText(field), ERROR.INVALID_FIELD_VALUE, error);
								RegtoppException e = new RegtoppException(ctx, ex);
								errors.add(e);
							}
						}
					}
				}
				parseableFile.getFileInfo().addError(new FileError(CODE.INVALID_FORMAT, ex.getMessage()));
				
			}
		});

		Object record = null;

		try {
			while ((record = (RegtoppObject) in.read()) != null) {
				rawContent.add(record);
			}
			log.info("Parsed file OK: " + parseableFile.getFile().getName());
			parseableFile.getFileInfo().setStatus(FILE_STATE.OK);
		} catch (InvalidRecordException ex) {
		} finally {
			in.close();
		}
		if (errors.size() > 0) {
			validationReporter.reportErrors(context, errors, fileName);
		}

	}

	public void dispose() {
		rawContent.clear();
	}
}

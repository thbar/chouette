package mobi.chouette.exchange.neptune.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.neptune.JobDataTest;
import mobi.chouette.exchange.neptune.jaxb.JaxbNeptuneFileConverter;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.trident.schema.trident.ChouettePTNetworkType;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static mobi.chouette.common.Constant.JOB_DATA;

@Log4j
public class NeptuneBrokenRouteFixerCommandTests {



	protected static InitialContext initialContext;

	private static String test1GeneratedPath = "src/test/data/brokenRoutesTestData/test1/input/B-generated.xml";
	private static String test1CopyPath = "src/test/data/brokenRoutesTestData/test1/B-generated.xml";
	private static String test1SrcCommandPath = "src/test/data/brokenRoutesTestData/test1/input/B.xml";
	private static String test1SrcPath = "src/test/data/brokenRoutesTestData/test1/B.xml";


	protected void init() {
		Locale.setDefault(Locale.ENGLISH);
		if (initialContext == null) {
			try {
				initialContext = new InitialContext();
			} catch (NamingException e) {
				e.printStackTrace();
			}


		}
	}
	protected Context initImportContext(String pathName) {
		init();

		Context context = new Context();
		JobDataTest jobData = new JobDataTest();
		jobData.setPathName(pathName);
		context.put(JOB_DATA,jobData);
		return context;
	}

	private void cleanFile(String pathToFile){
		Path path = Paths.get(pathToFile);
		File file = new File(path.toString());
		if (file.exists()){
			file.delete();
		}
	}

	private void cleanTest1Files(){
		cleanFile(test1CopyPath);
		cleanFile(test1GeneratedPath);
		cleanFile(test1SrcCommandPath);
	}

	@Test(groups = { "CommandChecked" }, description = "test if routes are correctly fixed")
	public void test1_verifyRouteCorrection() throws Exception {
		cleanTest1Files();
		FileUtils.copyFile(new File(Paths.get(test1SrcPath).toString()),new File(Paths.get(test1SrcCommandPath).toString()));

		NeptuneBrokenRouteFixerCommand command = (NeptuneBrokenRouteFixerCommand) CommandFactory.create(initialContext,NeptuneBrokenRouteFixerCommand.class.getName());
		Context context = initImportContext(Paths.get("src/test/data/brokenRoutesTestData/test1").toString());
		boolean returnCode=command.execute(context);

		Assert.assertEquals(returnCode, true, "Incorrect Return code");

		File generatedFile = new File(test1CopyPath.toString());

		Assert.assertEquals(generatedFile.exists(), true, "No generated file");


		try {
			JaxbNeptuneFileConverter converter = JaxbNeptuneFileConverter.getInstance();
			Optional<ChouettePTNetworkType> chouetteNetworkOpt = converter.read(Paths.get("src/test/data/brokenRoutesTestData/test1/B-generated.xml"));

			ChouettePTNetworkType network = chouetteNetworkOpt.get();
			ChouettePTNetworkType.ChouetteLineDescription description = network.getChouetteLineDescription();
			int totalNbOfRoutes = network.getChouetteLineDescription().getChouetteRoute().size();
			Assert.assertEquals(totalNbOfRoutes, 24, "Wrong Nb of generated Routes");
			checkVehicleJourneyAndJourneyPatternRouteIDs(description);

		}catch(Exception e){
			log.error("Can't open generated file");
			throw e;
		}
		cleanTest1Files();
	}

	private void checkVehicleJourneyAndJourneyPatternRouteIDs(ChouettePTNetworkType.ChouetteLineDescription routeDescription){

		List<String> allowedRouteIds = Arrays.asList("SAMPLE_NET:Route:BxA-1","SAMPLE_NET:Route:BxA-2","SAMPLE_NET:Route:BxA-3","SAMPLE_NET:Route:BxA-4","SAMPLE_NET:Route:BxA-5","SAMPLE_NET:Route:BxA-6",
													"SAMPLE_NET:Route:BxA-7","SAMPLE_NET:Route:BxA-8","SAMPLE_NET:Route:BxA-9","SAMPLE_NET:Route:BxA-10","SAMPLE_NET:Route:BxA-11","SAMPLE_NET:Route:BxA-12",
													"SAMPLE_NET:Route:BxR-1","SAMPLE_NET:Route:BxR-2","SAMPLE_NET:Route:BxR-3","SAMPLE_NET:Route:BxR-4","SAMPLE_NET:Route:BxR-5","SAMPLE_NET:Route:BxR-6",
													"SAMPLE_NET:Route:BxR-7","SAMPLE_NET:Route:BxR-8","SAMPLE_NET:Route:BxR-9","SAMPLE_NET:Route:BxR-10","SAMPLE_NET:Route:BxR-11","SAMPLE_NET:Route:BxR-12");

		routeDescription.getJourneyPattern().forEach(
				journeyPattern->Assert.assertEquals(allowedRouteIds.contains(journeyPattern.getRouteId()),true,"Unknown routeId : "+journeyPattern.getRouteId())
		);

		routeDescription.getVehicleJourney().forEach(
				vehicleJourney->Assert.assertEquals(allowedRouteIds.contains(vehicleJourney.getRouteId()),true,"Unknown routeId : "+vehicleJourney.getRouteId())
		);



	}



}

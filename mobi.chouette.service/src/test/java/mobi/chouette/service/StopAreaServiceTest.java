package mobi.chouette.service;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.ScheduledStopPointDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.dao.StopPointDAO;
import mobi.chouette.exchange.stopplace.StopAreaUpdateService;
import mobi.chouette.model.Provider;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.SimpleObjectReference;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.StopAreaTypeEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.type.TransportSubModeNameEnum;
import mobi.chouette.persistence.hibernate.ContextHolder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.Assert;
import org.testng.annotations.Test;

public class StopAreaServiceTest extends Arquillian {


	@EJB
	StopAreaService stopAreaService;

	@EJB
	StopAreaDAO stopAreaDAO;

	@EJB
	StopPointDAO stopPointDAO;

	@EJB
	ScheduledStopPointDAO scheduledStopPointDAO;

	@EJB(beanName = StopAreaUpdateService.BEAN_NAME)
	StopAreaUpdateService stopAreaUpdateService;

	@EJB
	private ProviderDAO providerDAO;

	@PersistenceContext(unitName = "public")
	private EntityManager em;

	@Inject
	UserTransaction utx;

	@Deployment
	public static EnterpriseArchive createDeployment() {


		EnterpriseArchive result;
		File[] files = Maven.resolver().loadPomFromFile("pom.xml").resolve("mobi.chouette:mobi.chouette.service")
				.withTransitivity().asFile();
		List<File> jars = new ArrayList<>();
		List<JavaArchive> modules = new ArrayList<>();
		for (File file : files) {
			if (file.getName().startsWith("mobi.chouette.exchange")
					|| file.getName().startsWith("mobi.chouette.service")
					|| file.getName().startsWith("mobi.chouette.dao")) {
				String name = file.getName().split("\\-")[0] + ".jar";
				JavaArchive archive = ShrinkWrap.create(ZipImporter.class, name).importFrom(file).as(JavaArchive.class);
				modules.add(archive);
			} else {
				jars.add(file);
			}
		}
		File[] filesDao = Maven.resolver().loadPomFromFile("pom.xml").resolve("mobi.chouette:mobi.chouette.dao")
				.withTransitivity().asFile();
		if (filesDao.length == 0) {
			throw new NullPointerException("no dao");
		}
		for (File file : filesDao) {
			if (file.getName().startsWith("mobi.chouette.dao")) {
				String name = file.getName().split("\\-")[0] + ".jar";

				JavaArchive archive = ShrinkWrap.create(ZipImporter.class, name).importFrom(file).as(JavaArchive.class);
				modules.add(archive);
				if (!modules.contains(archive))
					modules.add(archive);
			} else {
				if (!jars.contains(file))
					jars.add(file);
			}
		}

		List<File> jarsWithoutAntLR = jars.stream().filter(f -> !f.getName().contains("antlr"))
				.collect(Collectors.toList());


		final WebArchive testWar = ShrinkWrap.create(WebArchive.class, "test.war")
				.addAsResource("test-persistence.xml", "META-INF/persistence.xml")
				.addAsWebInfResource("postgres-ds.xml").addClass(DummyChecker.class)
				.addClass(StopPlaceRegistryIdFetcherMock.class)
				.addClass(StopAreaServiceTest.class);

		result = ShrinkWrap.create(EnterpriseArchive.class, "test.ear").addAsLibraries(jarsWithoutAntLR.toArray(new File[0]))
				.addAsModules(modules.toArray(new JavaArchive[0])).addAsModule(testWar)
				.addAsResource(EmptyAsset.INSTANCE, "beans.xml");
		return result;
	}


	@Test
	public void testUpdateQuaysOnChildStop() throws Exception {
		ContextHolder.setContext("chouette_gui"); // set tenant schema
		stopAreaDAO.truncate();
		utx.begin();
		em.joinTransaction();

		StopArea alreadyExistingParent = new StopArea();
		alreadyExistingParent.setAreaType(ChouetteAreaEnum.CommercialStopPoint);
		alreadyExistingParent.setObjectId("NSR:StopPlace:58291");
		stopAreaDAO.create(alreadyExistingParent);


		StopArea alreadyExistingChild = new StopArea();
		alreadyExistingChild.setAreaType(ChouetteAreaEnum.CommercialStopPoint);
		alreadyExistingChild.setObjectId("NSR:StopPlace:62034");

		alreadyExistingChild.setParent(alreadyExistingParent);
		stopAreaDAO.create(alreadyExistingChild);

		utx.commit();
		utx.begin();
		em.joinTransaction();


		stopAreaService.createOrUpdateStopPlacesFromNetexStopPlaces(new FileInputStream("src/test/data/StopAreasMergedQuaysInChildStop.xml"));

		utx.commit();
		utx.begin();
		em.joinTransaction();

		assertStopPlace(alreadyExistingChild.getObjectId(), "NSR:Quay:104061", "NSR:Quay:8128");

	}

	private void cleanAllschemas(){
		ContextHolder.setContext("chouette_gui");
		stopAreaDAO.truncate();
		ContextHolder.setContext("sky");
		stopAreaDAO.truncate();
		ContextHolder.setContext("rut");
		stopAreaDAO.truncate();
		ContextHolder.setContext("nri");
		stopAreaDAO.truncate();
		ContextHolder.setContext("tro");
		stopAreaDAO.truncate();
		ContextHolder.setContext("akt");
		stopAreaDAO.truncate();
	}

	private void initProducers(){
		ContextHolder.setContext("admin");
		providerDAO.truncate();
		Provider prov1 = new Provider();
		prov1.setCode("tro");
		prov1.setSchemaName("tro");
		providerDAO.create(prov1);

		Provider prov2 = new Provider();
		prov2.setCode("rut");
		prov2.setSchemaName("rut");
		providerDAO.create(prov2);

		Provider prov3 = new Provider();
		prov3.setCode("sky");
		prov3.setSchemaName("sky");
		providerDAO.create(prov3);

		Provider prov4 = new Provider();
		prov4.setCode("nri");
		prov4.setSchemaName("nri");
		providerDAO.create(prov4);

		Provider prov5 = new Provider();
		prov5.setCode("akt");
		prov5.setSchemaName("akt");
		providerDAO.create(prov5);
	}


	@Test
	public void testStopAreaUpdate() throws Exception {
		initProducers();
		cleanAllschemas();
		ContextHolder.setContext("chouette_gui"); // set tenant schema
		utx.begin();
		em.joinTransaction();


		stopAreaService.createOrUpdateStopPlacesFromNetexStopPlaces(new FileInputStream("src/test/data/StopAreasInitialSynch.xml"));

		utx.commit();
		utx.begin();
		ContextHolder.setContext("sky");
		Assert.assertTrue(StringUtils.isEmpty(stopAreaDAO.findByObjectId("NSR:Quay:7").getName()));

		utx.commit();
		utx.begin();
		ContextHolder.setContext("tro");
		assertStopPlace("NSR:StopPlace:1", "NSR:Quay:1a", "NSR:Quay:1b");
		assertStopPlace("NSR:StopPlace:2", "NSR:Quay:2a");
		assertStopPlace("NSR:StopPlace:3", "NSR:Quay:3a");

		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:StopPlace:4"), "Did not expect to find inactive stop place");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:StopPlace:4a"), "Did not expect to find quay for inactive stop place");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:StopPlace:4b"), "Did not expect to find quay for inactive stop place");

		utx.commit();

		utx.begin();
		em.joinTransaction();

		// Update stop places
		stopAreaService.createOrUpdateStopPlacesFromNetexStopPlaces(new FileInputStream("src/test/data/StopAreasUpdate.xml"));
		utx.commit();

		utx.begin();

		ContextHolder.setContext("sky"); // need to go back on sky schema after all modifications to recover NSR stop places

		Assert.assertFalse(StringUtils.isEmpty(stopAreaDAO.findByObjectId("NSR:Quay:7").getName()), "Expected quay name to be updated");

		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:StopPlace:1"), "Did not expect to find deactivated stop place");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:Quay:1a"), "Did not expect to find quay for deactivated stop place");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:Quay:1b"), "Did not expect to find quay for deactivated stop place");

		utx.commit();

		utx.begin();

		ContextHolder.setContext("tro");

		// New quay, removed quay and moved quay for 2
		assertStopPlace("NSR:StopPlace:2", "NSR:Quay:3a", "NSR:Quay:2b");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:Quay:2a"), "Did not expect to find removed quay");
		assertStopPlace("NSR:StopPlace:3");

		utx.commit();

		utx.begin();

		ContextHolder.setContext("sky");
		cleanStopPoints();
		// Create stop point contained in quay 5, later to be merged into quay 6.
		StopPoint spToHaveStopAreaRefReplacedByMerger = createStopPoint("1", stopAreaDAO.findByObjectId("NSR:Quay:5"));
		// Create stop point with ref to non NSR-id to be replaced by new Quay whit org id as import_id

		StopArea stopAreaWithImportId = new StopArea();
		stopAreaWithImportId.setAreaType(ChouetteAreaEnum.BoardingPosition);
		stopAreaWithImportId.setObjectId("SKY:Quay:777777");
		stopAreaDAO.create(stopAreaWithImportId);
		StopPoint spToHaveStopAreaRefReplacedByAddedOriginalId = createStopPoint("2", stopAreaWithImportId);

		utx.commit();
		utx.begin();
		em.joinTransaction();
		stopAreaService.createOrUpdateStopPlacesFromNetexStopPlaces(new FileInputStream("src/test/data/StopAreasUpdateMergedStops.xml"));


		utx.commit();

		utx.begin();

		ContextHolder.setContext("sky");
		// Quay 5 merged with quay 6
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:Quay:5"), "Did not expect to find quay merged into another quay");
		assertStopPlace("NSR:StopPlace:6", "NSR:Quay:6");

		assertStopPlace("NSR:StopPlace:7", "NSR:Quay:7");
		utx.commit();

		utx.begin();
		em.joinTransaction();
		ContextHolder.setContext("sky");
		StopPoint spWithReplacedStopAreaRefByMerger = stopPointDAO.findByObjectId(spToHaveStopAreaRefReplacedByMerger.getObjectId());
		Assert.assertEquals(spWithReplacedStopAreaRefByMerger.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId(), "NSR:Quay:6", "Expected stop point to updated when quays have been merged.");

		StopPoint spWithReplacedStopAreaRefByAddedOriginalId = stopPointDAO.findByObjectId(spToHaveStopAreaRefReplacedByAddedOriginalId.getObjectId());
		Assert.assertEquals(spWithReplacedStopAreaRefByAddedOriginalId.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId(), "NSR:Quay:7", "Expected stop point to updated when quay id has been added as original id to another quay.");


		stopAreaService.createOrUpdateStopPlacesFromNetexStopPlaces(new FileInputStream("src/test/data/StopAreasMovedQuay.xml"));

		utx.commit();

		utx.begin();
		em.joinTransaction();
		ContextHolder.setContext("nri");

		Assert.assertEquals(stopAreaDAO.findByObjectId("NSR:Quay:99319").getParent().getObjectId(), "NSR:StopPlace:62006", "Expected quay to have moved to new parent stop area");

		StopArea knownStopArea = stopAreaDAO.findByObjectId("NSR:StopPlace:62006");

		assertCodeValuesForKnownStop(knownStopArea);


		utx.commit();
	}

	private void assertCodeValuesForKnownStop(StopArea knownStopArea) {
		Assert.assertEquals(knownStopArea.getStopAreaType(), StopAreaTypeEnum.RailStation);
		Assert.assertEquals(knownStopArea.getTransportModeName(), TransportModeNameEnum.Rail);
		Assert.assertEquals(knownStopArea.getTransportSubMode(), TransportSubModeNameEnum.TouristRailway);
	}


	@Test
	public void testStopAreaUpdateForMultiModalStop() throws Exception {
		cleanAllschemas();
		ContextHolder.setContext("chouette_gui"); // set tenant schema
		stopAreaDAO.truncate();
		utx.begin();
		em.joinTransaction();

		String parentName = "Super stop place name";

		stopAreaService.createOrUpdateStopPlacesFromNetexStopPlaces(new FileInputStream("src/test/data/StopAreasMultiModalImport.xml"));

		utx.commit();
		utx.begin();

		ContextHolder.setContext("tro");
		StopArea stopAreaParent = assertStopPlace("NSR:StopPlace:4000");
		Assert.assertEquals(stopAreaParent.getName(), parentName);

		StopArea stopAreaChild1 = assertStopPlace("NSR:StopPlace:1000", "NSR:Quay:1000");
		Assert.assertEquals(stopAreaChild1.getParent(), stopAreaParent, "Expected child to have parent set");
		Assert.assertEquals(stopAreaChild1.getName(), parentName, "Expected child to get parents name");
		StopArea stopAreaChild2 = assertStopPlace("NSR:StopPlace:2000");
		Assert.assertEquals(stopAreaChild2.getParent(), stopAreaParent, "Expected child to have parent set");
		Assert.assertEquals(stopAreaChild2.getName(), parentName, "Expected child to get parents name");


		stopAreaService.createOrUpdateStopPlacesFromNetexStopPlaces(new FileInputStream("src/test/data/StopAreasMultiModalRemoval.xml"));

		utx.commit();
		utx.begin();

		ContextHolder.setContext("tro");

		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:StopPlace:4000"), "Did not expect to find deactivated parent stop place");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:StopPlace:2000"), "Did not expect to find stop with deactivated parent ");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:StopPlace:1000"), "Did not expect to find stop with deactivated parent");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:Quay:1000"), "Did not expect to find quay with deactivated stop place parent");

		utx.rollback();
	}

	@Test
	public void deleteExistingBoardingPositionsNoLongerValidForStopOnlyIfInSameCodeSpaceAsStop() throws Exception {

		cleanAllschemas();
		initProducers();
		ContextHolder.setContext("tro"); // set tenant schema
		ContextHolder.setDefaultSchema("tro");
		stopAreaDAO.truncate();

		StopArea bpInStopCodeSpace = new StopArea();
		bpInStopCodeSpace.setAreaType(ChouetteAreaEnum.BoardingPosition);
		bpInStopCodeSpace.setObjectId("NSR:Quay:1");

		StopArea bpInAnotherCodeSpace = new StopArea();
		bpInAnotherCodeSpace.setAreaType(ChouetteAreaEnum.BoardingPosition);
		bpInAnotherCodeSpace.setObjectId("SKY:Quay:2");

		StopArea commercialStop = new StopArea();
		commercialStop.setAreaType(ChouetteAreaEnum.CommercialStopPoint);
		commercialStop.setObjectId("NSR:StopPlace:1");

		bpInAnotherCodeSpace.setParent(commercialStop);
		bpInStopCodeSpace.setParent(commercialStop);

		stopAreaDAO.create(commercialStop);


		utx.begin();
		em.joinTransaction();

		stopAreaService.createOrUpdateStopPlacesFromNetexStopPlaces(new FileInputStream("src/test/data/StopAreasDeleteExistingBoardingPositionsNoLongerValidForStopOnlyIfInSameCodeSpaceAsStop.xml"));
		utx.commit();

		utx.begin();

		ContextHolder.setContext("tro"); //
		Assert.assertNull(stopAreaDAO.findByObjectId(bpInStopCodeSpace.getObjectId()), "Did not expect to find NSR quay no longer in latest version of stop");

		utx.rollback();
	}

	@Test
	public void testDeleteStopAreaWithQuays() throws Exception {
		cleanAllschemas();
		ContextHolder.setContext("tro"); // set tenant schema
		ContextHolder.setDefaultSchema("chouette_gui");
		stopAreaDAO.truncate();

		String stopAreaId = "NSR:StopPlace:1";
		utx.begin();
		em.joinTransaction();

		stopAreaService.createOrUpdateStopPlacesFromNetexStopPlaces(new FileInputStream("src/test/data/StopAreasInitialSynch.xml"));

		utx.commit();

		utx.begin();
		ContextHolder.setContext("tro");
		assertStopPlace(stopAreaId, "NSR:Quay:1a", "NSR:Quay:1b");

		stopAreaService.deleteStopArea(stopAreaId);

		Assert.assertNull(stopAreaDAO.findByObjectId(stopAreaId));
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:Quay:1a"), "Expected quay to have been cascade deleted");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:Quay:1b"), "Expected quay to have been cascade deleted");

		utx.rollback();
	}


	@Test
	public void testDeleteUnusedStopAreas() throws Exception {

		cleanAllschemas();
		ContextHolder.setContext("chouette_gui"); // set tenant schema
		utx.begin();
		em.joinTransaction();


		stopAreaService.createOrUpdateStopPlacesFromNetexStopPlaces(new FileInputStream("src/test/data/StopAreasInitialSynch.xml"));

		StopPlaceRegistryIdFetcherMock idfetcher = new StopPlaceRegistryIdFetcherMock();
		System.setProperty("iev.superspace.prefix", "mobiiti");
		Set<String> quayIds = new HashSet<>();
		quayIds.add("MOBIITI:Quay:12345");
		idfetcher.setQuayIds(quayIds);
		stopAreaUpdateService.setStopPlaceRegistryIdFetcher(idfetcher);
		stopAreaService.setStopAreaUpdateService(stopAreaUpdateService);


		stopAreaService.deleteUnusedStopAreas();


		utx.commit();
		utx.begin();
		ContextHolder.setContext("tro");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:Quay:1a"), "Expected unused stop area to be deleted");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:Quay:1b"), "Expected unused stop area to be deleted");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:Quay:2a"), "Expected unused stop area to be deleted");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:Quay:3a"), "Expected unused stop area to be deleted");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:StopPlace:1"), "Expected unused stop area to be deleted");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:StopPlace:2"), "Expected unused stop area to be deleted");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:StopPlace:3"), "Expected unused stop area to be deleted");



		utx.commit();
		utx.begin();
		ContextHolder.setContext("sky");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:Quay:5"), "Expected unused stop area to be deleted");
		Assert.assertNull(stopAreaDAO.findByObjectId("NSR:StopPlace:5"), "Expected unused stop area to be deleted");
	}

	private void cleanStopPoints() {
		stopPointDAO.truncate();
		scheduledStopPointDAO.truncate();
	}


	private StopArea commercialStopWithTwoBoardingPositions(String id) {
		StopArea bp1 = new StopArea();
		bp1.setAreaType(ChouetteAreaEnum.BoardingPosition);
		bp1.setObjectId("SKY:Quay:" + id + "a");

		StopArea bp2 = new StopArea();
		bp2.setAreaType(ChouetteAreaEnum.BoardingPosition);
		bp2.setObjectId("SKY:Quay:" + id + "b");

		StopArea commercialStop = new StopArea();
		commercialStop.setAreaType(ChouetteAreaEnum.CommercialStopPoint);
		commercialStop.setObjectId("SKY:StopPlace:" + id);

		bp1.setParent(commercialStop);
		bp2.setParent(commercialStop);

		stopAreaDAO.create(commercialStop);
		return commercialStop;
	}


	private StopArea assertStopPlace(String stopPlaceId, String... quayIds) {
		StopArea stopPlace = stopAreaDAO.findByObjectId(stopPlaceId);
		Assert.assertNotNull(stopPlace, "Expected to find stop place with known id: " + stopPlaceId);
		if (quayIds != null) {

			for (String quayId : quayIds) {
				StopArea quay = stopAreaDAO.findByObjectId(quayId);
				Assert.assertNotNull(quay, "Expected stop to have quay with known id: " + quayId);
				Assert.assertEquals(quay.getParent(), stopPlace);
			}
		}

		return stopPlace;
	}

	private StopPoint createStopPoint(String id, StopArea containedStopArea) {
		StopPoint sp = new StopPoint();
		sp.setObjectId("XXX:StopPoint:" + id);

		ScheduledStopPoint scheduledStopPoint = new ScheduledStopPoint();
		scheduledStopPoint.setObjectId("XXX:ScheduledStopPoint:" + id);

		scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference(containedStopArea));
		sp.setScheduledStopPoint(scheduledStopPoint);
		stopPointDAO.create(sp);
		return sp;
	}

	@Test
	public void testFeedOriginalStopIdAfterRestore() throws Exception {
		cleanAllschemas();
		ContextHolder.setContext("tro"); // set tenant schema
		stopAreaDAO.truncate();
		utx.begin();
		em.joinTransaction();



		stopAreaService.createOrUpdateStopPlacesFromNetexStopPlaces(new FileInputStream("src/test/data/StopAreasOriginalStopIdTest.xml"));

		utx.commit();
		utx.begin();

		ContextHolder.setContext("tro");
		StopArea createdParent = assertStopPlace("NSR:StopPlace:1000","NSR:Quay:1000");

		Assert.assertTrue(createdParent.getOriginalStopId() != null, "Original stop id should have been feeded during restoration with imported-id from xml");
		Assert.assertEquals(createdParent.getOriginalStopId(),"14758","Original stop id should be equal to imported id from the xml file");

		StopArea createdQuay = createdParent.getContainedStopAreas().get(0);
		Assert.assertTrue(createdQuay.getOriginalStopId() != null, "Original stop id should have been feeded during restoration with imported-id from xml");
		Assert.assertEquals(createdQuay.getOriginalStopId(),"12345","Original stop id should be equal to imported id from the xml file");

		utx.commit();
		utx.begin();

		ContextHolder.setContext("sky");
		StopArea createdParentOnSecondSchema = assertStopPlace("NSR:StopPlace:1000","NSR:Quay:1000");

		Assert.assertTrue(createdParentOnSecondSchema.getOriginalStopId() != null, "Original stop id should have been feeded during restoration with imported-id from xml");
		Assert.assertEquals(createdParentOnSecondSchema.getOriginalStopId(),"89632","Original stop id should be equal to imported id from the xml file");

		StopArea createdQuayOnSecondSchema = createdParentOnSecondSchema.getContainedStopAreas().get(0);
		Assert.assertTrue(createdQuayOnSecondSchema.getOriginalStopId() != null, "Original stop id should have been feeded during restoration with imported-id from xml");
		Assert.assertEquals(createdQuayOnSecondSchema.getOriginalStopId(),"56374","Original stop id should be equal to imported id from the xml file");

	}



}

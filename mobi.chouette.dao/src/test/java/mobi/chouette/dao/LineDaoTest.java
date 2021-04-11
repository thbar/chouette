package mobi.chouette.dao;

import java.io.File;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import mobi.chouette.model.Line;
import mobi.chouette.persistence.hibernate.ContextHolder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.Assert;
import org.testng.annotations.Test;


public class LineDaoTest extends Arquillian
{
	@EJB 
	LineDAO lineDao;

	@PersistenceContext(unitName = "referential")
	EntityManager em;

	@Resource
	private UserTransaction trx;


	@Deployment
	public static WebArchive createDeployment() {

		try
		{
		WebArchive result;
		File[] files = Maven.resolver().loadPomFromFile("pom.xml")
				.resolve("mobi.chouette:mobi.chouette.dao").withTransitivity().asFile();

		result = ShrinkWrap.create(WebArchive.class, "test.war").addAsWebInfResource("postgres-ds.xml")
				.addAsLibraries(files).addAsResource(EmptyAsset.INSTANCE, "beans.xml");
		return result;
		}
		catch (RuntimeException e)
		{
			System.out.println(e.getClass().getName());
			throw e;
		}

	}
	
	@Test
	public void checkSequence() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
		ContextHolder.setContext("chouette_gui"); // set tenant schema
		lineDao.truncate();

		trx.begin();

		em.createNativeQuery("ALTER SEQUENCE chouette_gui.lines_id_seq RESTART WITH 1").executeUpdate();

		trx.commit();

		for (int i = 0; i < 300; i++)
		{
			Line l = createLine();
			lineDao.create(l);
			Assert.assertEquals(l.getId(), Long.valueOf(i+1),"line id");
		}
	}
	
	private int id = 1;
	private Line createLine()
	{
		Line l = new Line();
		l.setName("toto");
		l.setObjectId("test:Line:"+id);
		id++;
		
		return l;
	}
	

}

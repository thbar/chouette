package mobi.chouette.dao;

import com.google.errorprone.annotations.Var;
import mobi.chouette.model.Line;
import mobi.chouette.model.Variations;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ejb.EJB;
import java.io.File;
import java.util.List;

public class VariationsDaoTest extends Arquillian {

    @EJB
    VariationsDAO variationsDAO;

    @EJB
    LineDAO lineDAO;

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
    public void checkSequence()
    {
        ContextHolder.setContext("chouette_gui"); // set tenant schema
        for (int i = 0; i < 300; i++)
        {
            Variations v = createVariations();
            variationsDAO.create(v);
            Assert.assertEquals(v.getId(), Long.valueOf(i+1),"variations id");
        }
    }

    private int id = 1;
    private Variations createVariations()
    {
        Variations v = new Variations();
        v.setType("toto");
        v.setDescription("eee");
        id++;

        return v;
    }


}

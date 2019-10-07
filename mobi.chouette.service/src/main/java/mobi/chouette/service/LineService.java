package mobi.chouette.service;

import mobi.chouette.dao.LineDAO;
import mobi.chouette.model.Line;
import mobi.chouette.persistence.hibernate.ContextHolder;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.util.List;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@ConcurrencyManagement(BEAN)
@Singleton(name = LineService.BEAN_NAME)
public class LineService {

    public static final String BEAN_NAME = "LineService";

    @EJB
    private LineDAO lineDAO;

    public List<Line> getLines(String referential) {
        ContextHolder.setContext(referential);
        return lineDAO.findAll();
    }

}

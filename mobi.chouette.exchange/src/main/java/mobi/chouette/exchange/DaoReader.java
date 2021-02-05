package mobi.chouette.exchange;

import mobi.chouette.dao.CompanyDAO;
import mobi.chouette.dao.GroupOfLineDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.NetworkDAO;
import mobi.chouette.dao.OperatorDAO;
import mobi.chouette.model.Company;
import mobi.chouette.model.GroupOfLine;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import mobi.chouette.model.Operator;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.*;
import java.util.stream.Collectors;

@Stateless 
public class DaoReader {

	@EJB 
	protected LineDAO lineDAO;

	@EJB 
	protected NetworkDAO ptNetworkDAO;

	@EJB 
	protected CompanyDAO companyDAO;

	@EJB 
	protected GroupOfLineDAO groupOfLineDAO;

	@EJB
	protected OperatorDAO operatorDAO;


	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Set<Long> loadLines(String type, List<Long> ids) {
		Set<Line> lines = new HashSet<Line>();
		if (ids == null || ids.isEmpty()) {
			lines.addAll(lineDAO.findAll());
		} else {
			if (type.equals("line")) {
				lines.addAll(lineDAO.findAll(ids));
			} else if (type.equals("network")) {
				List<Network> list = ptNetworkDAO.findAll(ids);
				for (Network ptNetwork : list) {
					lines.addAll(ptNetwork.getLines());
				}
			} else if (type.equals("company")) {
				List<Company> list = companyDAO.findAll(ids);
				for (Company company : list) {
					lines.addAll(company.getLines());
				}
			} else if (type.equals("group_of_line")) {
				List<GroupOfLine> list = groupOfLineDAO.findAll(ids);
				for (GroupOfLine groupOfLine : list) {
					lines.addAll(groupOfLine.getLines());
				}
			}
		}
		// ordonnancement des lignes
		LineComparator lineComp = new LineComparator();

		return lines.stream()
				     .sorted(lineComp)
				     .map(Line::getId)
				     .collect(Collectors.toCollection(LinkedHashSet::new));
	}


	public class LineComparator implements Comparator<Line> {

		@Override
		public int compare(Line o1, Line o2) {

			if (o1.getPosition() != null && o2.getPosition() != null){
				return o1.getPosition().compareTo(o2.getPosition());
			}

			if (o1.getPublishedName() != null && o2.getPublishedName() != null){
				return o1.getPublishedName().compareTo(o2.getPublishedName());
			}
			return o1.getObjectId().compareTo(o2.getObjectId());
		}
	}


	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public List<Operator> loadOperators() {
		List<Operator> operators = operatorDAO.findAll();
		return operators;
	}

}

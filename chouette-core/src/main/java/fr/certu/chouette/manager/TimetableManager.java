/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package fr.certu.chouette.manager;

import org.apache.log4j.Logger;

import fr.certu.chouette.model.neptune.Timetable;

/**
 * @author michel
 *
 */
public class TimetableManager extends AbstractNeptuneManager<Timetable> {

	public TimetableManager() {
		super(Timetable.class);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Logger getLogger() {
		// TODO Auto-generated method stub
		return null;
	}
}

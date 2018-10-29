package mobi.chouette.service;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.TimetableDAO;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.statistics.*;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateMidnight;
import org.joda.time.LocalDate;
import org.rutebanken.helper.calendar.CalendarPattern;
import org.rutebanken.helper.calendar.CalendarPatternAnalyzer;

import javax.ejb.*;
import java.util.*;
import java.util.stream.Collectors;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@ConcurrencyManagement(BEAN)
@Singleton(name = TransitDataStatisticsService.BEAN_NAME)
@Log4j
public class TransitDataStatisticsService {

	public static final String BEAN_NAME = "TransitDataStatisticsService";

	@EJB
	LineDAO lineDAO;

	@EJB
	TimetableDAO timetableDAO;

	/**
	 * Returns a list of Lines grouped by Line "number". Create merged timetable
	 * periods. Not supporting frequency based yet
	 *
	 * @param referential
	 * @param startDate
	 *            the first date to return data from (that is, filter away old
	 *            and obsolete periods
	 * @param minDaysValidityCategories
	 *            organize lineNumbers into validity categories. First category
	 *            is from 0 to lowest key value. Corresponding value in map is
	 *            used as categegory name,
	 * @return
	 * @throws ServiceException
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public LineStatistics getLineStatisticsByLineNumber(String referential, Date startDate, int days,
			Map<Integer, String> minDaysValidityCategories) throws ServiceException {

		ContextHolder.setContext(referential);

		log.debug(Color.CYAN+ "Gettings statistics for "
				+ referential
				+ " using startDate="
				+ startDate
				+ " and minDaysValidityCategories="
				+ ToStringBuilder.reflectionToString(minDaysValidityCategories));

		// Defaulting to today if not given
		if (startDate == null) {
			startDate = new DateMidnight().toDate();
		}

		Map<String, PublicLine> publicLines = new HashMap<String, PublicLine>();

		// Convert Chouette internal model to the statistics model used
		convertChouetteModelToStatisticsModel(startDate, publicLines);

		// If Line->Timetable->Period is empty, remove Line but keep publicLine
		filterLinesWithEmptyTimetablePeriods(publicLines);

		// Merge overlapping periods in PublicLine for readability
		mergePeriods(publicLines);

		List<PublicLine> pL = new ArrayList<>(publicLines.values());
		Collections.sort(pL);

		LineStatistics lineStats = new LineStatistics(startDate,days,pL);
		// Put lineNumbers into buckets depending on validity in the future
		categorizeValidity(lineStats, startDate, minDaysValidityCategories);

		// Merge identical names to display in PublicLines
		mergeNames(lineStats);

		return lineStats;
	}

	private void mergeNames(LineStatistics lineStats) {
		for (PublicLine pl : lineStats.getPublicLines()) {
			Set<String> names = new TreeSet<String>();
			for (Line l : pl.getLines()) {
				if (l.getName() != null) {
					names.add(l.getName());
				} else {
					log.warn("Ignored line with null name: " + l.getObjectId());
				}
			}

			pl.getLineNames().addAll(names);
		}
	}

	void categorizeValidity(LineStatistics lineStats, Date startDate, Map<Integer, String> minDaysValidityCategories) {

		ValidityCategory defaultCategory = new ValidityCategory(getCategoryName(minDaysValidityCategories, 0, "EXPIRING"), 0, new ArrayList<>());
		ValidityCategory invalidCategory = new ValidityCategory(getCategoryName(minDaysValidityCategories, -1, "INVALID"), -1, new ArrayList<>());
		List<ValidityCategory> validityCategories = new ArrayList<>();

		List<Integer> categories = new ArrayList<>(minDaysValidityCategories.keySet());
		Collections.sort(categories);
		Collections.reverse(categories);

		for (Integer numDays : categories) {
			if (numDays > 0) {
				ValidityCategory category = new ValidityCategory(getCategoryName(minDaysValidityCategories, numDays, numDays.toString()), numDays,
						new ArrayList<>());
				validityCategories.add(category);
			}
		}

		LocalDate startDateLocal = new LocalDate(startDate);

		for (PublicLine pl : lineStats.getPublicLines()) {
			boolean foundCategory = false;
			for (int i = 0; i < validityCategories.size(); i++) {
				ValidityCategory vc = validityCategories.get(i);

				foundCategory = isValidAtLeastNumberOfDays(pl, startDateLocal, vc.getNumDaysAtLeastValid());
				if (foundCategory) {
					vc.getLineNumbers().add(pl.getLineNumber());
					break;
				}

			}

			if (!foundCategory) {
				if (isValidAtLeastNumberOfDays(pl, startDateLocal, 0)) {
					defaultCategory.getLineNumbers().add(pl.getLineNumber());
				} else {
					invalidCategory.getLineNumbers().add(pl.getLineNumber());
				}
			}
		}

		lineStats.getValidityCategories().add(invalidCategory);
		lineStats.getValidityCategories().add(defaultCategory);
		lineStats.getValidityCategories().addAll(validityCategories);
	}

	private String getCategoryName(Map<Integer, String> minDaysValidityCategories, Integer limit, String defaultName) {
		String name = minDaysValidityCategories.get(limit);
		if (name != null) {
			return name;
		}
		return defaultName;
	}

	private boolean isValidAtLeastNumberOfDays(PublicLine pl, LocalDate startDateLocal, Integer numDays) {
		if (pl.getEffectivePeriods().size() > 0) {
			Date limitDate = startDateLocal.plusDays(numDays).toDate();
			return pl.getEffectivePeriods().stream().anyMatch(p -> !p.getFrom().after(startDateLocal.toDate()) && !p.getTo().before(limitDate));
		}

		return false;
	}

	protected void mergePeriods(Map<String, PublicLine> publicLines) {
		for (PublicLine pl : publicLines.values()) {
			Set<Period> uniquePeriods = new TreeSet<Period>();
			for (Line l : pl.getLines()) {
				for (Timetable t : l.getTimetables()) {
					t.setPeriods(mergeOverlappingPeriods(t.getPeriods()));
					uniquePeriods.addAll(t.getPeriods());
				}
			}

			pl.getEffectivePeriods().addAll(uniquePeriods);
			pl.setEffectivePeriods(mergeOverlappingPeriods(pl.getEffectivePeriods()));
		}
	}

	protected void convertChouetteModelToStatisticsModel(Date startDate, Map<String, PublicLine> publicLines) {
		// Load list of lineIds with corresponding Timetables
		long now = System.currentTimeMillis();
		Collection<LineAndTimetable> allTimetableForAllLines = timetableDAO.getAllTimetableForAllLines();
		log.debug("Timetables took " + (System.currentTimeMillis() - now) + "ms");

		// Find all ids and load all Chouette Lines
		Set<Long> lineIds = new HashSet<>();
		for (LineAndTimetable lat : allTimetableForAllLines) {
			lineIds.add(lat.getLineId());
		}
		now = System.currentTimeMillis();
		List<mobi.chouette.model.Line> lines = lineDAO.findAll(lineIds);
		log.debug("Lines took " + (System.currentTimeMillis() - now) + "ms");

		Map<Long, mobi.chouette.model.Line> lineIdToLine = new HashMap<>();
		for (mobi.chouette.model.Line l : lines) {
			lineIdToLine.put(l.getId(), l);
		}

		Map<String, String> lineNameToFakeLineNumber = new HashMap<>();

		int fakeLineNumberCounter = 0;

		for (LineAndTimetable lat : allTimetableForAllLines) {
			mobi.chouette.model.Line l = lineIdToLine.get(lat.getLineId());

			String number = StringUtils.trimToNull(l.getNumber());
			if (number == null) {
				String lineNameKey = l.getName() + "-" + l.getCompany().getName();
				number = lineNameToFakeLineNumber.get(lineNameKey);
				if (number == null) {
					number = "<" + (++fakeLineNumberCounter) + ">";
					lineNameToFakeLineNumber.put(lineNameKey, number);
				}
			}
			l.setNumber(number);

			PublicLine publicLine = publicLines.get(l.getNumber());
			if (publicLine == null) {
				publicLine = new PublicLine(l.getNumber());
				publicLines.put(l.getNumber(), publicLine);
			}

			Line line = new Line(l.getId(), l.getObjectId(), l.getName());
			publicLine.getLines().add(line);

			Set<CalendarDay> calendarDaysForLine = new HashSet<>();

			Timetable timetableForCalendarDays = null;
			
			boolean foundStartEndDateOfTimetable = false;
			
			
			for (mobi.chouette.model.Timetable t : lat.getTimetables()) {
				Timetable timetable = new Timetable(t.getId(), t.getObjectId());

				line.getTimetables().add(timetable);

				if (t.getStartOfPeriod() != null && t.getEndOfPeriod() != null) {
					timetable.getPeriods().add(new Period(t.getStartOfPeriod().toDate(), t.getEndOfPeriod().toDate()));
					foundStartEndDateOfTimetable = true;
				} else {

					if (t.getPeriods() != null && t.getPeriods().size() > 0) {
						// Use periods
						for (mobi.chouette.model.Period p : t.getPeriods()) {
							Period period = new Period(p.getStartDate().toDate(), p.getEndDate().toDate());
							if (!period.isEmpty() && !period.getTo().before(startDate)) {
								// log.info("Adding normal period " + p);
								timetable.getPeriods().add(period);
							}
						}
					}

					if (t.getCalendarDays() != null) {
						for (CalendarDay day : t.getCalendarDays()) {
							if (day.getIncluded() && !startDate.after(day.getDate().toDate())) {
								timetable.getPeriods().add(new Period(day.getDate().toDate(), day.getDate().toDate()));
								calendarDaysForLine.add(day);
								timetableForCalendarDays = timetable;
							}
						}
					}

					if (timetable.getPeriods().isEmpty()) {
						// Use timetable from/to as period
						t.computeLimitOfPeriods();
						Period period = new Period(t.getStartOfPeriod().toDate(), t.getEndOfPeriod().toDate());

						// TODO could be separate days here as well that should
						// be
						// included
						if (!period.isEmpty() && !period.getTo().before(startDate)) {
							// log.info("Adding timetable period " + period);
							timetable.getPeriods().add(period);
						} else {
							log.debug("No from/to in timetable objectId=" + t.getObjectId() + " id=" + t.getId());
						}

					}
				}

                List<mobi.chouette.model.Period> periodsToDelete = new ArrayList<>();
				if(t.getPeriods() != null && t.getPeriods().size() > 0){
				    for(mobi.chouette.model.Period p : t.getPeriods()){
				        if(t.getCalendarDays() != null){
				            for (CalendarDay dayExcluded : t.getCalendarDays()){
				                if(!dayExcluded.getIncluded() && startDate.equals(dayExcluded.getDate().toDate())){
                                    periodsToDelete.add(p);
				                    Calendar cal = Calendar.getInstance();
				                    cal.setTime(startDate);
				                    cal.add(Calendar.DATE, 1);
                                    timetable.getPeriods().add(new Period(cal.getTime(), p.getEndDate().toDate()));
                                }
                                else if(!dayExcluded.getIncluded() && p.getEndDate().equals(dayExcluded.getDate())){
                                    periodsToDelete.add(p);
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(startDate);
                                    cal.add(Calendar.DATE, -1);
                                    timetable.getPeriods().add(new Period(startDate, cal.getTime()));
                                }
                                else if(!dayExcluded.getIncluded() && !p.getStartDate().equals(dayExcluded.getDate().toDate()) && !p.getEndDate().equals(dayExcluded.getDate().toDate())){
                                    periodsToDelete.add(p);
                                    Calendar calEnd = Calendar.getInstance();
                                    Calendar calStart = Calendar.getInstance();

                                    calEnd.setTime(dayExcluded.getDate().toDate());
                                    calEnd.add(Calendar.DATE, -1);

                                    timetable.getPeriods().add(new Period(startDate, calEnd.getTime()));

                                    calStart.setTime(dayExcluded.getDate().toDate());
                                    calStart.add(Calendar.DATE, 1);

                                    timetable.getPeriods().add(new Period(calStart.getTime(), p.getEndDate().toDate()));
                                }
                            }
                        }
                    }
                }

                if(periodsToDelete != null && periodsToDelete.size() > 0){
                    timetable.getPeriods().removeAll(periodsToDelete);
                }

			}

			if (!foundStartEndDateOfTimetable) {
				// Legacy after reimport?
				Period fromCalendarDaysPattern = calculatePeriodFromCalendarDaysPattern(calendarDaysForLine);

				if (fromCalendarDaysPattern != null) {
					log.debug("Successfully created validity interval from included days for line: " + line.getId());
					timetableForCalendarDays.getPeriods().add(fromCalendarDaysPattern);
				}
			}

		}
	}

	private Period calculatePeriodFromCalendarDaysPattern(Collection<CalendarDay> calendarDays) {

		Set<java.time.LocalDate> includedDays = calendarDays.stream().filter(CalendarDay::getIncluded)
				.map(c -> java.time.LocalDate.of(c.getDate().getYear(),c.getDate().getMonthOfYear(),c.getDate().getDayOfMonth())).collect(Collectors.toSet());

		CalendarPattern pattern = new CalendarPatternAnalyzer().computeCalendarPattern(includedDays);

		if (pattern != null) {
			Date from = java.sql.Date.valueOf(pattern.from);
			Date to = java.sql.Date.valueOf(pattern.to);
			return new Period(from, to);
		}
		return null;
	}

	protected void filterLinesWithEmptyTimetablePeriods(Map<String, PublicLine> publicLines) {

		List<PublicLine> filteredPublicLines = new ArrayList<>();

		for (PublicLine pl : publicLines.values()) {
			List<Line> filteredLines = new ArrayList<>();
			for (Line l : pl.getLines()) {
				List<Timetable> filteredTimetables = new ArrayList<>();

				for (Timetable t : l.getTimetables()) {
					if (t.getPeriods().size() > 0) {
						filteredTimetables.add(t);
					}
				}

				l.getTimetables().clear();
				l.getTimetables().addAll(filteredTimetables);

				if (l.getTimetables().size() > 0) {
					filteredLines.add(l);
				}
			}

			pl.getLines().clear();
			pl.getLines().addAll(filteredLines);

			if (pl.getLines().size() > 0) {
				filteredPublicLines.add(pl);
			}

		}

		// List<String> lineNumbersToRemove = new ArrayList<>();
		//
		// for(String lineNumber : publicLines.keySet()) {
		// if(publicLines.get(lineNumber).getLines().size() == 0) {
		// lineNumbersToRemove.add(lineNumber);
		// }
		// }
		//
		// for(String lineNumber : lineNumbersToRemove) {
		// publicLines.remove(lineNumber);
		// }

	}

	public List<Period> mergeOverlappingPeriods(List<Period> intervals) {

		if (intervals.size() == 0 || intervals.size() == 1)
			return intervals;

		Collections.sort(intervals, new Comparator<Period>() {

			@Override
			public int compare(Period o1, Period o2) {
				return o1.getFrom().compareTo(o2.getFrom());
			}
		});

		Period first = intervals.get(0);
		Date start = first.getFrom();
		Date end = first.getTo();

		List<Period> result = new ArrayList<Period>();

		for (int i = 1; i < intervals.size(); i++) {
			Period current = intervals.get(i);
			if (!current.getFrom().after(DateUtils.addDays(end, 1))) {
				end = current.getTo().before(end) ? end : current.getTo();
			} else {
				result.add(new Period(start, end));
				start = current.getFrom();
				end = current.getTo();
			}
		}

		result.add(new Period(start, end));
		return result;
	}

}

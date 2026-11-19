package model;

import java.time.LocalDateTime;

/*
 * DateRange enum
 * 	- include Any Time, Today, Last 7 Days, This month
 * 	- to be used in the GUI for filtering search
 */
public enum DateRange {
	
	// TYPES
	ANY_TIME("Any time"),
	TODAY("Today"),
	LAST_7_DAYS("Last 7 days"),
	THIS_MONTH("This month");
	
	// METHODS
	/*
	 * DateRange(String)
	 * 	- constructor method
	 */
	private final String label;
	DateRange(String label) {
		this.label = label;
	} // end of constructor method

	/*
	 * contains(LocalDateTime, LocalDateTime)
	 * 	- returns true if the "now" falls inside the range of timestamp
	 */
	public boolean contains(LocalDateTime timestamp, LocalDateTime now) {
		
		if (timestamp == null) {
			return this == ANY_TIME;
		}
		switch (this) {
			case TODAY:
				return timestamp.toLocalDate().equals(now.toLocalDate());
			case LAST_7_DAYS:
				return !timestamp.toLocalDate().isBefore(now.toLocalDate().minusDays(6))
						&& !timestamp.isAfter(now);
			case THIS_MONTH:
				return timestamp.getYear() == now.getYear() && timestamp.getMonth() == now.getMonth();
			default:
				return true;
		}
		
	} // end of contains
	
	/*
	 * toString()
	 */
	@Override
	public String toString() {return label;}
	
} // end of DateRange enum
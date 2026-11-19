package model;

import java.time.format.DateTimeFormatter;

/*
 * DateFormats class
 * 	- to be used for filtering search in gui
 */
public final class DateFormats {
	
	// ATTRIBUTES
	public static final DateTimeFormatter AS_OF = DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm"); // November 19, 2026 17:36
	public static final DateTimeFormatter DATE_TIME_SECONDS = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss"); // 11/14/26 17:36:00
	public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm"); // 11/19/26 17:36
	public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm"); // 17:36
	
	// CONSTRUCTOR METHOD
	private DateFormats() {}
	
} // end of DateFormats
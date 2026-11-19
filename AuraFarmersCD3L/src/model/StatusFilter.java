package model;

/*
 * StatusFilter enum
 * 	- to be used for filtering search in gui
 */
public enum StatusFilter {
	
	// TYPES
	ALL("All"),
	ACTIVE("Active"),
	INACTIVE("Inactive");

	private final String label;
	StatusFilter(String label) {this.label = label;}

	// true if a product with that active flag passes this filter
	public boolean matches(boolean productIsActive) {
		return this == ALL || (this == ACTIVE) == productIsActive;
	}

	@Override
	public String toString() {return label;}
} // end of StatusFilter enum
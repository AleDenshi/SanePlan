import java.util.ArrayList;

public class Course {
	// Basic information
	private String code;
	private int credits;
	private String name;
	private String description;
	private ArrayList<SemesterType> availability;

	// Requisites
	private ArrayList<ArrayList<Course>> preRequisites;
	private ArrayList<ArrayList<Course>> coRequisites;
	
	// Getters
	public ArrayList<ArrayList<Course>> getPreRequisites() {
		return preRequisites;
	}

	public ArrayList<ArrayList<Course>> getCoRequisites() {
		return coRequisites;
	}
	public Course(String code, int credits, String name, String description, ArrayList<SemesterType> availability) {
		this.code = code;
		this.credits = credits;
		this.name = name;
		this.description = description;
		this.availability = availability;

		// Construct Set and ArrayLists
		this.preRequisites = new ArrayList<ArrayList<Course>>();
		this.coRequisites = new ArrayList<ArrayList<Course>>();
	}

	public void addAvailability(SemesterType availability) {
		if (availability == null) return;
		this.availability.add(availability);
	}

	public void addPreRequisite(ArrayList<Course> preRequisite) {
		if (preRequisite != null)
			this.preRequisites.add(preRequisite);
	}

	public void addCoRequisite(ArrayList<Course> coRequisite) {
		if (coRequisite != null)
			this.coRequisites.add(coRequisite);
	}

	// TODO: add further getters/setters/replacers for Course editing
	
	public String getCode() {
		return code;
	}

	@Override
	public String toString() {
		String overall = String.format("%s (%d): %s\n", code, credits, name);
		overall += "Availabile: " + availabilityToString() + "\n";
		overall += "Pre-Requisites: " + requisitesToString(preRequisites) + "\n";
		overall += "Co-Requisites: " + requisitesToString(coRequisites) + "\n";
		return overall;
	}
	
	private String availabilityToString() {
		String value = "[";
		for (int j = 0; j < availability.size(); j++) {
			SemesterType type = availability.get(j);
			if (type == null) {
				value += " NULL";
			} else {
				value += "\"" + type.toString() + "\"";
			}
			if (j < availability.size() - 1) value += ", ";
		}
		value += "]";
		return value;
	}

	private String requisitesToString(ArrayList<ArrayList<Course>> requisites) {
		String value = "[";
		for (int i = 0; i < requisites.size(); i++) {
			value += "[";
			ArrayList<Course> requisiteList = requisites.get(i);
			for (int j = 0; j < requisiteList.size(); j++) {
				Course requisite = requisiteList.get(j);
				if (requisite == null) {
					value += " \"NULL\"";
				} else {
					value += "\"" + requisite.getCode() + "\"";
				}
				if (j < requisiteList.size() - 1) value += ", ";
			}
			value += "]";
			if (i < requisites.size() - 1) value += ", "; 
		}
		value += "]";
		return value;
	}
	
	public String toJson() {
        return "{"
                + "\"code\": \"" + code + "\", "
                + "\"name\": \"" + name + "\", "
                + "\"credits\": " + credits + ", "
                + "\"description\": \"" + description + "\", "
                + "\"availability\": " + availabilityToString() + ", "
                + "\"preRequisites\": " + requisitesToString(preRequisites) + ", "
                + "\"coRequisites\": " + requisitesToString(coRequisites)
                + "}";
    }

	public int getCredits() {
		return credits;
	}

}

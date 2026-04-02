import java.util.ArrayList;

public class Course {

	private String code;
	private int credits;
	private String name;
	private String description;
	private ArrayList<SemesterType> availability;

	private ArrayList<ArrayList<Course>> preRequisites;
	private ArrayList<ArrayList<Course>> coRequisites;

	/**
	 * Constructs a Course with required parameters.
	 * 
	 * @param code         - A String representing the Course's code (eg. CS 225)
	 * @param credits      - The number of credits for the Course (eg. 3)
	 * @param name         - The human name for the Course (eg. Computer Science II)
	 * @param description  - The human description for the Course
	 * @param availability - An ArrayList of availabilities for the Course (eg.
	 *                     FALL, SPRING, SUMMER)
	 */
	public Course(String code, int credits, String name, String description, ArrayList<SemesterType> availability) {
		this.code = code;
		this.credits = credits;
		this.name = name;
		this.description = description;
		this.availability = availability;

		this.preRequisites = new ArrayList<ArrayList<Course>>();
		this.coRequisites = new ArrayList<ArrayList<Course>>();
	}
	
	/**
	 * Determines whether a course is available in a semester type.
	 * 
	 * @param type - The semester type being examined (eg. FALL, SPRING...)
	 * @return Whether or not the course is available that semester
	 */
	public boolean isType(SemesterType type) {
		return availability.contains(type);
	}

	/**
	 * Checks whether two Courses have an identical Course code.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof Course) {
			if (((Course) o).getCode() == this.code) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return this.code;
	}

	public String toJson() {
		JSONWriter js = new JSONWriter();
		return "{\n"
				+ "\"code\": \"" + code + "\",\n "
				+ "\"name\": \"" + name + "\",\n "
				+ "\"credits\": " + credits + ",\n "
				+ "\"description\": \"" + description + "\",\n "
				+ "\"availability\": " + js.listToJson(availability) + ",\n "
				+ "\"preRequisites\": " + js.listListToJson(preRequisites) + ",\n "
				+ "\"coRequisites\": " + js.listListToJson(coRequisites)
				+ "\n}";
	}

	/**
	 * Returns the type of the class (eg. FALLONLY, SPRINGONLY) as a String.
	 * @return - The type of the class, based on its availability.
	 */
	public String getTypeAsString() {
		// TODO Add further variants for weird scenarios (SUMMERONLY?)
		if (availability.size() == 1 && availability.get(0).equals(SemesterType.FALL)) {
			return "FALLONLY";
		} else if (availability.size() == 1 && availability.get(0).equals(SemesterType.SPRING)) {
			return "SPRINGONLY";
		}
		return "NORMAL";
	}
	
	// GETTERS
	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public int getCredits() {
		return credits;
	}

	public String getDescription() {
		return description;
	}

	public ArrayList<ArrayList<Course>> getPreRequisites() {
		return preRequisites;
	}

	public ArrayList<ArrayList<Course>> getCoRequisites() {
		return coRequisites;
	}

	public void addAvailability(SemesterType semesterType) {
		if (semesterType != null && !availability.contains(semesterType))
			this.availability.add(semesterType);
	}

	public void addPreRequisite(ArrayList<Course> preRequisite) {
		if (preRequisite != null && !preRequisites.contains(preRequisite))
			this.preRequisites.add(preRequisite);
	}

	public void addCoRequisite(ArrayList<Course> coRequisite) {
		if (coRequisite != null && !coRequisites.contains(coRequisite))
			this.coRequisites.add(coRequisite);
	}

	public void setPreRequisites(ArrayList<ArrayList<Course>> preRequisites) {
		this.preRequisites = preRequisites;
	}

	public void setCoRequisites(ArrayList<ArrayList<Course>> coRequisites) {
		this.coRequisites = coRequisites;
	}

}

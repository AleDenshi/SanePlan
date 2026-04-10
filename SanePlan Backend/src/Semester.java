import java.util.ArrayList;

public class Semester {
	
	private String name;
	private ArrayList<Course> semesterCourses;
	private SemesterType type;
	private int totalCredits;
	private int maxCredits;

	/**
	 * Construct a Semester given a name and a semester type.
	 * This assumes the default maxCredits limit (18)
	 * @param name - The vanity name of the Semester (eg. "FALL 2025")
	 * @param type - A SemesterType for this Semester (eg. FALL/SPRING/SUMMER...)
	 */
	public Semester(String name, SemesterType type) {
		this.name = name;
		this.type = type;
		this.totalCredits = 0;
		// TODO Maybe this should be some kind of global variable if it ever changes?
		this.maxCredits = 18;
		semesterCourses = new ArrayList<Course>();
	}

	/**
	 * Construct a Semester given a name, semester type, and a max number of credits for this semester.
	 * @param name - The vanity name of the Semester (eg. "FALL 2025")
	 * @param type - A SemesterType for this Semester (eg. FALL/SPRING/SUMMER...)
	 * @param maxCredits
	 */
	public Semester(String name, SemesterType type, int maxCredits) {
		this.name = name;
		this.type = type;
		this.totalCredits = 0;
		this.maxCredits = maxCredits;
		semesterCourses = new ArrayList<Course>();
	}

	/**
	 * Add a Course to the Semester.
	 * @param course - A Course type to be added to the Semester.
	 */
	public void addCourse(Course course) {
		if (course != null && !semesterCourses.contains(course)) {
			this.semesterCourses.add(course);
			this.totalCredits += course.getCredits();
		}
	}
	
	/**
	 * Add a Course to the Semester from its code.
	 * @param code - The Code of the Course to be added.
	 * @param catalog - The Catalog from which the Course is present.
	 */
	public void addCourseFromCode(String code, Catalog catalog) {
		Course course = catalog.findCourseByCode(code);
		addCourse(course);
	}
	
	/**
	 * Return a Course from the Semester by code. Returns null if it is not found.
	 * @param code - A String representing the code for a Course (eg. CS 225)
	 * @return - A Course in the Semester, or null if it is not present.
	 */
	public Course findCourseByCode(String code) {
		for (Course course : semesterCourses) {
			if (course.getCode().equals(code))
				return course;
		}
		return null;
	}
	
	/**
	 * Removes and returns a Course from the Semester. Returns null if it is not found.
	 * @param code
	 * @return
	 */
	public Course removeCourseByCode(String code) {
		for (Course course : semesterCourses) {
			if (course.getCode().equals(code)) {
				semesterCourses.remove(course);
				return course;
			}
		}
		return null;
	}

	// GETTERS
	public String getName() {
		return name;
	}

	public SemesterType getType() {
		return type;
	}

	public ArrayList<Course> getSemesterCourses() {
		return semesterCourses;
	}

	public int getTotalCredits() {
		return totalCredits;
	}
	
	public int getMaxCredits() {
		return maxCredits;
	}

	@Override
	public String toString() {
		String overall = name;
		for (Course course : semesterCourses) {
			overall += "\n" + course.getCode();
		}
		return overall;
	}
	
	public String toJson() {
		JSONWriter js = new JSONWriter();
		return "{\n"
				+ "\"name\": \"" + name + "\",\n "
				+ "\"totalCredits\": " + totalCredits + ",\n "
				+ "\"maxCredits\": " + maxCredits + ",\n "
				+ "\"type\": \"" + type + "\",\n "
				+ "\"courses\": " + js.listToJson(semesterCourses)
				+ "\n}";
	}
	
	public String courseListToJson() {
		String value = "[";
		for (int i = 0; i < semesterCourses.size(); i++) {
			String obj = semesterCourses.get(i).toJson();
			if (obj != null) {
				value += obj;
			}
			if (i < semesterCourses.size() - 1)
				value += ", ";
		}
		value += "]";
		return value;
	}
	
	public String toFullJson() {
		return "{\n"
				+ "\"name\": \"" + name + "\",\n "
				+ "\"totalCredits\": " + totalCredits + ",\n "
				+ "\"maxCredits\": " + maxCredits + ",\n "
				+ "\"type\": \"" + type + "\",\n "
				+ "\"courses\": " + courseListToJson()
				+ "\n}";
	}
}

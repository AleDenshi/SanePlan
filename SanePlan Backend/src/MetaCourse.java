import java.util.ArrayList;

public class MetaCourse extends Course {

	private ArrayList<Course> equivalencies;

	/**
	 * Constructs a Course with required parameters.
	 * 
	 * @param code         - A String representing the Course's code (eg. CS 225)
	 * @param credits      - The number of credits for the Course (eg. 3)
	 * @param name         - The human name for the Course (eg. Computer Science II)
	 */
	public MetaCourse(String equivalencyString, int highestCredits, String name, ArrayList<Course> equivalencies) {
		super(equivalencyString, highestCredits, name, null, null);
		this.equivalencies = equivalencies;
	}

	
	public void addEquivalency(Course course) {
		if (!this.equivalencies.contains(course)) {
			this.equivalencies.add(course);
		}
	}
	
	public void addEquivalency(ArrayList<Course> equivalencies) {
		for (Course equivalent : equivalencies) {
			if (!this.equivalencies.contains(equivalent)) {
				this.equivalencies.add(equivalent);
			}
		}
	}
	
	public Course findEquivalencyByCode(String courseCode) {
		for (Course course : equivalencies) {
			if (course.equals(courseCode)) {
				return course;
			}
		}
		return null;
	}
	
	public ArrayList<Course> getEquivalencies() {
		return equivalencies;
	}
	
	public Course removeEquivalencyByCode(String courseCode) {
		Course course = findEquivalencyByCode(courseCode);
		if (course != null) {
			equivalencies.remove(course);
			return course;
		} else {
			return null;
		}
	}

}

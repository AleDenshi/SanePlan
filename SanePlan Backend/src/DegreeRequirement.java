import java.util.ArrayList;

// TODO Refactor this to "DegreeRequisite" to be more consistent?
public class DegreeRequirement {
	private String name;
	private ArrayList<Course> equivalentCourses;

	public DegreeRequirement(String name) {
		this.name = name;
	}
	
	public void addEquivalentCourse(Course course) {
		if (!equivalentCourses.contains(course))
			equivalentCourses.add(course);
	}
	
	public String getName() {
		return name;
	}
	public ArrayList<Course> getEquivalentCourses() {
		return equivalentCourses;
	}
	
	public void setEquivalentCourses(ArrayList<Course> equivalentCourses) {
		this.equivalentCourses = equivalentCourses;
	}
	
	@Override
	public String toString() {
		return name + ": " + equivalentCourses;
	}
	
	
}
import java.util.ArrayList;

public class Semester {
	private String name;
	private ArrayList<Course> semesterCourses;
	private SemesterType type;
	
	// TODO: ADD CATEGORY TO SEMESTER CONSTRUCTOR!!!
	public Semester(String name) {
		this.name = name;
		semesterCourses = new ArrayList<Course>();
	}
	
	public void addCourse(Course course) {
		if (course != null) {
			this.semesterCourses.add(course);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public ArrayList<Course> getSemesterCourses() {
		return semesterCourses;
	}
	
	public int getTotalCredits() {
		int totalCredits = 0;
		for (Course course : semesterCourses) {
			totalCredits += course.getCredits();
		}
		return totalCredits;
	}
	
	@Override
	public String toString() {
		String overall = name;
		for (Course course : semesterCourses) {
			overall += "\n" + course.getCode();
		}
		return overall;
	}

	public Course findCourseByCode(String code) {
		for (Course course : semesterCourses) {
			if (course.getCode().equals(code))
				return course;
		}
		return null;
	}
}

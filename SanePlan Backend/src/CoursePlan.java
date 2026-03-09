import java.util.ArrayList;

public class CoursePlan {
	private String name;
	private ArrayList<Semester> semesters;
	private boolean isValid;
	
	public boolean isValid() {
		return this.isValid;
	}
	
	public CoursePlan(String name) {
		this.name = name;
		semesters = new ArrayList<Semester>();
	}
	
	public String getName() {
		return this.name;
	}
	
	
	public Semester findSemesterByCourseCode(String code) {
		for (Semester semester : semesters) {
			Course course = semester.findCourseByCode(code);
			if (course != null) return semester;
		}
		return null;
	}
	
	public int findSemesterIndexByCourseCode(String code) {
		for (int i = 0; i < semesters.size(); i++) {
			Semester semester = semesters.get(i);
			Course course = semester.findCourseByCode(code);
			if (course != null) return i;
		}
		return -1;
	}
	
	public void determineValidity() {
		this.isValid = true;
		for (int i = semesters.size()-1; i >= 0; i--) {
			Semester semester = semesters.get(i);
			System.out.println("Analyzing " + semester.getName() + "...");
			ArrayList<Course> semesterCourses = semester.getSemesterCourses();
			for (Course course : semesterCourses) {
				//System.out.println("Validating " + course.getCode());
				this.isValid &= validatePreReqs(i, course);
			}
		}
		System.out.println("Evaluation determined validity: " + this.isValid);
	}
	
	private boolean validatePreReqs(int semesterPosition, Course course) {
		if (semesterPosition > semesters.size() || semesterPosition < 0) {
			System.out.println("Semester " + semesterPosition + " considered invalid.");
			return false;
		}
		String code = course.getCode();
		ArrayList<ArrayList<Course>> preRequisites = course.getPreRequisites();
		ArrayList<ArrayList<Course>> coRequisites = course.getCoRequisites();
		if (preRequisites.size() == 0 && coRequisites.size() == 0) {
			System.out.println(code + " has no requisites. Skipping...");
			return true;
		}
		
		// TODO: Availability check (is the course FALL/SPRING exclusive?)
		// Check the current semester for corequisites
		for (ArrayList<Course> requisiteList : coRequisites) {
			for (Course requisite : requisiteList) {
				if (requisite == null) break;
				int semesterIndex = findSemesterIndexByCourseCode(requisite.getCode());
				if (semesterIndex == -1) {
					System.out.println("The course " + requisite.getCode() + " corequired by " + code + " was not found.");
					return false;
				} else if (semesterIndex == semesterPosition) {
					continue;
				} else {
					System.out.println("The course " + requisite.getCode() + " corequired by " + code + " was found before it.");
					return false;
				}
			}
		}
		
		// Check every past semester for prerequisites
		for (ArrayList<Course> requisiteList : preRequisites) {
			for (Course requisite : requisiteList) {
				if (requisite == null) break;
				int semesterIndex = findSemesterIndexByCourseCode(requisite.getCode());
				if (semesterIndex == -1) {
					System.out.println("The course " + requisite.getCode() + " pre-required by " + code + " was not found.");
					return false;
				} else if (semesterIndex == semesterPosition) {
					System.out.println("The course " + requisite.getCode() + " pre-required by " + code + " was found in the same semester.");
					return false;
				} else if (semesterIndex > semesterPosition) {
					System.out.println("The course " + requisite.getCode() + " pre-required by " + code + " was found after it.");
					return false;
				} else {
					return true;
				}
			}
		}
		
		return false;
	}

	@Override
	public String toString() {
		return name;
	}

	public void addSemester(Semester semester) {
		if (semester != null);
		semesters.add(semester);
		
	}
}

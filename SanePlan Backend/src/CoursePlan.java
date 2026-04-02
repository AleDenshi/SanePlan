import java.util.ArrayList;

public class CoursePlan {
	private String name;
	// String array to store problems with validity
	private ArrayList<String> validityIssues;
	private ArrayList<Semester> semesters;
	private boolean isValid;

	public boolean isValid() {
		return this.isValid;
	}

	/**
	 * Construct a CoursePlan, given a name.
	 * @param name - The name of the CoursePlan.
	 */
	public CoursePlan(String name) {
		this.name = name;
		this.semesters = new ArrayList<Semester>();
		this.validityIssues = new ArrayList<String>();
	}

	public String getName() {
		return this.name;
	}
	
	public ArrayList<Semester> getSemesters() {
		return this.semesters;
	}

	public ArrayList<String> getValidityIssues() {
		return validityIssues;
	}

	public Semester findSemesterByCourseCode(String code) {
		for (Semester semester : semesters) {
			Course course = semester.findCourseByCode(code);
			if (course != null)
				return semester;
		}
		return null;
	}

	public int findSemesterIndexByCourseCode(String code) {
		for (int i = 0; i < semesters.size(); i++) {
			Semester semester = semesters.get(i);
			Course course = semester.findCourseByCode(code);
			if (course != null)
				return i;
		}
		return -1;
	}

	public void determineValidity() {
		// Begin by assuming all is well
		this.isValid = true;
		this.validityIssues.clear();

		// Check every course per semester for:
		// - Course availability matching with semester type
		// - Course prerequisites
		// - Credit counts
		for (int i = semesters.size() - 1; i >= 0; i--) {
			Semester semester = semesters.get(i);
			ArrayList<Course> semesterCourses = semester.getSemesterCourses();
			for (Course course : semesterCourses) {
				if (!course.isType(semester.getType())) {
					this.isValid &= false;
					this.validityIssues
							.add(course.getCode() + " is not available for semester " + semester.getName() + ".");
				}
				validateReqs(i, course);
			}
		}
	}

	/**
	 * Determine if a course's co and pre-requirements have been met in this course plan
	 * @param semesterPosition - The position of the course in the course plan
	 * @param course - The course in question
	 */
	private void validateReqs(int semesterPosition, Course course) {
		if (semesterPosition > semesters.size() || semesterPosition < 0) {
			this.isValid &= false;
			return;
		}
		
		ArrayList<ArrayList<Course>> preRequisiteLists = course.getPreRequisites();
		ArrayList<ArrayList<Course>> coRequisiteLists = course.getCoRequisites();
		String code = course.getCode();
		
		// Check co-requisites
		for (ArrayList<Course> coRequisiteList : coRequisiteLists) {
			boolean met = coRequisiteMet(coRequisiteList, semesterPosition);
			this.isValid &= met;
			if (!met)
				this.validityIssues.add(diagnoseCoRequisiteIssues(coRequisiteList, semesterPosition, code));
		}
		
		// Check pre-requisites
		for (ArrayList<Course> preRequisiteList : preRequisiteLists) {
			boolean met = preRequisiteMet(preRequisiteList, semesterPosition);
			this.isValid &= met;
			if (!met)
				this.validityIssues.add(diagnosePreRequisiteIssues(preRequisiteList, semesterPosition, code));
		}
	
	}
	
	/**
	 * Determine if a list of co-requisites has been met by a course in semester.
	 * @param coRequisiteList - An ArrayList of Courses representing co-requisites for a course
	 * @param semesterPosition - The semester position of a course that must meet the co-requisites
	 * @return Whether or not the co-requisites have been met
	 */
	private boolean coRequisiteMet(ArrayList<Course> coRequisiteList, int semesterPosition) {
		boolean requirementMet = true;
		for (Course coRequisite : coRequisiteList) {
			int coRequisiteIndex = findSemesterIndexByCourseCode(coRequisite.getCode());
			if (coRequisiteIndex == semesterPosition) {
				requirementMet |= true;
				break;
			} else {
				requirementMet &= false;
			}
		}
		return requirementMet;
	}
	
	/**
	 * Produces an appropriate error message assuming a co-requisite issue is present with a course
	 * @param coRequisiteList - An ArrayList of Courses representing co-requisites for a course
	 * @param semesterPosition - The semester position of a course that must meet the co-requisites 
	 * @param code - The course code of a course that must meet the co-requisites
	 * @return A message diagnosing the issue
	 */
	private String diagnoseCoRequisiteIssues(ArrayList<Course> coRequisiteList, int semesterPosition, String code) {
		String message = "A co-requisite for " + code + " has not been met: ";
		for (Course coRequisite : coRequisiteList) {
			String coCode = coRequisite.getCode();
			int coRequisiteIndex = findSemesterIndexByCourseCode(coCode);
			if (coRequisiteIndex == -1) {
				message += coCode + " was not found. ";
			} else if (coRequisiteIndex < semesterPosition) {
				message += coCode + " was found before " + code + ". ";
			} else if (coRequisiteIndex > semesterPosition) {
				message += coCode + " was found after " + code + ". ";
			}
		}
		return message;
	}
		
	/**
	 * Determine if a list of pre-requisites has been met by a course in semester.
	 * @param preRequisiteList - An ArrayList of Courses representing pre-requisites for a course
	 * @param semesterPosition - The semester position of a course that must meet the pre-requisites
	 * @return Whether or not the pre-requisites have been met
	 */
	private boolean preRequisiteMet(ArrayList<Course> preRequisiteList, int semesterPosition) {
		boolean requirementMet = true;
		for (Course preRequisite : preRequisiteList) {
			int preRequisiteIndex = findSemesterIndexByCourseCode(preRequisite.getCode());
			if (preRequisiteIndex < semesterPosition && preRequisiteIndex != -1) {
				requirementMet |= true;
				break;
			} else {
				requirementMet &= false;
			}
		}
		return requirementMet;
	}
	
	/**
	 * Produces an appropriate error message assuming a pre-requisite issue is present with a course
	 * @param preRequisiteList - An ArrayList of Courses representing pre-requisites for a course
	 * @param semesterPosition - The semester position of a course that must meet the pre-requisites 
	 * @param code - The course code of a course that must meet the pre-requisites
	 * @return A message diagnosing the issue
	 */
	private String diagnosePreRequisiteIssues(ArrayList<Course> preRequisiteList, int semesterPosition, String code) {
		String message = "A pre-requisite for " + code + " has not been met: ";
		for (Course coRequisite : preRequisiteList) {
			String preCode = coRequisite.getCode();
			int preRequisiteIndex = findSemesterIndexByCourseCode(preCode);
			if (preRequisiteIndex == -1) {
				message += preCode + " was not found. ";
			} else if (preRequisiteIndex == semesterPosition) {
				message += preCode + " was found in the same semester as " + code + ". ";
			} else if (preRequisiteIndex > semesterPosition) {
				message += preCode + " was found after " + code + ". ";
			}
		}
		return message;
	}

	/**
	 * Add a semester to this course plan.
	 * @param semester - the semester to be added.
	 */
	public void addSemester(Semester semester) {
		if (semester != null)
			semesters.add(semester);
	}
	
	// TODO Clean this up by using org.json maybe?
	public String semesterListToJson() {
		String value = "[";
		for (int i = 0; i < semesters.size(); i++) {
			String obj = semesters.get(i).toJson();
			if (obj != null) {
				value += obj;
			}
			if (i < semesters.size() - 1)
				value += ", ";
		}
		value += "]";
		return value;
	}
	
	public String semesterListToFullJson() {
		String value = "[";
		for (int i = 0; i < semesters.size(); i++) {
			String obj = semesters.get(i).toFullJson();
			if (obj != null) {
				value += obj;
			}
			if (i < semesters.size() - 1)
				value += ", ";
		}
		value += "]";
		return value;
	}
	
	public String toJson() {
		JSONWriter js = new JSONWriter();
		return "{\n"
				+ "\"name\": \"" + name + "\",\n "
				+ "\"validity\":" + isValid + ",\n "
				+ "\"validityIssues\":" + js.listToJson(validityIssues) + ",\n "
				+ "\"semesters\":" + semesterListToJson() + "\n "
				+ "}";
	}
	
	public String toFullJson() {
		JSONWriter js = new JSONWriter();
		return "{\n"
				+ "\"name\": \"" + name + "\",\n "
				+ "\"validity\":" + isValid + ",\n "
				+ "\"validityIssues\":" + js.listToJson(validityIssues) + ",\n "
				+ "\"semesters\":" + semesterListToFullJson() + "\n "
				+ "}";
	}
}

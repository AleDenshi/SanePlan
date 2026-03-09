import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class Catalog {
	private ArrayList<Course> courses;

	// TODO Move File I/O methods to database class possibly?
	/**
	 * Cycles through a TSV file and creates the base entries for courses, without
	 * requisites.
	 * 
	 * @param filename The filename of a TSV file containing course information.
	 */
	private void readCoursesFromTSV(String filename) {
		File file = new File(filename);

		try {
			Scanner scan = new Scanner(file);
			scan.nextLine();
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				String[] values = line.split("	");

				// If the file has a gap in it, stop
				if (values.length < 4)
					break;

				// Convert to a course
				Course course = makeCourseFromValues(values);
				this.courses.add(course);
			}
			// Done scanning from file
			scan.close();
		} catch (Exception e) {
			System.out.println("Error detected when reading courses from TSV:");
			e.printStackTrace();
		}
	}

	/**
	 * Cycles through a TSV file and adds pre-requisites and co-requisites to all
	 * courses.
	 * 
	 * @param filename The filename of a TSV file containing course information.
	 */
	private void buildRequisitesFromTSV(String filename) {
		File file = new File(filename);
		try {
			Scanner scan = new Scanner(file);
			scan.nextLine();
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				String[] values = line.split("	");
				// If the file has a gap in it, stop
				if (values.length < 4)
					break;

				// Build requisites
				buildRequisitesFromValues(values);
			}

			// Done scanning from file
			scan.close();
		} catch (Exception e) {
			System.out.println("Error detected when trying to build requisites from TSV:");
			e.printStackTrace();
		}
	}

	/**
	 * Adds requisites to a course by taking in a String array of values.
	 * 
	 * @param values - A String array of values corresponding to a course.
	 */
	private void buildRequisitesFromValues(String[] values) {
		// If the line is cutoff, build no requisites.
		if (values.length < 5)
			return;
		// Obtain course
		Course course = findCourseByCode(values[0]);
		// Build co-requisites
		for (int i = 5; i <= 7; i++) {
			if (i >= values.length)
				return;
			String[] courseCodes = values[i].split(";");
			ArrayList<Course> requisites = getCoursesFromCodeArray(courseCodes);
			course.addPreRequisite(requisites);
		}
		for (int i = 8; i <= 9; i++) {
			if (i >= values.length)
				return;
			String[] courseCodes = values[i].split(";");
			ArrayList<Course> requisiteSet = getCoursesFromCodeArray(courseCodes);
			course.addCoRequisite(requisiteSet);
		}
	}

	/**
	 * Returns a set of Courses from the catalog when given a string array
	 * containing course codes.
	 * 
	 * @param courseCodes - A String array of course codes.
	 * @return requisiteSet A HashSet of Courses from the code.
	 */
	private ArrayList<Course> getCoursesFromCodeArray(String[] courseCodes) {
		ArrayList<Course> requisiteSet = new ArrayList<Course>();
		for (String courseCode : courseCodes) {
			Course courseRequisite = findCourseByCode(courseCode);
			// If this is a blank entry, return null to indicate to skip this
			if (courseRequisite == null)
				return null;
			requisiteSet.add(courseRequisite);
		}
		return requisiteSet;
	}

	/**
	 * 
	 * @param values - A String array containing values for a course.<br>
	 * values[0]: Course code (eg. CS 225)<br>
	 * values[1]: Course name (eg. Computer Science II)<br>
	 * values[2]: Course credits (eg. 3)<br>
	 * values[3]: Course description (eg. Preparing students for object oriented programming...)<br>
	 * @return A Course created from the values.
	 */
	private Course makeCourseFromValues(String[] values) {
		int credits = Integer.parseInt(values[1]);
		ArrayList<SemesterType> availability;
		if (values.length > 4) {
			availability = ValuesToAvailability(values[4].split(","));
		} else {
			availability = ValuesToAvailability(null);
		}
		Course course = new Course(values[0], credits, values[2], values[3], availability);
		return course;
	}
	
	/**
	 * Converts an array of Strings into an array of SemesterType enums.
	 * @param availabilityValues - A string array containing values corresponding to the SemesterType enum.
	 * @return An ArrayList of SemesterTypes.
	 */
	private ArrayList<SemesterType> ValuesToAvailability(String[] availabilityValues) {
		ArrayList<SemesterType> availability = new ArrayList<SemesterType>();
		try {
			for (int i = 0; i < availabilityValues.length; i++) {
				availability.add(SemesterType.valueOf(availabilityValues[i].trim()));
			}
		} catch (Exception e) {
			// If anything happens, assume the course is available Fall and Spring.
			//System.out.println(e);
			//e.printStackTrace();
			availability.add(SemesterType.FALL);
			availability.add(SemesterType.SPRING);
		}
		return availability;
	}

	/**
	 * Finds a course in the Catalog based on the given code.
	 * @param code - A course code (eg. CS 225)
	 * @return Either the Course corresponding to the code, or null if it cannot be found.
	 */
	public Course findCourseByCode(String code) {
		if (code.equals(""))
			return null;
		for (Course course : this.courses) {
			if (course.getCode().equals(code))
				return course;
		}
		System.out.println("WARN: Could not find " + code);
		return null;
	}
	
	public Catalog(String filename) {
		this.courses = new ArrayList<Course>();
		readCoursesFromTSV(filename);
		buildRequisitesFromTSV(filename);
	}

	// TODO Remove this debugging mess
	public void readPlanFromTSV(String filename) {
		File file = new File(filename);
		String[][] result = new String[7][8];
		try {
			int i = 0;
			Scanner scan = new Scanner(file);
			while (scan.hasNextLine() && i < result.length) {
				String line = scan.nextLine();
				result[i] = line.split("	");
				i++;
			}
			
			
			// Done scanning from file
			scan.close();
		} catch (Exception e) {
			System.out.println("Error detected when reading plan from TSV:");
			e.printStackTrace();
		}
		
		
		// Create a plan
		CoursePlan plan = new CoursePlan("My Plan");
		// Now that we have the array, let's turn it into semesters:
		for (int i = 0; i < 8; i++) {
			Semester sem = new Semester(result[0][i]);
			for (int j = 1; j < 7; j++) {
				if (i < result[j].length)
					sem.addCourse(findCourseByCode(result[j][i]));
			}
			plan.addSemester(sem);
			//System.out.println(sem);
		}
		
		plan.determineValidity();
	}

	public static void main(String[] args) {
		
		Catalog app = new Catalog("courses.tsv");
		//app.readPlanFromTSV("plan.tsv");
		
		for (Course c : app.courses) {
			System.out.println(c.toJson());
		}
		
	}

}

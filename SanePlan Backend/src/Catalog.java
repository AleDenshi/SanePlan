import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

public class Catalog {

	private ArrayList<Course> courses;

	/**
	 * Constructs a Catalog object from a given file.
	 * 
	 * @param filename - The filename of a TSV file containing course information.
	 */
	public Catalog(String filename) {
		this.courses = new ArrayList<Course>();
		readCoursesFromTSV(filename);
		buildRequisitesFromTSV(filename);
	}
	
	public Catalog(ArrayList<Course> courses) {
		this.courses = courses;
	}
	
	public void addCourse(Course course) {
		if (!courses.contains(course)) {
			courses.add(course);
		}
	}

	/**
	 * Get the list of all Courses in the catalog.
	 * 
	 * @return - An ArrayList of Courses contained in the Catalog.
	 */
	public ArrayList<Course> getCourses() {
		return courses;
	}

	/**
	 * Constructs and returns a Course from a JSON object.
	 * 
	 * @param jo - The JSON Object to convert to a Course.
	 * @return - A Course object generated from the JSON object, or null if
	 *         generation fails.
	 */
	public Course makeCourseFromJson(JSONObject jo) {
		try {
			String code = jo.getString("code");
			int credits = jo.getInt("credits");
			String name = jo.getString("name");
			String description = jo.getString("description");
			JSONArray availabilityJA = jo.getJSONArray("availability");
			ArrayList<SemesterType> availability = new ArrayList<SemesterType>();

			for (int i = 0; i < availabilityJA.length(); i++) {
				String semesterTypeString = availabilityJA.getString(i);
				SemesterType type = SemesterType.valueOf(semesterTypeString);
				availability.add(type);
			}

			Course course = new Course(code, credits, name, description, availability);
			JSONArray preRequisiteJAJA = jo.getJSONArray("preRequisites");
			JSONArray coRequisiteJAJA = jo.getJSONArray("coRequisites");

			for (int i = 0; i < preRequisiteJAJA.length(); i++) {
				ArrayList<Course> preRequisiteList = jsonArrayToCourseList(preRequisiteJAJA.getJSONArray(i));
				course.addPreRequisite(preRequisiteList);
			}

			for (int i = 0; i < coRequisiteJAJA.length(); i++) {
				ArrayList<Course> coRequisiteList = jsonArrayToCourseList(coRequisiteJAJA.getJSONArray(i));
				course.addCoRequisite(coRequisiteList);
			}

			return course;

		} catch (Exception e) {
			System.out.println("Error when trying to convert JSON to Course.");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Converts a JSON Array of Course codes into an ArrayList of Courses.
	 * 
	 * @param ja - The JSON Array of Course codes.
	 * @return - An ArrayList of Courses from the JSON Array of Course codes.
	 */
	public ArrayList<Course> jsonArrayToCourseList(JSONArray ja) {
		ArrayList<Course> courseList = new ArrayList<Course>();
		for (int i = 0; i < ja.length(); i++) {
			Course requisite = findCourseByCode(ja.getString(i));
			courseList.add(requisite);
		}
		return courseList;
	}

	/**
	 * Constructs and returns a Semester from a JSON object.
	 * 
	 * @param jo - The JSON Object to convert to a Semester.
	 * @return - The Semester converted from the JSON object, or null if conversion
	 *         failed.
	 */
	public Semester makeSemesterFromJson(JSONObject jo) {
		try {
			String name = jo.getString("name");
			int maxCredits = jo.getInt("maxCredits");
			SemesterType type = SemesterType.valueOf(jo.getString("type"));

			Semester semester = new Semester(name, type, maxCredits);
			ArrayList<Course> semesterCourses = jsonArrayToCourseList(jo.getJSONArray("courses"));
			for (Course c : semesterCourses) {
				semester.addCourse(c);
			}
			return semester;

		} catch (Exception e) {
			System.out.println("Error when trying to convert JSON to Semester.");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Constructs and returns a CoursePlan from a JSON object.
	 * 
	 * @param jo - The JSON Object to convert to a CoursePlan.
	 * @return - The CoursePlan converted from the JSON object, or null if
	 *         conversion failed.
	 */
	public CoursePlan makeCoursePlanFromJson(JSONObject jo) {
		try {
			String name = jo.getString("name");
			CoursePlan coursePlan = new CoursePlan(name);
			JSONArray ja = jo.getJSONArray("semesters");
			for (int i = 0; i < ja.length(); i++) {
				JSONObject semesterJo = ja.getJSONObject(i);
				Semester semester = makeSemesterFromJson(semesterJo);
				coursePlan.addSemester(semester);
			}
			return coursePlan;

		} catch (Exception e) {
			System.out.println("Error when trying to convert JSON to a CoursePlan.");
			e.printStackTrace();
		}
		return null;
	}

	public CoursePlan readCoursePlanFromTSV(String filename) {
		File file = new File(filename);
		CoursePlan plan = new CoursePlan(filename);

		try {
			Scanner scan = new Scanner(file);
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				String[] values = line.split("	");

				if (values.length < 1)
					continue;

				// TODO Change this name to something more descriptive
				String name = values[0];
				SemesterType type = SemesterType.valueOf(name.split(" ")[0]);
				// Assuming these are laid out like "FALL 2025" etc...
				Semester semester = new Semester(name, type);

				for (int i = 1; i < values.length; i++) {
					String[] codes = values[i].split("/");
					Course courseToAdd = findCourseByCode(codes[0]);

					// If this is a singleton course, add it and move on
					if (codes.length == 1 && courseToAdd != null) {
						semester.addCourse(courseToAdd);
						// If this isn't a singleton course, we must build a meta course
					} else {
						String metaName = "";
						ArrayList<Course> equivalencies = new ArrayList<Course>();
						// Go through all ORed courses (eg. HU:3:LL/SS:3:LL)
						for (int j = 0; j < codes.length; j++) {

							String code = codes[j];
							// TODO Instead of the raw code, use something else
							metaName += code.split(":")[0];
							if (j != codes.length - 1) {
								metaName += " or ";
							}
							
							ArrayList<Course> newEquivalencies = equivalentCoursesFromString(code);
							
							if (newEquivalencies != null) {
								equivalencies.addAll(newEquivalencies);
							} else {
								System.out.println("Some fatal error on building " + code);
							}
						}
						int highestCredits = determineHighestCredits(equivalencies);
						courseToAdd = new MetaCourse(values[i], highestCredits, metaName, equivalencies);
						courses.add(courseToAdd);
						semester.addCourse(courseToAdd);
					}
				}
				plan.addSemester(semester);

			}
			scan.close();

		} catch (Exception e) {
			System.out.println("Error detected when reading plan from TSV:");
			e.printStackTrace();
		}
		return plan;
	}

	public int determineHighestCredits(ArrayList<Course> equivalencies) {
		int highest = 0;
		for (Course course : equivalencies) {
			if (course.getCredits() > highest) {
				highest = course.getCredits();
			}
		}
		return highest;
	}

	/**
	 * Converts an equivalency string into an ArrayList of equivalent courses
	 * 
	 * @param equivalencyString - A string formatted as follows:
	 *                          CATEGORY:CREDITS:LEVEL (eg. HU:3:14X or MA:3:HL)
	 * @return
	 */
	public ArrayList<Course> equivalentCoursesFromString(String equivalencyString) {
		// If the course already exists, just return that
		Course existingCourse = findCourseByCode(equivalencyString);
		if (existingCourse != null) {
			ArrayList<Course> equivalents = new ArrayList<Course>();
			equivalents.add(existingCourse);
			return equivalents;
		}
		
		String[] courseValues = equivalencyString.split(":");
		if (courseValues.length > 1) {

			String category = courseValues[0];
			int requiredCredits = Integer.parseInt(courseValues[1]);

			// Determine min and max levels for the MetaCourse
			int minLevel, maxLevel;
			String levelString;
			if (courseValues.length > 2) {
				levelString = courseValues[2];
			} else {
				levelString = "";
			}

			switch (levelString) {
			case "":
				minLevel = 0;
				maxLevel = 700; // TODO This is arbitrary. Is there an actual max level?
				break;
			case "HL":
				minLevel = 300;
				maxLevel = 499;
				break;
			case "LL":
				// TODO Is "lower level" always 100?
				minLevel = 100;
				maxLevel = 499;
				break;
			default:
				String min = levelString.replace('X', '0');
				String max = levelString.replace('X', '9');
				minLevel = Integer.parseInt(min);
				maxLevel = Integer.parseInt(max);
				break;
			}
			return getEquivalent(category, requiredCredits, minLevel, maxLevel);
		}
		return null;

	}

	/**
	 * Returns a list of equivalent courses that match the given parameters.
	 * 
	 * @param category
	 * @param requiredCredits
	 * @param minLevel
	 * @param maxLevel
	 * @return
	 */
	private ArrayList<Course> getEquivalent(String category, int requiredCredits, int minLevel, int maxLevel) {
		ArrayList<Course> equivalencies = new ArrayList<Course>();
		boolean elective = category.contains("ELECT");
		
		for (Course equivalent : courses) {
			if (equivalent instanceof MetaCourse) continue;
			String[] codeValues = equivalent.getCode().split(" ");
			String thisCategory = codeValues[0];
			int thisLevel = Integer.parseInt(codeValues[1].replaceAll("[^0-9]", ""));
			
			if (thisLevel >= minLevel && thisLevel <= maxLevel && equivalent.getCredits() == requiredCredits) {
				// TODO This elective system assumes all courses are acceptable. Is that actually OK?
				if (thisCategory.equals(category) || elective) {
					equivalencies.add(equivalent);
				}
			}
		}
		return equivalencies;
	}

	// TODO Move File I/O methods to database or File I/O class
	/**
	 * Cycles through a TSV file and creates the base entries for courses, without
	 * requisites.
	 * 
	 * @param filename - The filename of a TSV file containing course information.
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
			System.exit(-1);
		}
	}

	/**
	 * Cycles through a TSV file and adds pre-requisites and co-requisites to all
	 * courses.
	 * 
	 * @param filename - The filename of a TSV file containing course information.
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
	public void buildRequisitesFromValues(String[] values) {
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
			if (courseCode.isEmpty())
				continue;
			
			Course courseRequisite = findCourseByCode(courseCode);
			// If this is a blank entry, return null to indicate to skip this
			if (courseRequisite == null) {
				if (courseCode.contains(":")) {
					requisiteSet.addAll(equivalentCoursesFromString(courseCode));
					System.out.println("[INFO] Built " + courseCode);
				} else {
					System.out.println("[WARN] Couldn't find/build " + courseCode);
					return null;
				}
			} else {
				requisiteSet.add(courseRequisite);
			}
		}
		return requisiteSet;
	}

	/**
	 * 
	 * @param values - A String array containing values for a course.<br>
	 *               values[0]: Course code (eg. CS 225)<br>
	 *               values[1]: Course name (eg. Computer Science II)<br>
	 *               values[2]: Course credits (eg. 3)<br>
	 *               values[3]: Course description (eg. Preparing students for
	 *               object oriented programming...)<br>
	 * @return A Course created from the values.
	 */
	public Course makeCourseFromValues(String[] values) {
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
	 * 
	 * @param availabilityValues - A string array containing values corresponding to
	 *                           the SemesterType enum.
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
			// System.out.println(e);
			// e.printStackTrace();
			availability.add(SemesterType.FALL);
			availability.add(SemesterType.SPRING);
		}
		return availability;
	}

	/**
	 * Finds a course in the Catalog based on the given code.
	 * 
	 * @param code - A course code (eg. CS 225)
	 * @return Either the Course corresponding to the code, or null if it cannot be
	 *         found.
	 */
	public Course findCourseByCode(String searchCode) {
		String code = searchCode;
		code = code.toUpperCase().replace(" ", "");
		if (code.equals(""))
			return null;
		for (Course course : this.courses) {
			if (course.getCode().replace(" ", "").equals(code))
				return course;
		}
		return null;
	}

	// TODO Remove this once testing is completed
	public CoursePlan loadCoursePlanFromFile(String filename) {
		String jsonContent;
		try {
			jsonContent = new String(Files.readAllBytes(Paths.get(filename)));
			JSONObject jo = new JSONObject(jsonContent);
			CoursePlan plan = makeCoursePlanFromJson(jo);
			return plan;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Cycles through a TSV file and creates a degree and its requirements from it.
	 * 
	 * @param filename - The filename of a TSV file containing degree information.
	 */
	public Degree readDegreeFromTSV(String filename) {
		// TODO Write this method.
		return null;
	}
}

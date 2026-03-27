import org.json.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class TestMain {

	public static void main(String[] args) {
		Catalog erauCatalog = new Catalog("courses.tsv");

		Course cs225 = erauCatalog.findCourseByCode("CS 225");

		Semester fall25 = new Semester("FALL 2025", SemesterType.FALL);
		fall25.addCourse(erauCatalog.findCourseByCode("CS 225"));
		fall25.addCourse(erauCatalog.findCourseByCode("CS 225L"));
		fall25.addCourse(erauCatalog.findCourseByCode("COM 219"));
		fall25.addCourse(erauCatalog.findCourseByCode("EGR 101"));
		fall25.addCourse(erauCatalog.findCourseByCode("MA 242"));

		JSONObject jo2 = new JSONObject(fall25.toJson());
		Semester sem = erauCatalog.makeSemesterFromJson(jo2);
		System.out.println(sem.toJson());

		// Database db = new Database();
		// db.addCatalog(erauCatalog);
		// System.out.println(db.getCourseFromCode("CS 225"));
		// db.getRequisiteGroupsFromCourseCode("CS 225", "co");

		JSONObject jo = new JSONObject(cs225.toJson());
		Course course = erauCatalog.makeCourseFromJson(jo);
		System.out.println(course.toJson());
		CoursePlan plan = tsvFileToCoursePlan("plan.tsv", "My Vertical Plan", erauCatalog);
		System.out.println(plan.toJson());
		CoursePlan plan2 = erauCatalog.makeCoursePlanFromJson(new JSONObject(plan.toJson()));
		System.out.println(plan2.toJson());
	}

	/**
	 * Reads a vertical column-based TSV from a file and converts it to a
	 * CoursePlan.
	 * 
	 * @param filePath - Path to the TSV file
	 * @param planName - Name for the CoursePlan
	 * @return - A populated CoursePlan
	 * @throws IOException - If reading the file fails
	 */
	public static CoursePlan tsvFileToCoursePlan(String filePath, String planName, Catalog catalog) {
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			String line = br.readLine();

			if (line == null)
				return new CoursePlan(planName);

			String[] semesterNames = line.split("\\t");
			int semesterCount = semesterNames.length;

			ArrayList<Semester> semesters = new ArrayList<>();
			for (String semName : semesterNames) {
				SemesterType type = SemesterType.valueOf(semName.split(" ")[0]);
				semesters.add(new Semester(semName.trim(), type));
			}

			try {
				while ((line = br.readLine()) != null) {
					String[] codes = line.split("\\t", -1);
					for (int col = 0; col < semesterCount; col++) {
						if (col >= codes.length)
							continue;
						String code = codes[col].trim();
						if (!code.isEmpty()) {
							Course course = catalog.findCourseByCode(code);
							semesters.get(col).addCourse(course);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			CoursePlan plan = new CoursePlan(planName);
			for (Semester sem : semesters) {
				plan.addSemester(sem);
			}
			plan.determineValidity();

			return plan;
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return null;
	}
}

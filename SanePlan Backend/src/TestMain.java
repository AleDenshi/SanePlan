import org.json.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class TestMain {

	public static void main(String[] args) {
		Catalog erauCatalog = new Catalog("courses.tsv");

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

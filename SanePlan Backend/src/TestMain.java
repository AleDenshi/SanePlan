import java.util.ArrayList;

public class TestMain {

	public static void main(String[] args) {
		Catalog catalog = new Catalog("courses.tsv");
		Degree degree = catalog.readDegreeFromTSV("degree.tsv");
		CoursePlan plan = catalog.readCoursePlanFromTSV("plan.tsv");
		plan.setDegree(degree);
		
		ArrayList<String> degreeIssues = catalog.determineIfMeetsDegree(plan);
		//System.out.println(degreeIssues);
	}
}

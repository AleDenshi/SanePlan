import java.util.ArrayList;

public class TestMain {

	public static void main(String[] args) {
		Catalog erauCatalog = new Catalog("courses.tsv");
		CoursePlan plan = erauCatalog.readCoursePlanFromTSV("plan.tsv");
	}
}

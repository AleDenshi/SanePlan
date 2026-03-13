import java.util.ArrayList;

public class TestMain {

	public static void main(String[] args) {
		Catalog erauCatalog = new Catalog("courses.tsv");
		Database db = new Database();
		db.addCatalog(erauCatalog);
		//System.out.println(db.getCourseFromCode("CS 225"));
		//db.getRequisiteGroupsFromCourseCode("CS 225", "co");
	}

}

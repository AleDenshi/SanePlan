import java.util.ArrayList;

public class Degree {
	String name, code, description;
	ArrayList<DegreeRequirement> requirements;
	
	public Degree(String name, String code, String description) {
		this.name = name;
		this.code = code;
		this.description = description;
	}
}

import java.util.ArrayList;

public class Degree {
	String name, code, description;
	ArrayList<DegreeRequirement> requirements;
	
	public Degree(String name, String code, String description) {
		this.name = name;
		this.code = code;
		this.description = description;
		this.requirements = new ArrayList<DegreeRequirement>();
	}
	
	public void addDegreeRequirement(DegreeRequirement requirement) {
		if (!requirements.contains(requirement))
			requirements.add(requirement);
	}
	
	public ArrayList<DegreeRequirement> getRequirements() {
		return requirements;
	}
	
	public String getCode() {
		return this.code;
	}
	
	@Override
	public String toString() {
		String value = "";
		value += name + " (" + code + ")\n";
		value += "Requirements:\n";
		for (DegreeRequirement dr : requirements) {
			value += dr + "\n";
		}
		return value;
	}
}

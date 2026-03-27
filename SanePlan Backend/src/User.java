import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class User {

	String username;
	String passwordHash;
	ArrayList<CoursePlan> plans;

	public User(String username, String password) {
		this.username = username;
		setPassword(password);
		this.plans = new ArrayList<CoursePlan>();
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public boolean isPassword(String password) {
		return hashString(password).equals(this.passwordHash);
	}

	public void setPassword(String password) {
		this.passwordHash = hashString(password);
	}

	public ArrayList<CoursePlan> getPlans() {
		return plans;
	}

	/**
	 * Adds a CoursePlan to the user.
	 * @param plan - The CoursePlan to be added.
	 */
	public void addPlan(CoursePlan plan) {
		if (plan != null && !plans.contains(plan))
			this.plans.add(plan);
	}

	/**
	 * Finds a CoursePlan owned by the user and returns it.
	 * @param planName - The name of the CoursePlan.
	 * @return - The CoursePlan, or null if it was not found.
	 */
	public CoursePlan findPlanByName(String planName) {
		for (CoursePlan plan : plans) {
			if (plan.getName().equals(planName))
				return plan;
		}
		return null;
	}
	
	/**
	 * Removes a CoursePlan from a user if it is found.
	 * @param planName - The name of the CoursePlan.
	 * @return - The CoursePlan that was removed, or null if it was not found.
	 */
	public CoursePlan removePlanByName(String planName) {
		CoursePlan plan = findPlanByName(planName);
		if (plan == null)
			return plan;
		plans.remove(plan);
		return plan;
	}

	/**
	 * Convert a String into a SHA256 hash.
	 * @param input - The String to be hashed.
	 * @return - A hash corresponding to that String.
	 */
	private String hashString(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(input.getBytes());

			StringBuilder hexString = new StringBuilder();
			for (byte b : hashBytes) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}

			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 algorithm not found", e);
		}
	}

}

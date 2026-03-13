import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class Database {
	Connection connection;
	// Cache to speed up course loading
	Map<String, Course> courseCache = new HashMap<>();

	public Database() {
		// TODO Add authentication configuration file instead of in-place creds!
		this.connection = connectToDB("SanePlan", "postgres", "");
		// TODO Add other database SQL files
		buildDatabaseFromFile("Catalog.sql");
	}

	/**
	 * Connect to a PostgreSQL database when given credentials
	 * 
	 * @param dbName   - The name of the database, eg. SanePlan
	 * @param username - The username used to access the database
	 * @param password - The password for the username
	 * @return - A Connection type to that database that can execute statements
	 */
	private Connection connectToDB(String dbName, String username, String password) {
		Connection connection = null;
		String url = "jdbc:postgresql://localhost:5432/";
		try {
			connection = DriverManager.getConnection(url + dbName, username, password);
			if (connection != null) {
				System.out.println("Connection established.");
			} else {
				System.out.println("Failed to connect to database " + dbName);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(-1);
		}
		return connection;
	}

	/**
	 * Takes in an SQL script filename and returns the statements in an array.
	 * 
	 * @param filename - The filepath of the .sql file
	 * @return A String array with SQL statements, eg. CREATE TABLE...
	 */
	private String[] getStatementsFromSqlFile(String filename) {
		String sqlData = null;
		try {
			sqlData = new String(Files.readAllBytes(Paths.get(filename)));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return sqlData.split("(?<=;)");
	}

	/**
	 * Takes in an SQL script and runs it on the connected database.
	 * 
	 * @param filename - The filepath of the .sql file
	 */
	private void buildDatabaseFromFile(String filename) {
		String[] sqlStatements = getStatementsFromSqlFile(filename);
		try {
			Statement stmt = connection.createStatement();

			for (String statement : sqlStatements) {
				statement = statement.trim();
				if (!statement.isEmpty()) {
					stmt.execute(statement);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Adds a course to the database. This does NOT add the prerequisites,
	 * corequisites or availability (FALL, SPRING etc...)
	 * 
	 * @param course - A Course type.
	 */
	public void addCourseInfo(Course course) {
		try {
			PreparedStatement insertStatement;
			insertStatement = connection
					.prepareStatement("INSERT INTO Courses VALUES (?, ?, ?, ?) ON CONFLICT (courseCode) DO NOTHING;");
			insertStatement.setString(1, course.getCode());
			insertStatement.setString(2, course.getName());
			insertStatement.setInt(3, course.getCredits());
			insertStatement.setString(4, course.getDescription());
			insertStatement.execute();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		courseCache.put(course.getCode(), course);
	}

	/**
	 * Returns all requisite groups for a given type (co or pre) for a course.
	 * 
	 * @param courseCode
	 * @return
	 */
	public ArrayList<ArrayList<Course>> getRequisiteGroupsFromCourseCode(String courseCode, String type) {
		ArrayList<ArrayList<Course>> requisiteGroups = new ArrayList<ArrayList<Course>>();
		if (type.equals("co") || type.equals("pre")) {
			try {
				// Get all existing groupIDs
				PreparedStatement query;
				query = connection.prepareStatement(
						"SELECT groupID FROM " + type + "RequisiteGroups WHERE courseCode = ?");
				query.setString(1, courseCode);
				ResultSet rs = query.executeQuery();
				
				while (rs.next()) {
					int groupID = rs.getInt("groupID");
					//System.out.printf("%s has %s-requisite groupID = %d\n", courseCode, type, groupID);
					ArrayList<Course> requisites = getRequisiteListFromGroupID(groupID, type);
					requisiteGroups.add(requisites);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		return requisiteGroups;
	}
	
	/**
	 * Return a list of requisites (either pre or co) from the groupID in the database.
	 * @param groupID
	 * @param type
	 * @return An ArrayList of Courses representing requisites.
	 */
	public ArrayList<Course> getRequisiteListFromGroupID(int groupID, String type) {
		ArrayList<Course> requisites = new ArrayList<Course>();
		if (type.equals("co") || type.equals("pre")) {
			try {
				// Get all existing groupIDs
				PreparedStatement query;
				query = connection.prepareStatement(
						"SELECT requisiteCode FROM " + type + "Requisites WHERE groupID = ?");
				query.setInt(1, groupID);
				ResultSet rs = query.executeQuery();
				
				while (rs.next()) {
					Course course = getCourseFromCode(rs.getString("requisiteCode"));
					requisites.add(course);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		if (requisites.size() == 0) {
			return null;
		}
		return requisites;
	}
	
	public void addRequisiteGroupToCourse(String courseCode, ArrayList<Course> requisiteGroup, String type) {
		if (type.equals("co") || type.equals("pre")) {
			
			// Before adding a new requisite, check if there already exists an equivalent group in the database
			ArrayList<ArrayList<Course>> requisiteGroups = getRequisiteGroupsFromCourseCode(courseCode, type);
			for (ArrayList<Course> originalRequisiteGroup : requisiteGroups) {
				if (requisiteGroup.equals(originalRequisiteGroup)) {
					System.out.println("Duplicate group found for " + courseCode);
					return;
				}
			}
			
			int groupID = -1;
			try {
				// Create a new group in the database
				PreparedStatement insertStatement;
				insertStatement = connection.prepareStatement(
						"INSERT INTO " + type + "RequisiteGroups (courseCode) VALUES (?) RETURNING groupID");
				insertStatement.setString(1, courseCode);
				// Obtain the groupID we just created for inserting the individual courses
				ResultSet rs = insertStatement.executeQuery();
				if (rs.next()) {
					groupID = rs.getInt("groupID");
				} else {
					System.out.println("Creating preRequisiteGroup failed, no ID obtained.");
					System.exit(-1);
				}

				// Go through all the requisite and add them to the database
				for (Course requisite : requisiteGroup) {
					insertStatement = connection
							.prepareStatement("INSERT INTO " + type + "Requisites (groupID, requisiteCode) VALUES (?, ?);");
					insertStatement.setInt(1, groupID);
					insertStatement.setString(2, requisite.getCode());
					insertStatement.executeUpdate();
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		} else {
			System.out.println("Invalid requisite type: " + type);
		}
	}

	/**
	 * Returns a Course from the database, if it exists, given the course code (eg.
	 * CS 225).
	 * 
	 * @param courseCode - The course code for the desired course.
	 * @return A Course object.
	 */
	public Course getCourseFromCode(String courseCode) {
		if (courseCache.containsKey(courseCode)) {
	        return courseCache.get(courseCode);
	    }
		
		try {
			PreparedStatement queryStatement;
			queryStatement = connection.prepareStatement("SELECT * FROM Courses WHERE courseCode = ?");
			queryStatement.setString(1, courseCode);
			ResultSet info = queryStatement.executeQuery();
			// TODO Change the name of the availableSemesters table to be consistent?
			queryStatement = connection
					.prepareStatement("SELECT semester FROM availableSemesters WHERE courseCode = ?");
			queryStatement.setString(1, courseCode);
			ResultSet availability = queryStatement.executeQuery();
			Course course = resultSetToCourse(info, availability);
			
			// Now obtain the pre and co requisites
			ArrayList<ArrayList<Course>> preRequisites = getRequisiteGroupsFromCourseCode(courseCode, "pre");
			ArrayList<ArrayList<Course>> coRequisites = getRequisiteGroupsFromCourseCode(courseCode, "co");
			course.setPreRequisites(preRequisites);
			course.setCoRequisites(coRequisites);
			return course;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	/**
	 * Creates a course given the results of two queries.
	 *
	 * @param info         - The ResultSet of a query on the information of the
	 *                     course from the Courses table.
	 * @param availability - The resultSet of a query on the availability of the
	 *                     course from the Availability table.
	 * @return A Course object or null if the conversion fails.
	 */
	private Course resultSetToCourse(ResultSet info, ResultSet availability) {
		ArrayList<SemesterType> availabilityList = new ArrayList<SemesterType>();
		try {
			while (availability.next()) {
				String semesterString = availability.getString("semester");
				availabilityList.add(SemesterType.valueOf(semesterString));
			}
			if (info.next()) {
				Course course = new Course(info.getString("courseCode"), info.getInt("credits"),
						info.getString("courseName"), info.getString("courseDescription"), availabilityList);
				return course;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	public void addRequisitesFromCourse(Course course) {
		for (ArrayList<Course> requisiteGroup : course.getCoRequisites()) {
			addRequisiteGroupToCourse(course.getCode(), requisiteGroup, "co");
		}

		for (ArrayList<Course> requisiteGroup : course.getPreRequisites()) {
			addRequisiteGroupToCourse(course.getCode(), requisiteGroup, "pre");
		}
	}

	public void addCatalog(Catalog catalog) {
		ArrayList<Course> courses = catalog.getCourses();
		// Do an initial run adding all the basic course info
		for (Course course : courses) {
			addCourseInfo(course);
		}
		for (Course course : courses) {
			addRequisitesFromCourse(course);
		}
		System.out.println("Added " + courses.size() + " courses to the database.");
	}
}

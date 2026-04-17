import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;

public class Database {
	Connection conn;

	/**
	 * Construct the database.
	 */
	public Database() {
		// TODO Add authentication configuration file instead of in-place creds!
		// this.connection = connectToDB("SanePlan", "postgres", "");
		this.conn = connectToSqlite("saneplan.sql");

		// TODO Add other database SQL files
		buildDatabaseFromFile("catalog.sql");
		buildDatabaseFromFile("users.sql");
	}
	
	/**
	 * Get a user from the database
	 * @param username
	 * @return
	 */
	public User getUser(String username) {
		String getUserSQL = "SELECT username, passwordHash, isAdmin FROM Users WHERE LOWER(username) = LOWER(?)";
		
		try (PreparedStatement ps = conn.prepareStatement(getUserSQL)) {
			ps.setString(1, username);
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				String passwordHash = rs.getString("passwordHash");
				boolean isAdmin = rs.getBoolean("isAdmin");
				return new User(username, passwordHash, isAdmin);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}
	
	/**
	 * Add a user to the system.
	 * @param username
	 * @param password
	 * @param isAdmin
	 * @return - Returns the user just created, or null if there was some error in the creation of said user.
	 */
	public User addUser(String username, String password, boolean isAdmin) {
		String insertUserSQL = "INSERT INTO Users (username, passwordHash, isAdmin) VALUES (?, ?, ?)";
		
		try (PreparedStatement ps = conn.prepareStatement(insertUserSQL)) {
			ps.setString(1, username);
			ps.setString(2, hashString(password));
			ps.setBoolean(3, isAdmin);
			ps.executeUpdate();
			
		} catch (SQLException e) {
			return getUser(username);
		}
		return new User(username, password, isAdmin);
	}
	
	/**
	 * Set a new password for a user.
	 * @param username
	 * @param password
	 */
	public void changeUserPassword(String username, String password) {
		String insertUserSQL = "UPDATE Users SET passwordHash = ? WHERE username = ?";
		
		try (PreparedStatement ps = conn.prepareStatement(insertUserSQL)) {
			ps.setString(1, hashString(password));
			ps.setString(2, username);
			ps.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * Give a user admin permissions.
	 * @param username
	 */
	public void makeUserAdmin(String username) {
		String insertUserSQL = "UPDATE Users SET isAdmin = TRUE WHERE username = ?";
		
		try (PreparedStatement ps = conn.prepareStatement(insertUserSQL)) {
			ps.setString(1, username);
			ps.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * Remove a user's admin permissions.
	 * @param username
	 */
	public void unmakeUserAdmin(String username) {
		String insertUserSQL = "UPDATE Users SET isAdmin = FALSE WHERE username = ?";
		
		try (PreparedStatement ps = conn.prepareStatement(insertUserSQL)) {
			ps.setString(1, username);
			ps.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Add a Course to the database.
	 * 
	 * @param course - A course object.
	 * @return Whether or not the course was validly added to the database.
	 */
	public boolean addCourse(Course course) {
		String insertCourseSQL = "INSERT INTO Courses (code, credits, name, description) VALUES (?, ?, ?, ?)";

		try (PreparedStatement ps = conn.prepareStatement(insertCourseSQL)) {
			ps.setString(1, course.getCode());
			ps.setInt(2, course.getCredits());
			ps.setString(3, course.getName());
			ps.setString(4, course.getDescription());
			ps.executeUpdate();
		} catch (SQLException e) {
			return false;
		}
		
		if (!addAvailability(course))
			return false;
		
		if (!addRequisites(course, "pre")) {
			System.out.println("Some fatal error adding prerequisites");
			return false;
		}
		if (!addRequisites(course, "co")) {
			System.out.println("Some fatal error adding corequisites");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Add availability to a course.
	 * @param course
	 * @return
	 */
	private boolean addAvailability(Course course) {
		String insertAvailabilitySQL = "INSERT INTO Availability (code, semester) VALUES (?, ?)";
		
		for (SemesterType type : course.getAvailability()) {
			try (PreparedStatement ps = conn.prepareStatement(insertAvailabilitySQL)) {
				ps.setString(1, course.getCode());
				ps.setString(2, type.toString());
				ps.executeUpdate();
			} catch (SQLException e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Adds either pre or co-requisites to the database when given a Course.
	 * @param course - The Course for which to add pre or co-requisites.
	 * @param prefix
	 * @return
	 */
	private boolean addRequisites(Course course, String prefix) {
		ArrayList<ArrayList<Course>> requisiteGroups;
		if (prefix.equals("co")) {
			requisiteGroups = course.getCoRequisites();
		} else if (prefix.equals("pre")) {
			requisiteGroups = course.getPreRequisites();
		} else {
			return false;
		}
		
		String insertRequisiteGroupsSQL = "INSERT INTO " + prefix + "RequisiteGroups (parentCode, groupID) VALUES (?, ?)";
		String insertRequisiteSQL = "INSERT INTO " + prefix + "RequisiteGroups (groupID, requisiteCode) VALUES (?, ?)";
		
		for (ArrayList<Course> requisiteGroup : requisiteGroups) {
			int groupID = nextGroupID(prefix + "RequisiteGroups");
			try (PreparedStatement ps = conn.prepareStatement(insertRequisiteGroupsSQL)) {
				ps.setString(1, course.getCode());
				ps.setInt(2, groupID);
				ps.executeUpdate();
				
				for (Course requisite : requisiteGroup) {
					try (PreparedStatement ps2 = conn.prepareStatement(insertRequisiteSQL)) {
						ps2.setInt(1, groupID);
						ps2.setString(2, requisite.getCode());
					} catch (SQLException e) {
						return false;
					}
				}
				
			} catch (SQLException e) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Determine the next groupID to use based on the maximum value present in the given table + 1.
	 * @param table - The name of the table to find this for. Eg. preRequisiteGroups
	 * @return
	 */
	private int nextGroupID(String table) {
        String selectStatement = "SELECT MAX(groupID) FROM " + table;

        try {
        	Statement stmt = conn.createStatement();
        	ResultSet rs = stmt.executeQuery(selectStatement);

            if (rs.next()) {
                int max = rs.getInt(1);

                if (rs.wasNull()) {
                    return 0;
                }
                return max;
            }
        } catch (SQLException e) {
        	e.printStackTrace();
        	System.exit(-1);
        }

        return 0;
	}

	/**
	 * Connect to an SQLite file database. FOR TESTING PURPOSES ONLY. DO NOT USE IN
	 * PRODUCTION.
	 * 
	 * @param filename - The filename of the database.
	 * @return
	 */
	private Connection connectToSqlite(String filename) {
		try {
			return DriverManager.getConnection("jdbc:sqlite:" + filename);
		} catch (Exception e) {
			System.out.println("Error when trying to connecting to database " + filename);
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	/**
	 * Connect to a PostgreSQL database.
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
				System.exit(-1);
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
		try (Statement stmt = conn.createStatement()) {

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
	 * Convert a String into a SHA256 hash.
	 * 
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
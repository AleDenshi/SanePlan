import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.URLConnection;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.json.*;
import java.nio.file.*;

public class WebServer {

	private HttpServer server;
	
	private ArrayList<User> users;
	private Map<String, String> sessionTokens;
	private Catalog catalog;
	private Path staticRoot;

	public static void main(String[] args) throws IOException {
		new WebServer().start();
	}

	public WebServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		users = new ArrayList<>();
		sessionTokens = new HashMap<>();
		staticRoot = Paths.get("static").toAbsolutePath().normalize();

		// TODO Transition this to database!
		this.catalog = new Catalog("courses.tsv");
		// Load test degree
		this.catalog.addDegree("degree.tsv");
		
		// TODO REMOVE THIS FOR PRODUCTION!
		// This is just the test user.
		User testUser = new User("test", "password", false);
		// This is the test admin.
		User testAdmin = new User("admin", "admin", true);
		CoursePlan plan = catalog.loadCoursePlanFromFile("plan.json");
		testUser.addPlan(plan);
		CoursePlan newplan = catalog.readCoursePlanFromTSV("plan.tsv");
		testUser.addPlan(newplan);
		users.add(testUser);
		users.add(testAdmin);
		
		server.createContext("/", this::handleStaticFile);
		server.createContext("/login", this::handleLogin);
		server.createContext("/submitPlan", this::handlePlanSubmission);
		server.createContext("/dashboard", this::handleDashboard);
		server.createContext("/plans", this::handlePlan);
		server.createContext("/addCourseToPlan", this::handleAddCourseToPlan);
		server.createContext("/deleteCourseFromPlan", this::handleDeleteCourseFromPlan);
		server.createContext("/addPlan", this::handleAddPlan);
		server.createContext("/deletePlan", this::handleDeletePlan);
		server.createContext("/addUser", this::handleAddUser);
		server.createContext("/deleteUser", this::handleDeleteUser);
		server.createContext("/addCourse", this::handleAddCourse);
		server.createContext("/deleteCourse", this::handleDeleteCourse);
		server.createContext("/editMetaCourse", this::handleEditMetaCourse);
		server.createContext("/addDegree", this::handleAddDegree);
		server.createContext("/deleteDegree", this::handleDeleteDegree);
		// TODO Add handler for deleteDegree
		// TODO Add handler for editSelf
		// TODO Add handler for editCourse
		// TODO Add handler for editUser
	}

	public void start() {
		server.start();
		System.out.println("Server running on http://localhost:" + server.getAddress().getPort());
	}

	/**
	 * Send a response to the client.
	 * 
	 * @param exchange
	 * @param status
	 * @param message
	 * @throws IOException
	 */
	private void sendResponse(HttpExchange exchange, int status, String message) throws IOException {
		byte[] bytes = message.getBytes();
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}

	/**
	 * Provides users with static files from /static when they ask for them.
	 * 
	 * @param exchange
	 * @throws IOException
	 */
	private void handleStaticFile(HttpExchange exchange) throws IOException {
		String requestPath = exchange.getRequestURI().getPath();

		if (requestPath.equals("/"))
			requestPath = "/login.html";

		Path filePath = staticRoot.resolve("." + requestPath).normalize();
		if (!filePath.startsWith(staticRoot)) {
			sendResponse(exchange, 403, "</h1>Forbidden</h1>");
			return;
		}

		if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
			sendResponse(exchange, 404, "<h1>404 Not Found</h1>");
			return;
		}

		String contentType = Files.probeContentType(filePath);
		if (contentType == null) {
			contentType = URLConnection.guessContentTypeFromName(filePath.toString());
		}
		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		exchange.getResponseHeaders().set("Content-Type", contentType);
		exchange.sendResponseHeaders(200, Files.size(filePath));
		try (OutputStream os = exchange.getResponseBody()) {
			Files.copy(filePath, os);
		}

	}

	/**
	 * Returns a user given a username.
	 * 
	 * @param username - A String username of a User in the system.
	 * @return The User, or null if the username doesn't exist.
	 */
	private User getUserByName(String username) {
		for (User user : users) {
			if (user.getUsername().equalsIgnoreCase(username))
				return user;
		}
		return null;
	}

	/**
	 * Returns the session token from a HTTP exchange for user authentication.
	 * 
	 * @param exchange - The HttpExchange being analyzed.
	 * @return A String representing a user's unique token.
	 */
	private String getSessionToken(HttpExchange exchange) {

		List<String> cookies = exchange.getRequestHeaders().get("Cookie");

		if (cookies == null)
			return null;

		for (String cookieHeader : cookies) {
			String[] cookiePairs = cookieHeader.split(";");

			for (String cookie : cookiePairs) {
				String[] kv = cookie.trim().split("=");

				if (kv.length == 2 && kv[0].equals("sessionToken")) {
					return kv[1];
				}
			}
		}

		return null;
	}

	// TODO This is awful. But I can't think of another way to do this monstrous
	// task.
	private String requisitesToList(Course course) {
		String list = "";

		int size = 0;
		for (ArrayList<Course> requisiteList : course.getPreRequisites()) {
			size += requisiteList.size();
		}

		int n = 0;
		for (ArrayList<Course> requisiteList : course.getPreRequisites()) {
			for (Course requisite : requisiteList) {
				list += requisite.getCode();
				if (n != size - 1) {
					list += ",";
				}
				n++;
			}
		}
		return list;
	}

	/**
	 * Verifies that a Http exchange is using POST, to make sure forms don't
	 * malfunction
	 * 
	 * @param exchange
	 * @throws IOException
	 */
	private void verifyPost(HttpExchange exchange) throws IOException {
		if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
			exchange.sendResponseHeaders(405, -1);
		}
	}

	/**
	 * Takes in HTML form data and converts it to a Map of Strings to Strings.
	 * 
	 * @param formData - The HTML form data.
	 * @return A Map of key Strings to value Strings.
	 * @throws UnsupportedEncodingException
	 */
	private Map<String, String> parseForm(String formData) throws UnsupportedEncodingException {
		Map<String, String> map = new HashMap<>();

		String[] pairs = formData.split("&");
		for (String pair : pairs) {
			String[] kv = pair.split("=");
			String key = URLDecoder.decode(kv[0], "UTF-8");
			String value = kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "";
			map.put(key, value);
		}

		return map;
	}

	/**
	 * Parses form data from a Http Exchange.
	 * 
	 * @param exchange
	 * @return
	 * @throws IOException
	 */
	private Map<String, String> getFormData(HttpExchange exchange) throws IOException {
		InputStream is = exchange.getRequestBody();
		String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		return parseForm(body);
	}

	/**
	 * Redirects the user on the current page to the URL location
	 * 
	 * @param exchange - The HttpExchange being handled.
	 * @param location - The URL to redirect the user to (eg. /dashboard).
	 * @throws IOException
	 */
	private void redirectTo(HttpExchange exchange, String location) throws IOException {
		exchange.getResponseHeaders().add("Location", location);
		exchange.sendResponseHeaders(302, -1);
	}

	/**
	 * Sends the user back to the page they came from.
	 * 
	 * @param exchange
	 * @throws IOException
	 */
	private void sendBackToPreviousPage(HttpExchange exchange) throws IOException {
		String referer = exchange.getRequestHeaders().getFirst("Referer");
		if (referer == null || referer.isEmpty()) {
			referer = "/dashboard";
		}
		redirectTo(exchange, referer);
	}

	/**
	 * Given a HttpExchange, returns the User currently authenticated, or null if no
	 * user is authenticated.
	 * 
	 * @param exchange
	 * @return
	 * @throws IOException
	 */
	private User authenticateSession(HttpExchange exchange) throws IOException {
		String token = getSessionToken(exchange);
		if (token == null || !sessionTokens.containsKey(token)) {
			String response = "<h1>401 - Unauthorized</h1><p>Please log in.</p>";
			sendResponse(exchange, 401, response);
			return null;
		}

		String username = sessionTokens.get(token);
		return getUserByName(username);
	}

	/**
	 * Given a HttpExchange, returns the Admin if the User is authenticated and an
	 * Admin, or null if these both are not fulfilled. Returns 401 if the user isn't
	 * logged in at all, and returns 403 (forbidden) if the user is authenticated
	 * but not an admin.
	 * 
	 * @param exchange
	 * @return
	 * @throws IOException
	 */
	private User authenticateAdmin(HttpExchange exchange) throws IOException {
		User admin = authenticateSession(exchange);
		if (admin.isAdmin()) {
			return admin;
		} else {
			String response = "<h1>403 - Forbidden</h1><p>You are not an admin.</p>";
			sendResponse(exchange, 403, response);
			return null;
		}
	}

	/**
	 * Authenticate the user if they have entered the correct password, and assign
	 * them a temporary cookie.
	 * 
	 * @param exchange
	 * @throws IOException
	 */
	private void handleLogin(HttpExchange exchange) throws IOException {
		verifyPost(exchange);
		Map<String, String> formData = getFormData(exchange);

		String username = formData.get("username");
		String password = formData.get("password");

		User matchedUser = getUserByName(username);

		if (matchedUser != null && matchedUser.isPassword(password)) {
			String token = UUID.randomUUID().toString();
			sessionTokens.put(token, username);

			exchange.getResponseHeaders().add("Set-Cookie", "sessionToken=" + token + "; Path=/");
			redirectTo(exchange, "/dashboard");
		} else {
			String response = (new HTMLPage("Login failed", "<a href=\"/\"><button>Back Home</button></a>")).toString();
			sendResponse(exchange, 401, response);
		}
	}

	/**
	 * Gives an authenticated user a basic dashboard.
	 * 
	 * @param exchange
	 * @throws IOException
	 */
	private void handleDashboard(HttpExchange exchange) throws IOException {
		User user = authenticateSession(exchange);
		if (user == null)
			return;

		// TODO Don't do it this way. Implement some kind of template system.
		String body = "<h1>Welcome, " + user.getUsername() + "!</h1>" + "<p>You are logged in.</p>";

		body += "<p>Here is a list of your course plans:</p>";
		body += "<ol>";
		for (CoursePlan plan : user.getPlans()) {
			// TODO Add delete button to this list entry?
			body += String.format("<li><a href=\"/plans/%s\">%s</a></li>", plan.getName(), plan.getName());
		}
		body += "</ol><br>";
		body += addPlanAsHTML();
		body += deletePlanAsHTML(user.getPlans());
		if (user.isAdmin()) {
			body += addUserAsHTML();
			body += deleteUserAsHTML();
			body += editUsernameAsHTML();
			body += addCourseAsHTML();
			body += deleteCourseAsHTML();
			body += deleteDegreeAsHTML();	
		}

		String response = (new HTMLPage("Dashboard", body)).toString();
		sendResponse(exchange, 200, response);
	}

	/**
	 * Displays a plan to the user.
	 * 
	 * @param exchange
	 * @throws IOException
	 */
	private void handlePlan(HttpExchange exchange) throws IOException {
		User user = authenticateSession(exchange);

		if (user == null)
			return;

		String path = exchange.getRequestURI().getPath();
		String[] parts = path.split("/");
		String body;
		CoursePlan plan;

		if (parts.length >= 3 && !parts[2].isEmpty()) {
			String name = parts[2];
			plan = user.findPlanByName(name);
			if (plan != null) {
				body = addCoursePromptAsHTML(plan.getName());
				body += deleteCoursePromptAsHTML(plan);
				body += planAsHTML(plan);
			} else {
				body = "<h1>Plan not found.</h1>";
			}
		} else {
			redirectTo(exchange, "/dashboard");
			return;
		}
		String response = (new HTMLPage(plan.getName(), body)).toString();
		sendResponse(exchange, 200, response);
	}

	/**
	 * Handles the adding of a new Course Plan to a user's list of plans.
	 * 
	 * @param exchange
	 * @throws IOException
	 */
	private void handleAddPlan(HttpExchange exchange) throws IOException {
		verifyPost(exchange);
		User user = authenticateSession(exchange);
		Map<String, String> formData = getFormData(exchange);

		String planName = formData.get("planName");
		// TODO Actually do something with the degree code
		String degreeCode = formData.get("degreeCode");
		System.out.println(formData);
		// TODO Does it make sense to do num semesters or maybe num years?
		int numSemesters = 0;
		String numSemestersString = formData.get("numSemesters");
		try {
			numSemesters = Integer.parseInt(numSemestersString);
		} catch (Exception e) {
			System.out.println("Unknown number of semesters " + numSemestersString);
		}
		boolean includeSummer = formData.containsKey("summer");

		if (!planName.isBlank() && numSemesters != 0) {
			System.out.println("Attempting to create plan named " + planName + " with " + numSemesters
					+ " semesters and summer: " + includeSummer);

			CoursePlan newPlan = new CoursePlan(planName);
			int year = 2026;
			for (int i = 0; i < numSemesters; i++) {
				SemesterType type;
				if (i % 2 == 0) {
					type = SemesterType.FALL;
				} else {
					type = SemesterType.SPRING;
				}
				Semester semester = new Semester(type.toString() + " " + year, type);
				newPlan.addSemester(semester);
				if (type == SemesterType.SPRING && includeSummer) {
					semester = new Semester(SemesterType.SUMMER.toString() + " " + year, SemesterType.SUMMER);
					newPlan.addSemester(semester);
				}
				if (i % 2 == 1) {
					year++;
				}
			}

			user.addPlan(newPlan);
		}
		sendBackToPreviousPage(exchange);
	}

	/**
	 * Handles the deletion of a plan from a user's list of plans.
	 * 
	 * @param exchange
	 * @throws IOException
	 */
	private void handleDeletePlan(HttpExchange exchange) throws IOException {
		verifyPost(exchange);
		User user = authenticateSession(exchange);
		Map<String, String> formData = getFormData(exchange);
		String planName = formData.get("planName");
		if (planName != null) {
			CoursePlan plan = user.removePlanByName(planName);
			if (plan == null) {
				System.out.println(planName + " not found, cannot be deleted.");
			}
		} else {
			System.out.println("No plan name found.");
		}

		sendBackToPreviousPage(exchange);
	}

	/**
	 * Handles the adding of a new user to the system.
	 * 
	 * @param exchange
	 * @throws IOException
	 */
	private void handleAddUser(HttpExchange exchange) throws IOException {
		verifyPost(exchange);
		authenticateAdmin(exchange);
		Map<String, String> formData = getFormData(exchange);

		String username = formData.get("username").toLowerCase();
		String password = formData.get("password");
		String passwordConfirm = formData.get("passwordConfirm");
		boolean isAdmin = formData.containsKey("admin");

		if (getUserByName(username) != null) {
			// TODO send a message to the user about this
		} else if (!password.equals(passwordConfirm)) {
			// TODO send a message to the user about this
		} else {
			User newUser = new User(username, password, isAdmin);
			users.add(newUser);
		}

		sendBackToPreviousPage(exchange);
	}

	private void handleDeleteUser(HttpExchange exchange) throws IOException {
		verifyPost(exchange);
		User user = authenticateAdmin(exchange);
		Map<String, String> formData = getFormData(exchange);
		String username = formData.get("username");

		User userToDelete = getUserByName(username);
		if (userToDelete == null) {
			// TODO send a message to the user about this
		} else if (userToDelete.equals(user)) {
			// TODO send a message to the user about this
		} else {
			// TODO We definitely should send a confirmation message at least
			users.remove(userToDelete);
			System.out.println("Removed " + username);
		}

		sendBackToPreviousPage(exchange);
	}
	
	private void handleEditUsername(HttpExchange exchange) throws IOException {
		// TODO Write this!!!
	}

	private void handleAddCourse(HttpExchange exchange) throws IOException {
		verifyPost(exchange);
		authenticateAdmin(exchange);
		Map<String, String> formData = getFormData(exchange);

		String courseCode = formData.get("courseCode");
		int courseCredits = Integer.parseInt(formData.get("courseCredits"));
		String courseName = formData.get("courseName");
		String courseDescription = formData.get("courseDescription");

		String availability = "";
		if (formData.containsKey("FALL")) {
			availability += "FALL, ";
		}
		if (formData.containsKey("SPRING")) {
			availability += "SPRING, ";
		}
		if (formData.containsKey("SUMMER")) {
			availability += "SUMMER";
		}

		String line = String.format("%s\t%d\t%s\t%s\t", courseCode, courseCredits, courseName, courseDescription, availability);

		for (int i = 1; i < 4; i++) {
			if (formData.containsKey("preRequisites"+i)) {
				line += "\t" + formData.get("preRequisites"+i);
			} else {
				line += "\t";
			}
		}
		
		for (int i = 1; i < 3; i++) {
			if (formData.containsKey("coRequisites"+i)) {
				line += "\t" + formData.get("coRequisites"+i);
			} else {
				line += "\t";
			}
		}

		String values[] = line.split("\t");
		Course newCourse = catalog.makeCourseFromValues(values);
		catalog.addCourse(newCourse);
		catalog.buildRequisitesFromValues(values);
		System.out.println("Done adding " + newCourse);
		sendBackToPreviousPage(exchange);
	}
	
	/**
	 * Handle adding a degree.
	 * @param exchange
	 * @throws IOException
	 */
	private void handleAddDegree(HttpExchange exchange) throws IOException {
		verifyPost(exchange);
		authenticateAdmin(exchange);
		Map<String, String> formData = getFormData(exchange);
		String name = formData.get("name");
		String code = formData.get("code");
		String description = formData.get("description");
		
		Degree degree = new Degree(name, code, description);
		
		int numRequirements = 1;
		while (true) {
			String nameId = "requirementName" + numRequirements;
			String codeId = "requirementCode" + numRequirements;
			System.out.println("Processing requirement " + numRequirements);
			if (formData.containsKey(nameId) && formData.containsKey(codeId)) {
				String reqName = formData.get(nameId);
				String reqCode = formData.get(codeId);
				ArrayList<Course> coursesRequired = catalog.getAllEquivalentCourses(reqCode);
				DegreeRequirement dr = new DegreeRequirement(reqName);
				dr.setEquivalentCourses(coursesRequired);
				degree.addDegreeRequirement(dr);
				numRequirements++;
			} else {
				break;
			}
		}
		catalog.addDegree(degree);
		sendBackToPreviousPage(exchange);
	}
	
	/**
	 * Handle deleting a degree.
	 * @param exchange
	 * @throws IOException
	 */
	private void handleDeleteDegree(HttpExchange exchange) throws IOException {
		verifyPost(exchange);
		authenticateAdmin(exchange);
		Map<String, String> formData = getFormData(exchange);
		String code = formData.get("code");
		if (catalog.deleteDegreeByCode(code) == null) {
			// TODO Tell the user the degree wasn't found
		}
		sendBackToPreviousPage(exchange);
	}

	private void handleDeleteCourse(HttpExchange exchange) throws IOException {
		verifyPost(exchange);
		authenticateAdmin(exchange);
		Map<String, String> formData = getFormData(exchange);
		String courseCode = formData.get("courseCode");
		Course course = catalog.findCourseByCode(courseCode);
		if (course == null) {
			// TODO Send a message to the user about this
		} else {
			catalog.getCourses().remove(course);
		}

		sendBackToPreviousPage(exchange);
	}

	/**
	 * Handles adding a course to a course plan
	 * 
	 * @param exchange
	 * @throws IOException
	 */
	private void handleAddCourseToPlan(HttpExchange exchange) throws IOException {
		verifyPost(exchange);
		User user = authenticateSession(exchange);
		Map<String, String> formData = getFormData(exchange);

		String planName = formData.get("planName");
		String code = formData.get("code");

		CoursePlan plan = user.findPlanByName(planName);
		Course course = catalog.findCourseByCode(code);

		if (course == null || plan == null) {
			// TODO Send the user a message about this
			System.out.println("Could not find plan or course.");
		} else if (plan.findSemesterIndexByCourseCode(code) != -1) {
			// TODO Send the user a message about this
			System.out.println(code + " is already present in " + planName);
		} else {
			// TODO Send the user a message about this
			System.out.println("Adding " + code + " to " + planName);
			plan.getSemesters().get(0).addCourse(course);
		}

		sendBackToPreviousPage(exchange);
	}

	/**
	 * Handles removing a course from a course plan
	 * 
	 * @param exchange
	 * @throws IOException
	 */
	private void handleDeleteCourseFromPlan(HttpExchange exchange) throws IOException {
		verifyPost(exchange);
		User user = authenticateSession(exchange);
		Map<String, String> formData = getFormData(exchange);

		String planName = formData.get("planName");
		String code = formData.get("code");

		CoursePlan plan = user.findPlanByName(planName);
		if (plan == null) {
			System.out.println("Could not find plan.");
		} else {
			Course course = plan.removeCourseByCode(code);
			if (course == null) {
				System.out.println("Could not find course " + code + " in plan " + planName);
			} else {
				System.out.println("Successfully removed " + code + " in plan " + planName);
			}
		}
		sendBackToPreviousPage(exchange);
	}

	private void handleEditMetaCourse(HttpExchange exchange) throws IOException {
		verifyPost(exchange);
		User user = authenticateSession(exchange);
		Map<String, String> formData = getFormData(exchange);

		String planName = formData.get("planName");
		String metaCode = formData.get("metaCode");
		String code = null;
		if (!formData.containsKey("code")) {
			sendBackToPreviousPage(exchange);
			return;
		} else {
			code = formData.get("code");
		}

		Course course = catalog.findCourseByCode(code);

		CoursePlan plan = user.findPlanByName(planName);
		if (plan == null) {
			// TODO we should print out an error to the user
		} else if (planName == null || metaCode == null || code == null) {
			// TODO we should print out an error to the user
		} else {
			plan.replaceCourseByCodes(metaCode, course);
		}

		sendBackToPreviousPage(exchange);
	}

	/**
	 * Parses a JSON course plan from a user and replaces it in their profile.
	 * 
	 * @param exchange
	 * @throws IOException
	 */
	private void handlePlanSubmission(HttpExchange exchange) throws IOException {
		if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
			exchange.sendResponseHeaders(405, -1);
			return;
		}

		User user = authenticateSession(exchange);
		if (user == null)
			return;
		System.out.println("Receiving plan from user " + user.getUsername());

		InputStream input = exchange.getRequestBody();
		BufferedReader reader = new BufferedReader(new InputStreamReader(input, "utf-8"));

		StringBuilder body = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			body.append(line);
		}

		try {
			JSONObject json = new JSONObject(body.toString());
			CoursePlan plan = catalog.makeCoursePlanFromJson(json);
			user.replacePlan(plan);

		} catch (Exception e) {
			e.printStackTrace();
			exchange.sendResponseHeaders(400, -1);
			return;
		}

		sendBackToPreviousPage(exchange);
	}

	private String addCourseAsHTML() {
		String html = """
				<form class="courseedit" method="POST" action="/addCourse">
				<p>Add a Course</p>
				<label>Course Code:</label>
				<input type="text" name="courseCode"><br/>
				<label>Course Credits:</label>
				<input type="number" name="courseCredits"><br/>
				<label>Course Name:</label>
				<input type="text" name="courseName"><br/>
				<label>Course Description:</label>
				<input type="textbox" name="courseName"><br/>
				
				<label for="courseAvailability">Course availability:</label><br/>
				<input type="checkbox" id="FALL" value="yes">
				<label>Fall</label><br/>
				<input type="checkbox" id="SPRING" value="yes">
				<label>Spring</label><br/>
				<input type="checkbox" id="SUMMER" value="yes">
				<label>Summer</label><br/>

				<p>Requisites should be entered as follows:
				<code>CS 223;EGR 115</code> (meaning either CS 223 or EGR 115)
				</p>
				<label>Pre-Requisite Group 1:</label>
				<input type="text" name="preRequisites1"><br/>
				<label>Pre-Requisite Group 2:</label>
				<input type="text" name="preRequisites2"><br/>
				<label>Pre-Requisite Group 3:</label>
				<input type="text" name="preRequisites3"><br/>

				<label>Co-Requisite Group 1:</label>
				<input type="text" name="coRequisites1"><br/>
				<label>Co-Requisite Group 2:</label>
				<input type="text" name="coRequisites2"><br/>
				<input type="submit" value="Create New Course">
				</form>
				""";
		return html;
	}
	


	private String deleteDegreeAsHTML() {
		String html = """
				<form class="degreeedit" method="POST" action="/deleteDegree">
				<p>Delete a Degree</p>
				<label>Choose a Degree to delete:</label>
				<select id="code" name="code">
				""";
		for (Degree degree : catalog.getDegrees()) {
			html += String.format("<option value=\"%s\">%s</option>", degree.getCode(), degree.getCode());
		}

		html += "</select><br/><input type=\"submit\" value=\"Delete Degree\"></form>";
		return html;
	}
	
	private String deleteCourseAsHTML() {
		String html = """
				<form class="courseedit" method="POST" action="/deleteCourse">
				<p>Delete a Course</p>
				<label>Choose a Course to delete:</label>
				<select id="courseCode" name="courseCode">
				""";
		for (Course course : catalog.getCourses()) {
			html += String.format("<option value=\"%s\">%s</option>", course.getCode(), course.getCode());
		}

		html += "</select><br/><input type=\"submit\" value=\"Delete Course\"></form>";
		return html;
	}

	private String addUserAsHTML() {
		String html = """
				<form class="useredit" method="POST" action="/addUser">
				<p>Add a new user</p>
				<label>Username:</label>
				<input type="text" name="username"><br>
				<label>Password:</label>
				<input type="password" name="password"><br>
				<label>Confirm Password:</label>
				<input type="password" name="passwordConfirm"><br>
				<input type="checkbox" id="isAdmin" value="yes">
				<label>Make an Admin</label><br/>
				<input type="submit" value="Create New User">
				</form>
				""";
		return html;
	}

	private String deleteUserAsHTML() {
		String html = """
				<form class="useredit" method="POST" action="/deleteUser">
				<p>Delete a user</p>
				<label>Choose a user to delete:</label>
				<select id="username" name="username">
				""";
		for (User user : users) {
			html += String.format("<option value=\"%s\">%s</option>", user.getUsername(), user.getUsername());
		}

		html += "</select><br/><input type=\"submit\" value=\"Delete User\"></form>";
		return html;
	}
	
	private String editUsernameAsHTML() {
		String html = """
				<form class="useredit" method="POST" action="/editUsername">
				<p>Edit a Username</p>
				<label>Choose a user to edit:</label>
				<select id="username" name="username">
				""";
		for (User user : users) {
			html += String.format("<option value=\"%s\">%s</option>", user.getUsername(), user.getUsername());
		}

		html += "</select><br/>";
		html += """
				<label for="newUsername">Enter the new Username:</label><br>
				<input type="text" id="newUsername" name="newUsername"><br>
				""";
		
		html += "<input type=\"submit\" value=\"Edit Username\"></form>";
		return html;
	}

	private String addPlanAsHTML() {
		// TODO make this a """ string
		// TODO Make the degree code field a list of all degrees
		String html = "<form class=\"planedit\" method=\"POST\" action=\"/addPlan\">\n"
				+ "<label>Plan Name:</label><br>\n" + "<input type=\"text\" name=\"planName\"><br>\n"
				+ "<label>Degree Code (optional):</label><br>\n" + "<input type=\"text\" name=\"degreeCode\"><br>\n"
				+ "<label>Number of Semesters:</label><br>\n" + "<input type=\"number\" name=\"numSemesters\"><br>\n"
				+ "<input type=\"checkbox\" id=\"summer\" name=\"summer\" value=\"yes\">\n"
				+ "<label for=\"summer\">Include Summer Semesters</label><br>\n"
				+ "<input type=\"submit\" value=\"Create New Plan\">\n" + "</form>";
		return html;
	}

	private String deletePlanAsHTML(ArrayList<CoursePlan> plans) {
		String html = "<form class=\"planedit\" method=\"POST\" action=\"/deletePlan\">\n";

		html += "<label for=\"planName\">Choose a Plan to Delete:</label><br>\n"
				+ "    <select id=\"planName\" name=\"planName\">\n";

		for (CoursePlan plan : plans) {
			html += String.format("<option value=\"%s\">%s</option>", plan.getName(), plan.getName());
		}
		html += "</select><br><br>";

		html += "<input type=\"submit\" value=\"Delete Plan\">\n" + "</form>";
		return html;
	}

	private String addCoursePromptAsHTML(String planName) {
		String html = "";
		html += "<form class=\"courseedit\" method=\"POST\" action=\"/addCourseToPlan\">\n"
				+ "<input type=\"hidden\" name=\"planName\" value=\"" + planName + "\">\n"
				+ "<label>Enter a Course Code to Add:</label><br>\n" + "<input type=\"text\" name=\"code\"><br><br>\n"
				+ "<input type=\"submit\" value=\"Add Course\">\n" + "</form>";

		return html;
	}

	private String deleteCoursePromptAsHTML(CoursePlan plan) {
		String html = "";
		html += "<form class=\"courseedit\" method=\"POST\" action=\"/deleteCourseFromPlan\">\n"
				+ "<input type=\"hidden\" name=\"planName\" value=\"" + plan.getName() + "\">\n";
		html += "<label for=\"code\">Choose a Course to Delete:</label><br>\n"
				+ "    <select id=\"code\" name=\"code\">\n";

		for (Course course : plan.getFlattenedCourseList()) {
			html += String.format("<option value=\"%s\">%s</option>", course.getCode(), course.getCode());
		}
		html += "</select><br><br>";

		html += "<input type=\"submit\" value=\"Remove Course\">\n" + "</form>";

		return html;
	}

	private String validityAsHTML(CoursePlan plan) {
		String html = "";
		if (plan.isValid()) {
			html += "<div class=\"validplan\"><p>This plan is <em>valid.</em><p></div>\n";
		} else {
			html += "<div class=\"invalidplan\"><p>This plan is <em>invalid.</em><p></div>\n";

			html += "<div class=\"validityissues\"><p>The following validity issues have been identified:</p>";
			html += "<ul>";
			ArrayList<String> validityIssues = plan.getValidityIssues();
			for (String issue : validityIssues) {
				html += String.format("<li>%s</li>", issue);
			}
			html += "</ul></div>";
		}
		return html;
	}

	private String degreeMetAsHTML(CoursePlan plan) {
		ArrayList<String> degreeIssues = catalog.determineIfMeetsDegree(plan);
		String html = "";
		if (degreeIssues == null) {
			html += "<div class=\"validplan\"><p>This plan <em>has no degree.</em><p></div>\n";
		} else if (degreeIssues.size() == 0) {
			html += "<div class=\"validplan\"><p>This plan <em>meets degree requirements.</em><p></div>\n";
		} else {
			html += "<div class=\"invalidplan\"><p>This plan <em>does not meet degree requirements.</em><p></div>\n";

			html += "<div class=\"validityissues\"><p>The following degree issues have been identified:</p>";
			html += "<ul>";
			for (String issue : degreeIssues) {
				html += String.format("<li>%s</li>", issue);
			}
			html += "</ul></div>";
		}
		
		return html;
	}
	
	private String planAsHTML(CoursePlan plan) {
		plan.determineValidity();
		String html = "";
		html += validityAsHTML(plan);
		html += degreeMetAsHTML(plan);
		ArrayList<Semester> semesters = plan.getSemesters();

		html += "<div class=\"board\" id=\"" + plan.getName() + "\">\n";

		for (Semester semester : semesters) {
			html += semesterAsHTML(semester, plan);
		}

		html += "</div>\n";
		html += "<svg id=\"arrows\"></svg>\n";
		html += "<script src=\"/planedit.js\" defer></script>\n";
		html += "<form id=\"exportForm\">\n" + "  <button type=\"submit\">Edit Plan</button>\n" + "</form>";

		html += "<script src=\"/planexport.js\" defer></script>\n";

		return html;
	}

	private String semesterAsHTML(Semester semester, CoursePlan plan) {
		String html = "<div class=\"column\" data-type=\"" + semester.getType() + "\">";
		html += "<h3>" + semester.getName() + "</h3>";
		for (Course course : semester.getSemesterCourses()) {
			html += courseAsHTML(course, plan);
		}
		html += "</div>\n";
		return html;
	}

	private String courseAsHTML(Course course, CoursePlan plan) {
		if (course instanceof MetaCourse) {
			return metaCourseAsHTML((MetaCourse) course, plan);
		}
		String template = """
				<div class="course" draggable="true" id="%s" style="height: %spx" data-type="%s" data-prereq="%s" data-credits="%s">
				<p class="code">%s</p>
				<p class="name">%s</p>
				<p class="credits">%s</p>
				</div>
						""";
		return String.format(template, course.getCode(), course.getCredits() * 40, course.getTypeAsString(),
				requisitesToList(course), course.getCredits(), course.getCode(), course.getName(), course.getCredits());
	}

	/**
	 * Special method for generating meta courses
	 * 
	 * @param course
	 * @param planName
	 * @return
	 */
	private String metaCourseAsHTML(MetaCourse course, CoursePlan plan) {
		String template = """
				<div class="course" draggable="true" id="%s" style="height: %spx" data-type="META" data-prereq="" data-credits="%s">
				<p class="code">%s</p>
				<p class="name">%s</p>
				<p class="credits">%s</p>
				<form class="meta" method="POST" action="/editMetaCourse">
				<input type="hidden" name="planName" value="%s">
				<input type="hidden" name="metaCode" value="%s">
				<select id="code" name="code">
				""";
		String html = String.format(template, course.getCode(), course.getCredits() * 40, course.getCredits(),
				course.getCode(), course.getName(), course.getCredits(), plan.getName(), course.getCode());

		for (Course equivalent : course.getEquivalencies()) {
			if (!plan.hasCourse(equivalent.getCode())) {
				html += String.format("<option value=\"%s\">%s</option>", equivalent.getCode(), equivalent.getCode());
			}
		}

		html += "</select><br/><input type=\"submit\" value=\"Replace\"></form></div>";
		return html;
	}
}
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

		// TODO REMOVE THIS FOR PRODUCTION!
		// This is just the test user.
		User testUser = new User("test", "password");
		CoursePlan plan = catalog.loadCoursePlanFromFile("plan.json");
		testUser.addPlan(plan);
		users.add(testUser);

		server.createContext("/", this::handleStaticFile);
		server.createContext("/login", this::handleLogin);
		server.createContext("/submit-plan", this::handlePlanSubmission);
		server.createContext("/dashboard", this::handleDashboard);
		server.createContext("/plans", this::handlePlan);
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
	 * Authenticate the user if they have entered the correct password, and assign
	 * them a temporary cookie.
	 * 
	 * @param exchange
	 * @throws IOException
	 */
	private void handleLogin(HttpExchange exchange) throws IOException {

		if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
			exchange.sendResponseHeaders(405, -1);
			return;
		}

		InputStream is = exchange.getRequestBody();
		String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

		Map<String, String> formData = parseForm(body);

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
	 * Parses a JSON course plan from a user and replaces it in their profile.
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

		String referer = exchange.getRequestHeaders().getFirst("Referer");
		if (referer == null || referer.isEmpty()) {
			referer = "/plans";
		}
		redirectTo(exchange, referer);
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
		body += "</ol>";

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
				body = planAsHTML(plan);
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

	private String planAsHTML(CoursePlan plan) {
		plan.determineValidity();
		String html = "";
		html += validityAsHTML(plan);
		ArrayList<Semester> semesters = plan.getSemesters();

		html += "<div class=\"board\" id=\"" + plan.getName() + "\">\n";

		for (Semester semester : semesters) {
			html += semesterAsHTML(semester);
		}
		
		html += "</div>\n";
		html+= "<svg id=\"arrows\"></svg>\n";
		html += "<script src=\"/planedit.js\" defer></script>\n";
		html += "<form id=\"exportForm\">\n"
				+ "  <button type=\"submit\">Edit Plan</button>\n"
				+ "</form>";
		
		html += "<script src=\"/planexport.js\" defer></script>\n";
		
		return html;
	}

	private String semesterAsHTML(Semester semester) {
		String html = "<div class=\"column\" data-type=\"" + semester.getType() + "\">";
		html += "<h3>" + semester.getName() + "</h3>";
		for (Course course : semester.getSemesterCourses()) {
			html += courseAsHTML(course);
		}
		html += "</div>\n";
		return html;
	}

	private String courseAsHTML(Course course) {
		String template = """
				<div class="course" draggable="true" id="%s" style="height: %spx" data-type="%s" data-prereq="%s">
				<p class="code">%s</p>
				<p class="name">%s</p>
				<p class="credits">%s</p>
				</div>
						""";
		return String.format(template, course.getCode(), course.getCredits() * 40, course.getTypeAsString(),
				requisitesToList(course), course.getCode(), course.getName(), course.getCredits());
	}
	
	// TODO This is awful. But I can't think of another way to do this monstrous task.
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
				if (n != size-1) {
					list += ",";
				}
				n++;
			}
		}
		return list;
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

	/**
	 * Returns a user given a username.
	 * 
	 * @param username - A String username of a User in the system.
	 * @return The User, or null if the username doesn't exist.
	 */
	private User getUserByName(String username) {
		for (User user : users) {
			if (user.getUsername().equals(username))
				return user;
		}
		return null;
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
}
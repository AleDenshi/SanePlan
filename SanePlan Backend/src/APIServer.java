//TODO Hook the API up to basic public functionality (Catalog info)
//TODO Hook the API up to private functionality (User CoursePlan reading and writing)
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class APIServer {

    private HttpServer server;
    private ArrayList<User> users;
    private Map<String, String> sessionTokens;
    private Catalog catalog;
    
    public static void main(String[] args) throws IOException {
        new APIServer().start();
    }

    public APIServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        users = new ArrayList<>();
        sessionTokens = new HashMap<>();

        // TODO Transition this to database!
        this.catalog = new Catalog("courses.tsv");
        
        // TODO REMOVE THIS FOR PRODUCTION!
        // This is just the test user.
        User testUser = new User("test", "password");
        CoursePlan plan = catalog.loadCoursePlanFromFile("plan.json");
        testUser.addPlan(plan);
        users.add(testUser);
        
        server.createContext("/", this::handleLoginPage);
        server.createContext("/style.css", this::handleCSS);
        server.createContext("/login", this::handleLogin);
        server.createContext("/dashboard", this::handleDashboard);
    }

    public void start() {
        server.start();
        System.out.println("Server running on http://localhost:" + server.getAddress().getPort());
    }

    private void handleLoginPage(HttpExchange exchange) throws IOException {

    	// TODO Make this its own method and repeat for handleCSS
        File file = new File("login.html");

        if (!file.exists()) {
            String response = "<h1>404 - login.html not found</h1>";
            exchange.sendResponseHeaders(404, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }

        byte[] fileBytes = Files.readAllBytes(Paths.get("login.html"));

        exchange.getResponseHeaders().add("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, fileBytes.length);

        OutputStream os = exchange.getResponseBody();
        os.write(fileBytes);
        os.close();
    }
    
    private void handleCSS(HttpExchange exchange) throws IOException {

        File file = new File("style.css");

        if (!file.exists()) {
            String response = "<h1>404 - style.css not found</h1>";
            exchange.sendResponseHeaders(404, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }

        byte[] fileBytes = Files.readAllBytes(Paths.get("style.css"));

        exchange.getResponseHeaders().add("Content-Type", "text/css");
        exchange.sendResponseHeaders(200, fileBytes.length);

        OutputStream os = exchange.getResponseBody();
        os.write(fileBytes);
        os.close();
    }

    
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

        User matchedUser = null;

        for (User user : users) {
            if (user.getUsername().equals(username) && user.isPassword(password)) {
                matchedUser = user;
                break;
            }
        }

        String response;

        if (matchedUser != null) {
            String token = UUID.randomUUID().toString();
            sessionTokens.put(token, username);

            exchange.getResponseHeaders().add(
                    "Set-Cookie",
                    "sessionToken=" + token + "; Path=/"
            );

            response = "<h1>Login successful</h1>";
            exchange.sendResponseHeaders(200, response.getBytes().length);
        } else {
            response = "<h1>Login failed</h1>";
            exchange.sendResponseHeaders(401, response.getBytes().length);
        }

        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    
    /**
     * Gives an authenticated user a basic dashboard.
     * @param exchange
     * @throws IOException
     */
    private void handleDashboard(HttpExchange exchange) throws IOException {

    	
        String token = getSessionToken(exchange);
        // TODO Turn this into a method for reuse
        if (token == null || !sessionTokens.containsKey(token)) {
            String response = "<h1>401 - Unauthorized</h1><p>Please log in.</p>";
            exchange.sendResponseHeaders(401, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }

        String username = sessionTokens.get(token);
        User user = getUserByName(username);
        
        // TODO Don't do it this way. Implement some kind of template system.
        String response = "<h1>Welcome, " + username + "!</h1>"
                        + "<p>You are logged in.</p>";
        
        response += "<p>Here is a list of your course plans:</p>";
        response += "<ol>";
        for (CoursePlan plan : user.getPlans()) {
        	response += "<li>" + plan.getName() + "</li>";
        	response += planAsHTML(plan);
        }
        response += "</ol>";

        exchange.sendResponseHeaders(200, response.getBytes().length);

        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    
    public String planAsHTML(CoursePlan plan) {
    	String html = "";
    	ArrayList<Semester> semesters = plan.getSemesters();
    	// TODO There has to be a better way of doing this HTML
    	for (Semester semester : semesters) {
    		html += "<div class=\"classcol\">\n";
    		for (Course course : semester.getSemesterCourses()) {
    			html += "<div class=\"course\">\n";
    			html += "<p class=\"code\">" + course.getCode() + "</p>\n";
    			html += "<p class=\"credits\">" + course.getCredits() + "</p>\n";
    			html += "<p class=\"name\">" + course.getName() + "</p>\n";
    			html += "</div>\n";
    		}
    		html += "</div>\n";
    	}
    	return html;
    }
    
    /**
     * Returns the session token from a HTTP exchange for user authentication.
     * @param exchange - The HttpExchange being analyzed.
     * @return A String representing a user's unique token.
     */
    private String getSessionToken(HttpExchange exchange) {

        List<String> cookies = exchange.getRequestHeaders().get("Cookie");

        if (cookies == null) return null;

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
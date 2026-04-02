/**
 * A webpage referencing a set stylesheet (defaults to /style.css) and various attributes that can be changed
 * @author alessandro
 *
 */
public class HTMLPage {
	String title, stylesheet, body;
	
	public HTMLPage() {
		this.title = "Default Title";
		this.body = "<p>This is the default page.</p>";
		this.stylesheet = "/style.css";
	}

	/**
	 * Construct a HTML webpage.
	 * @param title - The title of the page.
	 * @param stylesheet - A URL pointing to the stylesheet (eg. /style.css).
	 * @param body - The body content of the page, in HTML.
	 */
	public HTMLPage(String title, String stylesheet, String body) {
		this.title = title;
		this.stylesheet = stylesheet;
		this.body = body;
	}

	public HTMLPage(String title, String body) {
		this.title = title;
		this.body = body;
		this.stylesheet = "/style.css";
	}
	
	@Override
	public String toString() {
		String template = """
				<!DOCTYPE html>
				<html>
				<head>
				    <title>%s</title>
				    <link rel="stylesheet" href="%s">
				</head>
				<body>
				<header><h1>%s</h1>
				<nav>
				<p>
				<a href="/dashboard">Dashboard</a>
				<a href="/settings">Settings</a>
				</p>
				</nav>
				</header>
				<main>
				%s
				</main>
				</body>
				</html>
				""";
		return String.format(template, title, stylesheet, title, body);
	}
}

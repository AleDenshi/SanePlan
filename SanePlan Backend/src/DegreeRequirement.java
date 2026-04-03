import java.util.*;

public class DegreeRequirement {
	String expression;
	String name;

	static abstract class Node {
		abstract boolean eval(Set<String> values);
	}

	// TODO This is horrible. I've got to find a better way to do this.
	static class VarNode extends Node {
		String name;

		VarNode(String name) {
			this.name = name;
		}

		boolean eval(Set<String> values) {
			return values.contains(name);
		}
	}

	static class AndNode extends Node {
		Node left, right;

		AndNode(Node left, Node right) {
			this.left = left;
			this.right = right;
		}

		boolean eval(Set<String> values) {
			return left.eval(values) && right.eval(values);
		}
	}

	static class OrNode extends Node {
		Node left, right;

		OrNode(Node left, Node right) {
			this.left = left;
			this.right = right;
		}

		boolean eval(Set<String> values) {
			return left.eval(values) || right.eval(values);
		}
	}

	public static Node parse(String expr) {
		expr = expr.trim();
		if (expr.startsWith("(") && expr.endsWith(")")) {
			expr = expr.substring(1, expr.length() - 1);
		}

		int depth = 0;
		for (int i = 0; i < expr.length(); i++) {
			char c = expr.charAt(i);
			if (c == '(')
				depth++;
			if (c == ')')
				depth--;
			if (depth == 0 && c == '|') {
				return new OrNode(parse(expr.substring(0, i)), parse(expr.substring(i + 1)));
			}
		}
		depth = 0;
		for (int i = 0; i < expr.length(); i++) {
			char c = expr.charAt(i);
			if (c == '(')
				depth++;
			if (c == ')')
				depth--;
			if (depth == 0 && c == '&') {
				return new AndNode(parse(expr.substring(0, i)), parse(expr.substring(i + 1)));
			}
		}

		return new VarNode(expr);
	}

	public static boolean evaluate(String expr, Set<String> values) {
		Node root = parse(expr);
		return root.eval(values);
	}

	public static void main(String[] args) {
		String expr = "(CHM 111|GEO 215|WX 201|PS 150|PS 227) & ((BIO 120 & BIO 120L) | (CHM 110 & CHM 110L) | (PS 224 & PS 224L) | (PS 226 & PS 226L) | (PS 250 & PS 253))";
		expr = expr.replaceAll("\\s", "");
		Set<String> list = Set.of("WX201", "BIO120", "BIO120L");

		System.out.println(evaluate(expr, list)); // true
	}
}
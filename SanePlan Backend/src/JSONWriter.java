import java.util.ArrayList;

/*
 * Performs common JSON tasks needed amongst various classes
 * TODO Is this really best for coupling? I'm not sure...
 */
public class JSONWriter {

	/**
	 * Turn any list of toString()-able objects into a 1D JSON String list.
	 * @param <T> - The Type of the ArrayList.
	 * @param objList - The List in question.
	 * @return - A String representing a JSON list of the objects in the objList parameter.
	 */
	public <T> String listToJson(ArrayList<T> objList) {
		String value = "[";
		for (int i = 0; i < objList.size(); i++) {
			String obj = objList.get(i).toString();
			if (obj != null) {
				value += "\"" + obj + "\"";
			}
			if (i < objList.size() - 1)
				value += ", ";
		}
		value += "]";
		return value;
	}
	
	/**
	 * Turn any list of lists of toString()-able objects into a 2D JSON String list.
	 * @param <T> - The Type of the ArrayList.
	 * @param objList - The List in question.
	 * @return - A String representing a JSON list of lists of the objects in the objList parameter.
	 */
	public <T> String listListToJson(ArrayList<ArrayList<T>> objListList) {
		String value = "[";
		for (int i = 0; i < objListList.size(); i++) {
			value += "[";
			ArrayList<T> objList = objListList.get(i);
			for (int j = 0; j < objList.size(); j++) {
				T obj = objList.get(j);
				if (obj != null) {
					value += "\"" + obj.toString() + "\"";
				}
				if (j < objList.size() - 1)
					value += ", ";
			}
			value += "]";
			if (i < objListList.size() - 1)
				value += ", ";
		}
		value += "]";
		return value;
	}
	
	

}

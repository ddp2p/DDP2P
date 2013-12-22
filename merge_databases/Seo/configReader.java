/*
 * Author     : Srini Venkatesh, Yonglok Seo
 * Subject    : CSE 5260
 * Instructor : Dr. M. Silaghi
 * */
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

/*
 * Class: configReader
 * 
 * Implements the parser to parse the table name, and its attributes from configuraton file.
 * 
 */
public final class configReader {
	private final String fileName;

	/*
	 * Constructor
	 */
	public configReader(final String fileName) {
		this.fileName = fileName;
	}

	/*
	 * Function: read
	 * 
	 * Reads the configuration file line by line to parse the contents.
	 * 
	 */
	public final ArrayList<Config> read() {
		try {
			final BufferedReader br = new BufferedReader(new FileReader(
					fileName));
			String s = "";
			String a = "";
			final ArrayList<Config> clist = new ArrayList<Config>();
			while ((s = br.readLine()) != null) {

				if (s.length() != 0) {
					if (!s.contains(":")) {
						while (s.contains("(")) {
							int indexLB = s.indexOf("(");
							int indexRB = s.indexOf(")");

							s = s.substring(0, indexLB)
									+ s.substring(indexRB + 1, s.length());
						}
					}
					if (s.contains(".") && !s.contains(":")) {
						a += s.substring(0, s.indexOf(".")) + " ";
						if (!a.contains("application")) {
							clist.add(createConfig(a));
						}
						a = "";
					} else if (s.contains("#")) {
						int indexToCut = s.indexOf("#");
						a += s.substring(0, indexToCut) + " ";
					} else {
						a += s + " ";
					}
				}
			}
			br.close();
			return clist;
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	/*
	 * Function: createConfig
	 * 
	 * Creates the Config class objects based on the string extracted from configuration file.
	 * 
	 */
	public final Config createConfig(final String s) {
		final StringTokenizer st = new StringTokenizer(s, ":");
		final ArrayList<String[]> configList = new ArrayList<String[]>();
		String table = "";
		String[][] constraints = null;
		boolean constraintExist = false;
		while (st.hasMoreTokens()) {
			table = st.nextToken().trim();
			if (table.contains("(")) {
				constraintExist = true;
				int indexLB = table.indexOf("(") + 1;
				int indexRB = table.indexOf(")");
				String constr = table.substring(indexLB, indexRB);
				table = table.substring(0, indexLB - 1);
				StringTokenizer constST = new StringTokenizer(constr, ",");
				constraints = new String[constST.countTokens()][2];
				int counter = 0;
				while (constST.hasMoreTokens()) {
					String tempConst = constST.nextToken();
					int indxPeriod = tempConst.indexOf(".");
					constraints[counter][0] = tempConst
							.substring(0, indxPeriod);
					constraints[counter][1] = tempConst.substring(
							indxPeriod + 1, tempConst.length());
					counter++;
				}
			}
			final StringTokenizer st2 = new StringTokenizer(st.nextToken(), ";");
			while (st2.hasMoreTokens()) {
				final StringTokenizer st3 = new StringTokenizer(
						st2.nextToken(), ",");
				final String[] temp = new String[st3.countTokens()];
				int counter = 0;
				while (st3.hasMoreTokens()) {
					String val = st3.nextToken().trim();
					if (!val.equals("")) {
						temp[counter] = val;
						if (val.contains("->")) {
							temp[counter] = val.substring(0, val.indexOf("->"));
						}
					}
					counter++;
				}
				counter = 0;
				configList.add(temp);
			}
		}
		Config c = null;
		if (configList.size() == 6) {
			c = new Config(table, configList.get(0), configList.get(1),
					configList.get(2), configList.get(3), configList.get(4),
					configList.get(5));
		} else {
			c = new Config(table, configList.get(0), configList.get(1),
					configList.get(2), configList.get(3), configList.get(4),
					configList.get(5), configList.get(6));
		}
		if (constraintExist)
			c.setConstraints(constraints);
		return c;
	}
}

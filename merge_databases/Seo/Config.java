/*
 * Author     : Srini Venkatesh, Yonglok Seo
 * Subject    : CSE 5260
 * Instructor : Dr. M. Silaghi
 * */

/*
 * Class: Config
 * 
 * Implements the functionality to handle the config file.
 * 
 */
public final class Config {
	private final String table;
	private final String pseudo;
	private final String[] key;
	private final String[] date1;
	private final String[] globs;
	private final String[] date2;
	private final String[] pref;
	private final String[] instance_data;
	private String[][] constraints = null;
	private boolean constraintsExist = false;

	/*
	 * Constructor
	 * 
	 */
	public Config(final String table, final String[] pseudo,
			final String[] key, final String[] date1, final String[] globs,
			final String[] date2, final String[] pref) {
		this.table = table;
		this.pseudo = initPseudo(pseudo[0]);
		this.key = key;
		this.date1 = date1;
		this.globs = globs;
		this.date2 = date2;
		this.pref = pref;
		this.instance_data = null;
	}

	/*
	 * Constructor
	 * 
	 */
	public Config(final String table, final String[] pseudo,
			final String[] key, final String[] date1, final String[] globs,
			final String[] date2, final String[] pref,
			final String[] instance_data) {
		this.table = table;
		this.pseudo = initPseudo(pseudo[0]);
		this.key = key;
		this.date1 = date1;
		this.globs = globs;
		this.date2 = date2;
		this.pref = pref;
		this.instance_data = instance_data;
	}

	/*
	 * Function: initPseudo
	 * 
	 * Initializes the Pseudo key to "ROW" if it's null.
	 * 
	 */
	private final String initPseudo(String input){
		if(input == null) input = "ROW";
		return input;
	}
	
	/*
	 * Function: setConstraints
	 * 
	 * Sets the constraints value
	 * 
	 */
	public void setConstraints(final String[][] constraints) {
		this.constraints = constraints;
		this.constraintsExist = true;
	}

	/*
	 * Function: getConstraints
	 * 
	 * Retrieves the constrains value
	 * 
	 */
	public final String[][] getConstraints() {
		return this.constraints;
	}

	/*
	 * Function: hasConstraints
	 * 
	 * Check to see is there any constraints exists?
	 * 
	 */
	public final boolean hasConstraints() {
		return this.constraintsExist;
	}

	/*
	 * Function: print
	 * 
	 * Prints the complete configuration file details after parsing.
	 * 
	 */
	public void print() {
		System.out
				.println("---------------------------------------------------------");
		System.out.println("Table Name = " + table);
		if (constraints != null) {
			System.out.print("\tconstraints = ");
			for (int i = 0; i < constraints.length; ++i) {
				System.out.print(constraints[i][0] + " refers "
						+ constraints[i][1] + "  ");
			}
			System.out.println();
		}
		System.out.println("\tPseudo Key = " + pseudo);
		System.out.print("\tKey = ");
		if (key[0] != null) {
			for (int i = 0; i < key.length; ++i) {
				System.out.print(key[i] + ", ");

			}
		} else {
			System.out.print("none");
		}
		System.out.println();

		System.out.print("\tDate 1 = ");
		if (date1[0] != null) {
			for (int i = 0; i < date1.length; ++i) {

				System.out.print(date1[i] + ", ");

			}
		} else {
			System.out.print("none");
		}
		System.out.println();

		System.out.print("\tGlobs = ");
		if (globs[0] != null) {
			for (int i = 0; i < globs.length; ++i) {
				System.out.print(globs[i] + ", ");
			}
		} else {
			System.out.print("none");
		}
		System.out.println();

		System.out.print("\tDate 2 = ");
		if (date2[0] != null) {
			for (int i = 0; i < date2.length; ++i) {

				System.out.print(date2[i] + ", ");

			}
		} else {
			System.out.print("none");
		}
		System.out.println();

		System.out.print("\tPref = ");
		if (pref[0] != null) {
			for (int i = 0; i < pref.length; ++i) {
				System.out.print(pref[i] + ", ");
			}
		} else {
			System.out.print("none");
		}
		System.out.println();

		System.out.print("\tinstance_data = ");
		if (instance_data != null && instance_data[0] != null) {
			for (int i = 0; i < instance_data.length; ++i) {
				System.out.print(instance_data[i] + ", ");
			}
		} else {
			System.out.print("none");
		}
		System.out.println();
	}

	public final String getTable() {
		return this.table;
	}

	public final String getPseudo() {
		return this.pseudo;
	}

	public final String[] getKey() {
		return this.key;
	}

	public final String[] getDate1() {
		return this.date1;
	}

	public final String[] getGlobs() {
		return this.globs;
	}

	public final String[] getDate2() {
		return this.date2;
	}

	public final String[] getPref() {
		return this.pref;
	}

	public final String[] getInstanceD() {
		return this.instance_data;
	}	
}

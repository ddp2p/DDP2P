/*
 * Author     : Srini Venkatesh, Yonglok Seo
 * Subject    : CSE 5260
 * Instructor : Dr. M. Silaghi
 * */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/*
 * Class: Process
 * 
 * Class which implements the database connection and the two traversals for the database migration
 * 
 */
public final class Process {
	private final String JDBC_SQLITE_STR = "jdbc:sqlite:";
	private final String DB_LOCATION_Yonglok = "C:\\Users\\Yonglok\\workspace\\Database Project";
	private final String DB_LOCATION = "C:\\Users\\Srinivasa Venkatesh\\workspace\\DB Project";
	private final String DB_NEW = "new\\deliberation-app.db";
	private final String DB_OLD = "old\\deliberation-app.db";
	private final String DB_MERGE = "merged\\MySQLiteDB";
	private final String SEPERATOR = "\\";
	private final String DBStructureFileName;
	private final QBuilder qb;
	private final Connection conMerge;
	private final Connection conNew;

	/*
	 * Constructor
	 * 
	 * Establishes the connection to given Database
	 * 
	 */
	public Process(String DBStructureFileName) {
		this.DBStructureFileName = DBStructureFileName;
		this.qb = new QBuilder();
		final String DBLocationMerge = JDBC_SQLITE_STR + DB_LOCATION_Yonglok
				+ SEPERATOR + DB_MERGE;
		this.conMerge = new DBConnect(DBLocationMerge).connect();
		final String DBLocationNew = JDBC_SQLITE_STR + DB_LOCATION_Yonglok
				+ SEPERATOR + DB_NEW;
		this.conNew = new DBConnect(DBLocationNew).connect();
	}

	/*
	 * Constructor
	 * 
	 * Establishes the connection to Database
	 * 
	 */
	public Process() {
		this.DBStructureFileName = "createEmptyDelib.sqlite";
		this.qb = new QBuilder();
		final String DBLocationMerge = JDBC_SQLITE_STR + DB_LOCATION_Yonglok
				+ SEPERATOR + DB_MERGE;
		this.conMerge = new DBConnect(DBLocationMerge).connect();
		final String DBLocationNew = JDBC_SQLITE_STR + DB_LOCATION_Yonglok
				+ SEPERATOR + DB_NEW;
		this.conNew = new DBConnect(DBLocationNew).connect();
	}

	/*
	 * Function: phase1
	 * 
	 * Establish dependencies for foreign keys.
	 * Read the "GID_key_field_k" and "pseudo_key" for that table from extra_db!
	 * A missing pseudokey is interpreted as "ROW".
	 * The GID_key_field can be described as foreign->table.GID
	 * Search for it in the corresponding table in target_db extracting its pseudo_key!
	 * 
	 */
	public void phase1(final Config c) {
		try {
			final String q = "select * from " + c.getTable();
			System.out
					.println(String
							.format("==============================deleting from %s===========================",
									c.getTable()));
			boolean constraintsExist = c.hasConstraints();
			Statement stNew = conNew.createStatement();
			ResultSet rsNew = stNew.executeQuery(q);
			String[] dateField1 = c.getDate1();
			String[] dateField2 = c.getDate2();
			boolean dateExist = false;
			if (c.getDate1()[0] != null || c.getDate2()[0] != null)
				dateExist = true;
			if (!dateExist) {
				System.out.println("no dates to compare");
			} else {
				while (rsNew.next()) {
					String ID = rsNew.getString(c.getPseudo());
					String mergeQ = "";
					if (c.getPseudo().equals("ROW")) {
						String conds = "";
						for (int i = 0; i < c.getKey().length; ++i) {
							conds += c.getKey()[i] + " = '"
									+ rsNew.getString(c.getKey()[i]) + "'";
							if (i < c.getKey().length)
								conds += " AND ";
						}
						mergeQ = String.format("Select * from %s where %s",
								conds);
					} else {
						mergeQ = String.format(
								"Select * from %s where %s = '%s'",
								c.getTable(), c.getPseudo(), ID);
					}
					Statement stMerge = conMerge.createStatement();
					ResultSet rsMerge = stMerge.executeQuery(mergeQ);
					while (rsMerge.next()) {
						boolean newer = false;
						if (dateField1[0] != null) {
							for (int i = 0; i < dateField1.length; ++i) {
								Date dateNew = rsNew.getDate(dateField1[i]);
								Date dateOld = rsMerge.getDate(dateField1[i]);
								if (dateOld == null || dateNew == null) {
									newer = true;
								} else if (dateNew.after(dateOld)) {
									newer = true;
								}
								if (newer)
									break;
							}
							if (newer) {
								if (constraintsExist) {
									String[][] constraints = c.getConstraints();
									for (int i = 0; i < constraints.length; ++i) {
										String constTable = constraints[i][0];
										String constID = constraints[i][1];
										String deleteConstraintQ = qb
												.buildDelete(constTable,
														constID, ID);
										Statement deleteST = conMerge
												.createStatement();
										deleteST.execute(deleteConstraintQ);
									}
								}
								String deleteQ = qb.buildDelete(c.getTable(),
										c.getPseudo(), ID);
								Statement deleteST = conMerge.createStatement();
								deleteST.execute(deleteQ);
							} else {
								if (dateField2[0] != null) {
									for (int i = 0; i < dateField2.length; ++i) {
										Date dateNew2 = rsNew
												.getDate(dateField2[i]);
										Date dateOld2 = rsMerge
												.getDate(dateField2[i]);
										if (dateOld2 == null
												|| dateNew2 == null) {
											newer = true;
										} else if (dateNew2.after(dateOld2)) {
											newer = true;
										}
										if (newer)
											break;
									}
									if (newer) {
										if (constraintsExist) {
											String[][] constraints = c
													.getConstraints();
											for (int i = 0; i < constraints.length; ++i) {
												String constTable = constraints[i][0];
												String constID = constraints[i][1];
												String deleteConstraintQ = qb
														.buildDelete(constTable,
																constID, ID);
												Statement deleteST = conMerge
														.createStatement();
												deleteST.execute(deleteConstraintQ);
											}
										}
										String deleteQ = qb
												.buildDelete(c.getTable(),
														c.getPseudo(), ID);
										Statement deleteST = conMerge
												.createStatement();
										deleteST.execute(deleteQ);
									} else {
										System.out.println("not newer");
									}
								}
							}
						}
					}
					rsMerge.close();
				}
				rsNew.close();
			}
		} catch (SQLException e) {
			e.getSQLState();
			e.getStackTrace();
		} catch (Exception e) {
			System.out.println(e.getStackTrace());
		}
	}

	/*
	 * Function: phase2
	 * 
	 * Read the "GID_key_field" and "pseudo_key" for that table from extra_db!
	 * Compare extra_db.GID_date with target_db.GID_date
	 * Update / Insert target_db fields GID_field_k with corresponding value in extra_db
	 * 
	 */
	public void phase2(final Config c) {
		try {
			String q = "select * from " + c.getTable();
			System.out
					.println(String
							.format("==============================inserting to %s===========================",
									c.getTable()));
			Statement stNew = conNew.createStatement();
			ResultSet rsNew = stNew.executeQuery(q);

			while (rsNew.next()) {
				String ID = rsNew.getString(c.getPseudo());
				String mergeQ = "";
				if (c.getPseudo().equals("ROW")) {
					String conds = "";
					for (int i = 0; i < c.getKey().length; ++i) {
						conds += c.getKey()[i] + " = '"
								+ rsNew.getString(c.getKey()[i]) + "'";
						if (i < c.getKey().length)
							conds += " AND ";
					}
					mergeQ = String.format("Select * from %s where %s", conds);
				} else {
					mergeQ = String.format("Select * from %s where %s = '%s'",
							c.getTable(), c.getPseudo(), ID);
				}
				Statement stMerge = conMerge.createStatement();
				ResultSet rsMerge = stMerge.executeQuery(mergeQ);
				if (!rsMerge.next()) {
					ResultSetMetaData rd = rsNew.getMetaData();
					String[] values = new String[rd.getColumnCount()];
					String[] types = new String[rd.getColumnCount()];
					for (int i = 0; i < rd.getColumnCount(); ++i) {
						types[i] = rd.getColumnName(i + 1);
						values[i] = rsNew.getString(types[i]);
					}
					String insertQ = qb
							.buildInsert(c.getTable(), types, values);
					Statement insertST = conMerge.createStatement();
					insertST.execute(insertQ);
				}
				rsMerge.close();
			}
			rsNew.close();
		} catch (SQLException e) {
			e.getSQLState();
			e.getStackTrace();
		} catch (Exception e) {
			System.out.println(e.getStackTrace());
		}
	}

	/*
	 * Function: refreshDB
	 * 
	 * Implements the functionality to delete all the records from all the tables.
	 * 
	 */
	public void refreshDB() {
		try {
			Statement st = conMerge.createStatement();
			ResultSet rs = st
					.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
			while (rs.next()) {
				Statement st2 = conMerge.createStatement();
				String inQ = "delete from " + rs.getString("name");
				st2.execute(inQ);
			}
			rs.close();
		} catch (Exception e) {
			e.getStackTrace();
		}
	}

	/*
	 * Function: recreateDB
	 * 
	 * Implements the functionality to drop the existing tables and recreate it.
	 * 
	 */
	public void recreateDB() {
		try {
			Statement st = conMerge.createStatement();
			String q = "SELECT name FROM sqlite_master WHERE type='table'";
			ResultSet rs = st.executeQuery(q);
			while (rs.next()) {
				Statement st2 = conMerge.createStatement();
				String inQ = "drop table IF EXISTS " + rs.getString("name");
				st2.execute(inQ);
			}
			rs.close();
		} catch (Exception e) {
			e.getStackTrace();
			System.out.println(e.getMessage());
		}
	}

	/*
	 * Function: getTableName
	 * 
	 * Implements the functionality to get all the table names.
	 */
	public void getTableName() {
		try {

			String DBLocation = JDBC_SQLITE_STR + DB_LOCATION_Yonglok
					+ SEPERATOR + DB_OLD;
			Connection conOld = new DBConnect(DBLocation).connect();
			Statement st = conOld.createStatement();
			ResultSet rs = st
					.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
			while (rs.next()) {
				migrate(rs.getString("name"), conOld);
			}
			rs.close();
			conOld.close();
		} catch (Exception e) {
			e.getStackTrace();
		}
	}

	/*
	 * Function: migrate
	 * 
	 * Implements the functionality to merge the the tables with different dataset.
	 * 
	 */
	public void migrate(final String table, final Connection con) {
		try {
			QBuilder qb = new QBuilder();
			Statement st = con.createStatement();
			String q = String.format("select * from %s", table);
			ResultSet rs = st.executeQuery(q);
			while (rs.next()) {
				ResultSetMetaData rd = rs.getMetaData();
				int numOfColumns = rd.getColumnCount();
				String[] values = new String[numOfColumns];
				String[] types = new String[numOfColumns];
				for (int i = 1; i <= numOfColumns; ++i) {
					types[i - 1] = rd.getColumnLabel(i);
				}
				for (int i = 1; i <= numOfColumns; ++i) {
					values[i - 1] = rs.getString(i);
				}
				String insertQ = qb.buildInsert(table, types, values);
				Statement st2 = conMerge.createStatement();
				if (st2.execute(insertQ)) {
					throw new Exception("Insertion while migrating failed!!");
				}
			}
			rs.close();
		} catch (Exception e) {
			e.getStackTrace();
			System.out.println(e.getMessage());
		}
	}

	/*
	 * Function: createDB
	 * 
	 * Implements the functionality to create the new database ftom the template
	 * 
	 */
	public void createDB() {
		{
			String s = new String();
			StringBuffer sb = new StringBuffer();
			try {
				FileReader fr = new FileReader(new File(
						"createEmptyDelib.sqlite"));
				BufferedReader br = new BufferedReader(fr);
				while ((s = br.readLine()) != null) {
					sb.append(s);
				}
				br.close();
				String[] inst = sb.toString().split(";");
				Statement st = conMerge.createStatement();
				for (int i = 0; i < inst.length; i++) {
					if (!inst[i].trim().equals("")) {
						st.executeUpdate(inst[i]);
					}
				}
			} catch (Exception e) {
				e.getStackTrace();
			}
		}
	}

	/*
	 * Function: closeConnections
	 * 
	 * Implements the close connectons to database
	 * 
	 */
	public void closeConnecetions() {
		try {
			this.conNew.close();
			this.conMerge.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}

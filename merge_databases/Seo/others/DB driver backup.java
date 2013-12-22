import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.sql.Date;

public class Driver {

	public static final String JDBC_SQLITE_STR = "jdbc:sqlite:";
	public static final String DB_LOCATION_Yonglok = "C:\\Users\\Yonglok\\workspace\\Database Project";
	public static final String DB_LOCATION = "C:\\Users\\Srinivasa Venkatesh\\workspace\\DB Project";
	public static final String DB_NEW = "new\\deliberation-app.db";
	public static final String DB_OLD = "old\\deliberation-app.db";
	public static final String DB_MERGE = "merged\\MySQLiteDB";
	public static final String DB_TEST_NEW = "new.db";
	public static final String DB_TEST_OLD = "old.db";
	public static final String SEPERATOR = "\\";

	public static void main(String[] args) {
		try {

			QBuilder qb = new QBuilder();
			String DBLocationMerge = JDBC_SQLITE_STR + DB_LOCATION_Yonglok
					+ SEPERATOR + DB_MERGE;
			Connection conMerge = new DBConnect(DBLocationMerge).connect();
			String DBLocationNew = JDBC_SQLITE_STR + DB_LOCATION_Yonglok
					+ SEPERATOR + DB_NEW;
			Connection conNew = new DBConnect(DBLocationNew).connect();

			// dbtest();
			// createDB();
			// checkNewDB();

			refreshDB(conMerge);

			// createDB();
			System.out.println("Refreshing DB done");
			getTableName();
			System.out.println("Migration DB done");

			final configReader cr = new configReader("config_file.txt");
			final ArrayList<Config> clist = cr.read();

			for (int i = 0; i < clist.size(); ++i) {
				clist.get(i).print();
			}
			System.out.println("Reading Config file done");
			for (int i = 0; i < clist.size(); ++i) {
				phase1(clist.get(i), conNew, conMerge, qb);

			}
			for (int i = 0; i < clist.size(); ++i) {
				phase2(clist.get(i), conNew, conMerge, qb);

			}

			// for (int i = 0; i < clist.size(); ++i) {
			// System.out.println("Merging " + clist.get(i).getTable());
			for (int i = 0; i < clist.size(); ++i) {
				// merge(clist.get(i));
			}

			// recreateDB();
			// dbtestNew();
			// dbtest();
			// break;
			// }
			// System.out.println("Merging " + clist.get(i).getTable());

			// merge(clist.get(4));
			// compareDate1();
			// System.out.println("Merging DB done");

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}

	}

	public static void phase1(Config c, Connection conNew, Connection conMerge,
			QBuilder qb) {
		try {
			/*
			 * QBuilder qb = new QBuilder(); String DBLocationMerge =
			 * JDBC_SQLITE_STR + DB_LOCATION_Yonglok + SEPERATOR + DB_MERGE;
			 * Connection conMerge = new DBConnect(DBLocationMerge).connect();
			 * String DBLocationNew = JDBC_SQLITE_STR + DB_LOCATION_Yonglok +
			 * SEPERATOR + DB_NEW; Connection conNew = new
			 * DBConnect(DBLocationNew).connect();
			 */
			String q = "select * from " + c.getTable();
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
					// System.out.println("ID in while loop " + ID);
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
						// System.out.println(mergeQ);
						// System.exit(0);
					} else {
						mergeQ = String.format(
								"Select * from %s where %s = '%s'",
								c.getTable(), c.getPseudo(), ID);
						// System.out.println(mergeQ);

					}

					// System.out.println(mergeQ);
					// System.exit(0);
					Statement stMerge = conMerge.createStatement();
					ResultSet rsMerge = stMerge.executeQuery(mergeQ);

					while (rsMerge.next()) {
						boolean newer = false;
						if (dateField1[0] != null) {
							for (int i = 0; i < dateField1.length; ++i) {
								// System.out.println("comparing = "
								// + dateField1[i]);
								Date dateNew = rsNew.getDate(dateField1[i]);
								// System.out.println("after getting new");
								Date dateOld = rsMerge.getDate(dateField1[i]);
								// System.out.println("after getting old");
								if (dateOld == null || dateNew == null) {
									newer = true;
								} else if (dateNew.after(dateOld)) {
									newer = true;
								}
								if (newer)
									break;
							}
							if (newer) {
								// System.out.println("in newer");
								if (constraintsExist) {
									String[][] constraints = c.getConstraints();
									for (int i = 0; i < constraints.length; ++i) {
										String constTable = constraints[i][0];
										String constID = constraints[i][1];
										String deleteConstraintQ = qb
												.buidDelete(constTable,
														constID, ID);
										// System.out.println("\t"
										// + deleteConstraintQ);
										// execute query here
										Statement deleteST = conMerge
												.createStatement();
										deleteST.execute(deleteConstraintQ);

									}
								}
								String deleteQ = qb.buidDelete(c.getTable(),
										c.getPseudo(), ID);
								// System.out.println(deleteQ);
								Statement deleteST = conMerge.createStatement();
								deleteST.execute(deleteQ);
								// execute query here
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
									// System.out.println("after else-for-loop");
									if (newer) {
										if (constraintsExist) {
											String[][] constraints = c
													.getConstraints();
											for (int i = 0; i < constraints.length; ++i) {
												String constTable = constraints[i][0];
												String constID = constraints[i][1];
												// System.out.println(constTable
												// + "\t" + constID);
												String deleteConstraintQ = qb
														.buidDelete(constTable,
																constID, ID);
												// System.out.println("\t"
												// + deleteConstraintQ);
												// execute query here
												Statement deleteST = conMerge
														.createStatement();
												deleteST.execute(deleteConstraintQ);
											}
										}
										String deleteQ = qb
												.buidDelete(c.getTable(),
														c.getPseudo(), ID);
										// System.out.println(deleteQ);
										// execute query here
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
				// conNew.close();
				// conMerge.close();
			}
		} catch (SQLException e) {
			e.getSQLState();
			e.getStackTrace();
		} catch (Exception e) {
			System.out.println(e.getStackTrace());
		}
	}

	public static void phase2(Config c, Connection conNew, Connection conMerge,
			QBuilder qb) {
		try {
			/*
			 * QBuilder qb = new QBuilder(); String DBLocationMerge =
			 * JDBC_SQLITE_STR + DB_LOCATION_Yonglok + SEPERATOR + DB_MERGE;
			 * Connection conMerge = new DBConnect(DBLocationMerge).connect();
			 * String DBLocationNew = JDBC_SQLITE_STR + DB_LOCATION_Yonglok +
			 * SEPERATOR + DB_NEW; Connection conNew = new
			 * DBConnect(DBLocationNew).connect();
			 */

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
				// System.out.println("ID in while loop " + ID);
				if (c.getPseudo().equals("ROW")) {
					String conds = "";
					for (int i = 0; i < c.getKey().length; ++i) {
						conds += c.getKey()[i] + " = '"
								+ rsNew.getString(c.getKey()[i]) + "'";
						if (i < c.getKey().length)
							conds += " AND ";
					}
					mergeQ = String.format("Select * from %s where %s", conds);
					// System.out.println(mergeQ);
					// System.exit(0);
				} else {
					mergeQ = String.format("Select * from %s where %s = '%s'",
							c.getTable(), c.getPseudo(), ID);
					// System.out.println(mergeQ);

				}
				Statement stMerge = conMerge.createStatement();
				ResultSet rsMerge = stMerge.executeQuery(mergeQ);
				/*
				 * int counter = 0; while (rsMerge.next()) { counter++; }
				 * System.out.println(counter);
				 */
				if (!rsMerge.next()) {
					// System.out.println("in if");
					ResultSetMetaData rd = rsNew.getMetaData();
					String[] values = new String[rd.getColumnCount()];
					String[] types = new String[rd.getColumnCount()];
					// String conds = "";
					for (int i = 0; i < rd.getColumnCount(); ++i) {
						// String temp = rsNew.getString(i+1);
						// if(!temp.equals("NULL")){
						types[i] = rd.getColumnName(i + 1);
						values[i] = rsNew.getString(types[i]);
						// }
					}
					// System.out.println("before qb");
					String insertQ = qb
							.buildInsert(c.getTable(), types, values);
					// System.out.println(insertQ);
					Statement insertST = conMerge.createStatement();
					insertST.execute(insertQ);
				}
				rsMerge.close();
			}
			rsNew.close();
			// conNew.close();
			// conMerge.close();
		} catch (SQLException e) {
			e.getSQLState();
			e.getStackTrace();
		} catch (Exception e) {
			System.out.println(e.getStackTrace());
		}
	}

	public static void dbtestNew() {
		try {
			String DBLocation = JDBC_SQLITE_STR + DB_LOCATION + SEPERATOR
					+ DB_TEST_NEW;
			// String DBLocation =
			// "jdbc:sqlite:C:\\Users\\Yonglok\\workspace\\Database Project\\new.db";
			// String DBLocationMerge =
			// "jdbc:sqlite:C:\\Users\\Yonglok\\workspace\\Database Project\\merged\\deliberation-app.db";

			Connection con = new DBConnect(DBLocation).connect();
			// Connection con2 = new DBConnect(DBLocationMerge).connect();

			Statement st = con.createStatement();
			String q = "select * from organization";
			ResultSet rs = st.executeQuery(q);
			System.out.println(q);
			while (rs.next()) {
				ResultSetMetaData rd = rs.getMetaData();
				int numOfColumns = rd.getColumnCount();

				// System.out
				// .println("number of columns = " + rd.getColumnCount());

				for (int i = 1; i <= numOfColumns; ++i) {
					// System.out.println(rd.getColumnLabel(i) + ",\t");
					// System.out.print(rd.getColumnName(i) + ",\t");
					// System.out.print(rd.getColumnType(i) + ",\t");
					// System.out.println(rd.getColumnTypeName(i) + "\t");
				}

				for (int i = 1; i <= rd.getColumnCount(); ++i) {
					// System.out.println(rs.getString(i));
				}
				System.out.println(rs.getDate("preferences_date"));
				// break;
			}
			rs.close();
			con.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

	}

	public static void merge(Config c) {
		try {
			QBuilder qb = new QBuilder();
			String DBLocationMerge = JDBC_SQLITE_STR + DB_LOCATION + SEPERATOR
					+ DB_MERGE; // "jdbc:sqlite:C:\\Users\\Srinivasa Venkatesh\\workspace\\DB Project\\merged\\MySQLiteDB.db";
			Connection conMerge = new DBConnect(DBLocationMerge).connect();
			String DBLocationNew = JDBC_SQLITE_STR + DB_LOCATION + SEPERATOR
					+ DB_TEST_OLD; // "jdbc:sqlite:C:\\Users\\Srinivasa Venkatesh\\workspace\\DB Project\\new\\deliberation-app.db";
			Connection conNew = new DBConnect(DBLocationNew).connect();

			String q = "select * from " + c.getTable();
			System.out
					.println(String
							.format("==============================merging %s===========================",
									c.getTable()));
			Statement stNew = conNew.createStatement();

			ResultSet rsNew = stNew.executeQuery(q);

			String[] dateField = c.getDate1();
			if (dateField[0] == null)
				System.out.println("no date 1 to compare");
			String[] dateField2 = c.getDate2();

			Date[] datesMerge = new Date[dateField.length];
			Date[] datesNew = new Date[dateField.length];
			int countNew = 0;
			while (rsNew.next()) {
				countNew++;
				String ID = rsNew.getString(c.getPseudo());
				String mergeQ = String.format("Select * from %s where %s = %s",
						c.getTable(), c.getPseudo(), ID);
				Statement stMerge = conMerge.createStatement();
				ResultSet rsMerge = stMerge.executeQuery(mergeQ);

				if (!rsMerge.next()) {
					System.out
							.println("new data does not exist in old DB adding new...");
					ResultSetMetaData rd = rsNew.getMetaData();
					int numOfColumns = rd.getColumnCount();
					String[] values = new String[numOfColumns];
					String[] types = new String[numOfColumns];
					for (int i = 1; i <= numOfColumns; ++i) {
						types[i - 1] = rd.getColumnLabel(i);
					}
					for (int i = 1; i <= numOfColumns; ++i) {
						values[i - 1] = rsNew.getString(i);
					}

					String insertQ = qb
							.buildInsert(c.getTable(), types, values);
					System.out.println(insertQ);
				} else {

					do {
						countNew++;
						boolean newer = false;
						for (int i = 0; i < dateField.length; ++i) {
							if (rsNew.getDate(dateField[i]).after(
									rsMerge.getDate(dateField[i]))) {
								newer = true;
								break;
							}
						}
						if (newer) {
							String updateQ = qb.buildUpdateForDate1(c, rsNew);
							System.out.println(updateQ);
						} else {
							for (int i = 0; i < dateField2.length; ++i) {
								if (rsNew.getDate(dateField[i]).after(
										rsMerge.getDate(dateField[i]))) {
									newer = true;
								}
							}
							if (newer) {
								String updateQ = qb.buildUpdateForDate1(c,
										rsNew);
							} else {
								System.out.println("Not Newer");
							}
						}
					} while (rsMerge.next());
					rsMerge.close();
				}
			}
			rsNew.close();
			if (countNew < 1)
				System.out.println("no data to compare");
			conNew.close();
			conMerge.close();

		} catch (SQLException e) {
			e.getSQLState();
			e.getStackTrace();
		} catch (Exception e) {
			e.getStackTrace();
		}
	}

	public static void noCompareDate(Config c, Connection conMerge,
			Statement stNew) {
		try {
			QBuilder qb = new QBuilder();
			String q = "select * from " + c.getTable();
			ResultSet rsNew = stNew.executeQuery(q);
			String updateQ = qb.buildUpdateForDate1(c, rsNew);
			Statement stMerge = conMerge.createStatement();
			stMerge.executeQuery(updateQ);
			// rsNew.close();
			// stMerge.close();
		} catch (SQLException e) {

			e.printStackTrace();
		}
	}

	public static void compareDate1() {
		try {
			QBuilder qb = new QBuilder();
			final configReader cr = new configReader("config_file.txt");
			final ArrayList<Config> clist = cr.read();
			String DBLocationMerge = JDBC_SQLITE_STR + DB_LOCATION + SEPERATOR
					+ DB_MERGE; // "jdbc:sqlite:C:\\Users\\Srinivasa Venkatesh\\workspace\\DB Project\\merged\\MySQLiteDB.db";
			Connection conMerge = new DBConnect(DBLocationMerge).connect();

			String DBLocationNew = JDBC_SQLITE_STR + DB_LOCATION + SEPERATOR
					+ DB_NEW; // "jdbc:sqlite:C:\\Users\\Srinivasa Venkatesh\\workspace\\DB Project\\new\\deliberation-app.db";
			Connection conNew = new DBConnect(DBLocationNew).connect();

			Statement stNew = conNew.createStatement();
			for (int k = 0; k < clist.size(); ++k) {
				System.out.println("Merging " + clist.get(k).getTable());
				if (clist.get(k).getDate1()[0] == null) {

					// noCompareDate(clist.get(k), conMerge, stNew);
				} else {
					String q = "select * from " + clist.get(k).getTable();

					ResultSet rsNew = stNew.executeQuery(q);
					String[] dateField = clist.get(k).getDate1();

					while (rsNew.next()) {
						int id = rsNew.getInt(clist.get(k).getPseudo());
						// System.out.println(id);
						String qMerge = String.format(
								"select * from %s where %s = %s", clist.get(k)
										.getTable(), clist.get(k).getPseudo(),
								id);
						// System.out.println(qMerge);
						Statement stMerge = conMerge.createStatement();
						ResultSet rsMerge = stMerge.executeQuery(qMerge);
						Object[] datesMerge = new Object[dateField.length];
						Object[] datesNew = new Object[dateField.length];
						while (rsMerge.next()) {
							// System.out.println(rsMerge.getString("name"));
							for (int i = 0; i < dateField.length; ++i) {
								try {
									datesMerge[i] = rsMerge
											.getDate(dateField[i]);
									datesNew[i] = rsNew.getDate(dateField[i]);
									// System.out.println("in loop 1 try for = "
									// +dateField[i]);
								} catch (Exception e) {
									System.out.println("type mismatch for = "
											+ dateField[i]);
									datesMerge[i] = rsMerge
											.getString(dateField[i]);
									datesNew[i] = rsNew.getString(dateField[i]);
									// System.out.println("in loop 1 catch for = "
									// +dateField[i]);
								}
							}

							for (int i = 0; i < datesMerge.length; ++i) {
								try {
									Date d1 = (Date) datesMerge[i];
									Date d2 = (Date) datesNew[i];
									if (d1.before(d2)) {
										String updateQ = qb
												.buildUpdateForDate1(
														clist.get(k), rsNew);
										// System.out.println(updateQ);
										try {
											stMerge.executeQuery(updateQ);
										} catch (SQLException e) {
											System.out
													.println("sql query exception");
										}
									} else {
										// System.out.println("not newer");
									}
								} catch (Exception e) {
									System.out.println("error for field = "
											+ dateField[i]);
									System.out.println("\tvalue new = "
											+ datesMerge[i]);
									System.out.println("\tvalue merge = "
											+ datesMerge[i]);

									e.getStackTrace();
									String d1 = (String) datesMerge[i];
									String d2 = (String) datesNew[i];
									if (d1.compareTo(d1) > 0) {
										String updateQ = qb
												.buildUpdateForDate1(
														clist.get(k), rsNew);
										// System.out.println(updateQ);
										stMerge.executeQuery(updateQ);
									} else {
										// System.out.println("not newer");
									}
								}
							}
							rsMerge.close();
						}
					}
					rsNew.close();
				}
			}
			conNew.close();
			conMerge.close();
			System.out.println("done");
		} catch (Exception e) {
			System.out.println("error");
			e.getStackTrace();
		}
	}

	public static void refreshDB(Connection conMerge) {
		try {
			Statement st = conMerge.createStatement();
			ArrayList<String> arrTblNames = new ArrayList<String>();
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

	public static void recreateDB(Connection conMerge) {
		try {
			Statement st = conMerge.createStatement();
			String q = "SELECT name FROM sqlite_master WHERE type='table'";
			System.out.println(q);
			ResultSet rs = st.executeQuery(q);
			// System.out.println(st.execute(q));

			while (rs.next()) {

				Statement st2 = conMerge.createStatement();
				System.out.println(rs.getString("name"));
				String inQ = "drop table IF EXISTS " + rs.getString("name");
				System.out.println(inQ);
				st2.execute(inQ);
				// System.out.println(inQ);
			}
			rs.close();
		} catch (Exception e) {
			e.getStackTrace();
			System.out.println(e.getMessage());
		}
	}

	public static void getTableName(Connection conMerge) {
		try {

			String DBLocation = JDBC_SQLITE_STR + DB_LOCATION_Yonglok
					+ SEPERATOR + DB_OLD; // "jdbc:sqlite:C:\\Users\\Srinivasa Venkatesh\\workspace\\DB Project\\old\\deliberation-app.db";

			Connection conOld = new DBConnect(DBLocation).connect();

			Statement st = conOld.createStatement();
			ArrayList<String> arrTblNames = new ArrayList<String>();
			ResultSet rs = st
					.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");

			while (rs.next()) {
				migrate(rs.getString("name"), conOld, conMerge);
			}

			rs.close();
			conOld.close();

		} catch (Exception e) {
			e.getStackTrace();
		}
	}

	public static void migrate(final String table, Connection con,
			Connection con2) {
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
				Statement st2 = con2.createStatement();

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

	public static void checkNewDB() {
		String DBLocationMerge = JDBC_SQLITE_STR + DB_LOCATION + SEPERATOR
				+ DB_MERGE; // "jdbc:sqlite:C:\\Users\\Srinivasa Venkatesh\\workspace\\DB Project\\merged\\MySQLiteDB.db";
		try {
			Connection con = new DBConnect(DBLocationMerge).connect();
			Statement st = con.createStatement();
			String q = "select * from peer";
			q = "SELECT name FROM sqlite_master WHERE type = table";
			ResultSet rs = st.executeQuery(q);
			ResultSetMetaData rd = rs.getMetaData();
			System.out.println(rd.getColumnCount());
			while (rs.next()) {

				int numOfColumns = rd.getColumnCount();
				for (int i = 1; i <= numOfColumns; ++i) {
					System.out.print(rd.getColumnLabel(i) + ",\t");
					System.out.print(rd.getColumnName(i) + ",\t");
					System.out.print(rd.getColumnType(i) + ",\t");
					System.out.println(rd.getColumnTypeName(i) + "\t");
					System.out.println(rs.getString(i));
				}

			}
			con.close();

		} catch (Exception e) {
		}

	}

	public static void createDB() {
		{
			String s = new String();
			StringBuffer sb = new StringBuffer();
			String DBLocationMerge = JDBC_SQLITE_STR + DB_LOCATION + SEPERATOR
					+ DB_MERGE; // "jdbc:sqlite:C:\\Users\\Srinivasa Venkatesh\\workspace\\DB Project\\merged\\MySQLiteDB.db";
			try {
				FileReader fr = new FileReader(new File(
						"createEmptyDelib.sqlite"));

				BufferedReader br = new BufferedReader(fr);

				while ((s = br.readLine()) != null) {
					sb.append(s);
				}
				br.close();

				String[] inst = sb.toString().split(";");

				Connection con = new DBConnect(DBLocationMerge).connect();
				Statement st = con.createStatement();

				for (int i = 0; i < inst.length; i++) {
					if (!inst[i].trim().equals("")) {
						st.executeUpdate(inst[i]);
						System.out.println(">>" + inst[i]);
					}
				}
				con.close();

			} catch (Exception e) {
				e.getStackTrace();
			}

		}
	}

	public static void dbtest() {
		try {
			String DBLocation = JDBC_SQLITE_STR + DB_LOCATION + SEPERATOR
					+ DB_OLD; // "jdbc:sqlite:C:\\Users\\Srinivasa Venkatesh\\workspace\\DB Project\\old\\deliberation-app.db";
			// String DBLocationMerge =
			// "jdbc:sqlite:C:\\Users\\Yonglok\\workspace\\Database Project\\merged\\deliberation-app.db";

			Connection con = new DBConnect(DBLocation).connect();
			// Connection con2 = new DBConnect(DBLocationMerge).connect();
			QBuilder qb = new QBuilder();
			Statement st = con.createStatement();
			String q = "select * from peer";
			ResultSet rs = st.executeQuery(q);
			System.out.println(q);
			if (!rs.next()) {
				System.out.println("no data");
			} else {

				do {
					ResultSetMetaData rd = rs.getMetaData();
					int numOfColumns = rd.getColumnCount();

					System.out.println("number of columns = "
							+ rd.getColumnCount());
					String types[] = new String[rd.getColumnCount()];
					for (int i = 1; i <= numOfColumns; ++i) {
						/*
						 * System.out.print(rd.getColumnLabel(i) + ",\t");
						 * System.out.print(rd.getColumnName(i) + ",\t");
						 * System.out.print(rd.getColumnType(i) + ",\t");
						 * System.out.println(rd.getColumnTypeName(i) + "\t");
						 */
						types[i - 1] = rd.getColumnLabel(i);
					}

					String values[] = new String[rd.getColumnCount()];

					for (int i = 1; i <= rd.getColumnCount(); ++i) {
						// System.out.print("\t" + rs.getString(i));
						values[i - 1] = rs.getString(types[i - 1]);
						// System.out.println(rs.getString(types[i-1]));
					}
					// System.exit(0);

					// System.out.println();
					System.out.println(qb.buildInsert("peer", types, values));
				} while (rs.next());
			}
			rs.close();
			con.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

	}

	public static void querytest() {
		final configReader cr = new configReader("config_file.txt");
		final ArrayList<Config> clist = cr.read();

		for (int i = 0; i < clist.size(); ++i) {
			clist.get(i).print();
		}

		System.out
				.println("================sample query selecting one row at a time===================");
		QBuilder qb = new QBuilder();
		for (int i = 0; i < clist.size(); ++i) {
			System.out.println(qb.buildSelectFromNew(clist.get(i), 1));
		}

		System.out
				.println("=================sample query for data select=======================");
		String[] v = { "pseudovalue", "keyvalue", "date1value", "date2value" };
		System.out.println(qb.buildSelectToMatch(clist.get(0), v));
		System.out.println(qb.buildSelectToMatch(clist.get(1), v));

		String[] val = { "pseudovalue", "val1", "val2", "val3", "val4", "val5",
				"val6", "val7", "val8", "val9", "val11", "val12", "val13",
				"val14", "val15", "val16", "val17", "val18", "val19", "val20",
				"val21", "val22", "val23", "val24", "val25", "val26", "val27",
				"val28", "val29", "val30" };

		System.out
				.println("======================sample update query==============================");
		System.out.println(qb.buildUpdateForDate1(clist.get(0), val));
		System.out.println(qb.buildUpdateForDate2(clist.get(0), val));

		System.out.println(qb.buildUpdateForDate2(clist.get(3), val));
		System.out.println(qb.buildUpdateForDate2(clist.get(4), val));

	}
}

/*
 * Author     : Srini Venkatesh, Yonglok Seo
 * Subject    : CSE 5260
 * Instructor : Dr. M. Silaghi
 * */
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Class: QBuilder (Query Builder)
 * 
 * Implements the select, update, insert query which is required for database migration
 * 
 */
public final class QBuilder {
	public QBuilder() {

	}

	/*
	 * Function: buildSelectFromNew
	 * 
	 * Implements the select query from the given table.
	 * 
	 */
	public final String buildSelectFromNew(final Config c, final int row) {
		return String.format("SELECT * FROM %s LIMIT %d %d", c.getTable(),
				row - 1, row);
	}

	/*
	 * Function: buildSelectToMatch
	 * 
	 * Builds the query to match the data from the given string using the key, global date and preference date.
	 * 
	 */
	public final String buildSelectToMatch(final Config c, final String[] v) {
		String table = c.getTable();
		String pseudoCond = "";
		String keyCond = "";
		String date1Cond = "";
		String date2Cond = "";
		int valueCounter = 0;
		if (c.getPseudo() != null) {
			pseudoCond = c.getPseudo() + " = '" + v[valueCounter] + "'";
			valueCounter++;
		}

		if (c.getKey() != null) {
			if (!pseudoCond.equals(""))
				pseudoCond += " AND ";
			for (int i = 0; i < c.getKey().length; ++i) {
				if (c.getKey()[i] != null) {
					keyCond += c.getKey()[i] + " = '" + v[valueCounter] + "'";
					valueCounter++;
				}
				if (i != c.getKey().length - 1)
					keyCond += " AND ";
			}
		}

		if (c.getDate1() != null) {
			if (!keyCond.equals(""))
				keyCond += " AND ";
			for (int i = 0; i < c.getDate1().length; ++i) {
				if (c.getDate1()[i] != null) {
					date1Cond += c.getDate1()[i] + " = '" + v[valueCounter]
							+ "'";
					valueCounter++;
				}
				if (i != c.getDate1().length - 1)
					date1Cond += " AND ";
			}
		}

		if (c.getDate2() != null) {
			if (!date1Cond.equals(""))
				date1Cond += " AND ";
			for (int i = 0; i < c.getDate2().length; ++i) {
				if (c.getDate2()[i] != null) {
					date2Cond += c.getDate2()[i] + " = '" + v[valueCounter]
							+ "'";
					valueCounter++;
				}
				if (i != c.getDate2().length - 1)
					date2Cond += " AND ";
			}
		}

		return String.format("SELECT * FROM %s WHERE %s %s %s %s", table,
				pseudoCond, keyCond, date1Cond, date2Cond);

	}

	/*
	 * Function: buildSelectToMatch
	 * 
	 * Builds the query to match the data from the given result set using the key, global date and preference date.
	 * 
	 */
	public final String buildSelectToMatch(final Config c, final ResultSet rs) {
		try {
			String table = c.getTable();
			String pseudoCond = "";
			String keyCond = "";
			String date1Cond = "";
			String date2Cond = "";
			if (c.getPseudo() != null) {
				pseudoCond = c.getPseudo() + " = '"
						+ rs.getString(c.getPseudo()) + "'";
			}

			if (c.getKey() != null) {
				if (!pseudoCond.equals(""))
					pseudoCond += " AND ";
				for (int i = 0; i < c.getKey().length; ++i) {
					if (c.getKey()[i] != null) {
						keyCond += c.getKey()[i] + " = '"
								+ rs.getString(c.getKey()[i]) + "'";
					}
					if (i != c.getKey().length - 1)
						keyCond += " AND ";
				}
			}

			if (c.getDate1() != null) {
				if (!keyCond.equals(""))
					keyCond += " AND ";
				for (int i = 0; i < c.getDate1().length; ++i) {
					if (c.getDate1()[i] != null) {
						date1Cond += c.getDate1()[i] + " = '"
								+ rs.getString(c.getDate1()[i]) + "'";
					}
					if (i != c.getDate1().length - 1)
						date1Cond += " AND ";
				}
			}

			if (c.getDate2() != null) {
				if (!date1Cond.equals(""))
					date1Cond += " AND ";
				for (int i = 0; i < c.getDate2().length; ++i) {
					if (c.getDate2()[i] != null) {
						date2Cond += c.getDate2()[i] + " = '"
								+ rs.getString(c.getDate2()[i]) + "'";
					}
					if (i != c.getDate2().length - 1)
						date2Cond += " AND ";
				}
			}

			return String.format("SELECT * FROM %s WHERE %s %s %s %s", table,
					pseudoCond, keyCond, date1Cond, date2Cond);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	/*
	 * Function: buildUpdateForDate1
	 * 
	 * Builds the update query for Global Date from the given string
	 * 
	 */
	public final String buildUpdateForDate1(final Config c, final String v[]) {
		int valueCounter = 1;// 0 is pseudo key
		String table = c.getTable();
		String pseudo = c.getPseudo();
		String cond = pseudo + " = '" + v[0] + "'";

		String updateField = "";

		if (c.getGlobs() != null) {
			for (int i = 0; i < c.getGlobs().length; ++i) {
				if (c.getGlobs()[i] != null) {
					updateField += c.getGlobs()[i] + " = '" + v[valueCounter]
							+ "'";
					valueCounter++;
				}
				if (i != c.getGlobs().length - 1)
					updateField += ", ";
			}
		}
		return String.format("UPDATE %s SET %s WHERE %s", table, updateField,
				cond);
	}

	/*
	 * Function: buildUpdateForDate2
	 * 
	 * Builds the update query for Preference Date from the given string
	 * 
	 */
	public final String buildUpdateForDate2(final Config c, final String v[]) {
		int valueCounter = 1;// 0 is pseudo key
		String table = c.getTable();
		String pseudo = c.getPseudo();
		String cond = pseudo + " = '" + v[0] + "'";

		String updateField = "";

		if (c.getPref() != null) {
			for (int i = 0; i < c.getPref().length; ++i) {
				if (c.getPref()[i] != null) {
					updateField += c.getPref()[i] + " = '" + v[valueCounter]
							+ "'";

				}
				if (i != c.getPref().length - 1) {
					updateField += ", ";
					valueCounter++;
				}
			}
		}

		if (c.getInstanceD() != null) {
			if (!updateField.equals(""))
				updateField += ", ";
			for (int i = 0; i < c.getInstanceD().length; ++i) {
				if (c.getInstanceD()[i] != null) {
					updateField += c.getInstanceD()[i] + " = '"
							+ v[valueCounter] + "'";

				}
				if (i != c.getInstanceD().length - 1) {
					updateField += ", ";
					valueCounter++;
				}
			}
		}

		return String.format("UPDATE %s SET %s WHERE %s", table, updateField,
				cond);

	}

	/*
	 * Function: buildUpdateForDate1
	 * 
	 * Builds the update query for Preference Date from the given result set
	 * 
	 */	
	public final String buildUpdateForDate1(final Config c, final ResultSet rs) {
		try {
			String table = c.getTable();
			String cond = c.getPseudo() + " = '" + rs.getString(c.getPseudo())
					+ "'";

			String updateField = "";

			if (c.getGlobs() != null) {
				// date1
				for (int i = 0; i < c.getDate1().length; ++i) {
					if (c.getDate1()[i] != null) {
						updateField += c.getDate1()[i] + " = '"
								+ rs.getString(c.getDate1()[i]) + "'";
					}
					if (i != c.getDate1().length - 1)
						updateField += ", ";
				}
				// date2
				for (int i = 0; i < c.getDate2().length; ++i) {
					if (c.getDate2()[i] != null) {
						updateField += c.getDate2()[i] + " = '"
								+ rs.getString(c.getDate2()[i]) + "'";
					}
					if (i != c.getDate2().length - 1)
						updateField += ", ";
				}

				// for globs
				for (int i = 0; i < c.getGlobs().length; ++i) {
					if (c.getGlobs()[i] != null) {
						updateField += c.getGlobs()[i] + " = '"
								+ rs.getString(c.getGlobs()[i]) + "'";
					}
					if (i != c.getGlobs().length - 1)
						updateField += ", ";
				}
				// for pref data
				for (int i = 0; i < c.getPref().length; ++i) {
					if (c.getPref()[i] != null) {
						updateField += c.getPref()[i] + " = '"
								+ rs.getString(c.getPref()[i]) + "'";
					}
					if (i != c.getPref().length - 1)
						updateField += ", ";
				}
				// for instance data
				// for (int i = 0; i < c.getInstanceD().length; ++i) {
				// if (c.getInstanceD()[i] != null) {
				// updateField += c.getInstanceD()[i] + " = '"
				// + rs.getString(c.getInstanceD()[i]) + "'";
				// }
				// if (i != c.getInstanceD().length - 1)
				// updateField += ", ";
				// }
			}
			return String.format("UPDATE %s SET %s WHERE %s", table,
					updateField, cond);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	/*
	 * Function: buildUpdateForDate2
	 * 
	 * Builds the update query for Preference Date from the given result set
	 * 
	 */
	public final String buildUpdateForDate2(final Config c, final ResultSet rs) {
		try {
			String table = c.getTable();
			;
			String cond = c.getPseudo() + " = '" + rs.getString(c.getPseudo())
					+ "'";

			String updateField = "";

			if (c.getPref() != null) {
				for (int i = 0; i < c.getPref().length; ++i) {
					if (c.getPref()[i] != null) {
						updateField += c.getPref()[i] + " = '"
								+ rs.getString(c.getPref()[i]) + "'";

					}
					if (i != c.getPref().length - 1) {
						updateField += ", ";
					}
				}
			}

			if (c.getInstanceD() != null) {
				if (!updateField.equals(""))
					updateField += ", ";
				for (int i = 0; i < c.getInstanceD().length; ++i) {
					if (c.getInstanceD()[i] != null) {
						updateField += c.getInstanceD()[i] + " = '"
								+ rs.getString(c.getInstanceD()[i]) + "'";
					}
					if (i != c.getInstanceD().length - 1) {
						updateField += ", ";
					}
				}
			}

			return String.format("UPDATE %s SET %s WHERE %s", table,
					updateField, cond);

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	/*
	 * Function: buildInsert
	 * 
	 * Builds the Insert query for the given table and the value.
	 * 
	 */
	public final String buildInsert(final String table, final String[] values) {

		String v = "";
		for (int i = 0; i < values.length; ++i) {
			v += "'" + values[i] + "'";
			if (i != values.length - 1) {
				v += ", ";
			}
		}

		String query = String.format("INSERT INTO %s VALUES (%S)", table, v);
		return query;
	}

	/*
	 * Function: buildInsert
	 * 
	 * Builds the Insert query for the given table, types and the value.
	 * 
	 */
	public final String buildInsert(final String table, final String[] tpes,
			final String[] values) {
		String t = "";

		for (int i = 0; i < tpes.length; ++i) {
			t += tpes[i];
			if (i != tpes.length - 1) {
				t += ", ";
			}
		}

		String v = "";
		for (int i = 0; i < values.length; ++i) {

			if (values[i] == null || values[i].equals("NULL")) {
				v += values[i];
			} else {
				if (values[i].contains("\'")) {
					v += "\"" + values[i] + "\"";
				} else {
					v += "'" + values[i] + "'";
				}
			}

			if (i != values.length - 1) {
				v += ", ";
			}
		}

		String query = String.format("INSERT INTO %s (%s) VALUES (%s);", table,
				t, v);
		return query;
	}

	/*
	 * Function: buildDelete
	 * 
	 * Builds the delete query from the given table, keyname and the key.
	 * 
	 */
	public final String buildDelete(final String table, final String keyName, final String key){
		final String q = String.format("DELETE FROM %s WHERE %s = '%s'", table, keyName, key); 
		return q;
				
	}
}

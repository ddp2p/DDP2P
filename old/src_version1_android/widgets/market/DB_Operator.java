/**************************************************
 * This class operates the Database and packs the    
 * return result into the format that meet the
 * ASN1 description and send the result to client.
 * 
 *
 * Author:  Xu Mengqiu, Jiuyang Zhou.
 * Course:  Network Programming, Spring 2013
 * Project: Project 3
 * Charset: UTF-8
 ***************************************************/
package widgets.market;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;

public class DB_Operator {
	private Connection cn;
	public Statement stmt;

	public void database(String DB_path) throws IOException {
		try {
			Class.forName("org.sqlite.JDBC");
//			System.out.println("Load sqlite Driver sucess!");
		} catch (java.lang.ClassNotFoundException e) {
			System.out.println("Fail to Load sqlite Driver!");
			System.out.println(e.getMessage());
		}

		try {

			String connectionString = "jdbc:sqlite:" + DB_path;
			cn = DriverManager.getConnection(connectionString);
//			System.out.println("DB Connect sucessfully!");
			stmt = cn.createStatement();

		} catch (SQLException e) {
		}

	}

	public void operate_DB(String command) throws IOException, SQLException {

		try {
			stmt.execute(command);
		} catch (SQLException e) {
			System.out.println("Fail!");
			System.out.println(e.getMessage());
		}
	}
	public void clearMarketTabel() throws IOException, SQLException {

		try {
			stmt.execute("DELETE FROM market");
		} catch (SQLException e) {
			System.out.println("Fail!");
			System.out.println(e.getMessage());
		}
	}
	
	public ResultSet select_DB(String command) throws IOException, SQLException {

		try {
			stmt.execute(command);
		} catch (SQLException e) {
			System.out.println("Fail!");
			System.out.println(e.getMessage());
		}
		ResultSet r=stmt.getResultSet();
		return r;
	}

	/* debug */
	public void update(String command) throws IOException, SQLException {

		try {
			int i = stmt.executeUpdate(command);
			System.out.println(i);
		} catch (SQLException e) {
			System.out.println("Fail!");
			System.out.println(e.getMessage());
		}
	}

	public void insert(String command) throws IOException, SQLException {

		try {
			stmt.execute(command);
		} catch (SQLException e) {
			System.out.println("Fail!");
			System.out.println(e.getMessage());
		}
	}

	/* debug */
	public void select(String command) throws IOException, SQLException {

		try {
			ResultSet rs = stmt.executeQuery(command);
			while (rs.next()) {
				for (int i = 1; i <= 6; i++)
					System.out.print(rs.getString(i) + "\t");
				System.out.println();
			}
		} catch (SQLException e) {
			System.out.println("Fail!");
			System.out.println(e.getMessage());
		}
	}

	public synchronized String getOperate_DB(String command, String table_name)
			throws IOException, SQLException {
		String result = null;
		try {
			ResultSet rs = stmt.executeQuery(command);
			result = "{\n\r\tdatabase_name " + table_name + "\n\r\tdate ";

			java.util.Date date = new java.util.Date();
			String nowTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.format(date);
			java.sql.Timestamp ts_date = java.sql.Timestamp.valueOf(nowTime);
			String time = "";
			for (int index = 0; index < ts_date.toString().length(); index++) {
				if ((ts_date.toString().charAt(index) >= '0' && ts_date
						.toString().charAt(index) <= '9')
						|| ts_date.toString().charAt(index) == '.')
					time += ts_date.toString().charAt(index);
			}
			result += time + "00Z\n\r\t{\n\r\ttable_names \"" + table_name
					+ "\"\n\r\tfield_name {";
			result += this.getTableColum(table_name)
					+ "}\n\r\tfield_value\n\r\t{\n\r";
			result += this.getTableValue(table_name, rs);
			result += "\t}\n\r";
			result += "\tfiled_type {" + this.getTableColumType(table_name)
					+ "}\n\r\t}\n\r}\n\r";

		} catch (SQLException e) {
			System.out.println("Fail!");
			System.out.println(e.getMessage());
		}
		return result;
	}

	public String getTableColum(String Type) {
		String output = null;
		if (Type.equals("directory")) {
			output = "Directory_ID, Domain_IP, Port, Comments";
			return output;
		} else if (Type.equals("peer_address")) {
			output = "Peer_address_ID, peer_ID, Address, type, My_last_connection, Arrival_date";
			return output;
		} else if (Type.equals("peer")) {
			output = "Peer_ID, Global_peer_ID, name, Broadcastable, Slogan, Last_sync_data, Arrival_date";
			return output;
		} else if (Type.equals("organization")) {
			output = "global_organization_ID, certification_methods, hash_org_alg, hash_org, category, ";
			output += "certificate, crl,default_scoring_options, instructions_new_motions, ";
			output += "instructions_registration, languages, name, name_forum, name_justification, name_motion, ";
			output += "name_organization, crl_date, creation_time, arrival_time";
			return output;
		}
		return null;
	}

	public String getTableColumType(String Type) {
		String output = null;
		if (Type.equals("directory")) {
			output = "Integer, Text, Numeric, Text";
			return output;
		} else if (Type.equals("peer_address")) {
			output = "Integer, Integer, Text, Text, Text, timestamp";
			return output;
		} else if (Type.equals("peer")) {
			output = "Integer, Text, Text, Numeric, Text, Text, Timestamp";
			return output;
		} else if (Type.equals("organization")) {
			output = "INTEGER, TEXT, TEXT, TEXT, BLOB, TEXT, BLOB, BLOB, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TIMESTAMP";
			return output;
		}
		return null;
	}

	public String getTableValue(String Type, ResultSet rs) throws SQLException {
		String output = "";
		if (Type.equals("directory")) {
			while (rs.next()) {
				output += "\t\t{\"" + rs.getString("Directory_ID") + "\", \""
						+ rs.getString("Domain_IP") + "\", \""
						+ rs.getString("Port") + "\", \""
						+ rs.getString("Comments") + "\" },\n\r";
			}

			return output;
		} else if (Type.equals("peer_address")) {

			while (rs.next()) {
				output += "\t\t{\"" + rs.getString("Peer_address_ID")
						+ "\", \"" + rs.getString("peer_ID") + "\", \""
						+ rs.getString("Address") + "\", \""
						+ rs.getString("type") + "\", \""
						+ rs.getString("My_last_connection") + "\", \""
						+ rs.getString("Arrival_date") + "\" },\n\r";
			}
			return output;
		} else if (Type.equals("peer")) {

			while (rs.next()) {
				output += "\t\t{\"" + rs.getString("Peer_ID") + "\", \""
						+ rs.getString("Global_peer_ID") + "\", \""
						+ rs.getString("name") + "\", \""
						+ rs.getString("Broadcastable") + "\", \""
						+ rs.getString("Slogan") + "\", \""
						+ rs.getString("Last_sync_date") + "\", \""
						+ rs.getString("Arrival_date") + "\" },\n\r";
			}
			return output;
		} else if (Type.equals("organization")) {

			while (rs.next()) {
				output += "\t\t{\"" + rs.getString("organization_ID")
						+ "\", \"" + rs.getString("global_organization_ID")
						+ "\", \"" + rs.getString("certification_methods")
						+ "\", \"" + rs.getString("hash_org_alg") + "\", \""
						+ rs.getString("hash_org") + "\", \""
						+ rs.getString("category") + "\", \""
						+ rs.getString("certificate") + "\", \""
						+ rs.getString("crl") + "\", \""
						+ rs.getString("default_scoring_options") + "\", \""
						+ rs.getString("instructions_new_motions") + "\", \""
						+ rs.getString("instructions_registration") + "\", \""
						+ rs.getString("languages") + "\", \""
						+ rs.getString("name") + "\", \""
						+ rs.getString("name_forum") + "\", \""
						+ rs.getString("name_justification") + "\", \""
						+ rs.getString("name_motion") + "\", \""
						+ rs.getString("name_organization") + "\", \""
						+ rs.getString("crl_date") + "\", \""
						+ rs.getString("creation_time") + "\", \""
						+ rs.getString("arrival_time") + "\" },\n\r";
			}
			return output;
		}
		return null;

	}

	public int findRowNumber() throws SQLException {
		int count=0;
		ResultSet r = stmt.executeQuery("select * from market");
		while (r.next()) {
			count++;
		}
		return count;
	}

}

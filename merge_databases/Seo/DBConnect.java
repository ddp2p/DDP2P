/*
 * Author     : Srini Venkatesh, Yonglok Seo
 * Subject    : CSE 5260
 * Instructor : Dr. M. Silaghi
 * */
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

/*
 * Class: DBConnect
 * 
 * Establishes the JDBC connection to the database
 * 
 */
public final class DBConnect {
	Connection conn;
	final String DBLocation;
	
	/*
	 * Constructor
	 * 
	 */
	public DBConnect(final String DBLocation) {
		conn = null;
		this.DBLocation = DBLocation;
	}

	/*
	 * Function: connect
	 * 
	 * Gets the connection to the database using JDBC library.
	 *  
	 */
	public Connection connect() {
		try {
			Class.forName("org.sqlite.JDBC");		
			conn = DriverManager.getConnection(DBLocation);
			return conn;
		} catch (ClassNotFoundException e) {
			System.out.println("Where is your SQLite JDBC Driver?");
			e.printStackTrace();			
		} catch (SQLException e) {
			System.out.println("Connection Failed! Check output console");
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * Function: close
	 * 
	 * Closes the connection to database.
	 * 
	 */
	public void close() {
		try {
			conn.close();
		} catch (SQLException e) {			
			e.printStackTrace();
		}
	}

}

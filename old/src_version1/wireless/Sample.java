package wireless;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Sample
{
  public static void main(String[] args) throws ClassNotFoundException
  {
    // load the sqlite-JDBC driver using the current class loader
    Class.forName("org.sqlite.JDBC");

    Connection connection = null;
    try
    {
      // create a database connection
      connection = DriverManager.getConnection("jdbc:sqlite:deliberation-app.db");
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);  // set timeout to 30 sec.

      //statement.executeUpdate("drop table if exists person");
      //statement.executeUpdate("create table person (id integer, name string)");
      //statement.executeUpdate("insert into person values(1, 'leo')");
      //statement.executeUpdate("insert into person values(2, 'yui')");
      ResultSet rs1 = statement.executeQuery("SELECT DISTINCT m.motion_ID FROM motion AS m  WHERE m.organization_ID=1 AND " +
				"m.motion_ID NOT IN ( SELECT nm.motion_ID FROM motion AS nm LEFT JOIN signature AS s" +
				" ON(s.motion_ID=nm.motion_ID)  WHERE nm.organization_ID=1 AND s.constituent_ID=10) LIMIT 1 " +
				"OFFSET 194;");
      
      
      ResultSet rs2 = statement.executeQuery("SELECT DISTINCT m.motion_ID FROM motion AS m  WHERE m.organization_ID=1 AND " +
				"m.motion_ID NOT IN ( SELECT nm.motion_ID FROM motion AS nm LEFT JOIN signature AS s" +
				" ON(s.motion_ID=nm.motion_ID)  WHERE nm.organization_ID=1 AND s.constituent_ID=10) LIMIT 1 " +
				"OFFSET 194;");
      
      while(rs1.next())
      {
        // read the result set
        //System.out.println("name = " + rs.getString("name"));
       // System.out.println("id = " + rs.getInt("id"));
    	  System.out.println("id1 = " +  rs1.getString(1));
    	 
      }
      while(rs2.next())
      {
    	  System.out.println("id2 = " +  rs2.getString(1));
    	 
      }
      
    }
    catch(SQLException e)
    {
      // if the error message is "out of memory", 
      // it probably means no database file is found
      System.err.println(e.getMessage());
    }
    finally
    {
      try
      {
        if(connection != null)
          connection.close();
      }
      catch(SQLException e)
      {
        // connection close failed.
        System.err.println(e);
      }
    }
  }
}

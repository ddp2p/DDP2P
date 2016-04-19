package net.ddp2p.common.wireless;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
public class Sample
{
  public static void main(String[] args) throws ClassNotFoundException
  {
    Class.forName("org.sqlite.JDBC");
    Connection connection = null;
    try
    {
      connection = DriverManager.getConnection("jdbc:sqlite:deliberation-app.db");
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);  
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
    	  System.out.println("id1 = " +  rs1.getString(1));
      }
      while(rs2.next())
      {
    	  System.out.println("id2 = " +  rs2.getString(1));
      }
    }
    catch(SQLException e)
    {
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
        System.err.println(e);
      }
    }
  }
}

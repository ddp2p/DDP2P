package wireless;
import java.io.IOException;
import java.util.ArrayList;
import config.Application;
import util.DBInterface;
import util.P2PDDSQLException;

public class test
{

	
	public static void main(String[]args) throws IOException, P2PDDSQLException
	{
		boolean _DEBUG = true;
		
		Application.db = new DBInterface("deliberation-app.db");
		
		
		String sel_1 = "SELECT count(DISTINCT m.motion_ID)  FROM motion AS m  WHERE m.organization_ID=1 " +
				"AND m.motion_ID NOT IN ( SELECT nm.motion_ID FROM motion AS nm LEFT JOIN signature AS s " +
				"ON(s.motion_ID=nm.motion_ID)  WHERE nm.organization_ID=1 AND s.constituent_ID=23);";
		    
		String sel_2 = "SELECT DISTINCT m.motion_ID FROM motion AS m  WHERE m.organization_ID=1 AND " +
				"m.motion_ID NOT IN ( SELECT nm.motion_ID FROM motion AS nm LEFT JOIN signature AS s" +
				" ON(s.motion_ID=nm.motion_ID)  WHERE nm.organization_ID=1 AND s.constituent_ID=23) LIMIT 1 " +
				"OFFSET 194;";
		
	
		ArrayList<ArrayList<Object>> count_1 =
				Application.db.select(sel_1, new String[]{},_DEBUG);
		
		ArrayList<ArrayList<Object>> count_2 =
				Application.db.select(sel_2, new String[]{},_DEBUG);
		
		System.out.println();
	}
}

/*   Copyright (C) 2012 Song Qin
		Author: Song Qin: qsong2008@my.fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
package net.ddp2p.widgets.census;
import java.util.ArrayList;
import javax.swing.JFrame;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
@SuppressWarnings("serial")
public class CensusThresholdComputation{
	public String[] thresholdPCNC(String pco, String nco, String t) {
		try {
			ArrayList<ArrayList<Object>> c;
			String sql = "SELECT * FROM witness";
			String sql2 = "select target_ID, NC, PC, case when((PC+1.0*"+pco+")/(NC+"+nco+")>="+t+") then 'T' else 'F' end,name from"+
					"("+
					"		select target_ID, sum(N)AS NC, sum(Y) AS PC"+
					"		FROM"+
					"		(select target_ID, "+
					"		case when(S='Y') then cnt else '0' end AS Y,"+
					"		case when(S='N') then cnt else '0' end AS N"+
					"		FROM"+
					"		 ("+
					"		select "+
					"		 target_ID ,"+
					"		 count(*) AS cnt,"+
					"		 case when(sense_y_n='1') then 'Y' else 'N' end AS S"+
					"		 from witness"+
					"		 group by target_ID,sense_y_n"+
					"		)"+
					"		)"+
					"		 group by target_ID"+
					"		)t1, constituent where t1.target_ID=constituent.constituent_ID;";
			c = Application.getDB().select(sql2, new String[]{});
			System.out.println("[Target_ID, Negative Count, Positive Count, ValidityOfIdentity,Constituent Name]");
			System.out.println(c);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return null;
	}
public static void populateWitnessData1(){
			Integer sense_y_n=0;
			Integer source_ID=2;
			Integer target_ID=0;
			String insertSql="INSERT INTO witness(source_ID, target_ID,sense_y_n) values (?,?,?)";
			String sense_y_n_str=""+sense_y_n;
			String source_ID_str=""+source_ID;
			String target_ID_str=""+target_ID;
			try {
				Application.getDB().delete("DELETE FROM witness", new String[]{});
				Application.getDB().insert(insertSql, new String[] {""+0,""+2,""+0});
				Application.getDB().insert(insertSql, new String[] {""+0,""+3,""+1});
				Application.getDB().insert(insertSql, new String[] {""+0,""+4,""+1});
				Application.getDB().insert(insertSql, new String[] {""+3,""+1,""+0});
				Application.getDB().insert(insertSql, new String[] {""+4,""+1,""+1});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
}
public static void populateWitnessData2(){
			int sense_y_n=0;
			int source_ID=2;
			int target_ID=0;
			String insertSql="INSERT INTO witness(source_ID, target_ID,sense_y_n) values (?,?,?)";
			String sense_y_n_str=""+sense_y_n;
			String source_ID_str=""+source_ID;
			String target_ID_str=""+target_ID;
			try {
				Application.getDB().delete("DELETE FROM witness", new String[]{});
				Application.getDB().insert(insertSql, new String[] {""+0,""+2,""+1});
				Application.getDB().insert(insertSql, new String[] {""+0,""+3,""+1});
				Application.getDB().insert(insertSql, new String[] {""+0,""+4,""+1});
				Application.getDB().insert(insertSql, new String[] {""+3,""+1,""+0});
				Application.getDB().insert(insertSql, new String[] {""+4,""+1,""+1});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
}
	public static void main(String[] args) {
		try {
			Application.setDB(new DBInterface(Application.DELIBERATION_FILE));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		JFrame wg=new CensusFuzzy(-1,-1).getCensusFuzzyFrame();
        wg.pack();
        wg.setVisible(true);
	}
}

/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 
		Author: Ossamah Dhannoon
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
/* ------------------------------------------------------------------------- */
package wireless;





import java.awt.BorderLayout;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JTable;

import ASN1.Encoder;

import config.DD;

import simulator.WirelessLog;
import util.Util;


public class test
{

	
	public static void main(String[]args) throws IOException
	{
		/*String s = "1353446019094";
	long milis = Long.parseLong(s);
		Calendar c = Util.CalendargetInstance();
		c.setTimeInMillis(milis);
		String date = Encoder.getGeneralizedTime(c);
		System.out.println(date);*/
		JFrame frame = new JFrame("Table Demo");
		String columns[] = {"Name","Age","Gender"};
		Object data[][] = {
				{"Tom",new Integer(20),"Male"},
				{"Tina", new Integer(18), "Female"},
				{"Raj",new Integer(19),"Male"}		
		};
		
		
		JTable table = new JTable(data,columns);
		frame.setVisible(true);
		frame.setBounds(0,0,500,500);
		frame.add(table.getTableHeader(),BorderLayout.PAGE_START);
		frame.add(table);
	}
}

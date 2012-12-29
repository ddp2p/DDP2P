/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012
		Author: Khalid Alhamed and Marius Silaghi: msilaghi@fit.edu
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
package data;

import java.util.ArrayList;
import java.util.Calendar;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;

import util.Util;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;

public class D_UpdatesInfo extends ASN1.ASNObj{
	private static final boolean DEBUG = false;
	public D_TesterDefinition[] testerDef;     // dragged or data type can be ArrayList<TesterInfo>
	public String url;                    // dragged
	public String original_mirror_name;   // dragged
	public String my_mirror_name;
	public String last_version;
	public D_TesterInfo[] testerInfo;     // data type can be ArrayList<TesterInfo>
	public boolean used;
	public D_ReleaseQuality[] releaseQoT; // empty when dragged
	public long updates_ID;
	public Calendar last_contact_date;
	public String activity;
	public D_UpdatesInfo(ArrayList<Object> _u) {
		init(_u);
	}
	public void init(ArrayList<Object> _u){
		System.out.println("D_UpdatesInfo: <init>: start");
		updates_ID = Util.lval(_u.get(table.updates.F_ID),-1);
		original_mirror_name = Util.getString(_u.get(table.updates.F_ORIGINAL_MIRROR_NAME));
		my_mirror_name = Util.getString(_u.get(table.updates.F_MY_MIRROR_NAME));
		url = Util.getString(_u.get(table.updates.F_URL));
		last_version = Util.getString(_u.get(table.updates.F_LAST_VERSION));
		used = Util.stringInt2bool(_u.get(table.updates.F_USED), false);
		try {
			System.out.println("D_UpdatesInfo: <init>: reconstr");
			testerInfo = D_TesterInfo.reconstructArrayFromString(Util.getString(_u.get(table.updates.F_TESTER_INFO)));
			System.out.println("D_UpdatesInfo: <init>: reconstructed");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("D_UpdatesInfo: <init>: error handled");
		}
		last_contact_date = Util.getCalendar(Util.getString(_u.get(table.updates.F_LAST_CONTACT)));
		activity = Util.getString(_u.get(table.updates.F_ACTIVITY));
		System.out.println("D_UpdatesInfo: <init>: done");
		/*
	    releaseQoT = new D_ReleaseQuality[3];
	    releaseQoT[0]= new D_ReleaseQuality();
	    releaseQoT[0].quality="Security"; 
	    	                              releaseQoT[0].subQualities= new String[2];
	                                      releaseQoT[0].subQualities[0]="Code";
	     	                              releaseQoT[0].subQualities[1]="DoS"; 
	    releaseQoT[1]= new D_ReleaseQuality();
	    releaseQoT[1].quality="Platform"; releaseQoT[1].subQualities= new String[3];
	                                      releaseQoT[1].subQualities[0]="WIN";
	                                      releaseQoT[1].subQualities[1]="MAC";
	                                      releaseQoT[1].subQualities[2]="UNIX";
	    releaseQoT[2]= new D_ReleaseQuality();                                  
	    releaseQoT[2].quality="Useability"; releaseQoT[2].subQualities= new String[2];
	                                        releaseQoT[2].subQualities[0]="Easy";
	                                        releaseQoT[2].subQualities[1]="ResponseTime";  	
		*/
	}
	@Override
	public Encoder getEncoder() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Object decode(Decoder dec) throws ASN1DecoderFail {
		// TODO Auto-generated method stub
		return null;
	}
	public void store() throws SQLiteException {
		String params[] = new String[table.updates.F_FIELDS];
		params[table.updates.F_ORIGINAL_MIRROR_NAME] = this.original_mirror_name;
		params[table.updates.F_MY_MIRROR_NAME] = this.my_mirror_name;
		params[table.updates.F_URL] = this.url;
		params[table.updates.F_LAST_VERSION] = this.last_version;
		if(this.used)params[table.updates.F_USED] = "1"; else params[table.updates.F_USED] = "0";
		params[table.updates.F_TESTER_INFO] = D_TesterInfo.encodeArray(this.testerInfo);
		params[table.updates.F_LAST_CONTACT] = Encoder.getGeneralizedTime(this.last_contact_date);
		params[table.updates.F_ACTIVITY] = this.activity;
		params[table.updates.F_ID] = Util.getStringID(this.updates_ID);
	
		Application.db.updateNoSync(table.updates.TNAME, table.updates._fields_updates_no_ID,
				new String[]{table.updates.updates_ID},
				params,DEBUG);
	}
}
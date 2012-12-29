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

public class D_UpdatesKeysInfo extends ASN1.ASNObj{
	private static final boolean DEBUG = false;
	public String original_tester_name;
	public String my_tester_name;
	public String url;
	//public String last_version;
	//public boolean used;
	public float weight;
	public boolean reference;
	public boolean trusted_as_tester;
	public boolean trusted_as_mirror;
	public long updates_ID;
	public String public_key;
	public String public_key_hash;
	public Calendar last_contact_date;
	public String activity;
	public D_UpdatesKeysInfo(ArrayList<Object> _u) {
		updates_ID = Util.lval(_u.get(table.updatesKeys.F_ID),-1);
		original_tester_name = Util.getString(_u.get(table.updatesKeys.F_ORIGINAL_MIRROR_NAME));
		my_tester_name = Util.getString(_u.get(table.updatesKeys.F_MY_MIRROR_NAME));
		//url = Util.getString(_u.get(table.updatesKeys.F_URL));
		public_key = Util.getString(_u.get(table.updatesKeys.F_PUBLIC_KEY));
		//last_version = Util.getString(_u.get(table.updatesKeys.F_LAST_VERSION));
		//used = Util.stringInt2bool(_u.get(table.updatesKeys.F_USED), false);
		trusted_as_tester = Util.stringInt2bool(_u.get(table.updatesKeys.F_USED_TESTER), false);
		trusted_as_mirror = Util.stringInt2bool(_u.get(table.updatesKeys.F_USED_MIRROR), false);
		weight = Util.fval(_u.get(table.updatesKeys.F_WEIGHT),1);
		reference = Util.stringInt2bool(_u.get(table.updatesKeys.F_REFERENCE), false);
		//last_contact_date = Util.getCalendar(Util.getString(_u.get(table.updatesKeys.F_LAST_CONTACT)));
		//activity = Util.getString(_u.get(table.updatesKeys.F_ACTIVITY));
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
		String params[] = new String[table.updatesKeys.F_FIELDS];
		params[table.updatesKeys.F_ORIGINAL_MIRROR_NAME] = this.original_tester_name;
		params[table.updatesKeys.F_MY_MIRROR_NAME] = this.my_tester_name;
		//params[table.updatesKeys.F_URL] = this.url;
		params[table.updatesKeys.F_PUBLIC_KEY] = this.public_key;
		//params[table.updatesKeys.F_LAST_VERSION] = this.last_version;
		//if(this.used)params[table.updatesKeys.F_USED] = "1"; else params[table.updatesKeys.F_USED] = "0";
		if(this.trusted_as_mirror)params[table.updatesKeys.F_USED_MIRROR] = "1"; else params[table.updatesKeys.F_USED_MIRROR] = "0";
		if(this.trusted_as_tester)params[table.updatesKeys.F_USED_TESTER] = "1"; else params[table.updatesKeys.F_USED_TESTER] = "0";
		params[table.updatesKeys.F_WEIGHT] = ""+this.weight;
		if(this.reference)params[table.updatesKeys.F_REFERENCE] = "1"; else params[table.updatesKeys.F_REFERENCE]="0";
		//params[table.updatesKeys.F_LAST_CONTACT] = Encoder.getGeneralizedTime(this.last_contact_date);
		//params[table.updatesKeys.F_ACTIVITY] = this.activity;
		params[table.updatesKeys.F_ID] = Util.getStringID(this.updates_ID);
	
		Application.db.updateNoSync(table.updatesKeys.TNAME, table.updatesKeys._fields_updates_keys_no_ID,
				new String[]{table.updatesKeys.updates_keys_ID},
				params,DEBUG);
	}
}
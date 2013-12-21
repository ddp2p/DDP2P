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

import ciphersuits.Cipher;
import ciphersuits.PK;

import util.P2PDDSQLException;

import config.Application;

import table.tester;
import updates.VersionInfo;
import util.Util;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;

public class D_UpdatesKeysInfo extends ASN1.ASNObj{
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public String original_tester_name;
	public String my_tester_name;
	public String email;
	public String url; // just for ads
	public String description;
	public float weight;
	public boolean reference;
	public boolean trusted_as_tester;
	public boolean trusted_as_mirror;
	public long tester_ID = -1;
	public String public_key;
	public String public_key_hash;
	public String expected_test_thresholds;
	//public Calendar last_contact_date;
	//public String activity;
		public D_UpdatesKeysInfo() {}
	public D_UpdatesKeysInfo(ArrayList<Object> _u) {
		init(_u);
	}
	public void init(ArrayList<Object> _u) {
		tester_ID = Util.lval(_u.get(table.tester.F_ID),-1);
		original_tester_name = Util.getString(_u.get(table.tester.F_ORIGINAL_TESTER_NAME));
		my_tester_name = Util.getString(_u.get(table.tester.F_MY_TESTER_NAME));
		email = Util.getString(_u.get(table.tester.F_EMAIL));
		url = Util.getString(_u.get(table.tester.F_URL));
		description = Util.getString(_u.get(table.tester.F_DESCRIPTION));
		public_key = Util.getString(_u.get(table.tester.F_PUBLIC_KEY));
		public_key_hash = Util.getString(_u.get(table.tester.F_PUBLIC_KEY_HASH));
		//last_version = Util.getString(_u.get(table.tester.F_LAST_VERSION));
		//used = Util.stringInt2bool(_u.get(table.tester.F_USED), false);
		trusted_as_tester = Util.stringInt2bool(_u.get(table.tester.F_USED_TESTER), false);
		trusted_as_mirror = Util.stringInt2bool(_u.get(table.tester.F_USED_MIRROR), false);
		weight = Util.fval(_u.get(table.tester.F_WEIGHT),1);
		reference = Util.stringInt2bool(_u.get(table.tester.F_REFERENCE), false);
		expected_test_thresholds = Util.getString(_u.get(table.tester.F_EXPECTED_TEST_THRESHOLDS));
		//last_contact_date = Util.getCalendar(Util.getString(_u.get(table.tester.F_LAST_CONTACT)));
		//activity = Util.getString(_u.get(table.tester.F_ACTIVITY));
	}
	public D_UpdatesKeysInfo(String public_key2) {
		String sql = "SELECT "+tester.fields_tester+" FROM "+tester.TNAME+" WHERE "+table.tester.public_key+"=?;";
		String[]params = new String[]{public_key2};// where clause?
		ArrayList<ArrayList<Object>> u;
		try {
			u = Application.db.select(sql, params, _DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		if(u.size()>0) init(u.get(0));
	}
	public D_UpdatesKeysInfo(D_TesterDefinition d) {
		this.original_tester_name = d.name;
		this.public_key = d.public_key;
		this.public_key_hash = Util.getGIDhash(d.public_key);
	}
	public boolean existsInDB() {
		D_UpdatesKeysInfo old = new D_UpdatesKeysInfo(public_key);
		return old.tester_ID >=0 ;
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
	public void store(String cmd) throws P2PDDSQLException {
		if(DEBUG)System.out.println("in UpdateKeysInfo.store()");
		String params[] = new String[table.tester.F_FIELDS];
		params[table.tester.F_ORIGINAL_TESTER_NAME] = this.original_tester_name;
		params[table.tester.F_MY_TESTER_NAME] = this.my_tester_name;
		params[table.tester.F_EMAIL] = this.email;
		params[table.tester.F_URL] = this.url;
		params[table.tester.F_DESCRIPTION] = this.description;
		//params[table.tester.F_URL] = this.url;
		params[table.tester.F_PUBLIC_KEY] = this.public_key;
		params[table.tester.F_PUBLIC_KEY_HASH] = this.public_key_hash;
		//params[table.tester.F_LAST_VERSION] = this.last_version;
		//if(this.used)params[table.tester.F_USED] = "1"; else params[table.tester.F_USED] = "0";
		params[table.tester.F_USED_MIRROR] = Util.bool2StringInt(trusted_as_mirror);
		params[table.tester.F_USED_TESTER] = Util.bool2StringInt(trusted_as_tester);
		params[table.tester.F_WEIGHT] = ""+this.weight;
		params[table.tester.F_EXPECTED_TEST_THRESHOLDS] = this.expected_test_thresholds;
		if(this.reference)params[table.tester.F_REFERENCE] = "1"; else params[table.tester.F_REFERENCE]="0";
		//params[table.tester.F_LAST_CONTACT] = Encoder.getGeneralizedTime(this.last_contact_date);
		//params[table.tester.F_ACTIVITY] = this.activity;
		params[table.tester.F_ID] = Util.getStringID(this.tester_ID);
	    if(cmd.equals("update"))
			Application.db.updateNoSync(table.tester.TNAME, table.tester._fields_tester_no_ID,
										new String[]{table.tester.tester_ID},
										params,DEBUG);
				
		if(cmd.equals("insert")){
			if(DEBUG)System.out.println("in UpdateKeysInfo.store()/ insert");
			// check the existance based on PK or url?
			String params2[]=new String[table.tester.F_FIELDS_NOID];
			System.arraycopy(params,0,params2,0,params2.length);
			if(DEBUG)for(int i=0; i<params2.length;i++)
						System.out.println("params2["+i+"]: "+ params2[i]);
			this.tester_ID = Application.db.insertNoSync(table.tester.TNAME, table.tester._fields_tester_no_ID,params2, true);
		}
	}
	// TODO Auto-generated method stub
	//		for(PK pk : _trusted){
	//			if(pk==null){
	//				if(_DEBUG) System.out.println(" ClientUpdates: run: no key: ");
	//				continue;
	//			}
	//			if(a.verifySignature(pk)){
	//				if(DEBUG) System.out.println(" ClientUpdates: run: success with key: ");
	//				signed = true; 
	//				break;
	//			}else{
	//				if(_DEBUG) System.out.println(" ClientUpdates: run: fail with key: ");						
	//			}
	//		}
	//TODO
	public static boolean verifySignaturesOfVI(VersionInfo a) {
		System.out.println("\nD_UpdatesKeyInfo: verifySignaturesOfVI: start ************************");
		System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: input: VI="+a);
		System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: ************************");

		for(int i=0; i<a.testers_data.length; i++ ){
			D_TesterSignedData tsd = new D_TesterSignedData(a, i);
		//	System.out.println("a.testers_data["+i+"].public_key_hash" + a.testers_data[i].public_key_hash);
		//	System.out.println("a.testers_data["+i+"].signature" + Util.stringSignatureFromByte(a.testers_data[i].signature));
			PK pk = getKey(a.testers_data[i].public_key_hash);
			if(pk == null){
				System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: ************************");
				System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: PK is null: VI="+a);
				System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: ************************");
				System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: PK is null: VI="+tsd);
				System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: end null ************************\n");
				return false;
			}
			if((pk!=null) && !tsd.verifySignature(pk)){
				System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: ************************");
				System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: PK is not null: VI="+a);
				System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: ************************");
				System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: PK is not null: VItester="+tsd);
				System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: end no null ************************\n");
				return false;
			}
		}
		System.out.println("\nD_UpdatesKeyInfo: verifySignaturesOfVI: success ************************");
		return true;
	}
	/**
	 * Gets the public key for the hash in the message
	 * @param public_key_hash
	 * @return
	 */
	public static PK getKey(String public_key_hash) {
		ArrayList<ArrayList<Object>> a;
		try {
			a = Application.db.select(
					"SELECT "+table.tester.public_key+
					" FROM "+table.tester.TNAME+
					" WHERE "+table.tester.public_key_hash+"=?;",
					new String[]{public_key_hash}, _DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		if (a.size()==0) return null;
		return Cipher.getPK(Util.getString(a.get(0).get(0)));
	}
}


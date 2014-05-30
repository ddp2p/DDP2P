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

import static util.Util._;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import util.DBInterface;

import table.subscriber;
import util.Util;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class D_SubscriberInfo extends ASN1.ASNObj{
	private static final boolean _DEBUG = true;
	public static  boolean DEBUG = false;
	public long subscriber_ID;
	public String GID;
	public String GID_hash;
	public String instance;
	public String name;
	public String mode;
	public boolean topic;
	public boolean ad;
	public boolean plaintext;
	public boolean payment;                    
	public Calendar expiration;

	private DBInterface db;
	
	public D_SubscriberInfo() {
		
	}
	public D_SubscriberInfo(DBInterface db) {
		this.db = db;
	}

	public D_SubscriberInfo(ArrayList<Object> _u, DBInterface db) {
		this.db = db;
		init(_u);
	}
/*	public boolean existsInDB() {
		D_UpdatesInfo old = new D_UpdatesInfo(url);
		return old.updates_ID >=0 ;
	}
*/
	public String toString() {
		return "D_SubscriberInfo: ["+
				"\n\t subscriber_ID="+subscriber_ID+
				"\n\t GID="+GID+
				"\n\t GID="+GID_hash+
				"\n\t instance="+instance+
				"\n\t name="+name+
				"\n\t topic="+topic+
				"\n\t ad="+ad+
				"\n\t plaintext="+plaintext+
				"\n\t payment="+payment+
				"\n\t mode="+mode+
				"\n\t expiration="+expiration+	
				"]";
	}
	public void init(ArrayList<Object> _u){
		if(DEBUG) System.out.println("D_TermsInfo: <init>: start");
		subscriber_ID = Util.lval(_u.get(table.subscriber.F_ID),-1);
		GID = Util.getString(_u.get(table.subscriber.F_GID));
		GID_hash = Util.getString(_u.get(table.subscriber.F_GID_HASH));
		instance = Util.getString(_u.get(table.subscriber.F_INSTANCE));
		name = Util.getString(_u.get(table.subscriber.F_NAME));
		topic = Util.stringInt2bool(_u.get(table.subscriber.F_TOPIC), false);
		ad = Util.stringInt2bool(_u.get(table.subscriber.F_AD), false);
		plaintext = Util.stringInt2bool(_u.get(table.subscriber.F_PLAINTEXT), false);
		payment = Util.stringInt2bool(_u.get(table.subscriber.F_PAYMENT), false);
		mode = Util.getString(_u.get(table.subscriber.F_MODE));
		expiration = Util.getCalendar(Util.getString(_u.get(table.subscriber.F_EXPIRATION)));
		if(DEBUG) System.out.println("D_SubscriberInfo: <init>: done");
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
	public boolean SubExist(){
		String sql = " SELECT "+subscriber.subscriber_ID+
					 " FROM "+ subscriber.TNAME +
					 " WHERE "+subscriber.GID	+ " = ?"+
					 " AND " + subscriber.instance + " = ?" + 
					 " AND " + subscriber.name + " = ?" + 
					 " AND " + subscriber.mode + " = ?" + ";";
		System.out.println(sql);
		String[]params = new String[]{this.GID, this.instance, this.name, this.mode };// where clause?
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		if(u==null || u.size() == 0) return false;
		return true;
	}
	public void storeNoSync(String cmd) throws P2PDDSQLException {
		//Application.db.select("SELECT "+table.+" FROM", params)
		
		String params[] = new String[table.subscriber.F_FIELDS];
		params[table.subscriber.F_GID] = Util.getString(this.GID);
		params[table.subscriber.F_INSTANCE] = Util.getString(this.instance);
		params[table.subscriber.F_NAME] = Util.getString(this.name);
		params[table.subscriber.F_TOPIC] = Util.getIntStringBool(this.topic);
		params[table.subscriber.F_AD] = Util.getIntStringBool(this.ad);
		params[table.subscriber.F_PLAINTEXT] = Util.getIntStringBool(this.plaintext);
		params[table.subscriber.F_PAYMENT] = Util.getIntStringBool(this.payment);
		params[table.subscriber.F_EXPIRATION] = Encoder.getGeneralizedTime(this.expiration);
		params[table.subscriber.F_MODE] = Util.getString(this.mode);
		params[table.subscriber.F_ID] = Util.getString(this.subscriber_ID);
        
//        if(SubExist()){
//        	Application.warning("The subscriber is already exist with same sevice ", "Subscriber Exist");
//        	return;
//        }
		if(cmd.equals("update"))
			db.updateNoSyncNULL(
					table.subscriber.TNAME, 
					table.subscriber._fields_subscribers_no_ID,
					new String[]{table.subscriber.subscriber_ID},
					params,_DEBUG);

		if(cmd.equals("insert")){
			// check the existance based on PK or url?
			String params2[]=new String[table.subscriber.F_FIELDS_NOID];
			System.arraycopy(params,0,params2,0,params2.length);
			//System.out.println("params2[last]: "+ params2[table.updates.F_FIELDS_NOID-1]);
			this.subscriber_ID = 
					db.insertNoSync(
							table.subscriber.TNAME,
							table.subscriber._fields_subscribers_no_ID,
							params2, _DEBUG);
		}
		
	}
	
}


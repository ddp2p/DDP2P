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

import static util.Util.__;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;

import util.DBInterface;
import util.P2PDDSQLException;
import util.Summary;
import config.Application;
import config.Application_GUI;
import config.DD;
import table.mirror;
import updates.VersionInfo;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class D_MirrorInfo extends ASN1.ASNObj implements Summary{
	private static final boolean _DEBUG = true;
	public static final String action_update = "update";
	public static final String action_insert = "insert";
	public static  boolean DEBUG = false;
	public D_TesterDefinition[] testerDef;     // dragged (optional) it should be read from a mirror 
	public String public_key;			  // dragged as part of the url (need to be parsed from the url)
	public String url;                    // dragged
	public String original_mirror_name;   // dragged
	public String my_mirror_name;
	public String last_version;
	public String last_version_branch;
	public byte[] last_version_info;  // serializable versionInfo object, obtained form mirror
	public D_TesterInfo[] testerInfo;     // data type can be ArrayList<TesterInfo>
	public boolean used;
	public D_ReleaseQuality[] releaseQoT; // empty when dragged
	public long mirror_ID = -1;
	public Calendar last_contact_date;
	public String activity;
	public int version = 0;
	public String location ;// new field good for faster downloading! 
	public String protocol ; // new field "http" or "ftp"
	public String data_version ; // new field, signed fields
	public Calendar creation_date ; // new field, ??
	public Calendar preference_date ; // new field, ??
	
	public D_MirrorInfo() {
		
	}
	public D_MirrorInfo(String _url, DBInterface db) {
		String sql = "SELECT "+mirror.fields_updates+" FROM "+mirror.TNAME+" WHERE "+table.mirror.url+"=?;";
		String[]params = new String[]{_url};// where clause?
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}		
		if(u.size()>0) init(u.get(0));
	}
	public D_MirrorInfo(String _url) {
		String sql = "SELECT "+mirror.fields_updates+" FROM "+mirror.TNAME+" WHERE "+table.mirror.url+"=?;";
		String[]params = new String[]{_url};// where clause?
		ArrayList<ArrayList<Object>> u;
		try {
			u = Application.db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}		
		if(u.size()>0) init(u.get(0));
	}
	public D_MirrorInfo(ArrayList<Object> _u) {
		init(_u);
	}
	public D_MirrorInfo(long id) {
		String sql = "SELECT "+mirror.fields_updates+" FROM "+mirror.TNAME+" WHERE "+table.mirror.mirror_ID+"=?;";
		String[]params = new String[]{Util.getStringID(id)};// where clause?
		ArrayList<ArrayList<Object>> u;
		try {
			u = Application.db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}		
		if(u.size()>0) init(u.get(0));
	}
	public boolean existsInDB(DBInterface db) {
		D_MirrorInfo old = new D_MirrorInfo(url, db);
		return old.mirror_ID >=0 ;
	}
	@Override
	public D_MirrorInfo instance() throws CloneNotSupportedException{return new D_MirrorInfo();}
	public void init(ArrayList<Object> _u){
		if(DEBUG) System.out.println("D_MirrorInfo: <init>: start");
		mirror_ID = Util.lval(_u.get(table.mirror.F_ID),-1);
		public_key = Util.getString(_u.get(table.mirror.F_PUBLIC_KEY));
		original_mirror_name = Util.getString(_u.get(table.mirror.F_ORIGINAL_MIRROR_NAME));
		my_mirror_name = Util.getString(_u.get(table.mirror.F_MY_MIRROR_NAME));
		url = Util.getString(_u.get(table.mirror.F_URL));
		last_version = Util.getString(_u.get(table.mirror.F_LAST_VERSION));
		last_version_branch = Util.getString(_u.get(table.mirror.F_LAST_VERSION_BRANCH));
		used = Util.stringInt2bool(_u.get(table.mirror.F_USED), false);
		try {
			if(DEBUG) System.out.println("D_MirrorInfo: <init>: testerInfo reconstr");
			testerInfo = D_TesterInfo.reconstructArrayFromString(Util.getString(_u.get(table.mirror.F_TESTER_INFO)));
			if(DEBUG) System.out.println("D_MirrorInfo: <init>: reconstructed");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("D_MirrorInfo: testerInfo <init>: error handled");
		}
		try {
			if(DEBUG) System.out.println("D_MirrorInfo: releaseQoT <init>: reconstr");
			releaseQoT = D_ReleaseQuality.reconstructArrayFromString(Util.getString(_u.get(table.mirror.F_RELEASE_QOT)));
			if(DEBUG) System.out.println("D_MirrorInfo: releaseQoT <init>: reconstructed");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("D_MirrorInfo: releaseQoT <init>: error handled");
		}
		last_contact_date = Util.getCalendar(Util.getString(_u.get(table.mirror.F_LAST_CONTACT)));
		activity = Util.getString(_u.get(table.mirror.F_ACTIVITY));
		location = Util.getString(_u.get(table.mirror.F_LOCATION));// new field good for faster downloading! 
	    protocol = Util.getString(_u.get(table.mirror.F_PROTOCOL)); // new field "http" or "ftp"
	    data_version = Util.getString(_u.get(table.mirror.F_DATA_VERSION)); // how to insert it?
		creation_date = Util.getCalendar(Util.getString(_u.get(table.mirror.F_CREATION_DATE))); // how to insert it?
		preference_date = Util.getCalendar(Util.getString(_u.get(table.mirror.F_PREFERENCE_DATE))); // how to insert it?
		if(DEBUG) System.out.println("D_MirrorInfo: <init>: done");
		/*
	    releaseQoT = new D_ReleaseQuality[3];
	    releaseQoT[0]= new D_ReleaseQuality();
	    releaseQoT[0]._quality= new String[1];
	    releaseQoT[0]._quality[0]="Security"; 
//	    	                              releaseQoT[0].subQualities= new String[2];
//	                                      releaseQoT[0].subQualities[0]="Code";
//	     	                              releaseQoT[0].subQualities[1]="DoS"; 
	    releaseQoT[1]= new D_ReleaseQuality();
	    releaseQoT[1]._quality= new String[2];
	    releaseQoT[1]._quality[0]="Platform";
	    releaseQoT[1]._quality[1]="WIN"; 
	    	
	    releaseQoT[2]= new D_ReleaseQuality();
	    releaseQoT[2]._quality= new String[2];
	    releaseQoT[2]._quality[0]="Platform";
	    releaseQoT[2]._quality[1]="MAC";	
 
	    	                              releaseQoT[1].subQualities= new String[3];
	                                      releaseQoT[1].subQualities[0]="WIN";
	                                      releaseQoT[1].subQualities[1]="MAC";
	                                      releaseQoT[1].subQualities[2]="UNIX";
	    releaseQoT[2]= new D_ReleaseQuality();                                  
	    releaseQoT[2].quality="Useability"; releaseQoT[2].subQualities= new String[2];
	                                        releaseQoT[2].subQualities[0]="Easy";
	                                        releaseQoT[2].subQualities[1]="ResponseTime";  	
		*/
	}
	public static final String sql_upd = "SELECT "+table.mirror.url +
			" FROM "+table.mirror.TNAME+
			" WHERE "+table.mirror.used+"= ? ;";
	/**
	 * Retrieve mirrors urls from the DB
	 * @param 
	 * @return ArrayList<String>
	 * @throws P2PDDSQLException
	 */
	static public ArrayList<String> getUpdateURLs() throws P2PDDSQLException{
    	ArrayList<String> result =new ArrayList<String>() ;
    	ArrayList<ArrayList<Object>> urls;
    	urls=Application.db.select(sql_upd,
    			new String[]{"1"}, DEBUG);
    	if(urls.size()==0){
    		if(DEBUG) System.err.println(__("No URLs found in table: ")+table.mirror.TNAME);
    		return result;
    	}
    	for(int i=0; i<urls.size(); i++)
    		result.add((String)urls.get(i).get(0)) ;
   		return result;
	}
     /**
	 * Retrieve mirrors from the DB
	 * @param url 
	 * @return D_UpdateInfo
	 * @throws P2PDDSQLException
	 */
	static public D_MirrorInfo getUpdateInfo(String url) throws P2PDDSQLException{
    	D_MirrorInfo result =null ;
    	ArrayList<ArrayList<Object>> updateInfo;
    	updateInfo=Application.db.select("SELECT "+table.mirror.fields_updates +
    			" FROM "+table.mirror.TNAME+
    			" WHERE "+table.mirror.url+"= ? ;",
    			new String[]{url}, DEBUG);
    	if(updateInfo.size()==0){
    		if(DEBUG) System.err.println(__("No updateInfo record match given url found in table: ")+table.mirror.TNAME);
    		return result;
    	}
    	// only one record (updateInfo) to be retrieve
    	result = new D_MirrorInfo(updateInfo.get(0));
//    	for(int i=0; i<urls.size(); i++)
//    		result.add((String)urls.get(i).get(0)) ;
   		return result;
	}
	@Override
	public String toSummaryString() {
		return "D_UpdateInfo: v="+version+"\nname="+this.original_mirror_name+"\n"+"url="+this.url;
	}
	public String toString(){
		return toSummaryString();
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(this.original_mirror_name));
		enc.addToSequence(new Encoder(url));
		enc.setASN1Type(getASNType());
		return enc;
	}
	@Override
	public D_MirrorInfo decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getInteger().intValue();
		this.original_mirror_name = d.getFirstObject(true).getString();
		this.url = d.getFirstObject(true).getString();
		return this;
	}
	public void store() throws P2PDDSQLException {
		if(this.existsInDB(Application.db)) this.store(action_update); 
		else this.store(action_insert);
	}
	public void store(DBInterface db) throws P2PDDSQLException {
		if(this.existsInDB(db)) this.store(action_update); 
		else this.store(action_insert);
	}
	public void store(String cmd) throws P2PDDSQLException {
		String params[] = new String[table.mirror.F_FIELDS];
		params[table.mirror.F_ORIGINAL_MIRROR_NAME] = this.original_mirror_name;
		params[table.mirror.F_MY_MIRROR_NAME] = this.my_mirror_name;
		params[table.mirror.F_URL] = this.url;
		params[table.mirror.F_LAST_VERSION] = this.last_version;
		params[table.mirror.F_LAST_VERSION_BRANCH] = this.last_version_branch;
		if(this.used)params[table.mirror.F_USED] = "1"; else params[table.mirror.F_USED] = "0";
		params[table.mirror.F_RELEASE_QOT] = D_ReleaseQuality.encodeArray(this.releaseQoT);
		params[table.mirror.F_TESTER_INFO] = D_TesterInfo.encodeArray(this.testerInfo);
		params[table.mirror.F_LAST_CONTACT] = Encoder.getGeneralizedTime(this.last_contact_date);
		params[table.mirror.F_ACTIVITY] = this.activity;
		if(this.last_version_info!=null)params[table.mirror.F_LAST_VERSION_INFO] = new String(util.Base64Coder.encode(this.last_version_info));
		params[table.mirror.F_ID] = Util.getStringID(this.mirror_ID);
	    if(cmd.equals(action_update))
		Application.db.updateNoSync(table.mirror.TNAME, table.mirror._fields_updates_no_ID,
				new String[]{table.mirror.mirror_ID},
				params,DEBUG);
		if(cmd.equals(action_insert)){
		// check the existance based on PK or url?
		String params2[]=new String[table.mirror.F_FIELDS_NOID];
		System.arraycopy(params,0,params2,0,params2.length);
		//System.out.println("params2[last]: "+ params2[table.mirror.F_FIELDS_NOID-1]);
		this.mirror_ID = Application.db.insertNoSync(table.mirror.TNAME, table.mirror._fields_updates_no_ID,params2);
		}
	}
	public static boolean hasTrustedTesters() {
		ArrayList<ArrayList<Object>> a;
		try {
			a = Application.db.select("SELECT "+table.tester.trusted_as_tester+","+table.tester.trust_weight+" FROM "+table.tester.TNAME+
					" WHERE "+table.tester.trusted_as_tester+"='1' AND "+table.tester.trust_weight+"!='0'",
					new String[]{});
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		return a.size()>0;
	}
	// check testers preferances (weight+ number+ Required)
	public static Hashtable<VersionInfo, Hashtable<String, VersionInfo>> validateVersionInfo(
			Hashtable<VersionInfo, Hashtable<String, VersionInfo>> versions) {
		Hashtable<VersionInfo, Hashtable<String, VersionInfo>> result = new Hashtable<VersionInfo, Hashtable<String, VersionInfo>>();
		for(VersionInfo v : versions.keySet()) {
			Hashtable<String, VersionInfo> available = versions.get(v);
			HashSet<String> testers= new HashSet<String>();
			for(String url:available.keySet()) {
				VersionInfo vi = available.get(url);
				for(D_TesterInfo a : vi.testers_data) {
					testers.add(a.public_key_hash);
				}
			}
			if(!areRequiredTestersPresent(testers, available)){
				if(DEBUG)System.out.println("validateVersionInfo(): !areRequiredTestersPresent() ");
				continue;
			}
			if(!isTestersWeightOrNumberSatisfactory(testers, available)){
				if(DEBUG) System.out.println("validateVersionInfo(): !isTestersWeightOrNumberSatisfactory ");
				 continue;
			}
			result.put(v, available);
		}
		return result;
	}
	private static boolean isTestersWeightOrNumberSatisfactory(HashSet<String> testers, Hashtable<String, VersionInfo> available) {
		ArrayList<ArrayList<Object>> a;
		try {
			a = Application.db.select(
					"SELECT "+table.tester.public_key_hash+","+table.tester.trust_weight+","+table.tester.expected_test_thresholds+
					" FROM "+table.tester.TNAME+
					" ;", new String[]{}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return true;
		}
		String expectations = Util.getString(a.get(0).get(2));
		TestExpectation[] expect = TestExpectation.getTestExpectations(expectations);
		int cnt=0, total_count=0;
		float weight = 0, total_weight = 0;
		for(ArrayList<Object> t : a){
			String h = Util.getString(t.get(0));
			float f = Util.fval(t.get(1), 0.0f);
			if(DEBUG)System.out.println("isTestersWeightOrNumberSatisfactory(): f= "+ f + " h= " +h);
			if(testers.contains(h)){
				if(expectationsMet(h, available, expect)) {
					cnt++;
					weight += f;
				}
			}
			total_count ++;
			total_weight += f;
		}
		if(DEBUG)System.out.println("isTestersWeightOrNumberSatisfactory(): cnt= "+ cnt + " weight= " +weight);
		boolean absolute = isTesterThresholdAbsolute();
		if(DEBUG)System.out.println("isTestersWeightOrNumberSatisfactory(): absolute= "+ absolute);
		if(absolute) {
			if(threshold_testers_based_on_count()){
				return cnt >= threshold_testers_count();
			}else
				return (weight >= threshold_testers_weight());
		}else{
			if(threshold_testers_based_on_count()){
				return (total_count>0)&&(cnt/total_count > threshold_testers_count());
			}else
				return (total_weight>0)&&(weight/total_weight >= threshold_testers_weight());			
		}
	}
	private static boolean isTesterThresholdAbsolute() {
		try {
			return !DD.getAppBoolean(DD.UPDATES_TESTERS_THRESHOLDS_RELATIVE);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return true;
		}
	}
	private static float threshold_testers_weight() {
		try {
			String testers_count_weight = DD.getAppText(DD.UPDATES_TESTERS_THRESHOLD_WEIGHT_VALUE);
			if (testers_count_weight == null){
				return DD.UPDATES_TESTERS_THRESHOLD_WEIGHT_DEFAULT;
				// testers_count_weight = "" + DD.UPDATES_TESTERS_THRESHOLD_WEIGHT_DEFAULT; 							
			}
			return Float.parseFloat(testers_count_weight);
		} catch (Exception e) {
			e.printStackTrace();
			return DD.UPDATES_TESTERS_THRESHOLD_WEIGHT_DEFAULT;
		}
	}
	private static float threshold_testers_count() {
		try {
			String testers_count_value = DD.getAppText(DD.UPDATES_TESTERS_THRESHOLD_COUNT_VALUE);
			if (testers_count_value == null) testers_count_value = "" + DD.UPDATES_TESTERS_THRESHOLD_COUNT_DEFAULT; 
			return Float.parseFloat(testers_count_value);
		} catch (Exception e) {
			e.printStackTrace();
			return 0.0f;
		}
	}
	private static boolean threshold_testers_based_on_count() {
		try {
			return !DD.getAppBoolean(DD.UPDATES_TESTERS_THRESHOLD_WEIGHT);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return true;
		}
	}
	private static boolean areRequiredTestersPresent(HashSet<String> testers, Hashtable<String, VersionInfo> available) {
		ArrayList<ArrayList<Object>> a;
		try {
			a = Application.db.select(
					"SELECT "+table.tester.public_key_hash+","+table.tester.expected_test_thresholds+
					" FROM "+table.tester.TNAME+
					" WHERE "+table.tester.reference_tester+"='1';", new String[]{}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return true;
		}
		for(ArrayList<Object> t : a){
			String h = Util.getString(t.get(0));
			String expectations = Util.getString(t.get(1));
			TestExpectation[] expect = TestExpectation.getTestExpectations(expectations);
			if(!testers.contains(h)) return false;
			if(!expectationsMet(h, available, expect)) return false;
		}
		return true;
	}
	/**
	 * Check if Qot and RoT meet expectations for tester h
	 * @param h
	 * @param available
	 * @param expect 
	 * @return
	 */
	private static boolean expectationsMet(String h,
			Hashtable<String, VersionInfo> available, TestExpectation[] expect) {
		if((expect == null) || (expect.length==0)) return true;
		for(String _url: available.keySet()) {
			VersionInfo vi = available.get(_url);
			int testers = vi.testers_data.length;
			for(int i=0; i<testers; i++) {
				if(!h.equals(vi.testers_data[i].public_key_hash)) continue;
				for(int k=0; k<expect.length; k++) {
					int _test = getTestPosition(vi, expect[k].test_ID);
					if((_test<0) || (_test>=vi.testers_data[i].tester_QoT.length)){
						Application_GUI.warning(__("Updates test not found in new releases: ")+k+":"+expect[k].test_ID, __("Your Updates Tests Specifications"));
						return false;
					}
					if ((vi.testers_data[i].tester_QoT[_test] < expect[k].mQoT) ||
							(vi.testers_data[i].tester_RoT[_test] < expect[k].mRoT)) break;
					
					if(k==expect.length-1) return true;
				}
			}
		}
		return false;
	}
	/**
	 * Get integer Test_ID based on string_name in release_QD. If integer provided, then just return that integer
	 * @param vi
	 * @param test_ID
	 * @return
	 */
	private static int getTestPosition(VersionInfo vi, String test_ID) {
		if((vi==null) || (vi.releaseQD==null)) return Integer.parseInt(test_ID);
		for(int k=0; k< vi.releaseQD.length; k++) {
			if(!test_ID.equals(vi.releaseQD[0].getQualityName())) continue;
			return k;
		}
		return Integer.parseInt(test_ID);
	}
	/**
	 * 
	 * @param versions
	 */
	public static void store_QoTs_and_RoTs(Hashtable<VersionInfo, Hashtable<String, VersionInfo>> versions) {
		if(DEBUG)System.out.println("start store_QoTs_and_RoTs()");
		for(VersionInfo v : versions.keySet()) {
			Hashtable<String, VersionInfo> available = versions.get(v);
			for(String url:available.keySet()) {
				try{	
					VersionInfo vi = available.get(url);
					D_MirrorInfo ui = getUpdateInfo(url); // url here used as primary key to find a mirror
					ui.last_version = vi.version;
					ui.last_version_branch = vi.branch;
					ui.last_contact_date = new GregorianCalendar();
					ui.releaseQoT = vi.releaseQD;
					ui.testerInfo = vi.testers_data;
					ui.last_version_info = vi.encode(); // I need to change it to ASN1 encoding
					ui.store(D_MirrorInfo.action_update);
					if(DEBUG)System.out.println("store_QoTs_and_RoTs()ui.last_version= "+ui.last_version);
				}catch (P2PDDSQLException e) {
					e.printStackTrace();
					return;		
				}
			}
			Application.db.sync(new ArrayList<String>(Arrays.asList(table.mirror.TNAME,table.tester.TNAME)));		
		}
		if(DEBUG)System.out.println("end store_QoTs_and_RoTs()");
	}
	public static byte getASNType() {
		return DD.TAG_AC10;
	}
	public static ArrayList<D_MirrorInfo> retrieveMirrorDefinitions() {
		ArrayList<D_MirrorInfo> result = new ArrayList<D_MirrorInfo>();
		String sql = "SELECT "+table.mirror.mirror_ID+
				" FROM  " + table.mirror.TNAME+";";
		ArrayList<ArrayList<Object>> list=null;
		try{
			list = Application.db.select(sql, new String[]{}, DEBUG);
		}catch(util.P2PDDSQLException e){
			System.out.println(e);
		}
		if(list == null ){
			return null;
		}
		for(ArrayList<Object> id : list){
			long _id = Util.lval(id.get(0));
			if(DEBUG)System.out.println("D_MirrorInfo:<init>:Found: "+_id);
			D_MirrorInfo ui = new D_MirrorInfo(_id);
			result.add(ui);
		}
		return result;
	}
	
}
class TestExpectation extends ASN1.ASNObj {
	String test_ID;
	float mQoT;
	float mRoT;
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(test_ID));
		enc.addToSequence(new Encoder(mQoT));
		enc.addToSequence(new Encoder(mRoT));
		return enc;
	}
	public static byte getASN1Type() {
		return DD.TAG_AC28;
	}
	@Override
	public TestExpectation decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		test_ID = d.getFirstObject(true).getString();
		try{
		mQoT = Float.parseFloat(d.getFirstObject(true).getString());
		mRoT = Float.parseFloat(d.getFirstObject(true).getString());
		}catch(Exception e){
			throw new ASN1DecoderFail(e.getLocalizedMessage());
		}
		return this;
	}
	public ASNObj instance() throws CloneNotSupportedException{return new TestExpectation();}
	public static TestExpectation[] getTestExpectations(String expectations){
		TestExpectation[] expect = null;
		if(expectations!=null)
			try {
				expect = new Decoder(util.Base64Coder.decode(expectations)).getSequenceOf(TestExpectation.getASN1Type(), new TestExpectation[0], new TestExpectation());
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
			}
		return expect;
	}
}

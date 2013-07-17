package data;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;

import java.util.ArrayList;
import java.util.Calendar;

import util.P2PDDSQLException;

import config.Application;

import util.Util;


public class D_TesterDefinition extends ASNObj{
	public static boolean DEBUG = false;
	public long tester_ID = -1;
	public String name;
	public String public_key;
	public String email;
	public String url; // just for ads
	public String description;
		
	public D_TesterDefinition( String pk) {
		if(pk==null) return;
		String sql = "SELECT "+table.tester.fields_tester+" FROM  " +table.tester.TNAME +
				" WHERE "+ table.tester.public_key+"=?;";
		ArrayList<ArrayList<Object>> result=null;
		try{
			result = Application.db.select(sql,new String[]{pk},DEBUG);
		}catch(util.P2PDDSQLException e){
			System.out.println(e);
		}
		if(result.size()>0) init(result.get(0));
	}
	public D_TesterDefinition() {
		
	}
	public D_TesterDefinition(ArrayList<Object> _u) {
		init(_u);
	}
	public void init(ArrayList<Object> _u){
		if(DEBUG)System.out.println("D_TesterDefinition: <init>: start");
		tester_ID = Util.lval(_u.get(table.tester.F_ID),-1);
		name = Util.getString(_u.get(table.tester.F_NAME));
		public_key = Util.getString(_u.get(table.tester.F_PUBLIC_KEY));
		email = Util.getString(_u.get(table.tester.F_EMAIL));
		url = Util.getString(_u.get(table.tester.F_URL));
		description = Util.getString(_u.get(table.tester.F_DESCRIPTION));
	
		if(DEBUG)System.out.println("D_TesterDefinition: <init>: done");
	
	}
	
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(name));
		enc.addToSequence(new Encoder(email));
		enc.addToSequence(new Encoder(url));
		enc.addToSequence(new Encoder(description));
		enc.addToSequence(new Encoder(public_key, false));
		enc.setASN1Type(getASNType());
		return enc;
	}
	@Override
	public D_TesterDefinition decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		name = d.getFirstObject(true).getString();
		email = d.getFirstObject(true).getString();
		url = d.getFirstObject(true).getString();
		description = d.getFirstObject(true).getString();
		public_key = d.getFirstObject(true).getString();
		return this;
	}
	public ASNObj instance() throws CloneNotSupportedException{return new D_TesterInfo();}
	public static byte getASNType() {
		if(DEBUG) System.out.println("DD.TAG_AC23= "+ DD.TAG_AC23);
		return DD.TAG_AC23;
	}
	public void store() throws P2PDDSQLException {
		boolean update;
		
		D_TesterDefinition t = new D_TesterDefinition(this.public_key);
		if (t.tester_ID>=0) update=true;
		else update=false;
		
		String params[] = new String[table.tester.F_FIELDS];

		if(update) {
			params[table.tester.F_NAME] = this.name;
			params[table.tester.F_PUBLIC_KEY] = this.public_key;
			params[table.tester.F_EMAIL] = this.email;
			params[table.tester.F_URL] = this.url;
			params[table.tester.F_DESCRIPTION] = this.description;
			params[table.tester.F_ID] = Util.getStringID(this.tester_ID);

			if(t.description != null) params[table.tester.F_DESCRIPTION] = t.description;
			if(t.email != null) params[table.tester.F_EMAIL] = t.email;
			if(t.url != null) params[table.tester.F_URL] = t.url;
			//if(t.name != null) params[table.tester.F_NAME] = t.name;
			
			Application.db.update(table.tester.TNAME, table.tester._fields_tester_no_ID,
					new String[]{table.tester.tester_ID},
					params,DEBUG);
		}else{
			params[table.tester.F_NAME] = this.name;
			params[table.tester.F_PUBLIC_KEY] = this.public_key;
			params[table.tester.F_EMAIL] = this.email;
			params[table.tester.F_URL] = this.url;
			params[table.tester.F_DESCRIPTION] = this.description;
			params[table.tester.F_ID] = Util.getStringID(this.tester_ID);
			
			String params2[]=new String[table.tester.F_FIELDS_NOID];
			System.arraycopy(params,0,params2,0,params2.length);
			System.out.println("params2[last]: "+ params2[table.tester.F_FIELDS_NOID-1]);
			this.tester_ID = Application.db.insert(table.tester.TNAME, table.tester._fields_tester_no_ID,params2, DEBUG);
		}
	}
//	public static D_TesterDefinition retriveTesterInfo(long testerID){
//		//Application.db.   select ...
//	}
	public static D_TesterDefinition retriveTesterInfo(String name, String pubKey){
		String[] params = new String[]{name, pubKey};
		String sql = "SELECT "+table.tester.fields_tester+" FROM  " + table.tester.TNAME +
			         " WHERE "+table.tester.name + " = ? AND "+table.tester.public_key+"= ?";
		ArrayList<ArrayList<Object>> result=null;
		try{
			result = Application.db.select(sql, params, DEBUG);
		}catch(util.P2PDDSQLException e){
			System.out.println(e);
		}
		if(result == null ) return null;
		return new D_TesterDefinition(result.get(0));
	}
}
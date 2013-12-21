/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 KhalidAlhamed
		Author: Khalid Alhamed
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

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;

import java.util.ArrayList;
import java.util.Calendar;

import util.P2PDDSQLException;
import util.Summary;

import config.Application;

import util.Util;


public class D_TesterDefinition extends ASNObj implements Summary{
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public long tester_ID = -1;
	public String name;
	public String public_key;
	public String email;
	public String url; // just for ads
	public String description;

	@Override
	public String toString() {
		return "Tester: name="+name+"\n"+
			"email="+email+"\n"+
			"url="+url+"\n"+
			"description="+description+"\n"+
			"PK="+public_key;
	}

	@Override
	public String toSummaryString() {
		return "Tester: name="+name+"\n"+
			"email="+email+"\n"+
			"url="+url+"\n"+
			"description="+description;
	}

	public D_TesterDefinition( String pk) {
		if(pk==null) return;
		String sql = "SELECT "+table.tester.fields_tester+
				" FROM  " +table.tester.TNAME +
				" WHERE "+ table.tester.public_key+"=?;";
		ArrayList<ArrayList<Object>> result=null;
		try{
			result = Application.db.select(sql,new String[]{pk},DEBUG);
		}catch(util.P2PDDSQLException e){
			System.out.println(e);
		}
		if(result.size()>0) init(result.get(0));
	}
	public D_TesterDefinition(long id) {
		if(id < 0) return;
		String sql = "SELECT "+table.tester.fields_tester+
				" FROM  " +table.tester.TNAME +
				" WHERE "+ table.tester.tester_ID+"=?;";
		ArrayList<ArrayList<Object>> result=null;
		try{
			result = Application.db.select(sql,new String[]{Util.getStringID(id)},DEBUG);
		}catch(util.P2PDDSQLException e){
			System.out.println(e);
		}
		if(result.size()>0){
			init(result.get(0));
			if(DEBUG)System.out.println("D_TesterDefinition:<init>:Got: "+this);
		}else{
			if(DEBUG)System.out.println("D_TesterDefinition:<init>:Not found: "+id);
		}
	}
	public D_TesterDefinition() {
		
	}
	public D_TesterDefinition(ArrayList<Object> _u) {
		init(_u);
	}
	public void init(ArrayList<Object> _u){
		if(DEBUG)System.out.println("D_TesterDefinition: <init>: start");
		tester_ID = Util.lval(_u.get(table.tester.F_ID),-1);
		name = Util.getString(_u.get(table.tester.F_ORIGINAL_TESTER_NAME));
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
	@Override
	public D_TesterDefinition instance() throws CloneNotSupportedException{return new D_TesterDefinition();}
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
			params[table.tester.F_ORIGINAL_TESTER_NAME] = this.name;
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
			params[table.tester.F_ORIGINAL_TESTER_NAME] = this.name;
			params[table.tester.F_PUBLIC_KEY] = this.public_key;
			params[table.tester.F_EMAIL] = this.email;
			params[table.tester.F_URL] = this.url;
			params[table.tester.F_DESCRIPTION] = this.description;
			params[table.tester.F_ID] = Util.getStringID(this.tester_ID);
			
			String params2[]=new String[table.tester.F_FIELDS_NOID];
			System.arraycopy(params,0,params2,0,params2.length);
			if(DEBUG)System.out.println("params2[last]: "+ params2[table.tester.F_FIELDS_NOID-1]);
			this.tester_ID = Application.db.insert(table.tester.TNAME, table.tester._fields_tester_no_ID,params2, DEBUG);
		}
	}
	public static ArrayList<D_TesterDefinition> retrieveTesterDefinitions(){
		ArrayList<D_TesterDefinition> result = new ArrayList<D_TesterDefinition>();
		String sql = "SELECT "+table.tester.tester_ID+
				" FROM  " + table.tester.TNAME+";";
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
			if(DEBUG)System.out.println("D_TesterDefinition:<init>:Found: "+_id);
			D_TesterDefinition td = new D_TesterDefinition(_id);
			result.add(td);
		}
		return result;
	}
	public static D_TesterDefinition retrieveTesterDefinition(String name, String pubKey){
		String[] params = new String[]{name, pubKey};
		String sql = "SELECT "+table.tester.fields_tester+
				" FROM  " + table.tester.TNAME +
				" WHERE "+table.tester.F_ORIGINAL_TESTER_NAME + " = ? AND "+table.tester.public_key+"= ?";
		ArrayList<ArrayList<Object>> result=null;
		try{
			result = Application.db.select(sql, params, DEBUG);
		}catch(util.P2PDDSQLException e){
			System.out.println(e);
		}
		if(result == null ){
			return null;
		}
		return new D_TesterDefinition(result.get(0));
	}
	
	
	public static D_TesterDefinition retrieveTesterDefinition(String pubKey){
		String[] params = new String[]{pubKey};
		String sql = "SELECT "+table.tester.fields_tester+
				" FROM  " + table.tester.TNAME +
				" WHERE "+table.tester.public_key + " = ?";
		ArrayList<ArrayList<Object>> result=null;
		try{
			result = Application.db.select(sql, params, DEBUG);
		}catch(util.P2PDDSQLException e){
			System.out.println(e);
		}
		if(result == null ){
			return null;
		}
		return new D_TesterDefinition(result.get(0));
	}
}
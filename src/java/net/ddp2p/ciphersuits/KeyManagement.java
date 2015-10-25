/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
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

package net.ddp2p.ciphersuits;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.util.DD_SK;
import net.ddp2p.common.util.DD_SK_Entry;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
/**
 * This class contains methods to save keys in the database and to read them from database.
 * It is in this package since it accesses protected classes of this package (RSA_PK, etc).
 * 
 * Should be moved to util when finding a way.
 * @author msilaghi
 *
 */
public class KeyManagement {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final boolean EXIT_ON_NONMATCHING_PK = false;
/**
 *  Returns false if the key is absent!
 * @param gid
 * @param sk_file
 * @return
 * @throws P2PDDSQLException
 * @throws IOException
 */
	public static boolean saveSecretKey(String gid, String sk_file) throws P2PDDSQLException, IOException {
		String sql =
			"SELECT "+net.ddp2p.common.table.key.secret_key+","+net.ddp2p.common.table.key.name+","+net.ddp2p.common.table.key.type+","+net.ddp2p.common.table.key.creation_date+
			" FROM "+net.ddp2p.common.table.key.TNAME+
			" WHERE "+net.ddp2p.common.table.key.public_key+"=?;";
		ArrayList<ArrayList<Object>> a = Application.getDB().select(sql, new String[]{gid});
		if (a.size() == 0) return false;
		String sk = Util.getString(a.get(0).get(0));
		String name = Util.getString(a.get(0).get(1));
		String type = Util.getString(a.get(0).get(2));
		String date = Util.getString(a.get(0).get(3));
		BufferedWriter bw = new BufferedWriter(new FileWriter(sk_file));
		bw.write(sk);
		bw.newLine();
		bw.write(gid);
		bw.newLine();
		bw.write(type);
		bw.newLine();
		if (name != null) bw.write(name);
		else bw.write("Anonymous");
		bw.newLine();
		if (date != null) bw.write(date);
		else bw.write("");
		bw.newLine();
		bw.close();
		return true;
	}
	/**
	 * Returns false in the absence of the key
	 * @param dsk
	 * @param gid
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static boolean fill_sk(DD_SK dsk, String gid) throws P2PDDSQLException {
		String sql =
				"SELECT "+net.ddp2p.common.table.key.secret_key+","+net.ddp2p.common.table.key.name+","+net.ddp2p.common.table.key.type+","+net.ddp2p.common.table.key.creation_date+
				" FROM "+net.ddp2p.common.table.key.TNAME+
				" WHERE "+net.ddp2p.common.table.key.public_key+"=?;";
		ArrayList<ArrayList<Object>> a = Application.getDB().select(sql, new String[]{gid}, _DEBUG);
		if (a.size() == 0) {
			System.out.println("KeyManagement: fillsk: not finding key for: "+gid);
			return false;
		}
		String sk = Util.getString(a.get(0).get(0));
		String name = Util.getString(a.get(0).get(1));
		String type = Util.getString(a.get(0).get(2));
		String date = Util.getString(a.get(0).get(3));
		DD_SK_Entry dde = new DD_SK_Entry();
		dde.key = Cipher.getSK(sk);
		dde.name = name;
		dde.type = type;
		dde.creation = Util.getCalendar(date);
		
		dsk.sk.add(dde);
		System.out.println("KeyManagement: fillsk: Done: "+dsk);
		return true;
	}
	public static SK loadSecretKey(String sk_file, String[] __pk) throws IOException, P2PDDSQLException {
		
		return loadSecretKey(sk_file, __pk, null, null);
	}
	/**
	 * Loads and stores key in database
	 * @param sk_file
	 * @param __pk
	 * @param is_new 
	 * @return
	 * @throws IOException
	 * @throws P2PDDSQLException
	 */
	public static SK loadSecretKey(String sk_file, String[] __pk,
			String[] file_data, boolean[] is_new) throws IOException, P2PDDSQLException{
		if(DEBUG) System.out.println("KeyManagement:loadSecretKey: start "+sk_file);
		BufferedReader br = new BufferedReader(new FileReader(sk_file));
		String sk, pk, type=null, name=null, date=null;
		boolean eof = false;
		do{
			sk = br.readLine();
			if(sk==null){
				System.err.println("KeyManagement: Reach eof from start on."+sk_file);
				return null;
			}
			sk = sk.trim();
		}while(sk.length() == 0);
		if(DEBUG) System.out.println("KeyManagement:loadSecretKey: started");
		
		do{
			pk = br.readLine();
			if(pk==null){ eof = true; break;}
			pk = pk.trim();
		} while (pk.length() == 0);
		SK _sk = Cipher.getSK(sk);
		if (_sk==null) {
			System.err.println("KeyManagement: Secret key null from:."+sk);
			return null;
		}
		PK _pk = _sk.getPK();
		String i_pk = Util.stringSignatureFromByte(_pk.getEncoder().getBytes());
		if (pk != null) {
			PK f_pk = Cipher.getPK(pk);
			if (DEBUG) System.out.println ("KM: Cipher: "+f_pk);
			if (f_pk instanceof ECDSA_PK)
				if (DEBUG) System.out.println("KM: ECDSA_PK");
			if (f_pk instanceof RSA_PK)
				if (DEBUG) System.out.println("KM: RSA_PK");
			if (!_pk.__equals(f_pk)) {
				System.err.println("KeyManagement: Public key does not match: loaded="+f_pk+"\n computed from sk = "+_pk);
				if (EXIT_ON_NONMATCHING_PK){
					if (DEBUG) System.out.println("KeyManagement:loadSecretKey: quit nonmatching "+sk_file);
					if (__pk != null) __pk[0] = pk;
					return null;
				}
			}else{
				if (DEBUG) System.out.println("KM: equal");
				pk = i_pk;
			}
		}

		if (!eof)
			do {
				type = br.readLine();
				if (type==null) { eof= true; break;}
				type = type.trim();
			}while(type.length() == 0);

		if (!eof)
			do {
				name = br.readLine();
				if(name==null){ eof=true; break;}
				name = name.trim();
			} while(name.length() == 0);
		
		if (!eof)
			do {
				date = br.readLine();
				if(date==null){ eof=true; break;}
				date = date.trim();
			} while(date.length() == 0);
		
		if(DEBUG) System.out.println("KeyManagement:loadSecretKey: check existance");
		ArrayList<ArrayList<Object>> p = Application.getDB().select(
				"SELECT "+net.ddp2p.common.table.key.public_key+" FROM "+net.ddp2p.common.table.key.TNAME+" WHERE "+net.ddp2p.common.table.key.public_key+"=?;",
				new String[]{pk}, DEBUG);
		if (is_new != null)
			is_new[0] = (p.size() == 0);
		if (p.size() > 0) {
			if (DEBUG) System.out.println("KeyManagement:loadSecretKey: quit known "+sk_file);
			if (__pk!=null) __pk[0] = pk;
			return _sk;
		}
		if (DEBUG) System.out.println("KeyManagement:loadSecretKey: read");
		Cipher cipher = Cipher.getCipher(_sk,_pk);
		if (cipher == null) {
			System.out.println("KeyManagement: loadSecretKeys null cipher for\n _sk="+_sk+" \n pk="+_pk);
		}
		String hash = Util.getGIDhash(pk); //Util.getKeyedIDPKhash(cipher);
		if ((date==null) || (date.length()==0)) date = Util.getGeneralizedTime();
//		String sql_selkey = "SELECT "+table.key.key_ID+" FROM "+table.key.TNAME+
//				" WHERE "+table.key.public_key+"=?;";
//		if (_DEBUG) System.out.println("KeyManagement:loadSecretKey: select");
//		ArrayList<ArrayList<Object>> ok = Application.db.select(sql_selkey, new String[]{pk}, _DEBUG);
//		if (_DEBUG) System.out.println("KeyManagement:loadSecretKey: select #"+ok.size());
//		if (ok.size() == 0)
			Application.getDB().insert (net.ddp2p.common.table.key.TNAME,
					new String[]{net.ddp2p.common.table.key.secret_key,net.ddp2p.common.table.key.public_key,
					net.ddp2p.common.table.key.type,net.ddp2p.common.table.key.name, net.ddp2p.common.table.key.ID_hash,net.ddp2p.common.table.key.creation_date},
					new String[]{sk,pk,type,name,hash,date},
					DEBUG);
		
		if (__pk != null) __pk[0] = pk;
		if (file_data != null) {
			//if (file_data[0] == null) file_data[0] = new PeerInput();
			//file_data[0].name = name;
			file_data[0] = name;
		}
		
		return _sk;
	}
}

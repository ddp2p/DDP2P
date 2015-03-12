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

package ciphersuits;

import static util.Util._;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import util.DBInterface;
import util.P2PDDSQLException;
import util.Util;
import config.Application;
import config.Application_GUI;
import config.DD;
import data.D_Peer;
import data.HandlingMyself_Peer;

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
			"SELECT "+table.key.secret_key+","+table.key.name+","+table.key.type+","+table.key.creation_date+
			" FROM "+table.key.TNAME+
			" WHERE "+table.key.public_key+"=?;";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{gid});
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
		ArrayList<ArrayList<Object>> p = Application.db.select(
				"SELECT "+table.key.public_key+" FROM "+table.key.TNAME+" WHERE "+table.key.public_key+"=?;",
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
			Application.db.insert (table.key.TNAME,
					new String[]{table.key.secret_key,table.key.public_key,
					table.key.type,table.key.name, table.key.ID_hash,table.key.creation_date},
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
	public static void main(String[]args) {
		if(args.length<2){
			System.out.println("Usage: prog db sk setMyself");
			return;
		}
		//DEBUG = true;
		//D_PeerAddress.DEBUG = true;
		String db = args[0];
		String sk = args[1];
		try{
			String err;
			if((err=DD.testProperDB(db))!=null) {
				System.out.println("Usage: prog db sk setMyself.\n\n Improper db="+db+"\nerror="+err);
				return;				
			}
			Application.db = new DBInterface(db);
			File fileLoadSK = new File(sk);
			if(!fileLoadSK.exists()) {
				Application_GUI.warning(_("Inexisting file: "+fileLoadSK.getPath()), _("Inexisting file!"));
				return;
			}
			String [] __pk = new String[1];
			SK new_sk = KeyManagement.loadSecretKey(fileLoadSK.getCanonicalPath(), __pk);
			if(new_sk==null){
				System.out.println("Usage: prog db sk setMyself\n empty sk");
				return;
			}
			//String old_gid = model.getGID(row);
			String _pk=__pk[0];//Util.stringSignatureFromByte(new_sk.getPK().getEncoder().getBytes());
			if(_pk==null){
				System.out.println("null PK");
				return;
			}
			D_Peer peer = D_Peer.getPeerByGID(_pk, true, true);
			peer.component_basic_data.globalID = _pk;
			peer.component_basic_data.globalIDhash=null;
			peer._peer_ID = -1;
			peer.peer_ID = null;

			if ((args.length > 2) && (Util.stringInt2bool(args[2], false))) {
				HandlingMyself_Peer.setMyself(peer, true);
				HandlingMyself_Peer.updateAddress(peer);
			}
			peer.sign(new_sk);
			//String peerID = 
			peer.storeSynchronouslyNoException();

		}catch(Exception e2){e2.printStackTrace();}
	}
}


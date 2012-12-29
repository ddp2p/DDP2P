package ciphersuits;

import static util.Util._;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import util.DBInterface;
import util.Util;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;
import data.D_PeerAddress;

public class KeyManagement {
	private static boolean DEBUG = false;
	private static final boolean EXIT_ON_NONMATCHING_PK = false;
/**
 *  Returns false if the key is absent!
 * @param gid
 * @param sk_file
 * @return
 * @throws SQLiteException
 * @throws IOException
 */
	public static boolean saveSecretKey(String gid, String sk_file) throws SQLiteException, IOException {
		String sql =
			"SELECT "+table.key.secret_key+","+table.key.name+","+table.key.type+
			" FROM "+table.key.TNAME+
			" WHERE "+table.key.public_key+"=?;";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{gid});
		if(a.size() == 0) return false;
		String sk = Util.getString(a.get(0).get(0));
		String name = Util.getString(a.get(0).get(1));
		String type = Util.getString(a.get(0).get(2));
		BufferedWriter bw = new BufferedWriter(new FileWriter(sk_file));
		bw.write(sk);
		bw.newLine();
		bw.write(gid);
		bw.newLine();
		bw.write(type);
		bw.newLine();
		bw.write(name);
		bw.newLine();
		bw.close();
		return true;
	}
	
	public static SK loadSecretKey(String sk_file, String[] __pk) throws IOException, SQLiteException{
		if(DEBUG) System.out.println("KeyManagement:loadSecretKey: start "+sk_file);
		BufferedReader br = new BufferedReader(new FileReader(sk_file));
		String sk, pk, type=null, name=null;
		boolean eof = false;
		do{
			sk = br.readLine();
			if(sk==null){
				System.err.println("KeyManagement: Reach eof from start on."+sk_file);
				return null;
			}
			sk = sk.trim();
		}while(sk.length() == 0);
		
		do{
			pk = br.readLine();
			if(pk==null){ eof = true; break;}
			pk = pk.trim();
		}while(pk.length() == 0);
		SK _sk = Cipher.getSK(sk);
		if(_sk==null){
			System.err.println("KeyManagement: Secret key null from:."+sk);
		}
		PK _pk = _sk.getPK();
		String i_pk = Util.stringSignatureFromByte(_pk.getEncoder().getBytes());
		if(pk != null) {
			PK f_pk = Cipher.getPK(pk);
			if(!((RSA_PK)_pk).equals(f_pk)){
				System.err.println("KeyManagement: Public key does not match: loaded="+f_pk+"\n computed from sk = "+_pk);
				if(EXIT_ON_NONMATCHING_PK){
					if(DEBUG) System.out.println("KeyManagement:loadSecretKey: quit nonmatching "+sk_file);
					if(__pk!=null) __pk[0] = pk;
					return null;
				}
			}else{
				pk = i_pk;
			}
		}

		if(!eof)
			do{
				type = br.readLine();
				if(type==null){ eof= true; break;}
				type = type.trim();
			}while(type.length() == 0);

		if(!eof)
			do{
				name = br.readLine();
				if(name==null){ eof=true; break;}
				name = name.trim();
			}while(name.length() == 0);
		ArrayList<ArrayList<Object>> p = Application.db.select(
				"SELECT "+table.key.public_key+" FROM "+table.key.TNAME+" WHERE "+table.key.public_key+"=?;",
				new String[]{pk}, DEBUG);
		if(p.size()>0){
			if(DEBUG) System.out.println("KeyManagement:loadSecretKey: quit known "+sk_file);
			if(__pk!=null) __pk[0] = pk;
			return _sk;
		}
		String hash = Util.getKeyedIDPKhash(Cipher.getCipher(_sk,_pk));
		Application.db.insert(table.key.TNAME,
				new String[]{table.key.secret_key,table.key.public_key,table.key.type,table.key.name, table.key.ID_hash},
				new String[]{sk,pk,type,name,hash},
				DEBUG);
		if(__pk!=null) __pk[0] = pk;
		
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
				Application.warning(_("Inexisting file: "+fileLoadSK.getPath()), _("Inexisting file!"));
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
			D_PeerAddress peer = new D_PeerAddress(_pk);
			peer.globalID = _pk;
			peer.globalIDhash=null;
			peer._peer_ID = -1;
			peer.peer_ID = null;
			peer.sign(new_sk);
			String peerID = peer.storeVerified();

			if((args.length>2)&&(Util.stringInt2bool(args[2], false)))
				D_PeerAddress.setMyself(peerID);

		}catch(Exception e2){e2.printStackTrace();}
	}
}


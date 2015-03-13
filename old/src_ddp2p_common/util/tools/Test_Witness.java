package util.tools;

import hds.ASNSyncPayload;

import java.util.ArrayList;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import util.DBInterface;
import util.P2PDDSQLException;
import config.Application;
import data.D_Organization;
import data.D_Witness;

public class Test_Witness {
	public static void main(String args[]) {
		try{_main(args);}catch(Exception e){e.printStackTrace();}
	}
	public static void _main(String args[]) throws P2PDDSQLException {
		util.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		Application.db = new DBInterface(Application.DELIBERATION_FILE);
		System.out.println("D_Organization: main: prog pID, verify, sign, store");
		long pID = Long.parseLong(args[0]);
		int verif = Integer.parseInt(args[1]);
		int sign = Integer.parseInt(args[2]);
		int store = Integer.parseInt(args[3]);
		/*
		HandlingMyself_Peer.loadIdentity(null);
		D_Organization me = HandlingMyself_Peer.getPeer(Identity.current_peer_ID);
		System.out.println("D_Organization: main: me= "+me);
		HandlingMyself_Peer.setMyself(me, false);
		*/
		
		//D_Organization d = D_Organization.getPeerByLID(1, true);
		//System.out.println("D_Organization: main:read(1) "+d);
		D_Witness d2 = new D_Witness(pID);//.getOrgByLID_NoKeep(pID, true);
		
		ArrayList<String> dictionary = new ArrayList<String>();
		System.out.println("\nD_Witness: main: d["+pID+"]="+d2);
		ASNSyncPayload.prepareWitnDictionary(d2, dictionary);
		byte msg[] = d2.getEncoder(dictionary).getBytes();
		
		//System.out.println("D_Witness: main: cache="+dumpDirCache());
		System.out.println("\nD_Witness: main: d["+pID+"]="+d2);
		if (d2 == null) {
			System.out.println("D_Witness: no d2=: " + d2);
			return;
		}
		try {
			D_Witness d3 = new D_Witness().decode(new Decoder(msg));
			System.out.println("\nD_Witness: main: d3["+pID+"]="+d3);
			ASNSyncPayload.expandWitnDictionariesAtDecoding(d3, dictionary);
			System.out.println("\nD_Witness: main: d3+["+pID+"]="+d3);
			if (verif > 0) {
				boolean r = d3.verifySignature();
				System.out.println("\nD_Witness: main 3+: verif result="+r);			
			}
		} catch (ASN1DecoderFail e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//System.out.println("\nD_Organization: main 1: ********\n");
		//d2.verifySignature();
		//System.out.println("\nD_Organization: main 2: ********\n");
		//d2.sign();
		System.out.println("\nD_Witness: main 3: ********\n");
		if (verif > 0) {
			boolean r = d2.verifySignature();
			System.out.println("\nD_Witness: main 3: verif result="+r);			
		}
		if (sign > 0) {
			d2.sign(d2.witnessing_global_constituentID);
			boolean r2 = d2.verifySignature();
			System.out.println("\nD_Witness: main 4: verif result=" + r2);			
		}
		if (store > 0) d2.storeVerified();//.storeSynchronouslyNoException();
		
	}	
	
}
package util.tools;
import java.util.ArrayList;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.hds.ASNSyncPayload;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
public class Test_Witness {
	public static void main(String args[]) {
		try{_main(args);}catch(Exception e){e.printStackTrace();}
	}
	public static void _main(String args[]) throws P2PDDSQLException {
		net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		Application.setDB(new DBInterface(Application.DELIBERATION_FILE));
		System.out.println("D_Organization: main: prog pID, verify, sign, store");
		long pID = Long.parseLong(args[0]);
		int verif = Integer.parseInt(args[1]);
		int sign = Integer.parseInt(args[2]);
		int store = Integer.parseInt(args[3]);
		D_Witness d2 = new D_Witness(pID);
		ArrayList<String> dictionary = new ArrayList<String>();
		System.out.println("\nD_Witness: main: d["+pID+"]="+d2);
		ASNSyncPayload.prepareWitnDictionary(d2, dictionary);
		byte msg[] = d2.getEncoder(dictionary).getBytes();
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
			e.printStackTrace();
		}
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
		if (store > 0) d2.storeVerified();
	}	
}

package config;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;

import com.almworks.sqlite4java.SQLiteException;

import streaming.RequestData;
import util.DBInterface;

public class Interests {
	public static void main(String[] args){
		try {
			String cmd = args[0];
			if("DISPLAY".equals(cmd)) display(args);
			if("FREE".equals(cmd)) free(args);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private static void free(String[] args) throws Exception {
		Application.db = new DBInterface(args[1]);
		
		DD.setAppText(DD.WLAN_INTERESTS, null);
		
	}
	private static void display(String[] args) throws Exception {
			Application.db = new DBInterface(args[1]);
			
			RequestData rq = getCurrentInterests();
			
			System.out.println("Interests: have = "+rq);
	}
	public static RequestData getCurrentInterests() throws ASN1DecoderFail, SQLiteException{
			String interests_text = DD.getAppText(DD.WLAN_INTERESTS);
			if(interests_text==null){
				System.err.println("Interests:No WLAN interests!");
				return null;
			}
			byte[] interests_asn1 = util.Util.byteSignatureFromString(interests_text);
			RequestData rq = new RequestData();
			rq.decode(new Decoder(interests_asn1));
			return rq;
	}
}

package config;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;

import util.P2PDDSQLException;

import streaming.RequestData;
import util.DBInterface;

public class Interests {
	public static void main(String[] args){
		try {
			String cmd = args[0];
			if("DISPLAY".equals(cmd)){
				Application.db = new DBInterface(args[1]);
				display(args);
			}
			if("FREE".equals(cmd)){
				Application.db = new DBInterface(args[1]);
				free(args);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private static void free(String[] args) throws Exception {
		DD.setAppText(DD.WLAN_INTERESTS, null);
		
	}
	private static void display(String[] args) throws Exception {			
			RequestData rq = getCurrentInterests();
			
			System.out.println("Interests: have = "+rq);
	}
	public static RequestData getCurrentInterests() throws ASN1DecoderFail, P2PDDSQLException{
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

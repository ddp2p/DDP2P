package util.tools;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

public
class Test_SyncRequests {
	
	private static final boolean _DEBUG = true;
	static String bMD5 = "qM0SI3UKDLl/K1HF4VCFpQ==";
	static String gMD5 = "FWCMytVV09Qgji/2AN1PRg==";
	
	static String wrong = "Z4IDWAwBMhgTMjAxNDEwMDcxMjI3NTkuNDc4WmAMwARwZWVywARuZXdzYoICERMBMxOCAIBNRjRNQlVWRFJGTkJZQUV3QWdFRWJVWUNRVFpJb3poVncvVlhvWmdOM2ZTM0FrdG84TnJUS0gxd1Fxd3I3UWpKZEprenNwRmkyQVBNNUgwLythNExBai83bUM3ckp6VERwR1BmUUR5Y1YwaFdQaFh6QVFFQkRBZFRTRUV0TXpnMAwHU2lsYWdoaWAFU3RhdHNhE21zaWxhZ2hpQG15LmZpdC5lZHVjBkRldmljZWQ6awsCAQEFAAUABQAFAGsrAgEBDAZEZXZpY2UYEzIwMTQwOTI3MjE0NjMwLjUzOVoMAUIMBjAuOS41NhgTMjAxNDA5MDgxNjA4MzQuNzY3WjB3MCJgAQITDTE2My4xMTguNzguNDQCAmIjAgJiI2EDRElSZAEAMCJgAQITDTE2My4xMTguNzguNDACAmIjAgJiI2EDRElSZAEBMC1gAQITDTE2My4xMTguNzguNDACAicQAgInEGEDRElSYgFCYwYwLjkuNTVkAQIBAQAEggCMMIIAiAJCAUKylad744Mp/+7+LYrpbXPlBibmln5AlexkB71Ti1eiWij6SJTtE0axByZneikRPSN9q49UjpegJkXRzLRNWFMDAkIB34iWHt8AHbUSqRaQoTso5QHWjxFpB58dMtekoKjUjalsz4zQ31bL7p241ScpCfRS6AIKvOsfqO5zDLJ7qXjxmmBjcDBuMGxvADAAbSowKBMkUjpTSEEtMTpnajhHdkpmNEdBZjR1VXNlRlVqRTR0OE8rVk09EwAwADAAMABtADAAMAATLkc6KzN3dFVVeVh6NEt5ZWJPcUVlRzBvbHlPUlViZ1BESUZmVStxNjE1YklyYz1kAjAAZiZrJBMOQ2hhdF9CMV92MS4wLjAMB0NoYXRBcHAMBGluZm8TA3VybGhODAExcQByR3hFEy5HOjVBcG5kQmFQV2NLRzVpanU5M2lCZ3pIZ0xkOG9WM0Njb2JYLzJCSmo2ZE09GBMyMDE0MDkxNjAyMzkzMS42ODRaBABnKwIBAQwGRGV2aWNlGBMyMDE0MDkyNzIxNDYzMC41MzlaDAFCDAYwLjkuNTY=";	
	static String good  = "Z4IDzAwBMhgTMjAxNDEwMDcxMjI3NTkuNDc4WmAMwARwZWVywARuZXdzYoICSxMBMxOCAIBNRjRNQlVWRFJGTkJZQUV3QWdFRWJVWUNRVFpJb3poVncvVlhvWmdOM2ZTM0FrdG84TnJUS0gxd1Fxd3I3UWpKZEprenNwRmkyQVBNNUgwLythNExBai83bUM3ckp6VERwR1BmUUR5Y1YwaFdQaFh6QVFFQkRBZFRTRUV0TXpnMAwHU2lsYWdoaWAFU3RhdHNhE21zaWxhZ2hpQG15LmZpdC5lZHVjBkRldmljZWR0awsCAQEFAAUABQAFAGtlAgEBDAZEZXZpY2UYEzIwMTQwOTI3MjE0NjMwLjUzOVoMAUIMBjAuOS41NkI4TUNackpCTU9RMmhoZEY5Q01WOTJNUzR3TGpBTUIwTm9ZWFJCY0hBTUJHbHVabThUQTNWeWJBPT0YEzIwMTQwOTA4MTYwODM0Ljc2N1owdzAiYAECEw0xNjMuMTE4Ljc4LjQ0AgJiIwICYiNhA0RJUmQBADAiYAECEw0xNjMuMTE4Ljc4LjQwAgJiIwICYiNhA0RJUmQBATAtYAECEw0xNjMuMTE4Ljc4LjQwAgInEAICJxBhA0RJUmIBQmMGMC45LjU1ZAECAQEABIIAjDCCAIgCQgFCspWne+ODKf/u/i2K6W1z5QYm5pZ+QJXsZAe9U4tXoloo+kiU7RNGsQcmZ3opET0jfauPVI6XoCZF0cy0TVhTAwJCAd+Ilh7fAB21EqkWkKE7KOUB1o8RaQefHTLXpKCo1I2pbM+M0N9Wy+6duNUnKQn0UugCCrzrH6jucwyye6l48ZpgY3AwbjBsbwAwAG0qMCgTJFI6U0hBLTE6Z2o4R3ZKZjRHQWY0dVVzZUZVakU0dDhPK1ZNPRMAMAAwADAAbQAwADAAEy5HOiszd3RVVXlYejRLeWViT3FFZUcwb2x5T1JVYmdQRElGZlUrcTYxNWJJcmM9ZAIwAGYmayQTDkNoYXRfQjFfdjEuMC4wDAdDaGF0QXBwDARpbmZvEwN1cmxoTgwBMXEAckd4RRMuRzo1QXBuZEJhUFdjS0c1aWp1OTNpQmd6SGdMZDhvVjNDY29iWC8yQkpqNmRNPRgTMjAxNDA5MTYwMjM5MzEuNjg0WgQAZ2UCAQEMBkRldmljZRgTMjAxNDA5MjcyMTQ2MzAuNTM5WgwBQgwGMC45LjU2QjhNQ1pySkJNT1EyaGhkRjlDTVY5Mk1TNHdMakFNQjBOb1lYUkJjSEFNQkdsdVptOFRBM1Z5YkE9PQ==";
	
	public static void main(String args[]) {
		try{_main(args);}catch(Exception e){e.printStackTrace();}
	}
	public static void _main(String args[]) throws P2PDDSQLException, ASN1DecoderFail {
		net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		Application.setDB(new DBInterface(Application.DELIBERATION_FILE));
		byte[]g = Util.byteSignatureFromString(good);
		byte[]w = Util.byteSignatureFromString(wrong);
		if(_DEBUG)System.out.println("ASR:VerSigning g hash ="+Util.stringSignatureFromByte(Util.simple_hash(g,Cipher.MD5)));
		if(_DEBUG)System.out.println("ASR:VerSigning g ="+Util.stringSignatureFromByte(g));
		if(_DEBUG)System.out.println("ASR:VerSigning w hash ="+Util.stringSignatureFromByte(Util.simple_hash(w,Cipher.MD5)));
		if(_DEBUG)System.out.println("ASR:VerSigning w ="+Util.stringSignatureFromByte(w));

		if(_DEBUG)System.out.println("ASR:VerSigning g ="+Util.byteToHex(g,":"));
		if(_DEBUG)System.out.println("ASR:VerSigning w ="+Util.byteToHex(w,":"));

		net.ddp2p.common.hds.ASNSyncRequest payload_good = new net.ddp2p.common.hds.ASNSyncRequest().decode(new Decoder(g));
		net.ddp2p.common.hds.ASNSyncRequest payload_wrong = new net.ddp2p.common.hds.ASNSyncRequest().decode(new Decoder(w));
		System.out.println("SyncReq: test equal: good="+payload_good);
		System.out.println("SyncReq: test equal: wron="+payload_wrong);
		System.out.println("SyncReq: test equal: "+payload_good.equals(payload_wrong));
		payload_good.pEncoder();
	}
}
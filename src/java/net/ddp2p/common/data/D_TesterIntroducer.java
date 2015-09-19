package net.ddp2p.common.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.PK;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.recommendationTesters.TesterAndScore;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

public class D_TesterIntroducer extends ASNObj implements Comparable<D_TesterIntroducer> {
	private static final boolean DEBUG = false;
	
	public long testerIntroducerID; // primary key
	public long testerLID;
	public long introducerPeerLID;//// the peer who has introduced the tester in the recommendation 
	public String weight;  // the initial weight given by the creator peer; encoded in ASN1 as a String
	public float _weight;  // encoded in ASN1 as a String
	public Calendar creation_date; //date set by the creator when introduced the tester first time
	public byte[] signature; //signature of date,weight and tester_GID (from tester table if exist??)
	public Calendar testerRejectingDate=null;
	public int attackByIntroducer=0;// count how many attack attempts with in a time limit based on the expiration date
	String testerGID;
	String introducerPeerGID;
	@Override
	public D_TesterIntroducer instance() {
		return new D_TesterIntroducer();
	}
	
	public String toString() {
		return this.toTXT();
	}
	public String toTXT() {
		String result ="";
		result += "introducerPeerLID:"+this.introducerPeerLID+"\r\n";
		result += "testerLID:"+this.testerLID+"\r\n";
		result += "introducerPeerLID:"+this.introducerPeerLID+"\r\n";
		result += "weight:"+this.weight+"\r\n";
		result += "creation_date:"+this.creation_date+"\r\n";
		result += "attackByIntroducer:"+this.attackByIntroducer+"\r\n";
		result += "testerRejectingDate:"+this.testerRejectingDate+"\r\n";
		return result;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(introducerPeerGID));
		enc.addToSequence(new Encoder(testerGID));
		enc.addToSequence(new Encoder(weight));
		enc.addToSequence(new Encoder(creation_date));
		enc.addToSequence(new Encoder(signature));
		enc.setASN1Type(getASNType());
		return enc;
	}
	public Encoder getSignatureEncoder() {
		Encoder enc = new Encoder().initSequence();
		//enc.addToSequence(new Encoder(introducerPeerGID));
		enc.addToSequence(new Encoder(testerGID));
		enc.addToSequence(new Encoder(weight));
		enc.addToSequence(new Encoder(creation_date));
		//enc.addToSequence(new Encoder(signature));
		enc.setASN1Type(getASNType());
		return enc;
	}
	public byte[] sign() {
		D_Peer signer = D_Peer.getPeerByLID(introducerPeerLID, true, false);
		if (signer == null) return null;
		SK sk = signer.getSK();
		if (sk == null) return null;
		signature = Util.sign(this.getSignatureEncoder().getBytes(), sk);
		return signature;
	}
	public boolean verifySignature() {
		D_Peer signer = D_Peer.getPeerByLID(introducerPeerLID, true, false);
		if (signer == null) return false;
		PK pk = signer.getPK();
		if (pk == null) return false;
		boolean result = Util.verifySign(this.getSignatureEncoder().getBytes(), pk, signature);
		return result;
	}
	static byte getASNType() { 
		return DD.TAG_AC17;
	}
		@Override
	public D_TesterIntroducer decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		introducerPeerGID = d.getFirstObject(true).getString();
		try {
			introducerPeerLID = Util.lval(D_Peer.getPeerLIDbyGID(introducerPeerGID), -1);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} 
		testerGID = d.getFirstObject(true).getString();
		testerLID = net.ddp2p.common.data.D_Tester.getTesterLIDbyGID(testerGID);
		_weight = Util.fval(weight = d.getFirstObject(true).getString(), 0.0f);
		creation_date = d.getFirstObject(true).getGeneralizedTimeCalender_();
		signature = d.getFirstObject(true).getBytes();
		return this;
	}
		
		private static final String sql_load_testerIntroducers = 
				"SELECT "+net.ddp2p.common.table.tester_introducer.fields_tester_introducer 
				+ " FROM "+net.ddp2p.common.table.tester_introducer.TNAME+
				  " WHERE "+net.ddp2p.common.table.tester_introducer.testerLID+"=?;";
		
		
		public static ArrayList<D_TesterIntroducer> retrieveTesterIntroducers(String tLID) {
			ArrayList<D_TesterIntroducer> result = new ArrayList<D_TesterIntroducer>();
			ArrayList<ArrayList<Object>> obj;
			try {
				obj = Application.getDB().select(sql_load_testerIntroducers, new String[] {tLID}, DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return result;
			}
			//if (obj.size() <= 0) throw new  Exception("D_RecommendationOfTester: retrieveAllTestersLIDs:");
			//init(obj.get(0));
			if(DEBUG) System.out.println("D_RecommendationOfTester: retrieveAllTestersLIDs: got="+obj.size());
			for(int i = 0; i < obj.size(); i ++)
				result.add(buildRecord(obj.get(i)));
			//if(DEBUG) System.out.println("D_RecommendationOfTester: retrieveAllTestersLIDs: got="+result.size());//result);
			if(DEBUG){
				System.out.println("D_RecommendationOfTester: retrieveAllTestersLIDs: got="+result.size());
				for(int i=0; i<result.size();i++)
					System.out.print("D_RecommendationOfTester: retrieveAllTestersLIDs: got[="+result.get(i)+" ,");
				System.out.println("]");
			}
			
			Collections.sort(result);
			return result;
		}

		private static D_TesterIntroducer buildRecord(ArrayList<Object> a) {
			D_TesterIntroducer testerIntroducer =  new D_TesterIntroducer();
			testerIntroducer.weight = Util.getString(a.get(net.ddp2p.common.table.tester_introducer.F_WEIGH));
			testerIntroducer._weight = Util.fval(testerIntroducer.weight, 0.0f);
			testerIntroducer.creation_date = Util.getCalendar(Util.getString(a.get(net.ddp2p.common.table.tester_introducer.F_CREATION_DATE)));
			testerIntroducer.introducerPeerLID = Util.lval(a.get(net.ddp2p.common.table.tester_introducer.F_INTRODUCER_PEER_LID), -1);
			testerIntroducer.testerLID = Util.lval(a.get(net.ddp2p.common.table.tester_introducer.F_TESTER_LID), -1);
			testerIntroducer.signature = Util.byteSignatureFromString(Util.getString(a.get(net.ddp2p.common.table.tester_introducer.F_SIGNATURE))); 
			testerIntroducer.testerRejectingDate = Util.getCalendar(Util.getString(a.get(net.ddp2p.common.table.tester_introducer.F_TESTER_REJECTING_DATE)));
			testerIntroducer.attackByIntroducer = Util.get_int(a.get(net.ddp2p.common.table.tester_introducer.F_TESTER_REJECTING_DATE));
			
			return testerIntroducer;
		}

//		public static void updateTesterIntroducerDate(D_TesterIntroducer testerIntroducer) {
//			String params[] = new String[table.tester_introducer.F_FIELDS];
//
//			params[table.tester_introducer.F_TESTER_LID] = Util.getStringID(testerIntroducer.testerLID);
//			params[table.tester_introducer.F_INTRODUCER_PEER_LID] = Util.getStringID(testerIntroducer.introducerPeerLID);
//			params[table.tester_introducer.F_SIGNATURE] = Util.stringSignatureFromByte(testerIntroducer.signature);
//			params[table.tester_introducer.F_WEIGH] = testerIntroducer.weight;
//			params[table.tester_introducer.F_CREATION_DATE] = Encoder.getGeneralizedTime(testerIntroducer.creation_date);
//			// update the following:
//			params[table.tester_introducer.F_TESTER_REJECTING_DATE] = Encoder.getGeneralizedTime(Util.CalendargetInstance());
//			params[table.tester_introducer.F_ATTACK_BY_INTRODUCER] = Util.getString(testerIntroducer.attackByIntroducer++);
//			// ID:
//			params[table.tester_introducer.F_ID] = Util.getStringID(testerIntroducer.testerIntroducerID);
//			
//			try {
//				Application.db.update(table.tester_introducer.TNAME, table.tester_introducer._fields_tester_introducer_no_ID,
//						new String[]{table.tester_introducer.testerIntroducerID},
//						params, DEBUG);
//			} catch (P2PDDSQLException e) {
//				e.printStackTrace();
//			}
//			
//		}

		public static void insertTesterIntroducersInfo(
				ArrayList<D_TesterIntroducer> currentTesterIntroducers) {
			// TODO Auto-generated method stub
			
		}

		public static void insertTesterIntroducerInfo(D_TesterIntroducer testerIntroducer) {
			String params[] = new String[net.ddp2p.common.table.tester_introducer.F_FIELDS_NOID];

			params[net.ddp2p.common.table.tester_introducer.F_TESTER_LID] = Util.getStringID(testerIntroducer.testerLID);
			params[net.ddp2p.common.table.tester_introducer.F_INTRODUCER_PEER_LID] = Util.getStringID(testerIntroducer.introducerPeerLID);
			params[net.ddp2p.common.table.tester_introducer.F_SIGNATURE] = Util.stringSignatureFromByte(testerIntroducer.signature);
			params[net.ddp2p.common.table.tester_introducer.F_WEIGH] = testerIntroducer.weight;
			params[net.ddp2p.common.table.tester_introducer.F_CREATION_DATE] = Encoder.getGeneralizedTime(testerIntroducer.creation_date);
			// the following should be null!!
			params[net.ddp2p.common.table.tester_introducer.F_TESTER_REJECTING_DATE] = null;//Encoder.getGeneralizedTime(testerIntroducer.testerRejectingDate);
			params[net.ddp2p.common.table.tester_introducer.F_ATTACK_BY_INTRODUCER] = "0";//Util.getString(testerIntroducer.attackByIntroducer);
//			// ID:
//			params[table.tester_introducer.F_ID] = Util.getStringID(testerIntroducer.testerIntroducerID);
			
			try {
				Application.getDB().insert(net.ddp2p.common.table.tester_introducer.TNAME,
						net.ddp2p.common.table.tester_introducer._fields_tester_introducer_no_ID,
						params,
						DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}

			
		}

		public static void updateTesterIntroducer(
				long testerIntroducerID, D_TesterIntroducer testerIntroducer) {
			String params[] = new String[net.ddp2p.common.table.tester_introducer.F_FIELDS];

			params[net.ddp2p.common.table.tester_introducer.F_TESTER_LID] = Util.getStringID(testerIntroducer.testerLID);
			params[net.ddp2p.common.table.tester_introducer.F_INTRODUCER_PEER_LID] = Util.getStringID(testerIntroducer.introducerPeerLID);
			params[net.ddp2p.common.table.tester_introducer.F_SIGNATURE] = Util.stringSignatureFromByte(testerIntroducer.signature);
			params[net.ddp2p.common.table.tester_introducer.F_WEIGH] = testerIntroducer.weight;
			params[net.ddp2p.common.table.tester_introducer.F_CREATION_DATE] = Encoder.getGeneralizedTime(testerIntroducer.creation_date);
			params[net.ddp2p.common.table.tester_introducer.F_TESTER_REJECTING_DATE] = Encoder.getGeneralizedTime(testerIntroducer.testerRejectingDate);
			params[net.ddp2p.common.table.tester_introducer.F_ATTACK_BY_INTRODUCER] = Util.getString(testerIntroducer.attackByIntroducer);
			// ID:
			params[net.ddp2p.common.table.tester_introducer.F_ID] = Util.getStringID(testerIntroducerID);
			
			try {
				Application.getDB().update(net.ddp2p.common.table.tester_introducer.TNAME, net.ddp2p.common.table.tester_introducer._fields_tester_introducer_no_ID,
						new String[]{net.ddp2p.common.table.tester_introducer.testerIntroducerID},
						params, DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			
		}

		@Override
		public int compareTo(D_TesterIntroducer o) {
			if ( this.creation_date.equals(o.creation_date))
	            return 0;
	        else if ( this.creation_date.before(o.creation_date))
	            return 1;
	        else
	            return -1;
		}

	
}
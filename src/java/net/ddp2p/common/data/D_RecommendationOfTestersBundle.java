/*   Copyright (C) 2012 Khalid Alhamed and Marius Silaghi
		Author: Khalid Alhamed and Marius Silaghi: msilaghi@fit.edu
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
package net.ddp2p.common.data;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.recommendationTesters.RecommenderOfTesters;
import net.ddp2p.common.util.Util;
public class D_RecommendationOfTestersBundle extends ASNObj{
	public static boolean DEBUG = false;
	public static boolean _DEBUG = true;
	int version;
	public String senderGIDH;
	public ArrayList<D_TesterRatingByPeer> testersRatingList = new ArrayList<D_TesterRatingByPeer>();
	public Calendar creation_date; 
	public byte[] signature; 
	public String toString() {
		return this.toTXT();
	}
	public String toTXT() {
		String result ="";
		return result;
	}
	public static byte getASNType() {
		if(DEBUG) System.out.println("DD.TAG_AC23= "+ DD.TAG_AC23);
		return DD.TAG_AC23;
	}
	public boolean storeMessage() {
		D_Peer peer = D_Peer.getPeerByGID_or_GIDhash_NoCreate(null, senderGIDH, true, false);
		if (peer == null) {
			if (DEBUG) System.out.println("D_RecommendationOfTestersBundle:storeMessage(): unknown sender peer");
			return false;
		}
		ArrayList<D_RecommendationOfTester> oldies = D_RecommendationOfTester.retrieveAllRecommendationTestersKeep(peer.getLIDstr());
		/**
		 * Flag old recommendation to verify which one is reused
		 */
		for (D_RecommendationOfTester old : oldies) {
			old.setDeleteTestFlag(true);
		}
		if(DEBUG)System.out.println("D_RecommendationOfTestersBundle:storeMessage():testersRatingList.size(): "+testersRatingList.size());
		for (int k = 0 ; k < testersRatingList.size(); k ++ ) {
			D_TesterRatingByPeer testerRating = testersRatingList.get(k);
			D_Tester tester = D_Tester.getTesterInfoByGID(testerRating.testerGID, true, peer, testerRating.address);
			boolean newrIntroducerExist = processNewerIntroducers(tester.getLID(), testerRating.testerIntroducers);
			if( newrIntroducerExist)
				 continue;
			D_RecommendationOfTester crt = D_RecommendationOfTester.getRecommendationOfTester(peer.getLID(), tester.getLID(), true, true, null);
			boolean newRecom = crt.loadRemote(testerRating, this, (k==0)?this.signature:null);
			if (newRecom) {
				crt.setArrivalDate();
				crt.storeRequest();
			} else {
				if (_DEBUG) System.out.println("D_RecommendationOfTestersBundle:storeMessage(): old recommendation");
			}
			/**
			 * Clear the delete test flag to show that the tester is used.
			 */
			crt.setDeleteTestFlag(false);
			crt.releaseReferences();
		}
		for (D_RecommendationOfTester old : oldies) {
			/**
			 * If a flag was not cleared, then it has to be removed now
			 */
			if (old.isDeleteTestFlag()) old.delete();
			else {
				old.releaseReferences();
			}
		}
		return true;
	}
	private boolean passExpirationLimit(Calendar testerRejectingDate) {
		long days = daysBetween(testerRejectingDate, Util.CalendargetInstance());
		return days>RecommenderOfTesters.EXPERATION_OF_RECOMMENDATION_BY_INTRODUCER_IN_DAYS;
	}
	public static long daysBetween(Calendar startDate, Calendar endDate) {
	    long end = endDate.getTimeInMillis();
	    long start = startDate.getTimeInMillis();
	    return TimeUnit.MILLISECONDS.toDays(Math.abs(end - start));
	}
	private boolean processNewerIntroducers(long testerLID,
			ArrayList<D_TesterIntroducer> currentTesterIntroducers) {
		ArrayList<D_TesterIntroducer> StoredTesterIntroducers = D_TesterIntroducer.retrieveTesterIntroducers(Util.getStringID(testerLID));
		if(StoredTesterIntroducers==null || StoredTesterIntroducers.size()==0){
			D_TesterIntroducer.insertTesterIntroducersInfo(currentTesterIntroducers);
			return false;
		}
		int index=-1;
		boolean reject=false;
		for(D_TesterIntroducer crt : currentTesterIntroducers)
			if((index=inroducerExist(crt, StoredTesterIntroducers))!=-1){
				if(crt.creation_date.before(StoredTesterIntroducers.get(index).creation_date)){
					if(crt._weight != StoredTesterIntroducers.get(index)._weight){	
						if(!passExpirationLimit(StoredTesterIntroducers.get(index).testerRejectingDate) ){
							StoredTesterIntroducers.get(index).testerRejectingDate = Util.CalendargetInstance();
							reject=true;
						} else StoredTesterIntroducers.get(index).attackByIntroducer++;
						D_TesterIntroducer.updateTesterIntroducer(StoredTesterIntroducers.get(index).testerIntroducerID, StoredTesterIntroducers.get(index));	
					}
				}else{D_TesterIntroducer.updateTesterIntroducer(StoredTesterIntroducers.get(index).testerIntroducerID, crt);}
			}else D_TesterIntroducer.insertTesterIntroducerInfo(crt);
		return reject;
	}
	private int inroducerExist(D_TesterIntroducer crt,
			ArrayList<D_TesterIntroducer> storedTesterIntroducers) {
		for(int i=0; i<storedTesterIntroducers.size(); i++)
			if(crt.testerLID == storedTesterIntroducers.get(i).testerLID &&
			   crt.introducerPeerLID == storedTesterIntroducers.get(i).introducerPeerLID)
					return i;
		return -1;
	}
	public boolean verifySignature() {
		byte[] message = this.getSignableEncoder().getBytes();
		D_Peer peer = D_Peer.getPeerByGID_or_GIDhash_NoCreate(null, senderGIDH, true, false);
		if (peer == null) return false;
		return Util.verifySign(message, peer.getPK(), signature);
	}
	public byte[] sign() {
		D_Peer peer = D_Peer.getPeerByGID_or_GIDhash_NoCreate(null, senderGIDH, true, false);
		if (peer == null) return null;
		SK sk = peer.getSK();
		return sign(sk);
	}
	public byte[] sign(SK sk) {
		byte[] message = this.getSignableEncoder().getBytes();
		return signature = Util.sign(message, sk);
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version+""));
		enc.addToSequence(new Encoder(senderGIDH).setASN1Type(DD.TAG_AP0));
		enc.addToSequence(Encoder.getEncoder(testersRatingList).setASN1Type(DD.TAG_AC0));
		enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AP1));
		enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AP2));
		enc.setASN1Type(getASNType());
		return enc;
	}
	public Encoder getSignableEncoder() {
		D_TesterRatingByPeer[] _testersRatingList = testersRatingList.toArray(new D_TesterRatingByPeer[0]);
		Arrays.sort(_testersRatingList, new Comparator<D_TesterRatingByPeer>() {
			@Override
			public int compare(D_TesterRatingByPeer o1, D_TesterRatingByPeer o2) {
				if (o1.testerGID == null) return -1;
				if (o2.testerGID == null) return 1;
				return o1.testerGID.compareTo(o2.testerGID);
			}
		});
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version+""));
		enc.addToSequence(Encoder.getEncoder(_testersRatingList).setASN1Type(DD.TAG_AC0));
		enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AP2));
		enc.setASN1Type(getASNType());
		return enc;
	}
	@Override
	public D_RecommendationOfTestersBundle decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		version = Util.ival(d.getFirstObject(true).getString(), 0);
		switch (version) {
		case 0:
			decode_0(d);
			break;
		default:
			throw new ASN1DecoderFail("Unknown version: "+version);
		}
		return this;
	}
	private void decode_0(Decoder d) throws ASN1DecoderFail {
		senderGIDH = d.getFirstObject(true).getString(DD.TAG_AP0);
		testersRatingList = d.getFirstObject(true).getSequenceOfAL(D_TesterRatingByPeer.getASNType(), new D_TesterRatingByPeer());
		signature = d.getFirstObject(true).getBytes(DD.TAG_AP1);
		creation_date = d.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AP2);
	}	
}

/* ------------------------------------------------------------------------- */
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
/* ------------------------------------------------------------------------- */
package data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;

import ciphersuits.SK;
import util.Util;
import config.DD;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class D_RecommendationOfTestersBundle extends ASNObj{
	public static boolean DEBUG = false;
	int version;
	public String senderGIDH;
	public ArrayList<D_TesterRatingByPeer> testersRatingList = new ArrayList<D_TesterRatingByPeer>();
	public Calendar creation_date; //the timestamp of the given testres rating
	public byte[] signature; // of signed structure
	
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
//	public ArrayList<D_TesterItem> getNewItems() {
//		ArrayList<D_TesterItem> result = new ArrayList<D_TesterItem>();
//		for (int k = 0 ; k < testersRatingList.size(); k ++ ) {
//			result.add(new D_TesterItem(testersRatingList.get(k), this, k));
//		}
//		return result;
//	}
	
	public boolean storeMessage() {
		//ArrayList<D_TesterItem> result = new ArrayList<D_TesterItem>();
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
			// update Tester's Profile if needed
			D_Tester tester = D_Tester.getTesterInfoByGID(testerRating.testerGID, true, peer, testerRating.address);
			
			D_RecommendationOfTester crt = D_RecommendationOfTester.getRecommendationOfTester(peer.getLID(), tester.getLID(), true, true, null);
			// check for duplicate and store the newest recommendation
			boolean newRecom = crt.loadRemote(testerRating, this, (k==0)?this.signature:null);
			// ignore old recommendations based on the creation date
			if (! newRecom){
				if (DEBUG) System.out.println("D_RecommendationOfTestersBundle:storeMessage(): old recommendation");
			} else {
				crt.storeRequest();
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
		//enc.addToSequence(new Encoder(senderGIDH).setASN1Type(DD.TAG_AP0));
		enc.addToSequence(Encoder.getEncoder(_testersRatingList).setASN1Type(DD.TAG_AC0));
		//enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AP1));
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
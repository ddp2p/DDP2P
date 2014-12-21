/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014
		Authors: Khalid Alhamed, Marius Silaghi
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
package recommendationTesters;

import java.util.ArrayList;

import util.Util;

import data.D_Peer;
import data.D_RecommendationOfTestersBundle;
import data.D_Tester;
import data.D_TesterRatingByPeer;
import data.HandlingMyself_Peer;

/**
 * Called from RecommenderOfTesters after a new recommendation is made every day.
 * Prepares a recommendation bundle.
 * Iterates once over each peer (with the "used" flag) and send it the recommendation bundle.
 * 
 * Perform the sending by storing a bundle in the D_Peer object. Then, when building the next syncReply to this peer,
 * piggyback the bundle in the syncReply.
 * 
 */
public class RecommendationOfTestersSender {
	
	private static final boolean DEBUG = false;

	/**
	 * Building a message that contains the announced testers along with their rating scores.   
	 * @param usedTestersList 
	 * @param knownTestersList 
	 * @return
	 */
	static D_RecommendationOfTestersBundle buildBundle(TesterAndScore[] knownTestersList, TesterAndScore[] usedTestersList) {
		
		D_RecommendationOfTestersBundle message = new D_RecommendationOfTestersBundle();
		message.senderGIDH = HandlingMyself_Peer.getMyPeerIDhash();
		message.creation_date = Util.CalendargetInstance();
		// add used testers
		for(int i = 0; i< usedTestersList.length; i++){
			D_TesterRatingByPeer tRating = new D_TesterRatingByPeer();
			tRating._weight = usedTestersList[i].score;
			tRating.weight = ""+usedTestersList[i].score;
			D_Tester tester = D_Tester.getTesterInfoByLID(usedTestersList[i].testerID);
			tRating.testerGID = tester.testerGID;
			tRating.address = tester.url;
			message.testersRatingList.add(tRating);
		}
		
		// add known testers who are not in used testers list
		for(int i = 0; i< knownTestersList.length; i++)
			if(RecommenderOfTesters.isExist(knownTestersList[i], usedTestersList)==-1){
				D_TesterRatingByPeer tRating = new D_TesterRatingByPeer();
				tRating._weight = knownTestersList[i].score;
				tRating.weight = ""+knownTestersList[i].score;
				D_Tester tester = D_Tester.getTesterInfoByLID(knownTestersList[i].testerID);
				tRating.testerGID = tester.testerGID;
				tRating.address = tester.url;
				message.testersRatingList.add(tRating);
		     }
		
		return message;
	}
	
	public static void announceRecommendation(TesterAndScore[] knownTestersList, TesterAndScore[] usedTestersList) {
		D_RecommendationOfTestersBundle bundle = buildBundle(knownTestersList, usedTestersList);
		// retrieve neighbor peers of the current peer (destination peers) 
		ArrayList<D_Peer> peers = D_Peer.getUsedPeers();
		if(!DEBUG) if(peers==null || peers.size()==0)
						System.out.println("announceRecommendation(): NO neighbor peers? why send recom. ");
		for (D_Peer peer : peers) {
			peer.sendTesterRecommendationBundle(bundle);
		}
	}
}
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

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import data.D_Peer;
import data.D_RecommendationOfTestersBundle;

public class MessageReceiver {
	public static boolean DEBUG = true;
	//D_TesterItem[] testerItemList;
	public static void msgListener( D_Peer senderPeer, byte[] bundle) throws ASN1DecoderFail {
		data.D_RecommendationOfTestersBundle recommendation_testers = new D_RecommendationOfTestersBundle();
		recommendation_testers.decode(new Decoder(bundle));
		msgListener(senderPeer, recommendation_testers);
	}
	public static void msgListener(D_Peer senderPeer,
			D_RecommendationOfTestersBundle recommendation_testers) {
		//1) validate Signature
		if (! recommendation_testers.verifySignature()) {
			if (DEBUG) System.out.println("claas:MessageReceiver, msgListener() invalid signature");
			return;
		}
		//2) check duplicate and store the newest recommendation
		//3) update Tester's Profile if needed (e.g. add new tester or update address)
		recommendation_testers.storeMessage();
		//updateReceivedRecommendations(senderPeer, testerItemsMsg);
		
				 
		
	}

}
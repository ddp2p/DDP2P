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
package net.ddp2p.common.recommendationTesters;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_RecommendationOfTestersBundle;
import net.ddp2p.common.util.Util;
public class MessageReceiver {
	public static boolean DEBUG = false;
	public static void msgListener( D_Peer senderPeer, byte[] bundle) throws ASN1DecoderFail {
		net.ddp2p.common.data.D_RecommendationOfTestersBundle recommendation_testers = new D_RecommendationOfTestersBundle();
		recommendation_testers.decode(new Decoder(bundle));
		if(!DEBUG) System.out.println("--------------- MessageReceiver:msgListener():creationDate="+recommendation_testers.creation_date.getTime() +" No. of recieved testers="+recommendation_testers.testersRatingList.size());
		msgListener(senderPeer, recommendation_testers);
	}
	public static void msgListener(D_Peer senderPeer,
			D_RecommendationOfTestersBundle recommendation_testers) {
		if (! recommendation_testers.verifySignature()) {
			if (!DEBUG) System.out.println("claas:MessageReceiver, msgListener() invalid signature");
			return;
		}
		recommendation_testers.storeMessage();
	}
}

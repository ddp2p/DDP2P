/*   Copyright (C) 2014 Authors: Hang Dong <hdong2012@my.fit.edu>, Khalid Alhamed, Marius Silaghi <silaghi@fit.edu>
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
package com.HumanDecisionSupportSystemsLaboratory.DD_P2P.AndroidChat;

import java.util.Calendar;

import net.ddp2p.ASN1.Encoder;

public
class DatedChatMessage {
	public DatedChatMessage(ChatMessage cmsg, Calendar _time) {
		time = _time;
		msg = cmsg;
	}
	
	public Calendar time;
	public ChatMessage msg;
	public boolean received = false;
	public String peerGID;
	
	public String toString() {
		return "DCM[rcv="+received
				+ " time="+Encoder.getGeneralizedTime(time)
				+ "\n msg="+msg
				+"\n]";
	}
}
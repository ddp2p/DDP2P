/*   Copyright (C) 2012 Osamah Dhannoon
 		Author: Osamah Dhannoon: odhannoon2011@fit.edu
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
package net.ddp2p.common.handling_wb;
import java.util.ArrayList;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class BroadcastQueueCircular extends BroadcastQueue {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	String m_sG_org_id=null; 
	Prepare_messages pm = new Prepare_messages();
	public BroadcastQueueCircular() throws P2PDDSQLException{
		if(DEBUG)System.out.println("BroadcastQueueCircular() : Constructor ");
		loadAll();
	}
	public void loadAll() {
		this.m_PreparedMessagesVotes = loadVotes();
		this.m_iCrtVotes = 0;
		this.m_PreparedMessagesConstituents = loadConstituents();
		this.m_iCrtConstituents = 0;
		this.m_PreparedMessagesPeers = loadPeers();
		this.m_iCrtPeers = 0;
		this.m_PreparedMessagesOrgs = loadOrgs();
		this.m_iCrtOrgs = 0;
		this.m_PreparedMessagesWitnesses = loadWitnesses();
		this.m_iCrtWitnesses = 0;
		this.m_PreparedMessagesNeighborhoods = loadNeighborhoods();
		this.m_iCrtNeighborhoods = 0;
	}
	ArrayList<PreparedMessage> loadVotes() {
		if(DEBUG)System.out.println("BroadcastableMessages : loadVotes() : from db");
		ArrayList<PreparedMessage> newQueue = new ArrayList<PreparedMessage>();
		m_lastVote = pm.loadVotes(newQueue, m_lastVote);
		if(DEBUG)System.out.println("size of vote queue is : "+m_PreparedMessagesVotes.size());
		if(DEBUG)System.out.println("\n---------------------------------------------------------------\n");
		return newQueue;
	}
	ArrayList<PreparedMessage> loadConstituents() {
		if(DEBUG)System.out.println("BroadcastableMessages : loadConstituents() : from db");
		ArrayList<PreparedMessage> newQueue = new ArrayList<PreparedMessage>();
		m_lastConstituent = pm.loadConstituents(newQueue,m_lastConstituent);
		if(DEBUG)System.out.println("size of constituent queue is : "+m_PreparedMessagesConstituents.size());
		if(DEBUG)System.out.println("\n---------------------------------------------------------------\n");
		return newQueue;
	}
	ArrayList<PreparedMessage> loadPeers(){
		if(DEBUG)System.out.println("BroadcastableMessages : loadPeers() : from db");
		ArrayList<PreparedMessage> newQueue = new ArrayList<PreparedMessage>();
		m_lastPeer = pm.loadPeers(newQueue,m_lastPeer);
		if(DEBUG)System.out.println("size of peer queue is : "+m_PreparedMessagesPeers.size());
		if(DEBUG)System.out.println("\n---------------------------------------------------------------\n");
		return newQueue;
	}
	ArrayList<PreparedMessage> loadOrgs() {
		if(DEBUG)System.out.println("BroadcastableMessages : loadOrgs() : from db");
		ArrayList<PreparedMessage> newQueue = new ArrayList<PreparedMessage>();
		m_lastOrg = pm.loadOrgs(newQueue,m_lastOrg);
		if(DEBUG)System.out.println("size of Orgs queue is : "+m_PreparedMessagesOrgs.size());
		if(DEBUG)System.out.println("\n---------------------------------------------------------------\n");
		return newQueue;
	}
	ArrayList<PreparedMessage> loadWitnesses() {
		if(DEBUG)System.out.println("BroadcastableMessages : loadWitnesses() : from db");
		ArrayList<PreparedMessage> newQueue = new ArrayList<PreparedMessage>();
		m_lastWitness = pm.loadWitnesses(newQueue,m_lastWitness);
		if(DEBUG)System.out.println("size of Witnesses queue is : "+m_PreparedMessagesWitnesses.size());
		if(DEBUG)System.out.println("\n---------------------------------------------------------------\n");
		return newQueue;
	}
	ArrayList<PreparedMessage> loadNeighborhoods() {
		if(DEBUG)System.out.println("BroadcastableMessages : loadNeighborhoods() : from db");
		ArrayList<PreparedMessage> newQueue = new ArrayList<PreparedMessage>();
		m_lastNeighborhoods = pm.loadNeighborhoods(newQueue,m_lastNeighborhoods);
		if(DEBUG)System.out.println("m_lastNeighborhoods : "+m_lastNeighborhoods);
		if(DEBUG)System.out.println("size of Neighborhoods queue is : "+m_PreparedMessagesNeighborhoods.size());
		if(DEBUG)System.out.println("\n---------------------------------------------------------------\n");
		return newQueue;
	}
	@Override
	long loadVotes(ArrayList<PreparedMessage> m_PreparedMessagesVotes2,
			long m_lastVote2) {
		return 0;
	}
	@Override
	long loadConstituents(ArrayList<PreparedMessage> m_PreparedMessagesConstituents2,
			long m_lastConstituent2) {
		return 0;
	}
	@Override
	long loadPeers(ArrayList<PreparedMessage> m_PreparedMessagesPeers2,
			long m_lastPeer2) {
		return 0;
	}
	@Override
	long loadOrgs(ArrayList<PreparedMessage> m_PreparedMessagesOrgs2, long m_lastOrg2)
			throws P2PDDSQLException {
		return 0;
	}
	@Override
	long loadWitnesses(ArrayList<PreparedMessage> m_PreparedMessagesWitnesses2,
			long m_lastWitness2) {
		return 0;
	}
	@Override
	long loadNeighborhoods(ArrayList<PreparedMessage> m_PreparedMessagesNeighborhoods2,
			long m_lastNeighborhoods2) {
		return 0;
	}
	@Override
	public byte[] getNext(long msg_c) {
		return null;
	}
}

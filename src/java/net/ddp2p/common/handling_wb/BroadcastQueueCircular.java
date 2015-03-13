/* ------------------------------------------------------------------------- */
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
/* ------------------------------------------------------------------------- */

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
	//ArrayList<byte[]>  m_ALbaPreparedMessagesVotes = new ArrayList<byte[]>();
	//ArrayList<byte[]>  m_ALbaPreparedMessagesOrgs = new ArrayList<byte[]>();
	//ArrayList<byte[]>  m_ALbaPreparedMessagesConstituents = new ArrayList<byte[]>();
	//ArrayList<byte[]>  m_ALbaPreparedMessagesPeers = new ArrayList<byte[]>();
	//ArrayList<byte[]>  m_ALbaPreparedMessagesWitnesses = new ArrayList<byte[]>();
	
	/*
	 Constructor: when first running the program 
	 load everything currently available on db
	 */
	public BroadcastQueueCircular() throws P2PDDSQLException{
		if(DEBUG)System.out.println("BroadcastQueueCircular() : Constructor ");
		loadAll();
	}
	
	public void loadAll() {
		// LOADER_ADDED START
		this.m_PreparedMessagesVotes = loadVotes();
		this.m_iCrtVotes = 0;
		
		this.m_PreparedMessagesConstituents = loadConstituents();
		this.m_iCrtConstituents = 0;
		
		this.m_PreparedMessagesPeers = loadPeers();
		this.m_iCrtPeers = 0;
		
		this.m_PreparedMessagesOrgs = loadOrgs();
		this.m_iCrtOrgs = 0;//-1;
		
		this.m_PreparedMessagesWitnesses = loadWitnesses();
		this.m_iCrtWitnesses = 0;
		
		this.m_PreparedMessagesNeighborhoods = loadNeighborhoods();
		this.m_iCrtNeighborhoods = 0;
		// LOADER_ADDED END
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
		// LOADER_ADDED START create in a new array, keeping old array for current use
		ArrayList<PreparedMessage> newQueue = new ArrayList<PreparedMessage>();
		m_lastNeighborhoods = pm.loadNeighborhoods(newQueue,m_lastNeighborhoods);
		//synchronized(BroadcastQueue.loader_monitor){m_ALbaPreparedMessagesNeighborhoods = newQueue;}
		// LOADER_ADDED END
		if(DEBUG)System.out.println("m_lastNeighborhoods : "+m_lastNeighborhoods);
		if(DEBUG)System.out.println("size of Neighborhoods queue is : "+m_PreparedMessagesNeighborhoods.size());
		if(DEBUG)System.out.println("\n---------------------------------------------------------------\n");
		return newQueue;
	}
	/*
	protected void setNewRoundFromQueue(
			ArrayList<PreparedMessage> crt_witnesses_queue,
			int count_broadcasted_witnesses2) {
			for(int i=0;i < crt_witnesses_queue.size(); i++)
				crt_witnesses_queue.get(i).sent_flag = false;
			if(DEBUG)System.out.println("BQCircular: setNewRoundFromQueue");
	}
	@Override
	protected byte[] getItemFromQueue(
			ArrayList<PreparedMessage> crt_witnesses_queue,
			int crt_witnesses_idx, int count_broadcasted_witnesses2, int QUEUE) {
		if(count_broadcasted_witnesses2==0) { BroadcastQueue.crt_votes_idx=crt_witnesses_idx; return crt_witnesses_queue.get(crt_witnesses_idx).raw;}
		int qlen = crt_witnesses_queue.size();
		int actual_idx = (int)Math.round(Math.floor(Util.random(qlen+0.9f)));
		int i=0;
		for(; i<qlen; i++){
			if(crt_witnesses_queue.get((i+actual_idx)%qlen).sent_flag) continue;
			crt_witnesses_queue.get((i+actual_idx)%qlen).sent_flag = true;
			break;
		}
		if(DEBUG)System.out.println("BQCircular: getItemFromQueue");
		//int tt=(i+actual_idx)%qlen;
		//System.out.println("BQCircular: "+tt);
		BroadcastQueue.crt_votes_idx=(i+actual_idx)%qlen;
		return crt_witnesses_queue.get((i+actual_idx)%qlen).raw;
	}
	*/
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
		// TODO Auto-generated method stub
		return null;
	}
}

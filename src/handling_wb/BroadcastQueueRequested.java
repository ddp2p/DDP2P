package handling_wb;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import com.almworks.sqlite4java.SQLiteException;

import data.D_Interests;

import simulator.WirelessLog;
import streaming.RequestData;
import util.Util;
import wireless.Broadcasting_Probabilities;


public class BroadcastQueueRequested extends BroadcastQueue implements Runnable{
	
	 
	
	private static long min_interest;
	private static long life_span;
	private static long exp_time; 
	
	public static D_Interests myInterests;
	
	public static class Received_Interest_Ad
	{
		private String itemGIDhash;
		public long interest_expiration_date;
		
		public Received_Interest_Ad(String id, long value)
		{
			itemGIDhash = id;
			interest_expiration_date =value;
		}
	}
	public static ArrayList<PreparedMessage> requested_PreparedMessages = new ArrayList<PreparedMessage>();
	
	public static ArrayList<Received_Interest_Ad> rcv_interest_req_org_ID_hashes = new ArrayList<Received_Interest_Ad>();
	public static ArrayList<Received_Interest_Ad> rcv_interest_req_const_ID_hashes = new ArrayList<Received_Interest_Ad>();
	public static ArrayList<Received_Interest_Ad> rcv_interest_req_motion_ID= new ArrayList<Received_Interest_Ad>();
	public static ArrayList<Received_Interest_Ad> rcv_interest_req_neighborhood_ID= new ArrayList<Received_Interest_Ad>();
	
	
	public static boolean interestRequested(ArrayList<Received_Interest_Ad> list, String interest)
	{
		if(interest!=null)
		{
			for(int i=0; i<list.size();i++)
			{
				if(list.get(i).itemGIDhash.equals(interest))return true;
			}
		}
		return false;
	}
	public static void addTo_requested_PreparedMessages(PreparedMessage pm)
	{
		if(interestRequested(rcv_interest_req_org_ID_hashes,pm.org_ID_hash))
		{
			requested_PreparedMessages.add(pm);
		}else if(interestRequested(rcv_interest_req_motion_ID,pm.motion_ID)){
			requested_PreparedMessages.add(pm);
		}else{
			if(pm.constituent_ID_hash.size()>0)
			{
				for(int i=0; i<pm.constituent_ID_hash.size();i++)
				{
					if(interestRequested(rcv_interest_req_const_ID_hashes,pm.constituent_ID_hash.get(i)))
					{
						requested_PreparedMessages.add(pm);
					}
				}
			}
			
			if(pm.neighborhood_ID.size()>0)
			{
				for(int i=0; i<pm.neighborhood_ID.size();i++)
				{
					if(interestRequested(rcv_interest_req_neighborhood_ID,pm.neighborhood_ID.get(i)))
					{
						requested_PreparedMessages.add(pm);
					}
				}
			}
		}
	}
	public static boolean is_requested(PreparedMessage pm)
	{
		if(interestRequested(rcv_interest_req_org_ID_hashes,pm.org_ID_hash))
		{
			return true;
		}else if(interestRequested(rcv_interest_req_motion_ID,pm.motion_ID)){
			return true;
		}else{
			if(pm.constituent_ID_hash.size()>0)
			{
				for(int i=0; i<pm.constituent_ID_hash.size();i++)
				{
					if(interestRequested(rcv_interest_req_const_ID_hashes,pm.constituent_ID_hash.get(i)))
					{
						return true;
					}
				}
			}
			
			if(pm.neighborhood_ID.size()>0)
			{
				for(int i=0; i<pm.neighborhood_ID.size();i++)
				{
					if(interestRequested(rcv_interest_req_neighborhood_ID,pm.neighborhood_ID.get(i)))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public void run()
	{
		min_interest = Util.CalendargetInstance().getTimeInMillis();//++;
		exp_time=min_interest; // - 1000;
		
		
		//is this how we synchronize use of static members?? 
		synchronized(rcv_interest_req_org_ID_hashes) {
			for(int j=0; j< rcv_interest_req_org_ID_hashes.size();j++)
			{
				if(rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date < exp_time ){
					rcv_interest_req_org_ID_hashes.remove(j);
				}else break;
			}
		}
		synchronized(rcv_interest_req_const_ID_hashes){
			for(int j=0; j< rcv_interest_req_const_ID_hashes.size();j++)
			{
				if(rcv_interest_req_const_ID_hashes.get(j).interest_expiration_date < exp_time ){
					rcv_interest_req_const_ID_hashes.remove(j);
				}else break;
			}
		}
		synchronized(rcv_interest_req_motion_ID){
			for(int j=0; j< rcv_interest_req_motion_ID.size();j++)
			{
				if(rcv_interest_req_motion_ID.get(j).interest_expiration_date < exp_time ){
					rcv_interest_req_motion_ID.remove(j);
				}else break;
			}
		}
		synchronized(rcv_interest_req_neighborhood_ID){
			for(int j=0; j< rcv_interest_req_neighborhood_ID.size();j++)
			{
				if(rcv_interest_req_neighborhood_ID.get(j).interest_expiration_date < exp_time ){
					rcv_interest_req_neighborhood_ID.remove(j);
				}else break;
			}
		}
		
	}
	
	@Override
	public void loadAll() {
		//min_interest=0; 
		min_interest=Util.CalendargetInstance().getTimeInMillis();
		life_span=100;
		new Thread(new BroadcastQueueRequested()).start();
		
	}

	public void registerRequest(RequestData rq) {
		
		myInterests = new D_Interests();
		myInterests.motion_ID = rq.moti;
		myInterests.neighborhood_ID = rq.neig;
		myInterests.org_ID_hashes = rq.orgs;
		
		//Check this!!
		Set<String> set = rq.cons.keySet();
		Iterator<String> itr = set.iterator();
		while(itr.hasNext())
		{
			myInterests.const_ID_hashes.add(itr.next());
		}
		
		
	}

	@Override
	long loadVotes(ArrayList<PreparedMessage> m_PreparedMessagesVotes2,
			long m_lastVote2) {
		// TODO Auto-generated method stub
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
			throws SQLiteException {
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
		Util.printCallPath("Why call this function?");
		System.exit(1);
		return null;
	}
	public byte[] getNext(BroadcastQueue[] queues, long msg_c) {
		//queues: {b_md,b_c,/*b_ra,*/ b_re,b_r, b_h};
		byte[] result=null;
		
		PreparedMessage result_md = new PreparedMessage();//my data
		PreparedMessage result_c = new PreparedMessage();//circular
		PreparedMessage result_r = new PreparedMessage();//recent
		PreparedMessage result_h = new PreparedMessage();//handled
		
		result_md = queues[0].getNext(WirelessLog.MyData_queue,msg_c, result_md);
		//addTo_requested_PreparedMessages(result_md);		
		
		result_c = queues[1].getNext(WirelessLog.Circular_queue,msg_c, result_c);
		//addTo_requested_PreparedMessages(result_c);
		
		result_r = queues[3].getNext(WirelessLog.Recent_queue,msg_c, result_r);
		//addTo_requested_PreparedMessages(result_r);
		
		result_h = queues[4].getNext(WirelessLog.Handled_queue,msg_c, result_h);
		//addTo_requested_PreparedMessages(result_h);
		
		float rnd = Util.random(1.f);
		
		do{
			if((rnd < wireless.Broadcasting_Probabilities.get_broadcast_queue_br()) && (wireless.Broadcasting_Probabilities._broadcast_queue_br)){
				if(is_requested(result_r))
				{
					result= result_r.raw;
				}
			}else if((rnd < wireless.Broadcasting_Probabilities.get_broadcast_queue_bh()) && (wireless.Broadcasting_Probabilities._broadcast_queue_bh)){
				if(is_requested(result_h))
				{
					result= result_h.raw;
				}
			}else if((rnd < wireless.Broadcasting_Probabilities.get_broadcast_queue_c())&&(wireless.Broadcasting_Probabilities._broadcast_queue_c)){
				if(is_requested(result_c))
				{
					result= result_c.raw;
				}
			}else if((rnd < wireless.Broadcasting_Probabilities.get_broadcast_queue_md()) && (wireless.Broadcasting_Probabilities._broadcast_queue_md)){
				if(is_requested(result_md))
				{
					result= result_md.raw;
				}
			}
		}while(result==null);
		
		
		return result;		
	}
	/**
	 * 
	 * @param interests
	 * @param clientAddress
	 * @param iP
	 */
	public static void handleNewInterests(D_Interests interests,
			SocketAddress clientAddress, String iP) {
		long crt_time_millis = Util.CalendargetInstance().getTimeInMillis();
		
		// For all requested orgIDhash values
		for(int i=0;i<interests.org_ID_hashes.size();i++)
		{
			Received_Interest_Ad received_interest = new Received_Interest_Ad(interests.org_ID_hashes.get(i), crt_time_millis+life_span_peer(interests));

			boolean exists=false;
			synchronized(rcv_interest_req_org_ID_hashes) {
				for(int j=0; j< rcv_interest_req_org_ID_hashes.size();j++)
				{
					if(rcv_interest_req_org_ID_hashes.get(j).itemGIDhash.equals(received_interest.itemGIDhash) ){
						if(rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date < received_interest.interest_expiration_date)
						{
							Received_Interest_Ad item = rcv_interest_req_org_ID_hashes.get(j);
							item.interest_expiration_date = received_interest.interest_expiration_date;
							Util.insertSort(rcv_interest_req_org_ID_hashes, item, 0, j);
						}
						exists = true;
						break;
					}
				}

				if(!exists)Util.insertSort(rcv_interest_req_org_ID_hashes,received_interest,0,rcv_interest_req_org_ID_hashes.size());
			}
		}

		for(int i=0;i<interests.const_ID_hashes.size();i++)
		{
			Received_Interest_Ad received_interest = new Received_Interest_Ad(interests.const_ID_hashes.get(i), crt_time_millis+life_span_peer(interests));
			
			boolean exists=false;
			synchronized(rcv_interest_req_const_ID_hashes) {
				for(int j=0; j< rcv_interest_req_const_ID_hashes.size();j++)
				{
					if(rcv_interest_req_const_ID_hashes.get(j).itemGIDhash.equals(received_interest.itemGIDhash) ){

						if(rcv_interest_req_const_ID_hashes.get(j).interest_expiration_date < received_interest.interest_expiration_date)
						{
							Received_Interest_Ad item = rcv_interest_req_const_ID_hashes.get(j);
							item.interest_expiration_date = received_interest.interest_expiration_date;
							Util.insertSort(rcv_interest_req_const_ID_hashes, item, 0, j);
						}
						exists =true;
						break;
					}
				}

				if(!exists)Util.insertSort(rcv_interest_req_const_ID_hashes,received_interest,0,rcv_interest_req_const_ID_hashes.size());
			}
		}
		
		for(int i=0;i<interests.motion_ID.size();i++)
		{
			Received_Interest_Ad received_interest = new Received_Interest_Ad(interests.motion_ID.get(i),crt_time_millis+life_span_peer(interests));
			
			boolean exists=false;

			synchronized(rcv_interest_req_motion_ID){
				for(int j=0; j< rcv_interest_req_motion_ID.size();j++)
				{
					if(rcv_interest_req_motion_ID.get(j).itemGIDhash.equals(received_interest.itemGIDhash) ){
						if(rcv_interest_req_motion_ID.get(j).interest_expiration_date < received_interest.interest_expiration_date)
						{
							Received_Interest_Ad item = rcv_interest_req_motion_ID.get(j);
							item.interest_expiration_date = received_interest.interest_expiration_date;
							Util.insertSort(rcv_interest_req_motion_ID, item, 0, j);
						}
						exists =true;
						break;
					}
				}

				if(!exists)Util.insertSort(rcv_interest_req_motion_ID,received_interest,0,rcv_interest_req_motion_ID.size());
			}
		}
		
		for(int i=0;i<interests.neighborhood_ID.size();i++)
		{
			Received_Interest_Ad received_interest = new Received_Interest_Ad(interests.neighborhood_ID.get(i),crt_time_millis+life_span_peer(interests));
			
			boolean exists=false;
			synchronized(rcv_interest_req_neighborhood_ID){
				for(int j=0; j< rcv_interest_req_neighborhood_ID.size();j++)
				{
					if(rcv_interest_req_neighborhood_ID.get(j).itemGIDhash.equals(received_interest.itemGIDhash) ){
						if(rcv_interest_req_neighborhood_ID.get(j).interest_expiration_date < received_interest.interest_expiration_date)
						{
							Received_Interest_Ad item = rcv_interest_req_neighborhood_ID.get(j);
							item.interest_expiration_date = received_interest.interest_expiration_date;
							Util.insertSort(rcv_interest_req_neighborhood_ID, item, 0, j);
						}
						exists =true;
						break;
					}
				}

				if(!exists)Util.insertSort(rcv_interest_req_neighborhood_ID,received_interest,0,rcv_interest_req_neighborhood_ID.size());
			}
		}

	}
	private static long life_span_peer(D_Interests interests) {
		return life_span;
	}
}
package handling_wb;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;

import streaming.RequestData;
import util.DBInfo;
import util.DBListener;
import util.Util;

import ciphersuits.Cipher;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;
import simulator.WirelessLog;

public class BroadcastQueues implements BroadcastingQueue, DBListener{
	
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	BroadcastQueueMyData b_md;
	BroadcastQueueCircular b_c;
	BroadcastQueueRandom b_ra;
	BroadcastQueueRequested b_re;
	BroadcastQueueRecent b_r;
	BroadcastQueueHandled b_h;
	BroadcastQueue[] bq;

	public BroadcastQueues() throws SQLiteException {
		if(_DEBUG)System.out.println("Client Start\n");
		b_md = new BroadcastQueueMyData();
		b_c = new BroadcastQueueCircular();
		//b_ra = new BroadcastQueueRandom();
		b_re = new BroadcastQueueRequested();
		b_r = new BroadcastQueueRecent();
		b_h = new BroadcastQueueHandled();
		bq = new BroadcastQueue[]{b_md,b_c,/*b_ra,*/ b_re,b_r, b_h};
		
		Application.db.addListener(this, 
				new ArrayList<String>(Arrays.asList(
						table.signature.TNAME,
						table.organization.TNAME,
						table.peer.TNAME,
						table.motion.TNAME,
						table.constituent.TNAME,
						table.neighborhood.TNAME,
						table.witness.TNAME)),
				null);
		if(DEBUG)System.out.println("BroadcastQueues : End of constructor");
	}
	
	public void setMyPeerID(String _my_peer_ID){
		b_md.setMyPeerID(_my_peer_ID);
	}
	
	public void wakeup() {
		synchronized(this){
			this.notifyAll();
		}
	}
	
/**
	// normalized prefix values, from the largest to the smallest
	public static float broadcast_queue_md = 0.9f;
	public static float broadcast_queue_c = 0.1f;
	public static float broadcast_queue_ra = 0.5f;
	public static float broadcast_queue_re = 0.7f;
	public static float broadcast_queue_bh = 0.3f;
	public static float broadcast_queue_br = 0.4f;
 * 
 */
	
	@Override
	public byte[] getNext(long msg_c) {
		float rnd = Util.random(1.f);
		byte[] result=null;
		
		if(DEBUG)System.out.println("again");
		/*
		System.out.println("br : "+wireless.Broadcasting_Probabilities.get_broadcast_queue_br());
		System.out.println("bh : "+wireless.Broadcasting_Probabilities.get_broadcast_queue_bh());
		System.out.println("ra : "+wireless.Broadcasting_Probabilities.get_broadcast_queue_ra());
		System.out.println("re : "+wireless.Broadcasting_Probabilities.get_broadcast_queue_re());
		System.out.println("md : "+wireless.Broadcasting_Probabilities.get_broadcast_queue_md());
		*/
		do {
			if((rnd < wireless.Broadcasting_Probabilities.get_broadcast_queue_br()) && (wireless.Broadcasting_Probabilities._broadcast_queue_br)){
				result = b_r.getNext(WirelessLog.Recent_queue,msg_c);
				if(DEBUG)System.out.println("getnext : broadcast_queue_br : "+result);
				if(result!=null) {
					/*
				
					String log = WirelessLog.log_broadcast+
							Util.CalendargetInstance().getTimeInMillis()+
							WirelessLog.tab+
							WirelessLog.Recent_queue+
							WirelessLog.tab+
							WirelessLog.current_recent_idx;
					WirelessLog.Print_to_log(log);
					*/
					break;
				}
				
			}
			if((rnd < wireless.Broadcasting_Probabilities.get_broadcast_queue_bh()) && (wireless.Broadcasting_Probabilities._broadcast_queue_bh)){
				result = b_h.getNext(WirelessLog.Handled_queue,msg_c);
				
				if(DEBUG)System.out.println("getnext : broadcast_queue_bh : "+result);
				
				if(result!=null) {
					/*
					String hash =  Util.stringSignatureFromByte(Util.simple_hash(result, DD.APP_INSECURE_HASH));
					String log = WirelessLog.log_broadcast+
							Util.CalendargetInstance().getTimeInMillis()+
							WirelessLog.tab+
							WirelessLog.Handled_queue+
							WirelessLog.tab+
							WirelessLog.current_handled_idx+"|"+hash;
					WirelessLog.Print_to_log(log);
					*/
					break;
				}
			}
			/*
			if(rnd < wireless.Broadcasting_Probabilities.get_broadcast_queue_re()){
				result = b_re.getNext();
				if(DEBUG)System.out.println("getnext : broadcast_queue_re : "+result);
				if(result!=null) break;
			}
			if(rnd < wireless.Broadcasting_Probabilities.get_broadcast_queue_ra()){
				result = b_ra.getNext();
				if(DEBUG)System.out.println("getnext : broadcast_queue_ra : "+result);
				if(result!=null) break;
			}*/
			
			if((rnd < wireless.Broadcasting_Probabilities.get_broadcast_queue_re())&&(wireless.Broadcasting_Probabilities._broadcast_queue_re)){
				
				
				result = b_re.getNext(bq, msg_c);
				
				
				if(DEBUG)System.out.println("getnext : broadcast_queue_re : "+result);
				if(result!=null) {
//					String hash =  Util.stringSignatureFromByte(Util.simple_hash(result, DD.APP_INSECURE_HASH));
//					String log = WirelessLog.log_broadcast+
//							Util.CalendargetInstance().getTimeInMillis()+
//							WirelessLog.tab+
//							WirelessLog.Recent_queu+
//							WirelessLog.tab+
//							WirelessLog.current_request_idx+"|"+hash;
//					WirelessLog.Print_to_log(log);
					break;
				}
			}
			
			
			if((rnd < wireless.Broadcasting_Probabilities.get_broadcast_queue_c())&&(wireless.Broadcasting_Probabilities._broadcast_queue_c)){
				result = b_c.getNext(WirelessLog.Circular_queue,msg_c);
				if(DEBUG)System.out.println("getnext : broadcast_queue_c : "+result);
				if(result!=null) {
//					String hash =  Util.stringSignatureFromByte(Util.simple_hash(result, DD.APP_INSECURE_HASH));
//					String log = WirelessLog.log_broadcast+
//							Util.CalendargetInstance().getTimeInMillis()+
//							WirelessLog.tab+
//							WirelessLog.Circular_queue+
//							WirelessLog.tab+
//							WirelessLog.current_circular_idx+"|"+hash;
//					WirelessLog.Print_to_log(log);
					break;
				}
			}
			if((rnd < wireless.Broadcasting_Probabilities.get_broadcast_queue_md()) && (wireless.Broadcasting_Probabilities._broadcast_queue_md)){
				result = b_md.getNext(WirelessLog.MyData_queue,msg_c);
				if(DEBUG)System.out.println("getnext : broadcast_queue_md : "+result);
				if(result!=null) {
					//String hash =  Util.stringSignatureFromByte(Util.simple_hash(result, DD.APP_INSECURE_HASH));
//					String log = WirelessLog.log_broadcast+
//							Util.CalendargetInstance().getTimeInMillis()+
//							WirelessLog.tab+
//							WirelessLog.MyData_queue+
//							WirelessLog.tab+
//							WirelessLog.current_mydata_idx+"|"+hash;;
//					WirelessLog.Print_to_log(log);
					break;
				}
			}
			
			if(rnd == 0){
				try {
					synchronized(this){
						this.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			rnd = 0;
			if(DEBUG)System.out.println("BroadcastQueues : getNext() : NO DATA BREAK!");
			break;
		}while(result == null);
		
		
		//embed personal interests here!!!
		data.D_Interests_Message dim = new data.D_Interests_Message();
		dim.message =result;
		dim.interests = BroadcastQueueRequested.myInterests.encode();
		
		return dim.encode();
	}

	@Override
	public void loadAll() {
		if(DEBUG)System.out.println("loadAll() : in BroadcastQueues ");
		for(int k=0; k<bq.length; k++) bq[k].loadAll();
	}
	
	public void registerRecent(PreparedMessage pm, int type) {
		//PreparedMessage pm=new PreparedMessage();
		//pm.raw=msg;
		
		b_h.addMessage(pm, type);
	}
	
	public void registerRequest(RequestData rq){
		b_re.registerRequest(rq);
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		wakeup();
	}

}




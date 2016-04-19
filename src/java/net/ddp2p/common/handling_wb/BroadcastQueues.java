package net.ddp2p.common.handling_wb;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Interests;
import net.ddp2p.common.simulator.WirelessLog;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
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
	public BroadcastQueues() throws P2PDDSQLException {
		if(_DEBUG)System.out.println("Client Start\n");
		b_md = new BroadcastQueueMyData();
		b_c = new BroadcastQueueCircular();
		b_re = new BroadcastQueueRequested();
		b_re.loadAll();
		b_r = new BroadcastQueueRecent();
		b_h = new BroadcastQueueHandled();
		bq = new BroadcastQueue[]{b_md,b_c, b_re,b_r, b_h};
		Application.getDB().addListener(this, 
				new ArrayList<String>(Arrays.asList(
						net.ddp2p.common.table.signature.TNAME,
						net.ddp2p.common.table.organization.TNAME,
						net.ddp2p.common.table.peer.TNAME,
						net.ddp2p.common.table.motion.TNAME,
						net.ddp2p.common.table.constituent.TNAME,
						net.ddp2p.common.table.neighborhood.TNAME,
						net.ddp2p.common.table.witness.TNAME)),
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
		if(DEBUG)System.out.println("BroadcastQueue::Inside getNext()");
		do {
			if(DEBUG)System.out.println("BroadcastQueue::Inside getNext():inside do-while");
			if((rnd < net.ddp2p.common.wireless.Broadcasting_Probabilities.get_broadcast_queue_br()) && (net.ddp2p.common.wireless.Broadcasting_Probabilities._broadcast_queue_br)){
				result = b_r.getNext(WirelessLog.Recent_queue,msg_c);
				if(DEBUG)System.out.println("getnext : broadcast_queue_br : "+result);
				if(result!=null) {
					break;
				}
			}
			if((rnd < net.ddp2p.common.wireless.Broadcasting_Probabilities.get_broadcast_queue_bh()) && (net.ddp2p.common.wireless.Broadcasting_Probabilities._broadcast_queue_bh)){
				result = b_h.getNext(WirelessLog.Handled_queue,msg_c);
				if(DEBUG)System.out.println("getnext : broadcast_queue_bh : "+result);
				if(result!=null) {
					break;
				}
			}
			if((rnd < net.ddp2p.common.wireless.Broadcasting_Probabilities.get_broadcast_queue_re())&&(net.ddp2p.common.wireless.Broadcasting_Probabilities._broadcast_queue_re)){
				result = b_re.getNext(bq, msg_c);
				if(DEBUG)System.out.println("getnext : broadcast_queue_re : "+result);
				if(result!=null) {
					break;
				}
			}
			if((rnd < net.ddp2p.common.wireless.Broadcasting_Probabilities.get_broadcast_queue_c())&&(net.ddp2p.common.wireless.Broadcasting_Probabilities._broadcast_queue_c)){
				if(DEBUG)System.out.println("getnext : broadcast_queue_c selected!");
				result = b_c.getNext(WirelessLog.Circular_queue,msg_c);
				if(DEBUG)System.out.println("getnext : broadcast_queue_c : "+result);
				if(result!=null) {
					break;
				}
			}
			if((rnd < net.ddp2p.common.wireless.Broadcasting_Probabilities.get_broadcast_queue_md()) && (net.ddp2p.common.wireless.Broadcasting_Probabilities._broadcast_queue_md)){
				result = b_md.getNext(WirelessLog.MyData_queue,msg_c);
				if(DEBUG)System.out.println("getnext : broadcast_queue_md : "+result);
				if(result!=null) {
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
		}while(result == null);
		if(result==null)	if(_DEBUG)System.out.println("result is null");
		else 	if(_DEBUG)System.out.println("Message available for broadcast!");
		net.ddp2p.common.data.D_Interests_Message dim = new net.ddp2p.common.data.D_Interests_Message();
		if(BroadcastQueueRequested.myInterests==null)  if(_DEBUG)System.out.println("Myinterest is null");
		else{
			if(DEBUG) if(BroadcastQueueRequested.myInterests.org_ID_hashes!=null)System.out.println("myInterest.org size:"+BroadcastQueueRequested.myInterests.org_ID_hashes.size());
		}
		dim.message =result;
		BroadcastQueueRequested.myInterests.Random_peer_number = DD.Random_peer_Number;
		dim.interests = BroadcastQueueRequested.myInterests.encode();
		byte[] x = dim.encode();
		if(x==null)System.out.println("dim is null :"+x);
		return x;
	}
	@Override
	public void loadAll() {
		if(DEBUG)System.out.println("loadAll() : in BroadcastQueues ");
		for(int k=0; k<bq.length; k++) bq[k].loadAll();
	}
	public void registerRecent(PreparedMessage pm, int type) {
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

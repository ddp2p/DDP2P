package net.ddp2p.common.handling_wb;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import net.ddp2p.common.data.D_Interests;
import net.ddp2p.common.simulator.WirelessLog;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.common.wireless.Broadcasting_Probabilities;
public class BroadcastQueueRequested extends BroadcastQueue implements Runnable{
	public static boolean _DEBUG = true;
	public static boolean DEBUG = false;
	private static long min_interest;
	private static long life_span=1000;
	private static long exp_time; 
	public static boolean stopThread=false;
	public static D_Interests myInterests=new D_Interests();
	public static class Received_Interest_Ad
	{
		private String itemGIDhash;
		public long interest_expiration_date;
		public Received_Interest_Ad(String id, long value)
		{
			itemGIDhash = id;
			if(DEBUG)System.out.println("new request GID:"+itemGIDhash);
			interest_expiration_date =value;
			if(DEBUG)System.out.println("new request val:"+interest_expiration_date);
		}
	}
	public static ArrayList<PreparedMessage> requested_PreparedMessages = new ArrayList<PreparedMessage>();
	public static ArrayList<Received_Interest_Ad> rcv_interest_req_org_ID_hashes = new ArrayList<Received_Interest_Ad>();
	public static ArrayList<Received_Interest_Ad> rcv_interest_req_const_ID_hashes = new ArrayList<Received_Interest_Ad>();
	public static ArrayList<Received_Interest_Ad> rcv_interest_req_motion_ID= new ArrayList<Received_Interest_Ad>();
	public static ArrayList<Received_Interest_Ad> rcv_interest_req_neighborhood_ID= new ArrayList<Received_Interest_Ad>();
	public static synchronized boolean interestRequested(ArrayList<Received_Interest_Ad> list, String interest)
	{
		if(DEBUG)System.out.println("interstRequested:: list.size():"+list.size());
		if(interest!=null && list.size()>0 )
		{	
			for(int i=0; i<list.size();i++)
			{
				if(DEBUG)System.out.println("interstRequested:: list.get(i) i=:"+i+" : "+list.get(i).itemGIDhash);
				if(DEBUG)System.out.println("interstRequested:: interest:"+interest);
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
		if(pm==null){
			if(DEBUG)System.out.println("is_requested is returning false pm==null");
			return false;
		}
		if(pm.raw==null){
			if(DEBUG)System.out.println("is_requested is returning false pm.raw==null");
			return false;
		}
		if(DEBUG)System.out.println("is_requested checking org interest| org list size:"+rcv_interest_req_org_ID_hashes.size());
		if(rcv_interest_req_org_ID_hashes.size()>0 && pm.org_ID_hash!=null)
		{
			if(interestRequested(rcv_interest_req_org_ID_hashes,pm.org_ID_hash))
			{
				return true;
			}
		}
		if(DEBUG)System.out.println("is_requested checking motion interest");
		if(rcv_interest_req_motion_ID.size()>0 && pm.motion_ID!=null)
		{
			if(interestRequested(rcv_interest_req_motion_ID,pm.motion_ID))
			{
				return true;
			}
		}
		if(DEBUG)System.out.println("is_requested checking constituent interest");
		if(rcv_interest_req_const_ID_hashes.size()>0 && pm.constituent_ID_hash!=null)
		{
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
		}
		if(DEBUG)System.out.println("is_requested checking neighborhood interest");
		if(rcv_interest_req_neighborhood_ID.size()>0 && pm.neighborhood_ID!=null)
		{
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
		if(DEBUG)System.out.println("is_requested is returning false no match found");
		return false;
	}
	public void run()
	{
		while (!stopThread)
		{
			min_interest = Util.CalendargetInstance().getTimeInMillis();
			exp_time=min_interest; 
			if(DEBUG)System.out.println("Org_Adv_interest_list size:"+rcv_interest_req_org_ID_hashes.size());
			if(DEBUG)System.out.println("Checking for expired requested interests time:"+min_interest);
			synchronized(rcv_interest_req_org_ID_hashes) {
				for(int j=0; j< rcv_interest_req_org_ID_hashes.size();j++)
				{
					if(DEBUG)System.out.println("list size:"+rcv_interest_req_org_ID_hashes.size());
					if(DEBUG)System.out.println("current time:"+exp_time+" expiration time:"+rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date);
					if(rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date < exp_time ){
						if(DEBUG)System.out.println("current time:"+exp_time+" expiration time:"+rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date);
						rcv_interest_req_org_ID_hashes.remove(j);
						if(DEBUG)System.out.println("one of the Organization requests has expired");
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
	}
	@Override
	public void loadAll() {
		min_interest=Util.CalendargetInstance().getTimeInMillis();
		String org_hID[]=new String[10];
		org_hID[0]="O:SHA-1:tqjvuQcxaLPgkvqKI12P5DJGqsU=";
		org_hID[1]="O:SHA-1:1eGoFFfxCtH1ySLqNLlQv8YoNHM=";
		org_hID[2]="O:SHA-1:rx/avJadeOwEi7mm2XC0usXvgjg=";
		org_hID[3]="O:SHA-1:hrUTsG13zGZg3OlmJBOuKD7JmVo=";
		org_hID[4]="O:SHA-1:6heYYHY3jSw64TVo2fMMcj+9SNo=";
		org_hID[5]="O:SHA-1:rmWezQ8pKFkxnyxoUDdAyZXc+Ew=";
		org_hID[6]="O:SHA-1:b4+0FNOwkFc7X6QfI+O5g8PzrO4=";
		org_hID[7]="O:SHA-1:8b52GgWFYEcef5QUq90V9j6Fk7c=";
		org_hID[8]="O:SHA-1:y1UXlDoB/JvB4QH7Av0zAqfVivo=";
		org_hID[9]="O:SHA-1:07XxGGG2EoElNXjK+pAxJOJIfBI=";
		if(DEBUG)System.out.println("inside loadAll()");
		registerRequest(null);
		Received_Interest_Ad re; 
		int interests_already_in_queue=0;
		int x=0;
		while(x<interests_already_in_queue)
		{
			float rand_no =Util.random(1.f);
			min_interest=Util.CalendargetInstance().getTimeInMillis();
			if(rand_no < 0.1){
				re = new Received_Interest_Ad(org_hID[0], min_interest+life_span_peer(null));
				boolean exists=false;
				synchronized(rcv_interest_req_org_ID_hashes){
					for(int j=0; j< rcv_interest_req_org_ID_hashes.size();j++)
					{
						if(rcv_interest_req_org_ID_hashes.get(j).itemGIDhash.equals(re.itemGIDhash) ){
							if(rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date < re.interest_expiration_date)
							{
								Received_Interest_Ad item = rcv_interest_req_org_ID_hashes.get(j);
								item.interest_expiration_date = re.interest_expiration_date;
								rcv_interest_req_org_ID_hashes.remove(j);
								Util.insertSort(rcv_interest_req_org_ID_hashes, item, 0, j);
							}
							exists =true;
							break;
						}
					}
					if(!exists)
					{
						Util.insertSort(rcv_interest_req_org_ID_hashes,re,0,rcv_interest_req_org_ID_hashes.size());
						x++;
					}
				}
			}else if(rand_no < 0.2){
				re = new Received_Interest_Ad(org_hID[1], min_interest+life_span_peer(null));
				boolean exists=false;
				synchronized(rcv_interest_req_org_ID_hashes){
					for(int j=0; j< rcv_interest_req_org_ID_hashes.size();j++)
					{
						if(rcv_interest_req_org_ID_hashes.get(j).itemGIDhash.equals(re.itemGIDhash) ){
							if(rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date < re.interest_expiration_date)
							{
								Received_Interest_Ad item = rcv_interest_req_neighborhood_ID.get(j);
								item.interest_expiration_date = re.interest_expiration_date;
								rcv_interest_req_org_ID_hashes.remove(j);
								Util.insertSort(rcv_interest_req_org_ID_hashes, item, 0, j);
							}
							exists =true;
							break;
						}
					}
					if(!exists){
						Util.insertSort(rcv_interest_req_org_ID_hashes,re,0,rcv_interest_req_org_ID_hashes.size());
						x++;
					}
				}
			}else if(rand_no < 0.3)
			{
				re = new Received_Interest_Ad(org_hID[2], min_interest+life_span_peer(null));
				boolean exists=false;
				synchronized(rcv_interest_req_org_ID_hashes){
					for(int j=0; j< rcv_interest_req_org_ID_hashes.size();j++)
					{
						if(rcv_interest_req_org_ID_hashes.get(j).itemGIDhash.equals(re.itemGIDhash) ){
							if(rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date < re.interest_expiration_date)
							{
								Received_Interest_Ad item = rcv_interest_req_org_ID_hashes.get(j);
								item.interest_expiration_date = re.interest_expiration_date;
								rcv_interest_req_org_ID_hashes.remove(j);
								Util.insertSort(rcv_interest_req_org_ID_hashes, item, 0, j);
							}
							exists =true;
							break;
						}
					}
					if(!exists){
						Util.insertSort(rcv_interest_req_org_ID_hashes,re,0,rcv_interest_req_org_ID_hashes.size());
						x++;
					}
				}
			}else if(rand_no < 0.4)
			{
				re = new Received_Interest_Ad(org_hID[3],min_interest+ life_span_peer(null));
				boolean exists=false;
				synchronized(rcv_interest_req_org_ID_hashes){
					for(int j=0; j< rcv_interest_req_org_ID_hashes.size();j++)
					{
						if(rcv_interest_req_org_ID_hashes.get(j).itemGIDhash.equals(re.itemGIDhash) ){
							if(rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date < re.interest_expiration_date)
							{
								Received_Interest_Ad item = rcv_interest_req_org_ID_hashes.get(j);
								item.interest_expiration_date = re.interest_expiration_date;
								rcv_interest_req_org_ID_hashes.remove(j);
								Util.insertSort(rcv_interest_req_org_ID_hashes, item, 0, j);
							}
							exists =true;
							break;
						}
					}
					if(!exists){
						Util.insertSort(rcv_interest_req_org_ID_hashes,re,0,rcv_interest_req_org_ID_hashes.size());
						x++;
					}
				}
			}else if(rand_no < 0.5)
			{
				re = new Received_Interest_Ad(org_hID[4], min_interest+life_span_peer(null));
				boolean exists=false;
				synchronized(rcv_interest_req_org_ID_hashes){
					for(int j=0; j< rcv_interest_req_org_ID_hashes.size();j++)
					{
						if(rcv_interest_req_org_ID_hashes.get(j).itemGIDhash.equals(re.itemGIDhash) ){
							if(rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date < re.interest_expiration_date)
							{
								Received_Interest_Ad item = rcv_interest_req_org_ID_hashes.get(j);
								item.interest_expiration_date = re.interest_expiration_date;
								rcv_interest_req_org_ID_hashes.remove(j);
								Util.insertSort(rcv_interest_req_org_ID_hashes, item, 0, j);
							}
							exists =true;
							break;
						}
					}
					if(!exists){
						Util.insertSort(rcv_interest_req_org_ID_hashes,re,0,rcv_interest_req_org_ID_hashes.size());
						x++;
					}
				}
			}else if(rand_no < 0.6)
			{
				re = new Received_Interest_Ad(org_hID[5], min_interest+life_span_peer(null));
				boolean exists=false;
				synchronized(rcv_interest_req_org_ID_hashes){
					for(int j=0; j< rcv_interest_req_org_ID_hashes.size();j++)
					{
						if(rcv_interest_req_org_ID_hashes.get(j).itemGIDhash.equals(re.itemGIDhash) ){
							if(rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date < re.interest_expiration_date)
							{
								Received_Interest_Ad item = rcv_interest_req_org_ID_hashes.get(j);
								item.interest_expiration_date = re.interest_expiration_date;
								rcv_interest_req_org_ID_hashes.remove(j);
								Util.insertSort(rcv_interest_req_org_ID_hashes, item, 0, j);
							}
							exists =true;
							break;
						}
					}
					if(!exists){
						Util.insertSort(rcv_interest_req_org_ID_hashes,re,0,rcv_interest_req_org_ID_hashes.size());
						x++;
					}
				}
			}else if(rand_no < 0.7)
			{
				re = new Received_Interest_Ad(org_hID[6],min_interest+ life_span_peer(null));
				boolean exists=false;
				synchronized(rcv_interest_req_org_ID_hashes){
					for(int j=0; j< rcv_interest_req_org_ID_hashes.size();j++)
					{
						if(rcv_interest_req_org_ID_hashes.get(j).itemGIDhash.equals(re.itemGIDhash) ){
							if(rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date < re.interest_expiration_date)
							{
								Received_Interest_Ad item = rcv_interest_req_org_ID_hashes.get(j);
								item.interest_expiration_date = re.interest_expiration_date;
								rcv_interest_req_org_ID_hashes.remove(j);
								Util.insertSort(rcv_interest_req_org_ID_hashes, item, 0, j);
							}
							exists =true;
							break;
						}
					}
					if(!exists){
						Util.insertSort(rcv_interest_req_org_ID_hashes,re,0,rcv_interest_req_org_ID_hashes.size());
						x++;
					}
				}
			}else if(rand_no < 0.8)
			{
				re = new Received_Interest_Ad(org_hID[7], min_interest+life_span_peer(null));
				boolean exists=false;
				synchronized(rcv_interest_req_org_ID_hashes){
					for(int j=0; j< rcv_interest_req_org_ID_hashes.size();j++)
					{
						if(rcv_interest_req_org_ID_hashes.get(j).itemGIDhash.equals(re.itemGIDhash) ){
							if(rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date < re.interest_expiration_date)
							{
								Received_Interest_Ad item = rcv_interest_req_org_ID_hashes.get(j);
								item.interest_expiration_date = re.interest_expiration_date;
								rcv_interest_req_org_ID_hashes.remove(j);
								Util.insertSort(rcv_interest_req_org_ID_hashes, item, 0, j);
							}
							exists =true;
							break;
						}
					}
					if(!exists){
						Util.insertSort(rcv_interest_req_org_ID_hashes,re,0,rcv_interest_req_org_ID_hashes.size());
						x++;
					}
				}
			}else if(rand_no < 0.9)
			{
				re = new Received_Interest_Ad(org_hID[8], min_interest+life_span_peer(null));
				boolean exists=false;
				synchronized(rcv_interest_req_org_ID_hashes){
					for(int j=0; j< rcv_interest_req_org_ID_hashes.size();j++)
					{
						if(rcv_interest_req_org_ID_hashes.get(j).itemGIDhash.equals(re.itemGIDhash) ){
							if(rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date < re.interest_expiration_date)
							{
								Received_Interest_Ad item = rcv_interest_req_org_ID_hashes.get(j);
								item.interest_expiration_date = re.interest_expiration_date;
								rcv_interest_req_org_ID_hashes.remove(j);
								Util.insertSort(rcv_interest_req_org_ID_hashes, item, 0, j);
							}
							exists =true;
							break;
						}
					}
					if(!exists){
						Util.insertSort(rcv_interest_req_org_ID_hashes,re,0,rcv_interest_req_org_ID_hashes.size());
						x++;
					}
				}
			}else if(rand_no < 1.0)
			{
				re = new Received_Interest_Ad(org_hID[9], min_interest+life_span_peer(null));
				boolean exists=false;
				synchronized(rcv_interest_req_org_ID_hashes){
					for(int j=0; j< rcv_interest_req_org_ID_hashes.size();j++)
					{
						if(rcv_interest_req_org_ID_hashes.get(j).itemGIDhash.equals(re.itemGIDhash) ){
							if(rcv_interest_req_org_ID_hashes.get(j).interest_expiration_date < re.interest_expiration_date)
							{
								Received_Interest_Ad item = rcv_interest_req_org_ID_hashes.get(j);
								item.interest_expiration_date = re.interest_expiration_date;
								rcv_interest_req_org_ID_hashes.remove(j);
								Util.insertSort(rcv_interest_req_org_ID_hashes, item, 0, j);
							}
							exists =true;
							break;
						}
					}
					if(!exists){
						Util.insertSort(rcv_interest_req_org_ID_hashes,re,0,rcv_interest_req_org_ID_hashes.size());
						x++;
					}
				}
			}
		}
		if(DEBUG)System.out.println("BroadcastQueueRequested:loadAll: rcv_interest_req_org_ID_hashes size():"+rcv_interest_req_org_ID_hashes.size());
		if(DEBUG)System.out.println("BroadcastQueueRequested:loadAll myInterests.org_ID_hashes size():"+myInterests.org_ID_hashes.size());
		new Thread(new BroadcastQueueRequested()).start();
	}
	public void registerRequest(RequestData rq) {
		if(DEBUG)System.out.println("BroadcastQueueRequested:registerRequest: inside registerRequest");
		myInterests = new D_Interests();
		if(rq==null)rq = new RequestData();
		myInterests.motion_ID = rq.moti;
		myInterests.neighborhood_ID = rq.neig;
		myInterests.org_ID_hashes = rq.orgs;
		String org_hID[]=new String[10];
		org_hID[0]="O:SHA-1:tqjvuQcxaLPgkvqKI12P5DJGqsU=";
		org_hID[1]="O:SHA-1:1eGoFFfxCtH1ySLqNLlQv8YoNHM=";
		org_hID[2]="O:SHA-1:rx/avJadeOwEi7mm2XC0usXvgjg=";
		org_hID[3]="O:SHA-1:hrUTsG13zGZg3OlmJBOuKD7JmVo=";
		org_hID[4]="O:SHA-1:6heYYHY3jSw64TVo2fMMcj+9SNo=";
		org_hID[5]="O:SHA-1:rmWezQ8pKFkxnyxoUDdAyZXc+Ew=";
		org_hID[6]="O:SHA-1:b4+0FNOwkFc7X6QfI+O5g8PzrO4=";
		org_hID[7]="O:SHA-1:8b52GgWFYEcef5QUq90V9j6Fk7c=";
		org_hID[8]="O:SHA-1:y1UXlDoB/JvB4QH7Av0zAqfVivo=";
		org_hID[9]="O:SHA-1:07XxGGG2EoElNXjK+pAxJOJIfBI=";
		int x=0;
		int my_defaulf_interest_count=-1;
		while(x<=my_defaulf_interest_count)
		{
			float rand_no =Util.random(1.f);
			if(rand_no < 0.1){				
				if(x==0){myInterests.org_ID_hashes.add(org_hID[0]);x++;}
				else{
					for(int index=0;index<myInterests.org_ID_hashes.size();index++)
					{
						if(!myInterests.org_ID_hashes.get(index).equals(org_hID[0]))
						{
							myInterests.org_ID_hashes.add(org_hID[0]);x++;
						}
					}
				}
			}else if(rand_no < 0.2){
				if(x==0){myInterests.org_ID_hashes.add(org_hID[1]);x++;}
				else{
					for(int index=0;index<myInterests.org_ID_hashes.size();index++)
					{
						if(!myInterests.org_ID_hashes.get(index).equals(org_hID[1]))
						{
							myInterests.org_ID_hashes.add(org_hID[1]);x++;
						}
					}
				}
			}else if(rand_no < 0.3)
			{
				if(x==0){myInterests.org_ID_hashes.add(org_hID[2]);x++;}
				else{
					for(int index=0;index<myInterests.org_ID_hashes.size();index++)
					{
						if(!myInterests.org_ID_hashes.get(index).equals(org_hID[2]))
						{
							myInterests.org_ID_hashes.add(org_hID[2]);x++;
						}
					}
				}
			}else if(rand_no < 0.4)
			{
				if(x==0){myInterests.org_ID_hashes.add(org_hID[3]);x++;}
				else{
					for(int index=0;index<myInterests.org_ID_hashes.size();index++)
					{
						if(!myInterests.org_ID_hashes.get(index).equals(org_hID[3]))
						{
							myInterests.org_ID_hashes.add(org_hID[3]);x++;
						}
					}
				}
			}else if(rand_no < 0.5)
			{
				if(x==0){myInterests.org_ID_hashes.add(org_hID[4]);x++;}
				else{
					for(int index=0;index<myInterests.org_ID_hashes.size();index++)
					{
						if(!myInterests.org_ID_hashes.get(index).equals(org_hID[4]))
						{
							myInterests.org_ID_hashes.add(org_hID[4]);x++;
						}
					}
				}
			}else if(rand_no < 0.6)
			{
				if(x==0){myInterests.org_ID_hashes.add(org_hID[5]);x++;}
				else{
					for(int index=0;index<myInterests.org_ID_hashes.size();index++)
					{
						if(!myInterests.org_ID_hashes.get(index).equals(org_hID[5]))
						{
							myInterests.org_ID_hashes.add(org_hID[5]);x++;
						}
					}
				}
			}else if(rand_no < 0.7)
			{
				if(x==0){myInterests.org_ID_hashes.add(org_hID[6]);x++;}
				else{
					for(int index=0;index<myInterests.org_ID_hashes.size();index++)
					{
						if(!myInterests.org_ID_hashes.get(index).equals(org_hID[6]))
						{
							myInterests.org_ID_hashes.add(org_hID[6]);x++;
						}
					}
				}
			}else if(rand_no < 0.8)
			{
				if(x==0){myInterests.org_ID_hashes.add(org_hID[7]);x++;}
				else{
					for(int index=0;index<myInterests.org_ID_hashes.size();index++)
					{
						if(!myInterests.org_ID_hashes.get(index).equals(org_hID[7]))
						{
							myInterests.org_ID_hashes.add(org_hID[7]);x++;
						}
					}
				}
			}else if(rand_no < 0.9)
			{
				if(x==0){myInterests.org_ID_hashes.add(org_hID[8]);x++;}
				else{
					for(int index=0;index<myInterests.org_ID_hashes.size();index++)
					{
						if(!myInterests.org_ID_hashes.get(index).equals(org_hID[8]))
						{
							myInterests.org_ID_hashes.add(org_hID[8]);x++;
						}
					}
				}
			}else if(rand_no < 1.0)
			{
				if(x==0){myInterests.org_ID_hashes.add(org_hID[9]);x++;}
				else{
					for(int index=0;index<myInterests.org_ID_hashes.size();index++)
					{
						if(!myInterests.org_ID_hashes.get(index).equals(org_hID[9]))
						{
							myInterests.org_ID_hashes.add(org_hID[9]);x++;
						}
					}
				}
			}
		}
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
		Util.printCallPath("Why call this function?");
		System.exit(1);
		return null;
	}
	public byte[] getNext(BroadcastQueue[] queues, long msg_c) {
		byte[] result=null;
		PreparedMessage result_md = new PreparedMessage();
		PreparedMessage result_c = new PreparedMessage();
		PreparedMessage result_r = new PreparedMessage();
		PreparedMessage result_h = new PreparedMessage();
		if(DEBUG)System.out.println("BroadcastQueueRequested::Inside getNext()");
		do{
			float rnd = Util.random(1.f);
			if(DEBUG)System.out.print("BroadcastQueueRequested:getNext: rnd "+rnd+" ");
			if(DEBUG)System.out.println("BroadcastQueueRequested:getNext: rcv_interest_req_org_ID_hashes size():"+rcv_interest_req_org_ID_hashes.size());
			if(rcv_interest_req_org_ID_hashes.size() == 0)
			{
				result_c = queues[1].getNext(WirelessLog.Circular_queue,msg_c, result_c);
				if(result_c.raw!=null)result= result_c.raw;
			}else
			{
				if(rnd < 0.25f) 
				{
					if(DEBUG)System.out.println("BroadcastQueueRequested:getNext:: result_r chosen ");
						result_r = queues[3].getNext(WirelessLog.Recent_queue,msg_c, result_r);
						if(DEBUG)System.out.println("BroadcastQueueRequested:getNext result_r.raw:"+result_r.raw);
						if(is_requested(result_r))
						{
							if(result_r.raw!=null)result= result_r.raw;
						}
				}else if(rnd < 0.50f)
				{		
					if(DEBUG)System.out.println("BroadcastQueueRequested:getNext: result_h chosen ");
						result_h = queues[4].getNext(WirelessLog.Handled_queue,msg_c, result_h);
						if(DEBUG)System.out.println("BroadcastQueueRequested:getNext: result_h.raw:"+result_h.raw);
						if(is_requested(result_h))
						{
							if(DEBUG)System.out.println("result_h chosen ");
							if(result_h.raw!=null)result= result_h.raw;
						}
				}else if(rnd < 0.75f)
				{	
					if(DEBUG)System.out.println("BroadcastQueueRequested:getNext: result_c chosen data:"+result_c.raw +" rnd: "+ rnd+" " +" C's prob: "+ net.ddp2p.common.wireless.Broadcasting_Probabilities.get_broadcast_queue_c());
					result_c = queues[1].getNext(WirelessLog.Circular_queue,msg_c, result_c);
					if(DEBUG)System.out.println("BroadcastQueueRequested: result_c.raw:"+result_c.raw);
					if(is_requested(result_c))
					{
						if(result_c.raw!=null)result= result_c.raw;
					}
				}else if(rnd < 1) 
				{	
					if(DEBUG)System.out.println("result_md chosen ");
					result_md = queues[0].getNext(WirelessLog.MyData_queue,msg_c, result_md);
					if(DEBUG)System.out.println("BroadcastQueueRequested: result_md.raw:"+result_md.raw);
					if(is_requested(result_md))
					{
						if(result_md.raw!=null)result= result_md.raw;
					}
				}
			}
		}while(result==null);
		if(DEBUG)System.out.println("BroadcastQueueRequested:getNext: sending result:"+result);
		return result;		
	}
	/**
	 * 
	 * @param interests
	 * @param clientAddress
	 * @param iP
	 */
	public static synchronized void handleNewInterests(D_Interests interests,
			SocketAddress clientAddress, String iP) {
		long crt_time_millis = Util.CalendargetInstance().getTimeInMillis();
		if(interests.org_ID_hashes!=null)for(int i=0;i<interests.org_ID_hashes.size();i++)
		{
			if(i==0)if(DEBUG)System.out.println("BroadcastQueueRequested:handleNewInterests:Received an org interest!! size:"+interests.org_ID_hashes.size());
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
							rcv_interest_req_org_ID_hashes.remove(j);
							Util.insertSort(rcv_interest_req_org_ID_hashes, item, 0, j);
						}
						exists = true;
						break;
					}
				}
				if(DEBUG)System.out.println("BroadcastQueueRequested:handleNewInterests:checking if the interest exists already");
				if(!exists)
				{
					if(DEBUG)System.out.println("BroadcastQueueRequested:handleNewInterests:The interest is new to the DB");
					Util.insertSort(rcv_interest_req_org_ID_hashes,received_interest,0,rcv_interest_req_org_ID_hashes.size());
				}else{
					if(DEBUG)System.out.println("BroadcastQueueRequested:handleNewInterests:The interest is already in the DB size :"+rcv_interest_req_org_ID_hashes.size());
				}
			}
		}
		if(interests.const_ID_hashes!=null)for(int i=0;i<interests.const_ID_hashes.size();i++)
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
							rcv_interest_req_const_ID_hashes.remove(j);
							Util.insertSort(rcv_interest_req_const_ID_hashes, item, 0, j);
						}
						exists =true;
						break;
					}
				}
				if(!exists)Util.insertSort(rcv_interest_req_const_ID_hashes,received_interest,0,rcv_interest_req_const_ID_hashes.size());
			}
		}
		if(interests.motion_ID!=null)for(int i=0;i<interests.motion_ID.size();i++)
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
							rcv_interest_req_motion_ID.remove(j);
							Util.insertSort(rcv_interest_req_motion_ID, item, 0, j);
						}
						exists =true;
						break;
					}
				}
				if(!exists)Util.insertSort(rcv_interest_req_motion_ID,received_interest,0,rcv_interest_req_motion_ID.size());
			}
		}
		if(interests.neighborhood_ID!=null)for(int i=0;i<interests.neighborhood_ID.size();i++)
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
							rcv_interest_req_neighborhood_ID.remove(j);
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

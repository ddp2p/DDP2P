package Dos;
import hds.ASNSyncPayload;
import hds.ASNSyncRequest;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import streaming.RequestData;
import streaming.SpecificRequest;
import util.DBInterface;
import util.P2PDDSQLException;
import ASN1.Encoder;
import config.Application;
	 
public class MySending1 extends Thread {


	private int Count = 1;
    protected DatagramSocket dSocket;
    private String broadcastIP;
    
    private ServerSocket socket;
    private Socket client;
    String ss;
    public MySending1(String broadcast)
    {
         broadcastIP = broadcast;
    }
   
    
    
    public void run()
    {
    	//============================================================================
        int i =2 ;// IH I can make it to go over all Org IDs.
        String sql2 = "SELECT "+table.organization.global_organization_ID_hash+" FROM "
        +table.organization.TNAME+" Where "+table.organization.organization_ID+"="+i;
        ArrayList<ArrayList<Object>> _global_org_ID_hash = null;
		
        try {
        	Application.db = new DBInterface(MyServer.db);
			_global_org_ID_hash = Application.db.select(sql2, new String[]{});
		} catch (P2PDDSQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        String global_org_ID_hash =_global_org_ID_hash.toString();
        
        //System.out.println("The value of global_org_ID_hash String"+global_org_ID_hash);
        
    	ASNSyncPayload _HashToBSend = new ASNSyncPayload();
    	ASNSyncRequest asreq= new ASNSyncRequest();
    	RequestData r = new RequestData();
    	_HashToBSend.advertised = new SpecificRequest();
    	 
    	
//============================================================================
    	    	
         while(Count <= 5)
         {
        	 
			try
              {
     
            //  byte[] byte_HashToBSend = new byte[256];
      //==========================================================
              ss = RStringGen.generateRandomString
 					 (11,RStringGen.Mode.ALPHANUMERIC);
      //==========================================================
              
              r.global_organization_ID_hash = global_org_ID_hash;
              r.cons.put(ss, "20110101103256.000Z");
           	  // IH I just add  this   
           	  r.witn.add(ss);
           	  r.neig.add(ss);
           	  r.just.add(ss);
           	  r.news.add(ss);
           	  r.tran.add(ss);
           	  r.sign.put(ss, "20110101103256.000S");
           	  // End of my Added lines 
 //System.out.println("The values of Request Data are "+r);
           	  
              // HashToBSend is the value that U should send via UDP broadcast
           	  _HashToBSend.advertised.rd.add(r);
           	  Encoder HashToBSend = _HashToBSend.getEncoder();
           	  byte[] byte_HashToBSend =  HashToBSend.getBytes();
           	  //byte_HashToBSend = _byte_HashToBSend;
              System.out.println("\nASUS PEER sending ["+byte_HashToBSend+"] To SONY PEER FOR THE ["
           	                    +Count+"] TIMES\n");
                   
             InetAddress group = InetAddress.getByName(broadcastIP);
             DatagramPacket packet = new DatagramPacket(byte_HashToBSend,
            		                     byte_HashToBSend.length, group, hds.Server.PORT);
                   
             DatagramSocket dSocket = new DatagramSocket(8000);
             dSocket.setBroadcast(true);
             dSocket.send(packet);
             dSocket.close();
             Count++;
                   
             try
             {
                  sleep((long)(1000));
                  
             }
                catch (InterruptedException e)
             {
                  
             }
             }
              catch (Exception e)
              {

              }
         }
    }
    
}























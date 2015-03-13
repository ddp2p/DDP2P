package hds; 


import data.D_DirectoryServerPreapprovedTermsInfo;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Here we store the last message from each source, with source socket, source GID, instance, type of message, request, address 
 * @author msilaghi
 *
 */
public class DirMessage {
	final static String UDP = "UDP";
	final static String TCP = "TCP";
	final static String PING = "Ping";
	final static String EMPTY_PING = "Empty Ping";
	final static String ANNOUNCEMENT = "Announcement";
	final static String ANN_ANSWER = "Announcement Answer";
	final static String REQUEST = "Request";
	final static String REQUEST_ANSWER = "Request Answer";
	public Object msg;
	public String sourceGID; // GID of the sender of the message (as apear in the handeled msg)(request GID)(not used yet)
	public String sourceIP; // ip of the sender of the message (as apear in the handeled msg)
	public String sourceInstance; // Instance of the sender of the message (as apear in the handeled msg)(To be done!!)
	public String MsgType; // announce, ping, answer, type of service(forward, startup, address)
	public DIR_Terms_Preaccepted[] requestTerms ; // embedded terms in the request (change and resend??)
	//D_TermsInfo terms; // negatiated terms
	public DIR_Terms_Preaccepted[] respondTerms ; // Directory-accepted-terms send to the request initiator
	public String requestedPeerGIDhash; // Hash-GID of the requested peer ( the target peer)
	public String initiatorGIDhash; // Hash-GID of the request initator peer ( the initator peer)
	public String timestamp; // recording time for sorting messages 
	public String status; // finding agreement on terms and register the requested peer in table register???
	public String peerAddress; // address of the peer that has to be contacted
	public String peerGID; // GID of peer that has to be contacted
	
	public DirMessage(Object _msg) {
		msg = _msg;
	}
	public String getCurrentDateAndTime(){
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		return ""+dateFormat.format(date);
	}
	public String toString(){
		String rq_terms="";
		String rs_terms="";
		if(requestTerms!=null)
       		for(int i=0; i<requestTerms.length; i++){
       			rq_terms+="\n  terms["+i+"]\n"+ requestTerms[i];
            }
		else rq_terms="nothing";
		if(respondTerms!=null)
       		for(int i=0; i<respondTerms.length; i++){
       			rs_terms+="\n  terms["+i+"]\n"+ respondTerms[i];
            }
		else rs_terms="nothing";
			
		return "message:\n sourseGID="+sourceGID+"\n"+
			   "sourceIP="+sourceIP+"\n"+
			   "sourceInstance="+sourceInstance+"\n"+
			   "MsgType="+MsgType+"\n"+
			   "requestedPeerGIDhash="+requestedPeerGIDhash+"\n"+
			   "initiatorGIDhash="+initiatorGIDhash+"\n"+
			   "timestamp="+timestamp+"\n"+
			   "status="+status+"\n"+
			   "requestTerms="+rq_terms+"\n"+
			   "respondTerms="+rs_terms+"\n" ;
			   
			   
	}
}
package net.ddp2p.common.hds; 
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import net.ddp2p.common.data.D_DirectoryServerPreapprovedTermsInfo;
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
	public String sourceGID; 
	public String sourceIP; 
	public String sourceInstance; 
	public String MsgType; 
	public DIR_Terms_Preaccepted[] requestTerms ; 
	public DIR_Terms_Preaccepted[] respondTerms ; 
	public String requestedPeerGIDhash; 
	public String initiatorGIDhash; 
	public String timestamp; 
	public String status; 
	public String peerAddress; 
	public String peerGID; 
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

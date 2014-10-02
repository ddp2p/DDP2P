package hds;

import ciphersuits.CipherSuit;
import data.D_Peer;

/**
 * Data communicated by a CreatePeer Dialog
 * @author msilaghi
 *
 */
public
class PeerInput {
	public String name;
	public String slogan;
	public CipherSuit cipherSuite;
	public boolean valid;
	public String email;
	public String instance;
	public PeerInput() {
		
	}
	public PeerInput(String name2, String slogan2, String email2) {
		name = name2;
		email = email2;
		slogan = slogan2;
	}
	public PeerInput(String name2, String slogan2, String email2, CipherSuit object) {
		name = name2;
		email = email2;
		slogan = slogan2;
		if(object != null) {
			object = new CipherSuit(null);
		}
		this.cipherSuite = object;
	}
	public String toString() {
		String result = "";
		result += "\n\tname="+name;
		result += "\n\tslogan="+slogan;
		result += "\n\tvalid="+valid;
		result += "\n\temail="+email;
		result += "\n\tinstance="+instance;
		result += "\n\tciphersuit=["+cipherSuite+"]";
		return result;
	}
	public static PeerInput getPeerInput(D_Peer This) {
		PeerInput result = new PeerInput();
		result.email = This.getEmail();
		result.slogan = This.getSlogan();
		result.name = This.getName();
		result.instance = This.getInstance();
		result.cipherSuite = This.getCipherSuite();
		return result;
	}
	public void incName() {
		name = name+" (2)";
	}
}
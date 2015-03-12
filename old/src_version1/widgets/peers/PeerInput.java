package widgets.peers;

import ciphersuits.CipherSuit;

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
		result += "\n\temail="+email;
		result += "\n\tciphersuit="+cipherSuite;
		return result;
	}
}
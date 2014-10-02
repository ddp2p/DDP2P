package ciphersuits;


/**
 * Data communicated with CipherSelection  widget
 * @author msilaghi
 *
 */
public
class CipherSuit {
	public String cipher;
	public int ciphersize;
	public String hash_alg;
	public CipherSuit(){
		
	}
	/**
	 * Initialized to defaults
	 * @param _null
	 */
	public CipherSuit(Object _null){
		cipher = Cipher.ECDSA;
		this.ciphersize = ECDSA.P_521;
		this.hash_alg = Cipher.SHA256;
	}
	public String toString() {
		String result = "";
		result += "\n\tcipher="+cipher;
		result += "\n\tciphersize="+ciphersize;
		result += "\n\thash_alg="+hash_alg;
		return result;
	}
}
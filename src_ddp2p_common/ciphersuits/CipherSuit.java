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
	
	public CipherSuit() {
		
	}
	public CipherSuit(String _cipher, String _hash_alg, int _ciphersize) {
		cipher = _cipher;
		hash_alg = _hash_alg;
		ciphersize = _ciphersize;
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
	public static CipherSuit newCipherSuit(String _cipher, String _hash_alg, int _ciphersize) {
		CipherSuit cs = new CipherSuit();
		cs.cipher = _cipher;
		cs.hash_alg = _hash_alg;
		cs.ciphersize = _ciphersize;
		return cs;
	}	
	public String toString() {
		String result = "";
		result += "\n\tcipher="+cipher;
		result += "\n\tciphersize="+ciphersize;
		result += "\n\thash_alg="+hash_alg;
		return result;
	}
}
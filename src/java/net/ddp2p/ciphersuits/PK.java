/*   Copyright (C) 2012 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
package net.ddp2p.ciphersuits;
import net.ddp2p.ASN1.ASNObj;
abstract public class PK extends ASNObj{
	public String comment;
	/**
	 * Will encrypt as is, just exponentiation
	 * @param m
	 * @return
	 */
	public abstract byte[] encrypt(byte[] m);
	/**
	 * Will pad m and then encrypt it
	 * @param m
	 * @return
	 */
	public abstract byte[] encrypt_pad(byte[] m);
	/**
	 * Will verify that the message 'hashed' is the result of decryption of signature
	 * @param signature
	 * @param hashed (the message)
	 * @return
	 */
	public abstract boolean verify(byte[] signature, byte[]hashed);
	/**
	 * Will hash the message and compare to the unpadded decrypted signature 
	 */
	public abstract boolean verify_unpad_hash(byte[] signature, byte[]message);
	public abstract CipherSuit getCipherSuite();
	public abstract boolean __equals(PK o);
}

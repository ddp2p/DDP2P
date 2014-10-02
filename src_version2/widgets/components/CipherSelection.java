/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014 Marius C. Silaghi
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
/* ------------------------------------------------------------------------- */
package widgets.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


import ciphersuits.Cipher;
import ciphersuits.CipherSuit;
import ciphersuits.Cipher_Sizes;

@SuppressWarnings("serial")
public
class CipherSelection extends JPanel implements DocumentListener, ActionListener {
	private static final boolean DEBUG = false;
	private JComboBox<String> crt_cipher;
	private JComboBox<String> crt_sizes_list;
	private JTextField crt_sizes_int;
	private JComboBox<String> crt_hash_algos;
	public
	CipherSelection(CipherSuit cipherSuite){
		init();
		this.setSelectedCiphetSuite(cipherSuite);
	}
	public
	CipherSelection(){
		init();
	}
	void init() {
	    add(crt_cipher = new JComboBox<String>(ciphersuits.Cipher.getAvailableCiphers()));
	    add(crt_sizes_list = new JComboBox<String>());
	    add(crt_sizes_int = new JTextField());
	    add(crt_hash_algos = new JComboBox<String>());
	    
	    crt_sizes_list.setVisible(false);
	    crt_sizes_int.setVisible(false);
	    
	    crt_cipher.setSelectedItem(Cipher.getDefaultCipher());
	    setSelectedCipher(Cipher.getDefaultCipher());
	    if (crt_sizes_int.isVisible()) {
	    	this.setSelectedSizes(Cipher.getDefaultCipher(),
	    			Cipher.getAvailableSizes(Cipher.getDefaultCipher())._default);
			}
	    else if (crt_sizes_list.isVisible()) {
	    	this.setSelectedSizes(Cipher.getDefaultCipher(),
	    			this.getCrtCipherSize((String)crt_sizes_list.getSelectedItem()));				
	    }	
			
	    crt_cipher.addActionListener(this);
	    crt_sizes_list.addActionListener(this);
	    crt_sizes_int.getDocument().addDocumentListener(this);
	}
	private String getSelectedCipher() {
		return (String) this.crt_cipher.getSelectedItem();
	}
	public void setSelectedCiphetSuite(CipherSuit cipherSuite) {
		if (cipherSuite.cipher == null) return;
		this.setSelectedCipher(cipherSuite.cipher);
		this.setSelectedSizes(cipherSuite.cipher, cipherSuite.ciphersize);
		if (this.crt_hash_algos != null)
			this.crt_hash_algos.setSelectedItem(cipherSuite.hash_alg);
	}
	public
	CipherSuit getSelectedCipherSuite() {
		CipherSuit suit = new CipherSuit();
		suit.cipher = getSelectedCipher();
		if (suit.cipher != null) {
			if (this.crt_sizes_int.isVisible()) {
				suit.ciphersize = getCrtCipherSize();
			}
			else if (this.crt_sizes_list.isVisible()) {
				Object val = this.crt_sizes_list.getSelectedItem();
				if (val != null) {
					suit.ciphersize = getCrtCipherSize((String)val);
				}
			}
			Object hash = this.crt_hash_algos.getSelectedItem();
			suit.hash_alg = (String) hash;
//			if (hash != null) {
//				cad.ciphersuit = Cipher.buildCiphersuitID(cad.ciphersuit, (String)hash);
//			}
		}
		return suit;
	}
	private int getCrtCipherSize(String val) {
		int result = 0;
		String txt = val;
		try{
			result = Integer.parseInt(txt);
			return result;
		}catch(Exception e) {
		}
		try{
			int idx = txt.indexOf("-");
			result = Integer.parseInt(txt.substring(idx+1));
			return result;
		}catch(Exception e) {
		}
		return 0;
	}
	private int getCrtCipherSize() {
		try{
			return Integer.parseInt(this.crt_sizes_int.getText());
		}catch(Exception e) {
			return 0;
		}
	}
	void setSelectedCipher(String cipher) {
		crt_hash_algos.removeAllItems();
		crt_sizes_list.removeAllItems();
		Cipher_Sizes cs = ciphersuits.Cipher.getAvailableSizes(cipher);
		if (cs == null) {
			if (DEBUG) System.out.println("CipherSelection No sizes for: "+cipher+" cs="+cs);
			crt_sizes_list.setVisible (false);
			crt_sizes_int.setVisible (false);
		}
		else if (cs.type == Cipher_Sizes.INT_RANGE) {
			if (DEBUG) System.out.println("CipherSelection int range for: "+cipher+" cs="+cs);
			crt_sizes_list.setVisible (false);
			crt_sizes_int.setVisible (true);
			crt_sizes_int.setText(cs._default+"");
		}
		else if (cs.type == Cipher_Sizes.LIST) {
			if (DEBUG) System.out.println("CipherSelection list range for: "+cipher+" cs="+cs);
			crt_sizes_int.setVisible (false);
			crt_sizes_list.setVisible (true);
			crt_sizes_list.removeAllItems();
			if (cs.range instanceof String[]) {
				String[] l = (String[]) cs.range;
				for (String k : l) {
					crt_sizes_list.addItem(k);
				}
				if ((cs != null) && (cs._default < l.length))
					crt_sizes_list.setSelectedIndex(cs._default);
			}
		}
	}
	void setSelectedSizes(String cipher, int sizes) {
		if(cipher == null) return;
		String[] ha = Cipher.getHashAlgos(cipher, sizes);
		if(ha == null) {
			System.out.println("CipherSelection: No hashes for cipher: "+cipher+" sz="+sizes);
			return;
		}
		if (DEBUG) System.out.println("CipherSelection: setSelectedSizes for cipher: "+cipher+" sz="+sizes+" got:"+ha.length);
		crt_hash_algos.removeAllItems();
		for(String h : ha) 
			crt_hash_algos.addItem(h);
		
		if((ha != null) && (ha.length > 0))
			crt_hash_algos.setSelectedIndex(0);
	}
	private void cipherSizeChanged () {
		int size = this.getCrtCipherSize(); //Integer.parseInt(this.crt_sizes_int.getText());
		this.setSelectedSizes((String) this.crt_cipher.getSelectedItem(), size);		
	}
	@Override
	public void insertUpdate(DocumentEvent e) {
		cipherSizeChanged();
	}
	@Override
	public void removeUpdate(DocumentEvent e) {
		cipherSizeChanged();
	}
	@Override
	public void changedUpdate(DocumentEvent e) {
		cipherSizeChanged();
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.crt_cipher) {
			this.setSelectedCipher((String) this.crt_cipher.getSelectedItem());
		}
		if (e.getSource() == this.crt_sizes_list) {
			Object sel = this.crt_sizes_list.getSelectedItem();
			if(sel == null)
				return;
			this.setSelectedSizes((String) this.crt_cipher.getSelectedItem(),
					//this.crt_sizes_list.getSelectedIndex());
					this.getCrtCipherSize((String)sel));
		}
	}
}
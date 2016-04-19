/*   Copyright (C) 2011 Marius C. Silaghi
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
 package net.ddp2p.widgets.components;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.ddp2p.common.data.DDTranslation;
public
class JTextFieldIconed  extends JTextField implements ComboBoxEditor, DocumentListener {
		URL icon_url;
		IconedItem obj;
		BufferedImage image;
		boolean own=false;
		int x0=7;
		public boolean showIcon=true;
		public boolean translating = true;
		public JTextFieldIconed(){
			super();
			setBorder(BorderFactory.createEmptyBorder());
			Border border = UIManager.getBorder("TextField.border");  
			this.getDocument().addDocumentListener(this);
		}
		public void changedUpdate(DocumentEvent e) {
			if(!own) obj=null;
		}	
		public void removeUpdate(DocumentEvent e) {
			if(!own) obj=null;
		}
		public void insertUpdate(DocumentEvent e) {
			if(!own) obj=null;
		}
		public Component getEditorComponent(){
			return this;
		}
		public Object 	getItem() {
			if(obj!=null)
				return obj;
			if(!translating) return getText();
			return new Translation(getText(), DDTranslation.authorship_lang.lang, DDTranslation.authorship_lang.flavor);
		}
		public void setItem(Object obj) {
			own = true;
			if(obj instanceof IconedItem) {
				this.obj=(IconedItem)obj;
				this.setText(this.obj.toString());
				try{
					this.icon_url=this.obj.getIconURL();
					image = ImageIO.read(icon_url);  
				}catch(Exception e){
					image=null;
					icon_url=null;
				}
			}
			if(obj instanceof String){
				this.obj=null;
				setText((String)obj);
				image=null;
				icon_url=null;
			}
			own=false;
		}
	    protected void paintComponent(Graphics g) {
	    	super.paintComponent(g);
	    	if(showIcon){
	    		if(image==null) return;
	    		int y = (getEditorComponent().getHeight() - image.getHeight())/2;  
	    		g.drawImage(image, getEditorComponent().getWidth()-image.getWidth()-5, y, getEditorComponent()); 	
	    	}
	    }
}

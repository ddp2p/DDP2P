/* ------------------------------------------------------------------------- */
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
/* ------------------------------------------------------------------------- */
 package net.ddp2p.widgets.components;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;


@SuppressWarnings("serial")
public class LabelRenderer extends BasicComboBoxRenderer {
    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {
      if(!(value instanceof IconedItem)){
    	  //System.err.println("RENDERING !ICONITEM:"+value);
    	  return this;
      }
      IconedItem t_value = (IconedItem)value;
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
        if (-1 < index) {
          list.setToolTipText(t_value.getTip());
        }
      } else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
        if (-1 < index) {
            list.setToolTipText(((IconedItem)value).getTip());
          }
      }
      setFont(list.getFont());
      setText((value == null) ? "" : value.toString());
      //if(t_value.lang != null) 
      try{
      	// ((JLabel)this).setIcon(new ImageIcon(t_value.getIconURL()));
    	((JLabel)this).setIcon(t_value.getImageIcon());
      }catch(Exception e){
    	  //e.printStackTrace();
      }
	  //System.err.println("RENDERING: "+value);
     //t_value.jc.setToolTipText(((Translation)value).getTip());
      return this;
    }
}
/*
class JCBIconed  extends BasicComboBoxEditor implements ActionListener, DocumentListener {
	URL icon_url;
	Translation obj;
	BufferedImage image;
	int x0=7;
	JCBIconed(){
		super();
	}
	public void changedUpdate(DocumentEvent e) {}	
	public void removeUpdate(DocumentEvent e) {}
	public void insertUpdate(DocumentEvent e) {}
	public Object 	getItem() {
		return obj;
	}
	public void setItem(Object obj) {
		if(obj instanceof Translation) {
			this.obj=(Translation)obj;
			super.setItem(this.obj.text);
			try{
				this.icon_url=this.obj.getIconURL();
				image = ImageIO.read(icon_url);  
			}catch(Exception e){
				e.printStackTrace();
				image=null;
				icon_url=null;
			}
		}
		if(obj instanceof String){
			this.obj=null;
			super.setItem((String)obj);
		}
	}
	public void actionPerformed(ActionEvent evt) {
		System.err.print("JTextFieldIconed Action: "+evt);
	}
    protected void paint(Graphics g) {
    	getEditorComponent().paintAll(g);
    	if(image==null) return;
        int y = (getEditorComponent().getHeight() - image.getHeight())/2;  
        g.drawImage(image, getEditorComponent().getWidth()-image.getWidth()-5, y, getEditorComponent());  
    }  	
}
*/

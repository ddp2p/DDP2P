/* ------------------------------------------------------------------------- */
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
/* ------------------------------------------------------------------------- */

package hds;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import util.CommEvent;
import util.CommunicationEventListener;

public class Console extends JScrollPane implements CommunicationEventListener {
	private static final long serialVersionUID = 1L;
	JTextArea console = new JTextArea();
	private int MAX_LOG_LENGTH=100000;
	//JScrollPane scroll;
	public Console(){
		//super(console);
		//scroll = new JScrollPane(console);
		setViewportView(console);
		console.setEditable(false);
		//this.add(scroll);
	}
	//@Override
	synchronized public void processCommEvent(CommEvent e) {
		console.append(e+"\n");
		if (console.getDocument().getLength()>MAX_LOG_LENGTH) {
			String doc = console.getText();
			int end = doc.length();
			int begin = end - this.MAX_LOG_LENGTH;
			begin = doc.indexOf("\n", begin);
			doc = doc.substring(begin, end);
			console.setText(doc);
		}
		console.setCaretPosition(console.getDocument().getLength());
	}
}

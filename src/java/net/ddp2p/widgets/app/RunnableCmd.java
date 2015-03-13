/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012
		Author: Khalid Alhamed and Marius Silaghi: msilaghi@fit.edu
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
package net.ddp2p.widgets.app;

import javax.swing.JLabel;

import net.ddp2p.common.util.DDP2P_ServiceRunnable;

/**
 * Sets a text in a label
 * @author msilaghi
 *
 */
public class RunnableCmd extends net.ddp2p.common.util.DDP2P_ServiceRunnable{
	String cmd;
	JLabel crtProcessLabel;
	/**
	 * Sets the text in the label
	 * @param text
	 * @param label_widget
	 */
	public RunnableCmd(String text, Object label_widget) {
		// set as anonymous since it may be running on the Swing Thread
		super("Runnable Cmd", false, false, label_widget);
		crtProcessLabel = (JLabel) label_widget;
		cmd = text;
	}
	public void _run() {
		crtProcessLabel.setText(cmd);
	}
}
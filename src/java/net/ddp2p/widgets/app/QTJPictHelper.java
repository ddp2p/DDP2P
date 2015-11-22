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
 package net.ddp2p.widgets.app;
import java.awt.*; 
import java.io.*;
	public class QTJPictHelper extends Object {
		public
		static byte[] pictStreamToJavaImage (InputStream in)
			throws IOException { 
			Image image = null;
			byte[] buffy = new byte [2048]; 
			int off = 512; 
			int totalRead = 0; 
			int bytesRead = 0;
			while ((bytesRead = in.read (buffy, off, buffy.length-off)) > -1) {
				totalRead += bytesRead;
				off += bytesRead;
				if (off == buffy.length) {
					byte[] buffy2 = new byte [buffy.length * 2];
					System.arraycopy (buffy, 0, buffy2, 0, buffy.length);
					buffy = buffy2;
				}
			}
			return buffy;
		}
}

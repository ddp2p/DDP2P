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
 package util;
import java.awt.*; 
import java.io.*;
/*
import quicktime.*; 
import quicktime.qd.*; 
import quicktime.std.*; 
import quicktime.std.image.*; 
import quicktime.std.movies.media.*;
import quicktime.app.view.*;
*/
	public class QTJPictHelper extends Object {
		public
		static byte[] pictStreamToJavaImage (InputStream in)
			throws IOException { 
			Image image = null;
			// create a buffer for bytes read from stream 
			byte[] buffy = new byte [2048]; 
			// must have empty 512-byte header so GraphicsImporter 
			// will think it's a file 
			int off = 512; 
			int totalRead = 0; 
			// loop, attempting to read as many bytes as will fit 
			// in the array, growing array as necessary 
			int bytesRead = 0;
			while ((bytesRead = in.read (buffy, off, buffy.length-off)) > -1) {

				totalRead += bytesRead;
				off += bytesRead;
				if (off == buffy.length) {
					// reallocate new array
					byte[] buffy2 = new byte [buffy.length * 2];
					System.arraycopy (buffy, 0, buffy2, 0, buffy.length);
					buffy = buffy2;
				}
			}
			return buffy;
		}
		/*
		try {
			// hand it to QTJ GraphicsImporter
			QTSession.open( );
			Pict pict = new Pict (buffy);
			DataRef ref = new DataRef (pict,
						  StdQTConstants.kDataRefQTFileTypeTag,
						  "PICT"); 
			GraphicsImporter gi =
				new GraphicsImporter (StdQTConstants.kQTFileTypePicture);
			gi.setDataReference (ref);  
			QDRect rect = gi.getSourceRect ( );     
			Dimension dim = new Dimension (rect.getWidth( ),
						   rect.getHeight( ));
						   GraphicsImporterDrawer gid = 
				new GraphicsImporterDrawer (gi); 
			QTImageProducer ip = new QTImageProducer (gid, dim);

			// create AWT image  
			image = Toolkit.getDefaultToolkit( ).createImage (ip);

		} catch (QTException qte) {
			qte.printStackTrace( );
		} finally {
			QTSession.close( );
		}
		return image;
	}
	*/
}

/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012
		Author: Khalid Alhamed
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
import javax.imageio.ImageIO;

public class Jai
   {
   /**
    * Display file formats supported by JAI on your platform.
    * e.g BMP, bmp, GIF, gif, jpeg, JPEG, jpg, JPG, png, PNG, wbmp, WBMP
    * @param args not used
    */
   public static void main ( String[] args )
      {
      String[] names = ImageIO.getWriterFormatNames();
      for ( String name: names )
         {
         System.out.println( name );
         }
      }
   }

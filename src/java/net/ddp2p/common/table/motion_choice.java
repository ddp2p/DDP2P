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
package net.ddp2p.common.table;
public class motion_choice {
		public static final String choice_ID = "choice_ID";
		public static final String motion_ID = "motion_ID";
		public static final String choice_Name = "choiceName";
		public static final String shortName = "shortName";
		public static String fields =
			motion_ID +","+
			choice_Name+","+
			shortName+","+
			choice_ID;
		public static final int CH_MOTION_ID = 0;
		public static final int CH_NAME = 1;
		public static final int CH_SHORT_NAME = 2;
		public static final int CH_ID = 3;
		public static final String[] fields_array = fields.split(",");
		public static final int CH_FIELDS = fields_array.length;
		public static final String TNAME = "motion_choice";
}

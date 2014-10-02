/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 
		Author: Osamah Dhannoon
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
package wireless;


import util.P2PDDSQLException;

import config.DD;

public class Refresh extends Thread {
	
	private static final boolean DEBUG = false;
	public static boolean START_REFRESH = true;
	public static final Object wlanmonitor = new Object();
	public void run(){
		String _wireless;
		String old_wireless = null;
		synchronized(Refresh.wlanmonitor){
			//System.out.println("1"+Refresh.START_REFRESH);
			while(START_REFRESH){
				//System.out.println("2"+Refresh.START_REFRESH);
				try {
					_wireless = wireless.Detect_interface.detect_wlan();
					if((_wireless!=null) && !_wireless.equals(old_wireless)) {
						DD.setAppText(DD.APP_NET_INTERFACES, _wireless);
						old_wireless = _wireless;
					}
					if(DEBUG)System.out.println("Refresh : run : ");
					Thread.sleep(DD.ADHOC_REFRESH_TIMEOUT_MILLISECONDS);
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

}

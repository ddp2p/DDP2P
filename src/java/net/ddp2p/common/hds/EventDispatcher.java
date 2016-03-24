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

package net.ddp2p.common.hds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import net.ddp2p.common.util.CommEvent;
import net.ddp2p.common.util.CommunicationEventListener;
import net.ddp2p.common.util.DBListener;

public class EventDispatcher {
    private static final boolean DEBUG = false;
    
	HashSet <CommunicationEventListener>client_listeners=new HashSet<CommunicationEventListener>();
	HashSet <CommunicationEventListener>server_listeners=new HashSet<CommunicationEventListener>();

    
	public synchronized boolean addClientListener(CommunicationEventListener object){
    	//listeners.add(new Listener(object, tables));
    	if(DEBUG) System.out.println("LISTENING: "+object);
    	if (client_listeners.contains(object)) return false;
    	client_listeners.add(object);
    	return true;
    }
    public synchronized void delClientListener(CommunicationEventListener object){
    	client_listeners.remove(object);
    }
    public synchronized void fireClientUpdate(CommEvent e){
    	if(DEBUG) System.out.println("FIRE Client UPDATE");
    	for (CommunicationEventListener l : client_listeners) {
    		try{
    			l.processCommEvent(e);
    		}catch(Exception e2){
    				//e2.printStackTrace();
    		}
    		if(DEBUG) System.out.println("FIRED Client UPDATE to: "+l);
    	}
    	if(DEBUG) System.out.println("FIRED Client UPDATE");
    }
    
	public synchronized boolean addServerListener(CommunicationEventListener object){
    	//listeners.add(new Listener(object, tables));
    	if(DEBUG) System.out.println("LISTENING: "+object);
    	if (server_listeners.contains(object)) return false;
    	server_listeners.add(object);
    	return true;
    }
    public synchronized void delServerListener(CommunicationEventListener object){
    	server_listeners.remove(object);
    }
    public synchronized void fireServerUpdate(CommEvent e){
    	if(DEBUG) System.out.println("FIRE Server UPDATE");
    	for (CommunicationEventListener l : server_listeners) {
    		try{
    			l.processCommEvent(e);
    		}catch(Exception e2){
    			//e2.printStackTrace();
    		}
    		if(DEBUG) System.out.println("FIRED Server UPDATE to: "+l);
    	}
    	if(DEBUG) System.out.println("FIRED Server UPDATE");
    }

}

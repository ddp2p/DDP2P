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

import handling_wb.ReceivedBroadcastableMessages;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;

import simulator.WirelessLog;
import util.Util;

import ASN1.ASN1DecoderFail;

import util.P2PDDSQLException;

import config.DD;

public class BroadcastConsummerBuffer extends Thread {
	static final Object monitor = new Object();
	public static BroadcastConsummerBuffer queue; // = new BroadcastConsummerBuffer();
	public boolean running = true;
	ArrayList<MSG> waiting = new ArrayList<MSG>();
	class MSG{
		byte[] data;
		int offset;
		SocketAddress client;
		int msg_size;
		String IP;
		long counter;
		String Msg_Time;
	}
	public BroadcastConsummerBuffer(){
		this.start();
	}
	public void stopIt(){
		synchronized(monitor){
			running = false;
		}		
	}
	public void resumeIt(){
		synchronized(monitor){
			running = true;
			monitor.notifyAll();
		}		
	}
	/**
	 * Called by BroadcastServer
	 * @param obtained
	 * @param position_start
	 * @param clientAddress
	 * @param msg_size
	 * @param iP
	 * @param cnter_val
	 */
	public void add(byte[] obtained, int position_start,
			SocketAddress clientAddress, int msg_size, String iP,
			long cnter_val, String msg_rcv_time) {
		MSG item = new MSG();
		item.data = obtained;
		item.offset = position_start;
		item.client = clientAddress;
		item.msg_size = msg_size;
		item.IP = iP;
		item.counter = cnter_val;
		item.Msg_Time = msg_rcv_time;
		synchronized(waiting){
			if(waiting.size()<DD.ADHOC_SERVER_CONSUMMER_BUFFER_SIZE){
				//System.out.print(" | ");
				
				waiting.add(item);
				 int memsize=0;
				 for(int k=0; k<waiting.size(); k++) {
					 memsize += waiting.get(k).data.length;
				 }
				WirelessLog.Print_to_BS_log(Util.getGeneralizedTime()+"\t"+waiting.size()+"\t"+memsize);
				waiting.notifyAll();
			}
		}
	}
	public void run(){
		for(;;) {
			try {
				if(!_run()) return;
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	/**
	 * return "false" to stop
	 * @return
	 */
	public boolean _run(){
		synchronized(monitor){
			//System.out.println("Enter monitor block 1");
			while(!running){
				try {
					monitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					return false;
				}
			}
			//System.out.println("Exit monitor block 1");
		}
		MSG item = null;
		try {
			synchronized(waiting){
				//System.out.println("Enter monitor block 2");
				while(waiting.size()==0) waiting.wait();
				item = waiting.remove(0);
				 int memsize=0;
				 for(int k=0; k<waiting.size(); k++) {
					 memsize += waiting.get(k).data.length;
				 }
				 WirelessLog.Print_to_BS_log(Util.getGeneralizedTime()+"\t"+waiting.size()+"\t"+memsize);
				 //System.out.println("Exit monitor block 2");
			}
			byte[] obtained = item.data;
			
			int position_start = item.offset;
			SocketAddress cA = item.client;
			int msg_size = item.msg_size;
			String IP = item.IP;
			long cnter_val = item.counter;
			String Msg_time = item.Msg_Time;
			//System.out.println("Consuming msgs from the buffer calling integrateMessage!");
			ReceivedBroadcastableMessages.integrateMessage(obtained, position_start, cA, msg_size, IP, cnter_val,Msg_time);
			
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}
}
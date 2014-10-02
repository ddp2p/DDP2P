package util;

import config.Application_GUI;

abstract public class DDP2P_ServiceThread extends Thread {
	public Object ctx = null;
	public DDP2P_ServiceThread (String name, boolean daemon) {
		if (name != null) this.setName(name);
		this.setDaemon(daemon);
	}
	public DDP2P_ServiceThread (String name, boolean daemon, Object ctx) {
		if (name != null) this.setName(name);
		this.setDaemon(daemon);
		this.ctx = ctx;
	}
	public static void ping(String msg) {
		Application_GUI.ThreadsAccounting_ping(msg);
	}
	public void run () {
		Application_GUI.ThreadsAccounting_registerThread();
		try {
			_run();
		}catch(Exception e) {
			e.printStackTrace();
		}
		Application_GUI.ThreadsAccounting_unregisterThread();
	}
	abstract public void _run();
}

package util;

import config.Application_GUI;

abstract public class DDP2P_ServiceRunnable implements Runnable {
	public Object ctx;
	String name;
	boolean daemon;
	public DDP2P_ServiceRunnable (String name, boolean daemon) {
		this.name = name;
		this.daemon = daemon;
	}
	public DDP2P_ServiceRunnable (String name, boolean daemon, Object ctx) {
		this.name = name;
		this.daemon = daemon;
		this.ctx = ctx;
	}
	public DDP2P_ServiceRunnable (Object ctx) {
		this.ctx = ctx;
	}
	public Thread start() {
		Thread th = new Thread(this);
		th.setDaemon(daemon);
		if (name != null) th.setName(name);
		th.start();
		return th;
	}
	public static void ping(String msg) {
		Application_GUI.ThreadsAccounting_ping(msg);
	}
	public void run () {
		Application_GUI.ThreadsAccounting_registerThread();//ThreadsAccounting.registerThread();
		try {
			_run();
		}catch(Exception e) {
			e.printStackTrace();
		}
		Application_GUI.ThreadsAccounting_unregisterThread();//ThreadsAccounting.unregisterThread();
	}
	abstract public void _run();
}
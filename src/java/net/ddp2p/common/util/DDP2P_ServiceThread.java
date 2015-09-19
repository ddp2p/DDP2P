package net.ddp2p.common.util;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;

abstract public class DDP2P_ServiceThread extends Thread {
	public Object ctx = null;
	public boolean stop = false;
	public int installation;
	Object getContext() {
		return ctx;
	}
	public DDP2P_ServiceThread (String name, boolean daemon) {
		if (name != null) this.setName(name);
		this.setDaemon(daemon);
		this.installation = Application.getCurrentInstallationFromThread();
	}
	public DDP2P_ServiceThread (String name, boolean daemon, Object ctx) {
		if (name != null) this.setName(name);
		this.setDaemon(daemon);
		this.ctx = ctx;
		this.installation = Application.getCurrentInstallationFromThread();
	}
	public DDP2P_ServiceThread (String name, boolean daemon, Object ctx, int _installation) {
		if (name != null) this.setName(name);
		this.setDaemon(daemon);
		this.ctx = ctx;
		this.installation = _installation;
	}
	public void turnOff() {
		stop = true;
		interrupt();
	}
	public static void ping(String msg) {
		Application_GUI.ThreadsAccounting_ping(msg);
	}
	public void run () {
		Application_GUI.ThreadsAccounting_registerThread();
		Application.registerThreadInstallation(installation);
		try {
			_run();
		}catch(Exception e) {
			e.printStackTrace();
		}
		Application.unregisterThreadInstallation();
		Application_GUI.ThreadsAccounting_unregisterThread();
	}
	abstract public void _run();
}

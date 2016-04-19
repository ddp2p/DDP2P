package net.ddp2p.common.data;
public class D_Peer_SaverThread extends net.ddp2p.common.util.DDP2P_ServiceThread {
	boolean stop = false;
	/**
	 * The next monitor is needed to ensure that two D_Peer_SaverThreadWorker are not concurrently modifying the database,
	 * and no thread is started when it is not needed (since one is already running).
	 */
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	public void turnOff() {
		stop = true;
		this.interrupt();
	}
	public D_Peer_SaverThread() {
		super("D_Peer Saver", true);
	}
	public void _run() {
		for (;;) {
			if (stop) return;
			if (net.ddp2p.common.data.SaverThreadsConstants.getNumberRunningSaverThreads() < SaverThreadsConstants.MAX_NUMBER_CONCURRENT_SAVING_THREADS && D_Peer.getNumberItemsNeedSaving() > 0)
			synchronized(saver_thread_monitor) {
				new D_Peer_SaverThreadWorker().start();
			}
			synchronized(this) {
				try {
					long timeout = (D_Peer.getNumberItemsNeedSaving() > 0)?
							SaverThreadsConstants.SAVER_SLEEP_BETWEEN_PEER_MSEC:
								SaverThreadsConstants.SAVER_SLEEP_WAITING_PEER_MSEC;
					wait(timeout);
				} catch (InterruptedException e) {
				}
			}
		}
	}
}

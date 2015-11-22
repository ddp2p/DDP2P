package net.ddp2p.common.network.stun.keepalive;
public class SynchronizedTime
{
    private int time = 0;
    private boolean timedOut = false;
    private boolean timeChanged = false;
    private boolean threadStop = false; 
    private int controllerId = 0;
    private int observerId = 0;
    private byte[] stunTransactionID = null;
    public SynchronizedTime( int time, boolean timedOut, boolean timeChanged )
    {
        this.time = time;
        this.timedOut = timedOut;
        this.timeChanged = timeChanged;
    }
    public SynchronizedTime()
    {
    }
    public synchronized void setTime( int time ) { this.time = time; }
    public synchronized int getTime() { return time; }
    public synchronized void setTimedOut() { this.timedOut = true; }
    public synchronized void resetTimedOut() { this.timedOut = false; }
    public synchronized boolean isTimedOut() { return timedOut; }
    public synchronized void setTimeChanged() { this.timeChanged = true; }
    public synchronized void resetTimeChanged() { this.timeChanged = false; }
    public synchronized boolean didTimeChange() { return timeChanged; }
    public synchronized void setThreadStop() { this.threadStop = true; }
    public synchronized void resetThreadStop() { this.threadStop = false; } 
    public synchronized boolean shouldStopThread() { return threadStop; }
    public synchronized byte[] getStunTransactionID() { return stunTransactionID; }
    public synchronized void setStunTransactionID( byte[] stunTransactionID )
    {
        this.stunTransactionID = stunTransactionID;
    }
    public int getControllerId() { return controllerId; }
    public void setControllerId( int controllerId ) { this.controllerId = controllerId; }
    public int getObserverId() { return observerId; }
    public void setObserverId( int observerId ) { this.observerId = observerId; }
}

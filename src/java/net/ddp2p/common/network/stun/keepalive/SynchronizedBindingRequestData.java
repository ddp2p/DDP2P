package net.ddp2p.common.network.stun.keepalive;
public class SynchronizedBindingRequestData
{
    private int time = 0;
    private boolean timedOut = false;
    private byte[] stunTransactionID = null;
    public SynchronizedBindingRequestData( int time, boolean timedOut, byte[] stunTransactionID )
    {
        this.time = time;
        this.timedOut = timedOut;
        this.stunTransactionID = stunTransactionID;
    }
    public SynchronizedBindingRequestData() { }
    public synchronized int getTime() { return time; }
    public synchronized void setTime(int time) { this.time = time; }
    public synchronized boolean isTimedOut() { return timedOut; }
    public synchronized void setTimedOut() { this.timedOut = true; }
    public synchronized void resetTimeOut() { this.timedOut = false; }
    public synchronized byte[] getStunTransactionID() { return stunTransactionID;}
    public synchronized void setStunTransactionID(byte[] stunTransactionID)
    {
        this.stunTransactionID = stunTransactionID;
    }
}

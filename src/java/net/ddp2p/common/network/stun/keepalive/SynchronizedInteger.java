package net.ddp2p.common.network.stun.keepalive;

public class SynchronizedInteger
{
    private int value;

    public synchronized int getValue() { return value; }
    public synchronized void setValue( int value ) { this.value = value; }
    public synchronized void increment() { value++; }
}

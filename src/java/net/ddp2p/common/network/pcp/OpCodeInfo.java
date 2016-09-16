package net.ddp2p.common.network.pcp;

// MAP and PEER classes can implement this.
public interface OpCodeInfo
{
    public static final byte PROTOCOL_UDP = 17;
    public static final byte PROTOCOL_TCP = 6;

    public String getName();
    public byte[] getBytes();
    public int getLength();
    public String toString();
}


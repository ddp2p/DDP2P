package net.ddp2p.common.network.stun.stun;
public abstract class Attribute
{
	public static final int HEADER_SIZE = 4;
	public static final int TYPE_MAPPED_ADDRESS = 0x01;
	public static final int TYPE_XOR_MAPPED_ADDRESS = 0x20;
	public static final int TYPE_RESPONSE_PORT = 0x27;
	public static final int TYPE_KEEPALIVE_TIMER = 0x25; 
	short type;  
	short length; 
	public void setType( short type ) { this.type = type; }
	public short getType() { return type; }
	public short getLength()
    {
		calculateLength();
		return length;
	}
	public abstract byte[] getBytes();
	public abstract void parseBytes( byte[] data );
	public abstract void calculateLength();
	public abstract String getName();
}

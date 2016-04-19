package net.ddp2p.common.network.stun.stun;
import java.nio.ByteBuffer;
import net.ddp2p.common.util.Util;
public class MappedAddress extends Attribute
{
	final String name = "MappedAddress";
	byte family;
	int port;
	int address; 
	public MappedAddress()
    {
		this.type = TYPE_MAPPED_ADDRESS ;
	}
	public MappedAddress( byte[] data )
    {
		this.parseBytes( data );
	}
	@Override
    public String getName() { return name; }
	public byte getFamily() { return family; }
	public void setFamily(byte family) { this.family = family; }
	public int getPort() { return port; }
	public void setPort(int port) { this.port = port; }
	public int getAddress() { return address; }
	public void setAddress(int address) { this.address = address; }
	@Override
    public byte[] getBytes()
    {
		calculateLength();
		byte[] value = new byte[12];
		Util.copyBytes( this.type, value, 0, 2 );
		Util.copyBytes( this.length, value, 2, 2 );
		Util.copyBytes( family, value, 5, 1 );
		Util.copyBytes( (short)port, value, 6, 2 );
		Util.copyBytes( address, value, 8, 4);
		return value;
	}
	@Override
    public void parseBytes( byte[] data )
    {
		byte[] temp = new byte[2];
		Util.copyBytes_src_dst( data, 0, temp, 0, 2 );
		this.type = ByteBuffer.allocate( 2 ).put( temp ).getShort( 0 );
		Util.copyBytes_src_dst( data, 2, temp, 0, 2 );
		this.length = ByteBuffer.allocate( 2 ).put( temp ).getShort( 0 );
		Util.copyBytes_src_dst( data, 5, temp, 0, 1 );
		this.family = temp[0];
		Util.copyBytes_src_dst( data, 6, temp, 0, 2 );
		this.port = ByteBuffer.allocate( 2 ).put( temp ).getShort( 0 ) & 0xffff;
		temp = new byte[4];
		Util.copyBytes_src_dst( data, 8, temp, 0, 4 );
		this.address = ByteBuffer.allocate( 4 ).put( temp ).getInt( 0 );
	}
	@Override
    public void calculateLength()
    {
		this.length = 8; 
	}
}

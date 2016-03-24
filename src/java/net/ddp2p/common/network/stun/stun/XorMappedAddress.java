package net.ddp2p.common.network.stun.stun;

import java.nio.ByteBuffer;

import net.ddp2p.common.util.Util;

public class XorMappedAddress extends Attribute
{

	final String name = "XorMappedAddress";

	// Value Items.
	byte family;
	int xPort;
	int xAddress; // 32-bits for IPv4.

	byte[] magicCookie = { 0x21, 0x12, (byte)0xA4, 0x42 }; // used only for reference.

	public XorMappedAddress()
    {
		this.type = TYPE_XOR_MAPPED_ADDRESS;
	}

	public XorMappedAddress( byte[] data )
    {
		this.parseBytes( data );
	}

	// Shortcut to build the XORed address from an existing attribute.
	public XorMappedAddress( MappedAddress mappedAddress )
    {
		this.type = (short)0x20;

		this.family = mappedAddress.getFamily();
		setXPort( mappedAddress.getPort(), false );
		setXAddress( mappedAddress.getAddress(), false );
	}

	@Override
    public String getName() { return name; }

	public byte getFamily() { return family; }
	public void setFamily(byte family) { this.family = family; }

	public int getXPort() { return xPort; }
	public void setXPort(int xPort, boolean argIsXORed)
    {
		if( argIsXORed )
        {
			this.xPort = xPort;
		}
		else
        {
			// XOR with 2 most significant bytes of magic cookie.
			byte[] xPortBytes = ByteBuffer.allocate( 2 ).putShort( (short)xPort ).array();
			byte[] xoredBytes = new byte[2];
			for( int i = 0; i < xPortBytes.length; i++ )
            {
				xoredBytes[i] = (byte)( 0xff &  (xPortBytes[i] ^ magicCookie[i]) );
			}
			this.xPort = ByteBuffer.allocate( 2 ).put( xoredBytes ).getShort( 0 );
		}
	}

	public int getXAddress() { return xAddress; }
	public void setXAddress(int xAddress, boolean argIsXORed)
    {
		if( argIsXORed )
        {
			this.xAddress = xAddress;
		}
		else
        {
			// XOR with the magic cookie.
			byte[] xAddressBytes = ByteBuffer.allocate( 4 ).putInt( xAddress ).array();
			byte[] xoredBytes = new byte[4];
			for( int i = 0; i < xAddressBytes.length; i++ )
            {
				xoredBytes[i] = (byte)( 0xff &  (xAddressBytes[i] ^ magicCookie[i]) );
			}
			this.xAddress = ByteBuffer.allocate( 4 ).put( xoredBytes ).getInt( 0 );
		}
	}

	@Override
    public byte[] getBytes()
    {
		calculateLength();

		byte[] value = new byte[12];
		// Normal Attribute Items.
		Util.copyBytes( this.type, value, 0, 2 );
		Util.copyBytes( this.length, value, 2, 2 );

		//Attribute Specific Items.
		Util.copyBytes( family, value, 5, 1 );
		Util.copyBytes( (short)xPort, value, 6, 2 );
		Util.copyBytes( xAddress, value, 8, 4 );

		return value;
	}

	@Override
    public void parseBytes( byte[] data )
    {
		byte[] temp = new byte[2];
		// Normal Attribute Items.
		Util.copyBytes_src_dst( data, 0, temp, 0, 2 );
		this.type = ByteBuffer.allocate( 2 ).put( temp ).getShort( 0 );
		Util.copyBytes_src_dst( data, 2, temp, 0, 2 );
		this.length = ByteBuffer.allocate( 2 ).put( temp ).getShort( 0 );

		// Attribute Specific items.
		Util.copyBytes_src_dst( data, 5, temp, 0, 1 );
		this.family = temp[0];
		Util.copyBytes_src_dst( data, 6, temp, 0, 2 );
		this.xPort = ByteBuffer.allocate( 2 ).put( temp ).getShort( 0 ) & 0xffff;
		temp = new byte[4];
		Util.copyBytes_src_dst( data, 8, temp, 0, 4 );
		this.xAddress = ByteBuffer.allocate( 4 ).put( temp ).getInt( 0 );
	}

	@Override
    public void calculateLength()
    {
		this.length = 8; // Fixed size for this attribute.
	}
}


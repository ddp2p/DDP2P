package net.ddp2p.common.network.stun.stun;

// Bytes must align along a 32-bit boundary.
// Specific attributes should inherit from this class.
public abstract class Attribute
{
	public static final int HEADER_SIZE = 4;
	public static final int TYPE_MAPPED_ADDRESS = 0x01;
	public static final int TYPE_XOR_MAPPED_ADDRESS = 0x20;
	public static final int TYPE_RESPONSE_PORT = 0x27;

	public static final int TYPE_KEEPALIVE_TIMER = 0x25; // test

	short type;  // 16 bits
	short length; // 16 bits, does not include header or padding bytes

	public void setType( short type ) { this.type = type; }
	public short getType() { return type; }

	public short getLength()
    {
		calculateLength();
		return length;
	}

	// Gets the bytes for the entire attribute to add to the STUN message.
	public abstract byte[] getBytes();
	// Fill in the Attribute values based on a byte array.
	public abstract void parseBytes( byte[] data );
	// Calculate the length of the attribute.
	public abstract void calculateLength();
	// Get the name of the attribute as a string.
	public abstract String getName();
}

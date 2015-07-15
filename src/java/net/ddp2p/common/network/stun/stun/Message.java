package net.ddp2p.common.network.stun.stun;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.ddp2p.common.util.Logger;
import net.ddp2p.common.util.Util;

public class Message
{

	public static final int STUN_HEADER_SIZE = 20; // defined in the RFC.
	public static final int STUN_MAX_IPV4_SIZE = 576;
	public static final int STUN_PORT = 3478;
	public static final short STUN_BINDING_REQUEST =  1;
	public static final short STUN_BINDING_RESPONSE = 257;

	public static final short STUN_CALC_KEEPALIVE_STOP_REQUEST = 0x05;
	public static final short STUN_CALC_KEEPALIVE_REQUEST = 0x06;
	public static final short STUN_CALC_KEEPALIVE_RESPONSE = 0x07;
	public static final short STUN_CALC_KEEPALIVE_RESET_REQUEST = 0x08;

	public static final short STUN_GENTRAF_START_REQUEST = 0x10;
	public static final short STUN_GENTRAF_STOP_REQUEST = 0x11;

	// Values
	byte[] messageType;
	short messageLength;
	byte[] magicCookie = { 0x21, 0x12, (byte)0xA4, 0x42 }; // 32 bits - defined as constant in RFC.
	byte[] transactionID; // 96 bits.
	HashMap<String, Attribute> attributes;

    Logger logger = null;

	// General Methods.
	public Message()
    {
		// NOTE: Transaction id is generated or set separately.
		attributes = new HashMap<String, Attribute>();
        logger = new Logger( true, true, true, true );
	}

	public Message( byte[] data )
    {
        logger = new Logger( true, true, true, true );
		this.parseBytes( data );
	}

	// NOTE: These accessor methods do not make copies of assigned arrays!

	public void setMessageType( byte[] messageType ) { this.messageType = messageType; }
	public void setMessageType( short messageTypeShort )
    {
        messageType = ByteBuffer.allocate( 2 ).putShort( messageTypeShort ).array();
    }

	public byte[] getMessageType() { return messageType; }

	public void setMessageLength( short messageLength ) { this.messageLength = messageLength; }
	public short getMessageLength() { return messageLength; }

	public void setMagicCookie( byte[] magicCookie ) { this.magicCookie = magicCookie; }
	public byte[] getMagicCookie() { return magicCookie; }

	public void setTransactionID( byte[] transactionID ) { this.transactionID = transactionID; }
	public byte[] getTransactionID() { return transactionID; }

	public void addAttribute( String name, Attribute attribute )
    {
        attributes.put( name, attribute );
    }

	public Attribute getAttribute( String attributeType )
    {
        return attributes.get( attributeType );
    }

	// TODO: getAttributes(); - only if needed.

	public void generateTransactionID()
	{
		// Rough method of generating the transaction id
		BigInteger temp = new BigInteger( 96, new Random() );
		transactionID = temp.toByteArray();

		// Since the generated value is always positive, we can drop the initial
		// sign bit. This will make the array the correct size (if needed).
		// Sometimes the result is 13 bytes because of the sign bit.
		// http://docs.oracle.com/javase/7/docs/api/java/math/BigInteger.html#toByteArray%28%29
		// http://stackoverflow.com/a/4408124
		if( transactionID.length > 12 ) // 96 bits.
		{
		    byte[] truncatedID = new byte[ transactionID.length - 1];
		    System.arraycopy( transactionID, 1, truncatedID, 0, truncatedID.length );
		    transactionID = truncatedID;
		}
	}

	public byte[] getBytes()
    {
		// Determine what size buffer is needed for the message.
		for( Map.Entry<String, Attribute> entry : attributes.entrySet() )
        {
			this.messageLength += Attribute.HEADER_SIZE + entry.getValue().getLength();
		}

		// Allocate a buffer of the appropriate size.
		byte[] data = new byte[ STUN_HEADER_SIZE + messageLength ];

		// Get general data.
		Util.copyBytes( messageType, data, 0, 2 );
		Util.copyBytes( messageLength, data, 2, 2 );
		Util.copyBytes( magicCookie, data, 4, 4 );
		Util.copyBytes( transactionID, data, 8, 12 );

		// Get attributes.
		int locationMarker = 20;
		for( Map.Entry<String, Attribute> entry : attributes.entrySet() )
        {
			Util.copyBytes( entry.getValue().getBytes(), data, locationMarker,
                            Attribute.HEADER_SIZE + entry.getValue().getLength() );
			locationMarker += Attribute.HEADER_SIZE + entry.getValue().getLength();
		}

		return data;
	}

	public void parseBytes( byte[] data )
    {
		attributes = new HashMap<String, Attribute>();

		ByteBuffer dataBuffer = ByteBuffer.allocate( Message.STUN_MAX_IPV4_SIZE ).put( data );
		// Parse out the message type and length.
		short type = dataBuffer.getShort( 0 );
		short length = dataBuffer.getShort( 2 );

		this.setMessageType( type );
		this.setMessageLength( length );

//		logger.debug( "Parsing STUN Message -> Type: " + type +", Length: " +  length );

		// Parse out magicCookie and transactionID.
		int magicCookie = dataBuffer.getInt( 4 );
		byte[] transactionID = new byte[12];
		Util.copyBytes_src_dst( data, 8, transactionID, 0, 12 );

		this.setMagicCookie( ByteBuffer.allocate( 4 ).putInt( magicCookie ).array() );
		this.setTransactionID( transactionID );

		// Parse out the attributes.
		int i = 0;
		while( i < length )
        {
			AttributeParseResult temp = parseAttribute( data, length, i );
			this.addAttribute( temp.attribute.getName(), temp.attribute );
			i = temp.index;
		}
	}

	public AttributeParseResult parseAttribute( byte[] data, int messageLength, int index )
    {
		int startPosition = Message.STUN_HEADER_SIZE + index;
		byte[] temp = new byte[ messageLength - index ];
		Util.copyBytes_src_dst( data, startPosition, temp, 0, messageLength - index );
		ByteBuffer dataBuffer = ByteBuffer.allocate( messageLength - index ).put( temp );

		// Get type and length.
		short type = dataBuffer.getShort( 0 );
		short length = dataBuffer.getShort( 2 );

		// Isolate the data for the attribute
		byte[] attributeData = new byte[ Attribute.HEADER_SIZE + length ];
		Util.copyBytes( temp, attributeData, 0, Attribute.HEADER_SIZE + length );

		// Create the attribute based on the type.
		Attribute newAttribute = null;
		switch( type ) {
			case Attribute.TYPE_MAPPED_ADDRESS:
				newAttribute = new MappedAddress( attributeData );
				break;
			case Attribute.TYPE_XOR_MAPPED_ADDRESS:
				newAttribute = new XorMappedAddress( attributeData );
				break;
			case Attribute.TYPE_KEEPALIVE_TIMER:
				newAttribute = new KeepAliveTimer( attributeData );
				break;
			case Attribute.TYPE_RESPONSE_PORT:
			    newAttribute = new ResponsePort( attributeData );
			    break;
			default:
				break;
		}

		AttributeParseResult result = new AttributeParseResult();
		result.attribute = newAttribute;
		result.index = index + Attribute.HEADER_SIZE + length;

		return result;
	}

	public String toByteString()
    {
		byte[] data = getBytes();
		return Util.getBitString( data );
	}

	@Override
    public String toString()
    {
		String result = "";
		result += "messageType: " + Util.getBitString( messageType ) + "\n";
		result += "messageLength: " + Util.getBitString( messageLength, true ) + "\n";
		result += "magicCookie: " + Util.getBitString( magicCookie ) + "\n";
		result += "transactionID: " + Util.getBitString( transactionID ) + "\n";

		result += "Attributes:\n";
		for( Map.Entry<String, Attribute> entry : attributes.entrySet() )
        {
			result += entry.getKey() + " : "
                + Util.getBitString( entry.getValue().getBytes() ) + "\n";
		}
		return result;
	}

	// Internal class to return tuple.
	public class AttributeParseResult
    {
		public Attribute attribute;
		public int index;
	}
}


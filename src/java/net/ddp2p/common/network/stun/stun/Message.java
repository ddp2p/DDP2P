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
	public static final int STUN_HEADER_SIZE = 20; 
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
	byte[] messageType;
	short messageLength;
	byte[] magicCookie = { 0x21, 0x12, (byte)0xA4, 0x42 }; 
	byte[] transactionID; 
	HashMap<String, Attribute> attributes;
    Logger logger = null;
	public Message()
    {
		attributes = new HashMap<String, Attribute>();
        logger = new Logger( true, true, true, true );
	}
	public Message( byte[] data )
    {
        logger = new Logger( true, true, true, true );
		this.parseBytes( data );
	}
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
	public void generateTransactionID()
	{
		BigInteger temp = new BigInteger( 96, new Random() );
		transactionID = temp.toByteArray();
		if( transactionID.length > 12 ) 
		{
		    byte[] truncatedID = new byte[ transactionID.length - 1];
		    System.arraycopy( transactionID, 1, truncatedID, 0, truncatedID.length );
		    transactionID = truncatedID;
		}
	}
	public byte[] getBytes()
    {
		for( Map.Entry<String, Attribute> entry : attributes.entrySet() )
        {
			this.messageLength += Attribute.HEADER_SIZE + entry.getValue().getLength();
		}
		byte[] data = new byte[ STUN_HEADER_SIZE + messageLength ];
		Util.copyBytes( messageType, data, 0, 2 );
		Util.copyBytes( messageLength, data, 2, 2 );
		Util.copyBytes( magicCookie, data, 4, 4 );
		Util.copyBytes( transactionID, data, 8, 12 );
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
		short type = dataBuffer.getShort( 0 );
		short length = dataBuffer.getShort( 2 );
		this.setMessageType( type );
		this.setMessageLength( length );
		int magicCookie = dataBuffer.getInt( 4 );
		byte[] transactionID = new byte[12];
		Util.copyBytes_src_dst( data, 8, transactionID, 0, 12 );
		this.setMagicCookie( ByteBuffer.allocate( 4 ).putInt( magicCookie ).array() );
		this.setTransactionID( transactionID );
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
		short type = dataBuffer.getShort( 0 );
		short length = dataBuffer.getShort( 2 );
		byte[] attributeData = new byte[ Attribute.HEADER_SIZE + length ];
		Util.copyBytes( temp, attributeData, 0, Attribute.HEADER_SIZE + length );
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
	public class AttributeParseResult
    {
		public Attribute attribute;
		public int index;
	}
}

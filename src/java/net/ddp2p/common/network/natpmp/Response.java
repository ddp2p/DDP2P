package net.ddp2p.common.network.natpmp;
import java.nio.ByteBuffer;
import net.ddp2p.common.util.Logger;
import net.ddp2p.common.util.Util;
public class Response
{
    public static final int NATPMP_MAX_SIZE = 16; 
    public static final int NATPMP_SERVER_PORT = 5351;
    public static final int NATPMP_CLIENT_PORT = 5350;
    public static final byte NATPMP_OPCODE_UDP = 1;
    public static final byte NATPMP_OPCODE_TCP = 2;
    public static final byte NATPMP_PROTOCOL_VERSION = 0;
    public enum ResponseType
    {
         MAPPING_RESPONSE
        ,ADDRESS_INFO_RESPONSE
    }
    private ResponseType type;
    private int version = NATPMP_PROTOCOL_VERSION;
    private byte opCode;
    private short resultCode;
    private int secondsSinceEpoch;
    private int internalPort;
    private int mappedExternalPort;
    private int lifetime;
    private byte[] externalIPv4Address; 
    Logger logger = null;
    public ResponseType getType() { return type; }
    public int getVersion() { return version; }
    public void setVersion( int version ) { this.version = version; }
    public byte getOpCode() { return opCode; }
    public void setOpCode( byte opCode ) { this.opCode = opCode; }
    public short getResultCode() { return resultCode; }
    public void setResultCode( short resultCode ) { this.resultCode = resultCode; }
    public int getSecondsSinceEpoch() { return secondsSinceEpoch; }
    public void setSecondsSinceEpoch( int secondsSinceEpoch )
    {
        this.secondsSinceEpoch = secondsSinceEpoch;
    }
    public int getInternalPort() { return internalPort; }
    public void setInternalPort( int internalPort ) { this.internalPort = internalPort; }
    public int getMappedExternalPort() { return mappedExternalPort; }
    public void setMappedExternalPort( int suggestedExternalPort )
    {
        this.mappedExternalPort = suggestedExternalPort;
    }
    public int getLifetime() { return lifetime; }
    public void setLifetime( int requestedLifetime )
    {
        this.lifetime = requestedLifetime;
    }
    public byte[] getExternalIPv4Address() { return externalIPv4Address; }
    public void setExternalIPv4Address( byte[] externalIpv4Address )
    {
        this.externalIPv4Address = externalIpv4Address;
    }
    public Response( ResponseType responseType)
    {
        this.type = responseType;
        logger = new Logger( true, true, true, true );
    }
    public Response( byte[] data, ResponseType responseType )
    {
        this.type = responseType;
        logger = new Logger( true, true, true, true );
        this.parseBytes( data );
    }
    public byte[] getBytes()
    {
        byte[] data = null;
        switch( type )
        {
            case MAPPING_RESPONSE:
                data = new byte[ 16 ];
                Util.copyBytes( (byte)version, data, 0, 1 );
                Util.copyBytes( opCode, data, 1, 1 );
                Util.copyBytes( resultCode, data, 2, 2 );
                Util.copyBytes( secondsSinceEpoch, data, 4, 4 );
                Util.copyBytes( (short)internalPort, data, 8, 2 );
                Util.copyBytes( (short)mappedExternalPort, data, 10, 2 );
                Util.copyBytes( lifetime, data, 12, 4 );
                break;
            case ADDRESS_INFO_RESPONSE:
                data = new byte[ 12 ];
                Util.copyBytes( (byte)version, data, 0, 1 );
                Util.copyBytes( opCode, data, 1, 1 );
                Util.copyBytes( resultCode, data, 2, 2 );
                Util.copyBytes( secondsSinceEpoch, data, 4, 4 );
                Util.copyBytes( externalIPv4Address, data, 8, 4 );
                break;
        }
        return data;
    }
    public void parseBytes( byte[] data )
    {
        ByteBuffer dataBuffer = ByteBuffer.allocate( NATPMP_MAX_SIZE ).put( data );
        byte version = dataBuffer.get( 0 );
        byte opCode = dataBuffer.get( 1 );
        short resultCode = dataBuffer.getShort( 2 );
        int secondsSinceEpoch = dataBuffer.getInt( 4 );
        setVersion( version );
        setOpCode( opCode  );
        setResultCode( resultCode );
        setSecondsSinceEpoch( secondsSinceEpoch );
        switch( type )
        {
            case MAPPING_RESPONSE:
                int internalPort = dataBuffer.getShort( 8 ) & 0xffff; 
                int mappedExternalPort = dataBuffer.getShort( 10 ) & 0xffff;
                int lifetime = dataBuffer.getInt( 12 );
                setInternalPort( internalPort );
                setMappedExternalPort( mappedExternalPort );
                setLifetime( lifetime );
                break;
            case ADDRESS_INFO_RESPONSE:
                externalIPv4Address = new byte[4];
                Util.copyBytes_src_dst( data, 8, externalIPv4Address, 0, 4 );
                break;
        }
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
        result += "version: " + version + "\n";
        result += "opCode: " + (opCode & 0xff)  + "\n";
        result += "resultCode: " + resultCode + "\n";
        result += "secondsSinceEpoch: " + secondsSinceEpoch + "\n";
        switch( type )
        {
            case MAPPING_RESPONSE:
                result += "internalPort: " + internalPort + "\n";
                result += "mappedExternalPort: " + mappedExternalPort + "\n";
                result += "lifetime: " + lifetime + "\n";
                break;
            case ADDRESS_INFO_RESPONSE:
                result += "externalIPv4Address: " + Util.getDecimalString( externalIPv4Address ) + "\n";
                result += "externalIPv4Address: " + Util.getBitString( externalIPv4Address );
                break;
        }
        return result;
    }
    public static void main(String[] args)
    {
        Response req = new Response( ResponseType.MAPPING_RESPONSE );
        req.setVersion( NATPMP_PROTOCOL_VERSION );
        req.setOpCode( NATPMP_OPCODE_UDP );
        req.setResultCode( (short)650 );
        req.setSecondsSinceEpoch( 89234 );
        req.setInternalPort( 5130 );
        req.setMappedExternalPort( 12345 );
        req.setLifetime( 30 );
        byte[] ipv4Address = new byte[4];
        ipv4Address[0] = (byte)192; ipv4Address[1] = (byte)168;
        ipv4Address[2] = (byte)1; ipv4Address[3] = (byte)1;
        req.setExternalIPv4Address( ipv4Address );
        System.out.println( req.toString() );
        byte[] testData = req.getBytes();
        Response req2 = new Response( testData, ResponseType.MAPPING_RESPONSE );
        System.out.println( req2.toString() );
    }
}

package net.ddp2p.common.network.natpmp;
import java.nio.ByteBuffer;
import net.ddp2p.common.util.Logger;
import net.ddp2p.common.util.Util;
public class Request
{
    public static final int NATPMP_MAX_SIZE = 16; 
    public static final int NATPMP_SERVER_PORT = 5351;
    public static final int NATPMP_CLIENT_PORT = 5350;
    public static final byte NATPMP_OPCODE_UDP = 1;
    public static final byte NATPMP_OPCODE_TCP = 2;
    public static final byte NATPMP_PROTOCOL_VERSION = 0;
    public enum RequestType
    {
         REQUEST_MAPPING
        ,REQUEST_ADDRESS_INFO
    }
    private RequestType type;
    private int version = NATPMP_PROTOCOL_VERSION;
    private byte opCode;
    private int internalPort;
    private int suggestedExternalPort;
    private int requestedLifetime;
    Logger logger = null;
    public RequestType getType() { return type; }
    public int getVersion() { return version; }
    public void setVersion( int version ) { this.version = version; }
    public byte getOpCode() { return opCode; }
    public void setOpCode( byte opCode ) { this.opCode = opCode; }
    public int getInternalPort() { return internalPort; }
    public void setInternalPort( int internalPort ) { this.internalPort = internalPort; }
    public int getSuggestedExternalPort() { return suggestedExternalPort; }
    public void setSuggestedExternalPort( int suggestedExternalPort )
    {
        this.suggestedExternalPort = suggestedExternalPort;
    }
    public int getRequestedLifetime() { return requestedLifetime; }
    public void setRequestedLifetime( int requestedLifetime )
    {
        this.requestedLifetime = requestedLifetime;
    }
    public Request( RequestType requestType )
    {
        this.type = requestType;
        logger = new Logger( true, true, true, true );
    }
    public Request( byte[] data, RequestType requestType )
    {
        this.type = requestType;
        logger = new Logger( true, true, true, true );
        this.parseBytes( data );
    }
    public byte[] getBytes()
    {
        byte[] data = null;
        switch( type )
        {
            case REQUEST_MAPPING:
                data = new byte[ 12 ];
                Util.copyBytes( (byte)version, data, 0, 1 );
                Util.copyBytes( opCode, data, 1, 1 );
                Util.copyBytes( (short)internalPort, data, 4, 2 );
                Util.copyBytes( (short)suggestedExternalPort, data, 6, 2 );
                Util.copyBytes( requestedLifetime, data, 8, 4 );
                break;
            case REQUEST_ADDRESS_INFO:
                data = new byte[ 2 ]; 
                break;
        }
        return data;
    }
    public void parseBytes( byte[] data )
    {
        ByteBuffer dataBuffer = ByteBuffer.allocate( NATPMP_MAX_SIZE ).put( data );
        switch( type )
        {
            case REQUEST_MAPPING:
                byte version = dataBuffer.get( 0 );
                byte opCode = dataBuffer.get( 1 );
                int internalPort = dataBuffer.getShort( 4 ) & 0xffff ; 
                int suggestedExternalPort = dataBuffer.getShort( 6 ) & 0xffff ;
                int requestedLifetime = dataBuffer.getInt( 8 );
                setVersion( version );
                setOpCode( opCode  );
                setInternalPort( internalPort );
                setSuggestedExternalPort( suggestedExternalPort );
                setRequestedLifetime( requestedLifetime );
                break;
            case REQUEST_ADDRESS_INFO:
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
        result += "opCode: " + (opCode & 0xff) + "\n";
        if( type == RequestType.REQUEST_MAPPING )
        {
            result += "internalPort: " + internalPort + "\n";
            result += "suggestedExternalPort: " + suggestedExternalPort + "\n";
            result += "requestedLifetime: " + requestedLifetime + "\n";
        }
        return result;
    }
    public static void main(String[] args)
    {
        Request req = new Request( RequestType.REQUEST_MAPPING);
        req.setVersion( NATPMP_PROTOCOL_VERSION );
        req.setOpCode( NATPMP_OPCODE_UDP );
        req.setInternalPort( 5130 );
        req.setSuggestedExternalPort( 12345 );
        req.setRequestedLifetime( 30 );
        System.out.println( req.toString() );
        byte[] testData = req.getBytes();
        Request req2 = new Request( testData, RequestType.REQUEST_MAPPING );
        System.out.println( req2.toString() );
    }
}

package net.ddp2p.common.network.pcp;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import net.ddp2p.common.util.Logger;
import net.ddp2p.common.util.Util;
public class Request
{
    public static final int PCP_HEADER_SIZE = 24;
    public static final int PCP_MAX_MESSAGE_SIZE = 1100; 
    public static final int PCP_SERVER_PORT = 5351;
    public static final int PCP_CLIENT_PORT = 5350;
    public static final byte PCP_OPCODE_ACCOUNCE = 0;
    public static final byte PCP_OPCODE_MAP = 1;
    public static final byte PCP_OPCODE_PEER = 2;
    public static final byte PCP_PROTOCOL_VERSION = 2;
    public static final byte PCP_TYPE_REQUEST = 0;
    public static final byte PCP_TYPE_RESPONSE = 1;
    public static Map<Byte, Integer> opCodeInfoSizes;
    private int length = 0;
    private int version = PCP_PROTOCOL_VERSION;
    private byte R = PCP_TYPE_REQUEST;
    private byte opCode;
    private int requestedLifetime;
    private byte[] clientIPAddress;
    private OpCodeInfo opCodeInfo;
    private HashMap<String, Option> options;
    Logger logger = null;
    static
    {
        opCodeInfoSizes = new HashMap<Byte, Integer>();
        opCodeInfoSizes.put( PCP_OPCODE_MAP, 36 );
        opCodeInfoSizes.put( PCP_OPCODE_PEER, 56 );
    }
    public int getVersion() { return version; }
    public void setVersion( int version ) { this.version = version; }
    public byte getR() { return R; }
    public void setR( byte R ) { this.R = R; }
    public byte getOpCode() { return opCode; }
    public void setOpCode( byte opCode ) { this.opCode = opCode; }
    public int getRequestedLifetime() { return requestedLifetime; }
    public void setRequestedLifetime( int requestedLifetime )
    {
        this.requestedLifetime = requestedLifetime;
    }
    public byte[] getClientIPAddress() { return clientIPAddress; }
    public void setClientIPAddress( byte[] clientIPAddress )
    {
        this.clientIPAddress = clientIPAddress;
    }
    public OpCodeInfo getOpCodeInfo() { return opCodeInfo; }
    public void setOpCodeInfo( OpCodeInfo opCodeInfo ) { this.opCodeInfo = opCodeInfo; }
    public Option getOption( String name ) { return options.get( name ); }
    public void addOption( String name, Option option )
    {
        if( ! options.containsKey( name ) )
        {
            options.put( name, option );
        }
    }
    public Request()
    {
        logger = new Logger( true, true, true, true );
        options = new HashMap<String, Option>();
    }
    public Request( byte[] data )
    {
        logger = new Logger( true, true, true, true );
        options = new HashMap<String, Option>();
        this.parseBytes( data );
    }
    public byte[] getBytes()
    {
        length += opCodeInfo.getLength();
        for( Map.Entry<String, Option> entry : options.entrySet() )
        {
            length += entry.getValue().getLength() + Option.PCP_OPTION_HEADER_LENGTH;
        }
        byte[] data = new byte[ PCP_HEADER_SIZE + length ];
        Util.copyBytes( (byte)version, data, 0, 1 );
        Util.copyBytes( (byte)((R << 7) | opCode), data, 1, 1 );
        Util.copyBytes( requestedLifetime, data, 4, 4 );
        Util.copyBytes( clientIPAddress, data, 8, 16 );
        Util.copyBytes( opCodeInfo.getBytes(), data, 24, opCodeInfo.getLength() );
        int locationMarker = 24 + opCodeInfo.getLength();
        for( Map.Entry<String, Option> entry : options.entrySet() )
        {
            Util.copyBytes( entry.getValue().getBytes(), data, locationMarker,
                            entry.getValue().getLength() + Option.PCP_OPTION_HEADER_LENGTH );
            locationMarker += entry.getValue().getLength() + Option.PCP_OPTION_HEADER_LENGTH;
        }
        return data;
    }
    public void parseBytes( byte[] data )
    {
        options = new HashMap<String, Option>();
        ByteBuffer dataBuffer = ByteBuffer.allocate( Request.PCP_MAX_MESSAGE_SIZE ).put( data );
        byte version = dataBuffer.get( 0 );
        byte RAndOpCode = dataBuffer.get( 1 );
        setVersion( version );
        byte R = (byte)((RAndOpCode >> 7) & 1);
        setR( R );
        byte opCode = (byte)(RAndOpCode & ~(1 << 7));
        setOpCode( opCode  );
        setRequestedLifetime( dataBuffer.getInt( 4 ) );
        byte[] clientIPAddress = new byte[16];
        Util.copyBytes_src_dst( data, 8, clientIPAddress, 0, 16 );
        setClientIPAddress( clientIPAddress );
        int locationMarker = 24;
        int opCodeInfoSize = opCodeInfoSizes.get( opCode );
        System.out.println( "DEBUG: " + opCode + " " + opCodeInfoSize );
        byte[] opCodeInfoBytes = new byte[opCodeInfoSize];
        Util.copyBytes_src_dst( data, locationMarker, opCodeInfoBytes, 0, opCodeInfoSize );
        switch( opCode )
        {
            case PCP_OPCODE_MAP:
                opCodeInfo = new MapOpCodeInfoRequest( opCodeInfoBytes );
                break;
            case PCP_OPCODE_PEER:
                break;
        }
        locationMarker += opCodeInfoSize;
        while( locationMarker < data.length )
        {
            int totalLength = Option.peekOptionLength( data, locationMarker )
                            + Option.PCP_OPTION_HEADER_LENGTH;
            byte[] optionData = new byte[ totalLength ];
            Util.copyBytes_src_dst( data, locationMarker, optionData, 0, totalLength );
            Option current = Option.buildOption( optionData, 0 );
            options.put( current.getName(), current );
            locationMarker += totalLength;
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
        result += "R: " + R + "\n";
        result += "OpCode: " + opCode + "\n";
        result += "RequestedLifetime: " + requestedLifetime + "\n";
        result += "ClientIPAddress: " + Util.getBitString( clientIPAddress ) + "\n";
        result += opCodeInfo.toString();
        for( Map.Entry<String, Option> entry : options.entrySet() )
        {
            result += "Option: " + entry.toString();
        }
        return result;
    }
    public static void main(String[] args)
    {
        Request req = new Request();
        req.setVersion( PCP_PROTOCOL_VERSION );
        req.setOpCode( PCP_OPCODE_MAP );
        req.setR( (byte)0 );
        req.setRequestedLifetime( 30 );
        byte[] ip = new byte[16];
        ip[0] = (byte)192; ip[1] = (byte)168; ip[2] = (byte)1; ip[3] = (byte)1;
        req.setClientIPAddress( ip );
        MapOpCodeInfoRequest r1 = new MapOpCodeInfoRequest();
        r1.setProtocol( OpCodeInfo.PROTOCOL_UDP );
        r1.setInternalPort( 26 );
        r1.setSuggestedExternalPort( 100 );
        byte[] extip = new byte[16];
        extip[0] = (byte)192; extip[1] = (byte)168; extip[2] = (byte)1; extip[3] = (byte)1;
        r1.setSuggestedExternalIPAddress( extip );
        req.setOpCodeInfo( r1 );
        Option pf = new PreferFailure( null, Option.PCP_OPTION_CODE_PREFER_FAILURE, (short)0 );
        req.addOption( pf.getName(), pf );
        System.out.println( req.toString() );
        byte[] testData = req.getBytes();
        Request req2 = new Request( testData );
        System.out.println( req2.toString() );
    }
}

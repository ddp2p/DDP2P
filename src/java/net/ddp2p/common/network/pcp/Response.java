package net.ddp2p.common.network.pcp;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import net.ddp2p.common.util.Logger;
import net.ddp2p.common.util.Util;
public class Response
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
    private byte resultCode = 0;
    private int lifetime;
    private int epochTime = 0;
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
    public byte getResultCode() { return resultCode; }
    public void setResultCode( byte resultCode ) { this.resultCode = resultCode; }
    public int getLifetime() { return lifetime; }
    public void setLifetime( int requestedLifetime )
    {
        this.lifetime = requestedLifetime;
    }
    public int getEpochTime() { return epochTime; }
    public void setEpochTime( int epochTime ) { this.epochTime = epochTime; }
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
    public Response()
    {
        logger = new Logger( true, true, true, true );
        options = new HashMap<String, Option>();
    }
    public Response( byte[] data )
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
        Util.copyBytes( resultCode, data, 3, 1 );
        Util.copyBytes( lifetime, data, 4, 4 );
        Util.copyBytes( epochTime, data, 8, 4 );
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
        ByteBuffer dataBuffer = ByteBuffer.allocate( Response.PCP_MAX_MESSAGE_SIZE ).put( data );
        byte version = dataBuffer.get( 0 );
        byte RAndOpCode = dataBuffer.get( 1 );
        setVersion( version );
        byte R = (byte)((RAndOpCode >> 7) & 1);
        setR( R );
        byte opCode = (byte)(RAndOpCode & ~(1 << 7));
        setOpCode( opCode  );
        byte resultCode = dataBuffer.get( 3 );
        setResultCode( resultCode );
        setLifetime( dataBuffer.getInt( 4 ) );
        setEpochTime( dataBuffer.getInt( 8 ) );
        int locationMarker = 24;
        int opCodeInfoSize = opCodeInfoSizes.get( opCode );
        System.out.println( "DEBUG: " + opCode + " " + opCodeInfoSize );
        byte[] opCodeInfoBytes = new byte[opCodeInfoSize];
        Util.copyBytes_src_dst( data, locationMarker, opCodeInfoBytes, 0, opCodeInfoSize );
        switch( opCode )
        {
            case PCP_OPCODE_MAP:
                opCodeInfo = new MapOpCodeInfoResponse( opCodeInfoBytes );
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
            if( current == null )
            {
                break;
            }
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
        result += "resultCode: " + resultCode + "\n";
        result += "lifetime: " + lifetime + "\n";
        result += "epochTime: " + epochTime + "\n";
        result += opCodeInfo.toString();
        for( Map.Entry<String, Option> entry : options.entrySet() )
        {
            result += "Option: " + entry.toString();
        }
        return result;
    }
    public static void main(String[] args)
    {
        Response response = new Response();
        response.setVersion( PCP_PROTOCOL_VERSION );
        response.setOpCode( PCP_OPCODE_MAP );
        response.setR( (byte)0 );
        response.setResultCode( (byte)5 );
        response.setLifetime( 30 );
        response.setEpochTime( 123456 );
        MapOpCodeInfoResponse r1 = new MapOpCodeInfoResponse();
        r1.setProtocol( OpCodeInfo.PROTOCOL_UDP );
        r1.setInternalPort( 26 );
        r1.setAssignedExternalPort( 100 );
        byte[] extip = new byte[16];
        extip[0] = (byte)192; extip[1] = (byte)168; extip[2] = (byte)1; extip[3] = (byte)1;
        r1.setAssignedExternalIPAddress( extip );
        response.setOpCodeInfo( r1 );
        Option pf = new PreferFailure( null, Option.PCP_OPTION_CODE_PREFER_FAILURE, (short)0 );
        response.addOption( pf.getName(), pf );
        System.out.println( response.toString() );
        byte[] testData = response.getBytes();
        Response req2 = new Response( testData );
        System.out.println( req2.toString() );
    }
}

package net.ddp2p.common.network.pcp;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;
import net.ddp2p.common.util.Util;
public class MapOpCodeInfoResponse implements OpCodeInfo
{
    private String name = "MAP_OPCODE_INFO";
    private int length = 36;
    private byte[] mappingNonce;
    private byte protocol;
    private int internalPort;
    private int assignedExternalPort;
    private byte[] assignedExternalIPAddress;
    @Override
    public String getName() { return name; }
    @Override
    public int getLength() { return length; }
    public byte[] getMappingNonce() { return mappingNonce; }
    public void setMappingNonce( byte[] mappingNonce ) { this.mappingNonce = mappingNonce; }
    public byte getProtocol() { return protocol; }
    public void setProtocol( byte protocol ) { this.protocol = protocol;}
    public int getInternalPort() { return internalPort; }
    public void setInternalPort( int internalPort ) { this.internalPort = internalPort; }
    public int getAssignedExternalPort() { return assignedExternalPort; }
    public void setAssignedExternalPort( int suggestedExternalPort )
    {
        this.assignedExternalPort = suggestedExternalPort;
    }
    public byte[] getAssignedExternalIPAddress() { return assignedExternalIPAddress; }
    public void setAssignedExternalIPAddress( byte[] suggestedExternalIPAddress )
    {
        this.assignedExternalIPAddress = suggestedExternalIPAddress;
    }
    public void setName( String name ) { this.name = name; }
    public void setLength( int length ) { this.length = length; }
    public MapOpCodeInfoResponse( byte[] data )
    {
        parseBytes( data );
    }
    public MapOpCodeInfoResponse()
    {
        generateMappingNonce();
    }
    public void generateMappingNonce()
    {
        BigInteger nonce = new BigInteger( 96, new Random() );
        byte[] nonceBytes = nonce.toByteArray();
        byte[] finalBytes = new byte[12];
        Util.copyBytes( nonceBytes, finalBytes, 0, 12 );
        mappingNonce = finalBytes;
    }
    @Override
    public byte[] getBytes()
    {
        byte[] data = new byte[length];
        Util.copyBytes( mappingNonce, data, 0, 12 );
        data[12] = protocol;
        Util.copyBytes( (short)internalPort, data, 16, 2 );
        Util.copyBytes( (short)assignedExternalPort, data, 18, 2 );
        Util.copyBytes(  assignedExternalIPAddress, data, 20, 16 );
        return data;
    }
    public void parseBytes( byte[] data )
    {
        ByteBuffer dataBuffer = ByteBuffer.allocate( length ).put( data );
        mappingNonce = new byte[12];
        Util.copyBytes( data, mappingNonce, 0, 12 );
        protocol = dataBuffer.get( 12 );
        internalPort = dataBuffer.getShort( 16 ) & 0xffff;
        assignedExternalPort = dataBuffer.getShort(  18 ) & 0xffff;
        assignedExternalIPAddress = new byte[16];
        Util.copyBytes_src_dst( data, 20, assignedExternalIPAddress, 0, 16 );
    }
    @Override
    public String toString()
    {
        String info = name + ":\n";
        info += "mappingNonce: " + Util.getBitString( mappingNonce ) + "\n";
        info += "protocol: " + protocol + "\n";
        info += "internalPort: " + internalPort + "\n";
        info += "assignedExternalPort: " + assignedExternalPort + "\n";
        info += "assignedExternalIPAddress: " + Util.getDecimalString( assignedExternalIPAddress )
                + "\n";
        info += "assignedExternalIPAddress: " + Util.getBitString( assignedExternalIPAddress )
                + "\n";
        return info;
    }
    public static void main(String[] args)
    {
        MapOpCodeInfoResponse r1 = new MapOpCodeInfoResponse();
        r1.setProtocol( OpCodeInfo.PROTOCOL_UDP );
        r1.setInternalPort( 26 );
        r1.setAssignedExternalPort( 100 );
        byte[] ip = new byte[16];
        ip[0] = (byte)192; ip[1] = (byte)168; ip[2] = (byte)1; ip[3] = (byte)1;
        r1.setAssignedExternalIPAddress( ip );
        System.out.println( r1 );
        byte[] data = r1.getBytes();
        MapOpCodeInfoResponse r2 = new MapOpCodeInfoResponse( data );
        System.out.println( r2 );
    }
}

package net.ddp2p.common.network.pcp;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;
import net.ddp2p.common.util.Util;
public class MapOpCodeInfoRequest implements OpCodeInfo
{
    private String name = "MAP_OPCODE_INFO";
    private int length = 36;
    private byte[] mappingNonce;
    private byte protocol;
    private int internalPort;
    private int suggestedExternalPort;
    private byte[] suggestedExternalIPAddress;
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
    public int getSuggestedExternalPort() { return suggestedExternalPort; }
    public void setSuggestedExternalPort( int suggestedExternalPort )
    {
        this.suggestedExternalPort = suggestedExternalPort;
    }
    public byte[] getSuggestedExternalIPAddress() { return suggestedExternalIPAddress; }
    public void setSuggestedExternalIPAddress( byte[] suggestedExternalIPAddress )
    {
        this.suggestedExternalIPAddress = suggestedExternalIPAddress;
    }
    public void setName( String name ) { this.name = name; }
    public void setLength( int length ) { this.length = length; }
    public MapOpCodeInfoRequest( byte[] data )
    {
        parseBytes( data );
    }
    public MapOpCodeInfoRequest()
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
        Util.copyBytes( (short)suggestedExternalPort, data, 18, 2 );
        Util.copyBytes(  suggestedExternalIPAddress, data, 20, 16 );
        return data;
    }
    public void parseBytes( byte[] data )
    {
        ByteBuffer dataBuffer = ByteBuffer.allocate( length ).put( data );
        mappingNonce = new byte[12];
        Util.copyBytes( data, mappingNonce, 0, 12 );
        protocol = dataBuffer.get( 12 );
        internalPort = dataBuffer.getShort( 16 ) & 0xffff;
        suggestedExternalPort = dataBuffer.getShort(  18 ) & 0xffff;
        suggestedExternalIPAddress = new byte[16];
        Util.copyBytes_src_dst( data, 20, suggestedExternalIPAddress, 0, 16 );
    }
    @Override
    public String toString()
    {
        String info = name + ":\n";
        info += "mappingNonce: " + Util.getBitString( mappingNonce ) + "\n";
        info += "protocol: " + protocol + "\n";
        info += "internalPort: " + internalPort + "\n";
        info += "suggestedExternalPort: " + suggestedExternalPort + "\n";
        info += "suggestedExternalIPAddress: " + Util.getDecimalString( suggestedExternalIPAddress )
                + "\n";
        info += "suggestedExternalIPAddress: " + Util.getBitString( suggestedExternalIPAddress )
                + "\n";
        return info;
    }
    public static void main(String[] args)
    {
        MapOpCodeInfoRequest r1 = new MapOpCodeInfoRequest();
        r1.setProtocol( OpCodeInfo.PROTOCOL_UDP );
        r1.setInternalPort( 26 );
        r1.setSuggestedExternalPort( 100 );
        byte[] ip = new byte[16];
        ip[0] = (byte)192; ip[1] = (byte)168; ip[2] = (byte)1; ip[3] = (byte)1;
        r1.setSuggestedExternalIPAddress( ip );
        System.out.println( r1 );
        byte[] data = r1.getBytes();
        MapOpCodeInfoRequest r2 = new MapOpCodeInfoRequest( data );
        System.out.println( r2 );
    }
}

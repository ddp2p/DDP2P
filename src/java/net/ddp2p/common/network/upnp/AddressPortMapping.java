package net.ddp2p.common.network.upnp;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import net.ddp2p.common.util.Util;
public class AddressPortMapping
{
    private String internalIPAddress;
    private String externalIPAddress;
    private int internalPort;
    private int externalPort;
    public String getInternalIPAddress() { return internalIPAddress; }
    public void setInternalIPAddress(String internalIPAddress)
    {
        this.internalIPAddress = internalIPAddress;
    }
    public String getExternalIPAddress() { return externalIPAddress; }
    public void setExternalIPAddress(String externalIPAddress)
    {
        this.externalIPAddress = externalIPAddress;
    }
    public int getInternalPort() { return internalPort; }
    public void setInternalPort( int internalPort ) { this.internalPort = internalPort; }
    public int getExternalPort() { return externalPort; }
    public void setExternalPort( int externalPort ) { this.externalPort = externalPort; }
    public AddressPortMapping() { }
    public AddressPortMapping( byte[] data )
        throws UnsupportedEncodingException
    {
        parseExternalBytes( data );
    }
    public byte[] getExternalBytes()
        throws UnsupportedEncodingException
    {
        byte[] ipAddress = externalIPAddress.getBytes( "UTF-8" );
        byte[] result = new byte[ipAddress.length + 3]; 
        result[0] = (byte)ipAddress.length;
        Util.copyBytes( ipAddress, result, 1, ipAddress.length );
        Util.copyBytes( (short)externalPort, result, ipAddress.length + 1, 2 );
        return result;
    }
    public void parseExternalBytes( byte[] data )
        throws UnsupportedEncodingException
    {
        ByteBuffer buffer = ByteBuffer.allocate( data.length ).put( data );
        int ipAddressLength = data[0];
        byte[] ipAddress = new byte[ipAddressLength];
        Util.copyBytes_src_dst( data, 1, ipAddress, 0, ipAddressLength );
        externalIPAddress = new String( ipAddress, "UTF-8" );
        externalPort = buffer.getShort( ipAddressLength + 1 ) & 0xffff;
    }
    @Override
    public String toString()
    {
        String s = "";
        s += "internalIPAddress: " + internalIPAddress + "\n";
        s += "externalIPAddress: " + externalIPAddress + "\n";
        s += "internalPort: " + internalPort + "\n";
        s += "externalPort: " + externalPort + "\n";
        return s;
    }
}

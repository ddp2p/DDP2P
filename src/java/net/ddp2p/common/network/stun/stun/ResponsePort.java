package net.ddp2p.common.network.stun.stun;
import java.nio.ByteBuffer;
import net.ddp2p.common.util.Util;
public class ResponsePort extends Attribute
{
    final String name = "ResponsePort";
    int port;
    public ResponsePort()
    {
        this.type = TYPE_RESPONSE_PORT;
    }
    public ResponsePort( int port )
    {
        this();
        this.port = port;
    }
    public ResponsePort( byte[] data )
    {
        this.parseBytes( data );
    }
    @Override
    public String getName() { return name; }
    public int getPort() { return port; }
    public void setPort( int port ) { this.port = port; }
    @Override
    public byte[] getBytes()
    {
        calculateLength();
        byte[] value = new byte[12];
        Util.copyBytes( this.type, value, 0, 2 );
        Util.copyBytes( this.length, value, 2, 2 );
        Util.copyBytes( (short)port, value, 4, 2 );
        return value;
    }
    @Override
    public void parseBytes( byte[] data )
    {
        byte[] temp = new byte[2];
        Util.copyBytes_src_dst( data, 0, temp, 0, 2 );
        this.type = ByteBuffer.allocate( 2 ).put( temp ).getShort( 0 );
        Util.copyBytes_src_dst( data, 2, temp, 0, 2 );
        this.length = ByteBuffer.allocate( 2 ).put( temp ).getShort( 0 );
        Util.copyBytes_src_dst( data, 4, temp, 0, 2 );
        this.port = ByteBuffer.allocate( 2 ).put( temp ).getShort( 0 ) & 0xffff;
    }
    @Override
    public void calculateLength()
    {
        this.length = 4; 
    }
}

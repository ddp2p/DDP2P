package net.ddp2p.common.network.pcp;
public class PreferFailure extends Option
{
    public PreferFailure( byte[] data, byte code, short length )
    {
        setName( "PREFER_FAILURE" );
        setCode( code );
        setLength( length );
    }
    public void parseBytes( byte[] bytes ) { } 
    public byte[] getBytes()
    {
        byte[] data = new byte[4];
        data[0] = getCode();
        return data;
    }
    public String toString()
    {
        return getName();
    }
}

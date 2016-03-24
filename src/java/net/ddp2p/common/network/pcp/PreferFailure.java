package net.ddp2p.common.network.pcp;

public class PreferFailure extends Option
{
    public PreferFailure( byte[] data, byte code, short length )
    {
        setName( "PREFER_FAILURE" );
        setCode( code );
        setLength( length );
    }

    public void parseBytes( byte[] bytes ) { } // There is no additional information to parse.
    public byte[] getBytes()
    {
        // Set up header information.
        byte[] data = new byte[4];
        data[0] = getCode();
        // Reserved (1 byte) and length (2 bytes) are both zero.

        // There is no additional data for this option.

        return data;
    }
    
    public String toString()
    {
        return getName();
    }
}


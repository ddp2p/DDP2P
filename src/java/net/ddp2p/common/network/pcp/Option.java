package net.ddp2p.common.network.pcp;

import java.nio.ByteBuffer;

import net.ddp2p.common.util.Util;

// For use with MAP and PEER opcode-specific options.
public abstract class Option
{
    public static final int PCP_OPTION_HEADER_LENGTH = 4;
    public static final byte PCP_OPTION_CODE_THIRD_PARTY = 1;
    public static final byte PCP_OPTION_CODE_PREFER_FAILURE = 2;
    public static final byte PCP_OPTION_CODE_FILTER = 3;

    private String name = null;
    private byte code = 0;
    private byte reserved = 0;
    private short length = 0;

    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }

    public byte getCode() { return code; }
    public void setCode( byte code ) { this.code = code; }

    public int getLength() { return length; }
    public void setLength( short length ) { this.length = length; }

    public Option() { }

    abstract public void parseBytes( byte[] data );
    abstract public byte[] getBytes();
    abstract public String toString();

    // Builds an option of the correct type - should probably be a factory.
    public static Option buildOption( byte[] data, int optionStart )
    {
        byte[] headerData = new byte[4];
        Util.copyBytes_src_dst( data, optionStart, headerData, 0, 4 );
        ByteBuffer dataBuffer = ByteBuffer.allocate( 4 ).put( headerData );
        byte code = dataBuffer.get( 0 );
        short length = dataBuffer.getShort( 2 );

        Option newOption = null;
        switch( code )
        {
            case PCP_OPTION_CODE_THIRD_PARTY:
                // TODO
                break;
            case PCP_OPTION_CODE_PREFER_FAILURE:
                newOption = new PreferFailure( data, code, length );
                break;
            case PCP_OPTION_CODE_FILTER:
                // TODO
                break;
            case 0:
                // In this case this probably isn't an option, just extra 0's in the buffer.
                // Return NULL to indicate that the parsing process should stop.
        }

        return newOption;
    }

    // Utility method to get the option code.
    public static int peekOptionCode( byte[] data, int optionStart )
    {
        byte[] headerData = new byte[4];
        Util.copyBytes_src_dst( data, optionStart, headerData, 0, 4 );
        ByteBuffer dataBuffer = ByteBuffer.allocate( 4 ).put( headerData );
        byte code = dataBuffer.get( 0 );
        return code;
    }

    // Utility method to get the length for memory allocation purposes.
    public static int peekOptionLength( byte[] data, int optionStart )
    {
        byte[] headerData = new byte[4];
        Util.copyBytes_src_dst( data, optionStart, headerData, 0, 4 );
        ByteBuffer dataBuffer = ByteBuffer.allocate( 4 ).put( headerData );
        short length = dataBuffer.getShort( 2 );
        return length;
    }
}


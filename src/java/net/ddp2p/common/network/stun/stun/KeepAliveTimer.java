package net.ddp2p.common.network.stun.stun;
import java.nio.ByteBuffer;
import net.ddp2p.common.util.Util;
public class KeepAliveTimer extends Attribute
{
    public static final byte LINEAR_INCREMENT = 0;
    public static final byte GEOMETRIC_INCREMENT = 1;
    public static final byte NO_STARTER_MESSAGE = 0;
    public static final byte USES_STARTER_MESSAGE = 1;
    public static final byte DIRECTION_FORWARD = 0;
    public static final byte DIRECTION_BACKWARD = 1;
    public static final byte FALSE = 0;
    public static final byte TRUE = 1;
    public static final byte MAINTAIN_CURRENT = 0;
    public static final byte STAY_CONSTANT = 1;
    public static final byte CHANGE_FORWARD = 2;
    public static final byte CHANGE_BACKWARD = 3;
    public static final byte PLACEHOLDER = 0; 
    public static final byte SIZE_1_BYTE = 0;
    public static final byte SIZE_2_BYTES = 1;
    public static final byte SIZE_4_BYTES = 2;
    public static final byte SIZE_8_BYTES = 3;
    public static final byte SIZE_16_BYTES = 4;
    public static final byte BF_USES_STARTER_MESSAGE = (byte)(0x80);
    public static final byte BF_SERVER_ADJUSTS_Z = (byte)(0x80 >> (byte)1);
    public static final byte BF_INCREMENT_TYPE_LINEAR = 0;
    public static final byte BF_INCREMENT_TYPE_GEOMETRIC = (byte)(0x80 >> (byte)2);
    public static final byte BF_DIRECTION_FORWARD = 0;
    public static final byte BF_DIRECTION_BACKWARD = (byte)(0x80 >> (byte)3);
	final String name = "KeepAliveTimer";
	byte version;
	int id;
	int time;	  
	double deltaT; 
	int k;
	byte incrementType; 
	byte usesStarterMessage;
	byte direction; 
	byte repeats;
	int maxTime;
	byte actionAfterMax;
	byte serverAdjustsZ;
	int z;
	int zMultiplier;
	double deltaTAfterMax;
	byte zSizeType = SIZE_4_BYTES;
	byte zMultiplierSizeType = SIZE_4_BYTES;
	byte timeSizeType = SIZE_4_BYTES;
	byte deltaTSizeType = SIZE_4_BYTES;
	@Override
	public String getName() { return name; }
	public int getId() { return id; }
	public void setId(int id) { this.id = id; }
	public int getTime() { return time; }
	public void setTime(int time) { this.time = time; }
	public double getDeltaT() { return deltaT; }
	public void setDeltaT( double deltaT ) { this.deltaT = deltaT; }
	public int getK() { return k; } 
	public void setK(int k) { this.k = k; }
	public byte getIncrementType() { return incrementType; }
	public void setIncrementType( byte incrementType ) { this.incrementType = incrementType; }
	public byte getUsesStarterMessage() { return usesStarterMessage; }
	public void setUsesStarterMessage( byte doesStarterMessage )
	{
	    this.usesStarterMessage = doesStarterMessage;
	}
	public byte getDirection() { return direction; }
	public void setDirection( byte direction ) { this.direction = direction; }
	public byte getRepeats() { return repeats; }
	public void setRepeats( byte repeats ) { this.repeats = repeats; }
	public int getMaxTime() { return maxTime; }
	public void setMaxTime( int maxTime ) { this.maxTime = maxTime; }
	public byte getActionAfterMax() { return actionAfterMax; }
	public void setActionAfterMax( byte actionAfterMax )
	{
	    this.actionAfterMax = actionAfterMax;
	}
	public byte getServerAdjustsZ() { return serverAdjustsZ; }
    public void setServerAdjustsZ(byte usesConstK) { this.serverAdjustsZ = usesConstK; }
    public int getZ() { return z; }
    public void setZ(int z) { this.z = z; }
    public int getzMultiplier() { return zMultiplier; }
    public void setzMultiplier(int zMultiplier) { this.zMultiplier = zMultiplier; }
    public double getDeltaTAfterMax() { return deltaTAfterMax; }
    public void setDeltaTAfterMax( double deltaTAfterMax )
    {
        this.deltaTAfterMax = deltaTAfterMax;
    }
    public KeepAliveTimer()
    {
		this.type = TYPE_KEEPALIVE_TIMER;
	}
	public KeepAliveTimer( byte[] data )
    {
		this.parseBytes( data );
	}
	@Override
	public byte[] getBytes()
    {
		calculateLength();
		byte[] value = new byte[40];
		Util.copyBytes( this.type, value, 0, 2 );
		Util.copyBytes( this.length, value, 2, 2 );
		value[4] = version;
		value[5] = actionAfterMax;
		Util.copyBytes( (short)k, value, 6, 2 );
		Util.copyBytes( id, value, 8, 4 );
		byte flagByte = 0;
		if( usesStarterMessage == TRUE )
		{
		    flagByte |= BF_USES_STARTER_MESSAGE;
		}
		if( serverAdjustsZ == TRUE )
		{
		    flagByte |= BF_SERVER_ADJUSTS_Z;
		}
		if( incrementType == GEOMETRIC_INCREMENT )
		{
		    flagByte |= BF_INCREMENT_TYPE_GEOMETRIC;
		}
		if( direction == DIRECTION_BACKWARD )
		{
		    flagByte |= BF_DIRECTION_BACKWARD;
		}
		value[12] = flagByte;
		value[13] = repeats;
		byte tmpSizeTypes = 0;
		tmpSizeTypes |= (byte)(zSizeType << (byte)4);
		tmpSizeTypes |= zMultiplierSizeType;
		value[14] = tmpSizeTypes;
		tmpSizeTypes = 0;
		tmpSizeTypes |= (byte)(timeSizeType << (byte)4);
		tmpSizeTypes |= deltaTSizeType;
		value[15] = tmpSizeTypes;
		Util.copyBytes( z, value, 16, 4 );
		Util.copyBytes( zMultiplier, value, 20, 4 );
		Util.copyBytes( time, value, 24, 4 );
		Util.copyBytes( maxTime, value, 28, 4 );
        int intDeltaT = (int)deltaT;
        int intDeltaTAfterMax = (int)deltaTAfterMax;
		Util.copyBytes( intDeltaT, value, 32, 4 );
		Util.copyBytes( intDeltaTAfterMax, value, 36, 4 );
		return value;
	}
	@Override
	public void parseBytes( byte[] data )
    {
		byte[] temp = new byte[4];
		Util.copyBytes_src_dst( data, 0, temp, 0, 2 );
		this.type = ByteBuffer.allocate( 4 ).put( temp ).getShort( 0 );
		Util.copyBytes_src_dst( data, 2, temp, 0, 2 );
		this.length = ByteBuffer.allocate( 4 ).put( temp ).getShort( 0 );
        version = data[4];
        actionAfterMax = data[5];
        Util.copyBytes_src_dst( data, 6, temp, 0, 2 );
        this.k = ByteBuffer.allocate( 4 ).put( temp ).getShort( 0 );
        Util.copyBytes_src_dst( data, 8, temp, 0, 4 );
        this.id = ByteBuffer.allocate( 4 ).put( temp ).getInt( 0 );
        byte flagByte = data[12];
        if( (flagByte & BF_USES_STARTER_MESSAGE) == BF_USES_STARTER_MESSAGE )
        {
            usesStarterMessage = TRUE;
        }
        if( (flagByte & BF_SERVER_ADJUSTS_Z) == BF_SERVER_ADJUSTS_Z )
        {
            serverAdjustsZ = TRUE;
        }
        incrementType = LINEAR_INCREMENT;
        if( (flagByte & BF_INCREMENT_TYPE_GEOMETRIC) == BF_INCREMENT_TYPE_GEOMETRIC )
        {
            incrementType = GEOMETRIC_INCREMENT;
        }
        direction = DIRECTION_FORWARD;
        if( (flagByte & BF_DIRECTION_BACKWARD) == BF_DIRECTION_BACKWARD )
        {
            direction = DIRECTION_BACKWARD;
        }
        repeats = data[13];
        byte sizeTypes = data[14];
        zSizeType = (byte)(sizeTypes >>> (byte)4);
        zMultiplierSizeType = (byte)(sizeTypes & 0xF);
        sizeTypes = data[15];
        timeSizeType = (byte)(sizeTypes >>> (byte)4);
        deltaTSizeType = (byte)(sizeTypes & 0xF);
        Util.copyBytes_src_dst( data, 16, temp, 0, 4 );
        this.z = ByteBuffer.allocate( 4 ).put( temp ).getInt( 0 );
        Util.copyBytes_src_dst( data, 20, temp, 0, 4 );
        this.zMultiplier = ByteBuffer.allocate( 4 ).put( temp ).getInt( 0 );
        Util.copyBytes_src_dst( data, 24, temp, 0, 4 );
        this.time = ByteBuffer.allocate( 4 ).put( temp ).getInt( 0 );
        Util.copyBytes_src_dst( data, 28, temp, 0, 4 );
        this.maxTime = ByteBuffer.allocate( 4 ).put( temp ).getInt( 0 );
        Util.copyBytes_src_dst( data, 32, temp, 0, 4 );
        this.deltaT = ByteBuffer.allocate( 4 ).put( temp ).getInt( 0 );
        Util.copyBytes_src_dst( data, 36, temp, 0, 4 );
        this.deltaTAfterMax = ByteBuffer.allocate( 4 ).put( temp ).getInt( 0 );
	}
	@Override
	public void calculateLength()
    {
		this.length = 36; 
	}
	public static void main(String[] args)
	{
        KeepAliveTimer timer = new KeepAliveTimer();
        timer.setId( 123456 );
        timer.setK( 452 );
        timer.setTime( 5000 );
        timer.setDeltaT( 5000 );
        timer.setIncrementType( LINEAR_INCREMENT );
        timer.setUsesStarterMessage( TRUE );
        timer.setDirection( DIRECTION_FORWARD );
        timer.setRepeats( (byte)10 );
        timer.setMaxTime( 30000 );
        timer.setActionAfterMax( STAY_CONSTANT );
        timer.setServerAdjustsZ( TRUE );
        timer.setZ( 1024 );
        timer.setzMultiplier( 2 );
        timer.setDeltaTAfterMax( 100 );
        byte[] tmp = timer.getBytes();
        KeepAliveTimer timer2 = new KeepAliveTimer( tmp );
        int s = 5;
	}
}

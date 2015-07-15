package net.ddp2p.common.network.stun.keepalive;

public class KeepAliveData {
	public static final int TYPE_INCREMENT = 0;
	public static final int TYPE_FIXED = 1;

    private SessionKey key;         // Contains id, address, and port
	private long nextSendTime = 0;
	private double deltaT = 0;		// Delta T
	private int currentTime = 0;	// T
	private int k = 1;				// K
	private byte incrementType = 0;  // Used by the server to determine the method to use for
	                                 // incrementing the currentTime, if the deltaT > 0.
	private byte usesStarterMessage = 0; // Used by the server to determine if a "starter" message
	                                     // should be sent at the beginning of each session to help
	                                     // establish a more accurate deltaT.

	private byte direction = 0;
	private int maxRepeats = 0;
	private int currentRepeats = 2; // Account for the "starter" message and initial for time.
	private int maxTime = 0;
	private byte actionAfterMax = 0;

	private byte serverAdjustsZ = 0;
	private int z = 1;
	private int zMultiplier = 1;

	private double initialDeltaT = 0;
	private double initialDeltaTAfterMax = 0;
	private boolean maxWasReached = false;
	private double deltaTAfterMax = 0;

	private int kNoRepeat = 0; // local variable only. K indepedent of wheel repeats for RUN and REFINE.

	private int localK = 0;

	private int initialK = 0;

	public void setInitialK( int initialK ) { this.initialK = initialK; }
	public int getInitialK() { return initialK; }

    public SessionKey getSessionKey() { return key; }
    public void setSessionKey( SessionKey key ) { this.key = key; }

	public long getNextSendTime() { return nextSendTime; }
	public void setNextSendTime(long nextSendTime) { this.nextSendTime = nextSendTime; }

	public double getDeltaT() { return deltaT; }
	public void setDeltaT(double deltaT ) { this.deltaT = deltaT; }

	public int getCurrentTime() { return currentTime; }
	public void setCurrentTime(int currentTime) { this.currentTime = currentTime; }

	public int getK() { return k; }
	public void setK(int k) { this.k = k; }
	public void incrementK() { this.k += 1; }

	public byte getIncrementType() { return incrementType; }
	public void setIncrementType( byte incrementType ) { this.incrementType = incrementType; }

	public byte getUsesStarterMessage() { return usesStarterMessage; }
	public void setUsesStarterMessage( byte doesStarterMessage )
	{
	    this.usesStarterMessage = doesStarterMessage;
	}
    public byte getDirection() { return direction; }
    public void setDirection(byte direction) { this.direction = direction; }

    public int getMaxRepeats() { return maxRepeats; }
    public void setMaxRepeats(int maxRepeats) { this.maxRepeats = maxRepeats; }

    public int getCurrentRepeats() { return currentRepeats; }
    public void setCurrentRepeats(int currentRepeats) { this.currentRepeats = currentRepeats; }
    public void incrementCurrentRepeats() { this.currentRepeats++; }
    public void resetCurrentRepeats() { this.currentRepeats = 1; } // Account for initial message.

    public int getMaxTime() { return maxTime; }
    public void setMaxTime( int maxTime ) { this.maxTime = maxTime; }

    public byte getActionAfterMax() { return actionAfterMax; }
    public void setActionAfterMax( byte reverseAfterMaxTime )
    {
        this.actionAfterMax = reverseAfterMaxTime;
    }

    public int getKNoRepeat() { return kNoRepeat; }
    public void setKNoRepeat( int kNoRepeat ) { this.kNoRepeat = kNoRepeat; }
    public void resetKNoRepeat() { kNoRepeat = 0; }
    public void incrementKNoRepeat() { kNoRepeat++; }

    public byte getServerAdjustsZ() { return serverAdjustsZ; }
    public void setServerAdjustsZ(byte serverAdjustsZ) { this.serverAdjustsZ = serverAdjustsZ; }

    public int getZ() { return z; }
    public void setZ(int z) { this.z = z; }

    public int getzMultiplier() { return zMultiplier; }
    public void setzMultiplier(int zMultiplier) { this.zMultiplier = zMultiplier; }

    public double getInitialDeltaT() { return initialDeltaT; }
    public void setInitialDeltaT( double initialDeltaT ) { this.initialDeltaT = initialDeltaT; }

    public double getInitialDeltaTAfterMax() { return initialDeltaTAfterMax; }
    public void setInitialDeltaTAfterMax( double initialDeltaTAfterMax)
    {
        this.initialDeltaTAfterMax = initialDeltaTAfterMax;
    }

    public boolean getMaxWasReached() { return maxWasReached; }
    public void setMaxWasReached( boolean maxWasReached ) { this.maxWasReached = maxWasReached; }

    public double getDeltaTAfterMax() { return deltaTAfterMax; }
    public void setDeltaTAfterMax( double intervalForAfterMax )
    {
        this.deltaTAfterMax = intervalForAfterMax;
    }

    public int getLocalK() { return localK; }
    public void setLocalK( int localK ) { this.localK = localK; }
    public void incrementLocalK() { localK++; }
}


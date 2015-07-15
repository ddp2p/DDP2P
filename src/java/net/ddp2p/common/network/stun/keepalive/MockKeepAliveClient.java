package net.ddp2p.common.network.stun.keepalive;


public class MockKeepAliveClient
{
    public static void main(String[] args)
    {
        int natdeviceTimeout = 30000;

        // SEARCH params
        int baseDeltaT = 5000;
        int deltaT = baseDeltaT;

        int zMax = 16;
        int zMultiplier = 2;

        // RUN params
        double successRate;

//         // REFINE params (may not be needed here)
//        int deltaT;
//        int changeMultiplier;
//        int maxAttempts;
//        int pingsPerAttempt;

        // Local tracking variables
        int time = 0;
        int z = 1;
        int k = 0;
        int failureCount = 0;
        int lastGoodTime = 0;
        // this is used to track the total number of runs in RUN mode since k is reset for each
        // session, but always updated.
        // int localK = 0;

        int messageCount = 0;

        while( true )
        {
            if( time > natdeviceTimeout ) // TODO: some kind of buffer?
            {
                time = lastGoodTime + k * (deltaT / z);
                z *= zMultiplier;
                k = 0;
                deltaT = baseDeltaT / z;

                System.out.println( "[" + messageCount + "][" + k + "] - NEW time: " + time
                        + " z: " + z );

                if( z > zMax )
                {
                    System.out.println( "z is greater than " + zMax + ", EXITING LOOP" );
                    break;
                }
            }
            else if( time == natdeviceTimeout || Math.abs( time - natdeviceTimeout ) < 1000 )
            {
                System.out.println( "Found suitable time: " + time );
                break;
            }
            else
            {
                lastGoodTime = time;

                time = time + (k * deltaT);
                k++;

                System.out.println( "[" + messageCount + "][" + k + "] " + lastGoodTime + " : "
                        + time );
            }

            messageCount++;
        }

        System.out.println( "Message Count: " + messageCount );
    }

}

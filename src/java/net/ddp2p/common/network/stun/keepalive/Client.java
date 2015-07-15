package net.ddp2p.common.network.stun.keepalive;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.ddp2p.common.network.stun.stun.KeepAliveTimer;
import net.ddp2p.common.network.stun.stun.MappedAddress;
import net.ddp2p.common.network.stun.stun.Message;
import net.ddp2p.common.network.stun.stun.ResponsePort;
import net.ddp2p.common.network.upnp.AddressPortMapping;
import net.ddp2p.common.util.Logger;

public class Client
{
    public final int TIMEOUT_BUFFER_MS = 2010;
    public final int SERVER_TIMEOUT_MS = 10000;
    public final int MAX_MESSAGE_SIZE = 10000;

    public final int PORT_FOR_RANDOM = Message.STUN_PORT;
    public final int RANDOM_MS_MULTIPLIER = 55000;
    public final int MAX_RESET_TRIES = 10;
    public final int MIN_RESET_INTERVAL_MS = 5000;


    private Logger logger = null;
    private SynchronizedTime sharedTime = null;
    private int clientId = 0;

    // Custom Exception for tracking problems related to peer communication.
    private class PeerCommunicationException extends Exception
    {
        public PeerCommunicationException( String message )
        {
            super( message );
        }
    }


    // The state of operation for the client.
    public enum State
    {
        SEARCH
       ,RUN
       ,REFINE
       ,EXIT
       ,NONE;
    }

    // The type of Search being performed for the client.
    public enum SearchType
    {
        LINEAR
       ,GEOMETRIC
       ,SWITCH
    }

    // Used for KeepAlive returns and tracking overall statistics.
    public class RunData
    {
        private int newTime; // May need to be updated via a SynchronizedTime object change.
        private final State newState;
        private final State previousState;
        private int receivedMessageCount;
        private int startMessageCount;
        private int stopMessageCount;
        private int resetMessageCount;
        private long sessionDuration;
        private int timeoutCount;
        private int refineDelta;
        private int timeUnreachable;
        private DatagramSocket socket = null;


        public RunData( int newTime, State newState, State previousState, int receivedMessageCount,
                        int startMessageCount, int stopMessageCount, int resetMessageCount,
                        long sessionDuration, int timeoutCount, int refineDelta,
                        int timeUnreachable )
        {
            this.newTime = newTime;
            this.newState = newState;
            this.previousState = previousState;
            this.receivedMessageCount = receivedMessageCount;
            this.startMessageCount = startMessageCount;
            this.stopMessageCount = stopMessageCount;
            this.resetMessageCount = resetMessageCount;
            this.sessionDuration = sessionDuration;
            this.timeoutCount = timeoutCount;
            this.refineDelta = refineDelta;
            this.timeUnreachable = timeUnreachable;
        }

        public RunData( int newTime, State newState, State previousState )
        {
            this( newTime, newState, previousState, 0, 0, 0, 0, 0, 0, 0, 0 );
        }

        public void updateNewTime( int newTime ) { this.newTime = newTime; }
        public int getNewTime() { return newTime; }
        public State getNewState() { return newState; }
        public int getReceivedMessageCount() { return receivedMessageCount; }
        public int getStartMessageCount() { return startMessageCount; }
        public int getStopMessageCount() { return stopMessageCount; }
        public int getResetMessageCount() { return resetMessageCount; }
        public State getPreviousState() { return previousState; }
        public long getSessionDuration() { return sessionDuration; }
        public int getTimeoutCount() { return timeoutCount; }
        public int getRefineDelta() { return refineDelta; }
        public int getTimeUnreachable() { return timeUnreachable; }
        public DatagramSocket getSocket() { return socket; }

        // Setters for use in between states.
        public void addToReceivedMessageCount( int prevReceivedMessageCount )
        {
            this.receivedMessageCount += prevReceivedMessageCount;
        }

        public void addToStartMessageCount( int prevSentMessageCount )
        {
            this.startMessageCount += prevSentMessageCount;
        }

        public void addToStopMessageCount( int prevStopMessageCount )
        {
            this.stopMessageCount += prevStopMessageCount;
        }

        public void addToResetMessageCount( int prevResetMessageCount )
        {
            this.resetMessageCount += prevResetMessageCount;
        }

        public void addToSessionDuration( long prevSessionTime )
        {
            this.sessionDuration += prevSessionTime;
        }

        public void addToTimeUnreachable( int timeUnreachable )
        {
            this.timeUnreachable = timeUnreachable;
        }

        public void addToTimeoutCount( int prevTimeoutCount )
        {
            this.timeoutCount += prevTimeoutCount;
        }
    }

    public interface TestParams
    {
        public String getName();
    }

    // Class used to specify the KeepAlive run parameters.
    public class StateSearchParams implements TestParams
    {
        private int deltaT;
        private int zMax;
        private int zMultiplier;
        private int minDeltaT;
        private SearchType type;


        private int z;
//        private int k; // handled by the server.
//        private int t; // handled by the RunData object.

        public int getDeltaT() { return deltaT; }
        public void setDeltaT( int deltaT ) { this.deltaT = deltaT; }

        public int getZMax() { return zMax; }
        public void setZMax(int zMax) { this.zMax = zMax; }

        public int getZMultiplier() { return zMultiplier; }
        public void setZMultiplier( int zMultiplier ) { this.zMultiplier = zMultiplier; }

        public int getZ() { return z; }
        public void setZ( int z ) { this.z = z; }

        public int getMinDeltatT() { return minDeltaT; }
        public void setMinDeltaT( int minDeltaT ) { this.minDeltaT = minDeltaT; }

        public SearchType getType() { return type; }
        public void setType( SearchType type ) { this.type = type; }

//        public int getK() { return k; }
//        public void setK( int k ) { this.k = k; }

//        public int getT() { return t; }
//        public void setT( int t ) { this.t = t; }

        @Override
        public String getName() { return "StateSearchParams"; }
    }

    public class StateRunParams implements TestParams
    {
        private double failureRate;
        private int maxRunCount; // Used for testing. Set to 0 for no maximum.

        public double getFailureRate() { return failureRate; }
        public void setFailureRate( double failureRate ) { this.failureRate = failureRate; }

        public int getMaxRunCount() { return maxRunCount; }
        public void setMaxRunCount( int maxRunCount ) { this.maxRunCount = maxRunCount; }

        @Override
        public String getName() { return "StateRunParams"; }
    }

    public class StateRefineParams implements TestParams
    {
        private int deltaT;
        private int zMultiplier;
        private int z;
        private int pingsPerAttempt;

        public int getDeltaT() { return deltaT; }
        public void setDeltaT( int deltaT ) { this.deltaT = deltaT; }

        public int getzMultiplier() { return zMultiplier; }
        public void setzMultiplier( int zMultiplier ) { this.zMultiplier = zMultiplier; }

        public int getZ() { return z; }
        public void setZ( int maxAttempts ) { this.z = maxAttempts; }

        public int getPingsPerAttempt() { return pingsPerAttempt; }

        public void setPingsPerAttempt( int pingsPerAttempt )
        {
            this.pingsPerAttempt = pingsPerAttempt;
        }

        @Override
        public String getName() { return "StateRefineParams"; }
    }

    public static class ConstantClientRunner implements Runnable
    {
        private SynchronizedTime sharedTime = null;
        private String[] args = null;

        public ConstantClientRunner( String[] args, SynchronizedTime sharedTime )
        {
            this.sharedTime = sharedTime;
            this.args = args;
        }

        @Override
        public void run()
        {
            try
            {
                Client client = new Client( "const", sharedTime, false );
                client.doConstantKeepAliveUDP( args );
            }
            catch( IOException e )
            {
                System.out.println( "Unexpected Exception: " + e );
                return;
            }
        }
    }

    public static class RandomClientRunner implements Runnable
    {
        private DatagramSocket socket = null;
        private String address = null;
        private int port = 0;
        private int multiplier = 0;
        private SynchronizedTime sharedTime = null;

        public RandomClientRunner( DatagramSocket socket, String address, int port, int multiplier,
                                   SynchronizedTime sharedTime )
        {
            this.socket = socket;
            this.address = address;
            this.port = port;
            this.multiplier = multiplier;
            this.sharedTime = sharedTime;
        }

        @Override
        public void run()
        {
            try
            {
                Client client = new Client( "randm", sharedTime, false );
                client.sendRandomNetworkTraffic( socket, address, port, sharedTime, multiplier );
            }
            catch( IOException e )
            {
                System.out.println( "Unexpected Exception: " + e );
                return;
            }
            catch( InterruptedException e )
            {
                System.out.println( "Unexpected Exception: " + e );
                return;
            }
        }
    }

    private static class PatternedTrafficClientRunner implements Runnable
    {
        private DatagramSocket socket = null;
        private String address = null;
        private int port = 0;
        private SynchronizedTime sharedTime = null;
        private String patternFilePath = null;

        public PatternedTrafficClientRunner( DatagramSocket socket, String address, int port,
                                           String patternFilePath )
        {
            this.socket = socket;
            this.address = address;
            this.port = port;
            this.patternFilePath = patternFilePath;

            this.sharedTime = new SynchronizedTime(); // NOT USED.
        }

        @Override
        public void run()
        {
            try
            {
                Client client = new Client( "patrn", sharedTime, false );
                client.sendPatternedNetworkTraffic( socket, address, port, patternFilePath, true );
            }
            catch( IOException e )
            {
                System.out.println( "Unexpected Exception: " + e );
                return;
            }
            catch( InterruptedException e )
            {
                System.out.println( "Patterned Traffic Generation Interrupted: " + e );
                return;
            }
        }
    }

    private static class BindingRequestHelper implements Runnable
    {
        private SynchronizedBindingRequestData sharedBRData = null;
        private long nextMessageTime = 0;
        private String serverAddressText = null;
        private int serverPort = 0;
        private int otherPort = 0;

        private Client client = null;

        public BindingRequestHelper( Client client, SynchronizedBindingRequestData sharedBRData,
                                     long nextMessageTime, String serverAddressText, int serverPort,
                                     int otherPort )
        {
            this.client = client;
            this.sharedBRData = sharedBRData;
            this.nextMessageTime = nextMessageTime;
            this.serverAddressText = serverAddressText;
            this.serverPort = serverPort;
            this.otherPort = otherPort;
        }

        @Override
        public void run()
        {
            if( this.sharedBRData == null )
            {
                client.logger.error( "No SynchronizedBindingRequestData object to update."
                        + " Exiting." );
                return;
            }

            DatagramSocket socket = null;
            try
            {
                socket = new DatagramSocket();

                int timeout = 0;
                if( this.sharedBRData != null )
                {
                    timeout = this.sharedBRData.getTime() + client.TIMEOUT_BUFFER_MS;
                }

                // Build a binding request message.
                net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
                message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_REQUEST );
                message.generateTransactionID();

                this.sharedBRData.setStunTransactionID( message.getTransactionID() );

                // If a response port was specified, add the attribute.
                if( this.otherPort > 0 )
                {
                    ResponsePort responsePortAttribute = new ResponsePort( this.otherPort );
                    message.addAttribute( responsePortAttribute.getName(), responsePortAttribute );
                }

                // Wait if necessary before sending the request.
                long difference = this.nextMessageTime - System.currentTimeMillis();
                if( difference > 0 )
                {
                    try
                    {
                        Thread.sleep( difference );
                    }
                    catch( InterruptedException e )
                    {
                        client.logger.error( "BindingRequestHelper interrupted." );
                    }
                }

                // Send UDP Request
                byte[] udpData = message.getBytes();
                client.sendUDP( udpData, serverAddressText, serverPort, socket );

                // Set the timeout.
                socket.setSoTimeout( timeout );

                // Wait for Response.
                // UDP must be received all at once (as far as I can tell).
                byte[] data = new byte[ Message.STUN_MAX_IPV4_SIZE ];
                DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
                socket.receive( receivedPacket );

                // Currently any response to this port is disregarded.
            }
            catch( SocketTimeoutException e )
            {
                if( client.isSharedTimeController() )
                {
                    client.logger.debug( "BindingRequestHelper Timeout." );
                    this.sharedBRData.setTimedOut();
                }
            }
            catch( IOException e )
            {
                client.logger.error( "Unexpected IO Exception in BindingRequestHelper: " + e );
            }
            finally
            {
                if( socket != null ) { socket.close(); }
            }
        }
    }

    public static class DelayedResetSender implements Runnable
    {
        private DatagramSocket socket;
        private InetAddress serverAddress;
        private int serverPort;
        private int sessionId;
        private Client parentClient = null;
        private long delay;
        private SynchronizedInteger clientNumResetTries;

        private boolean isScheduled = false;
        Thread curThread = null;

        public DelayedResetSender( DatagramSocket socket, InetAddress serverAddress,
                                   int serverPort, int sessionId,
                                   SynchronizedInteger clientNumResetTries, Client parentClient )
        {
            this.socket = socket;
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
            this.sessionId = sessionId;
            this.parentClient = parentClient;
            this.clientNumResetTries = clientNumResetTries;
        }

        @Override
        public void run()
        {
            while( true )
            {
                try
                {
                    long localDelay = 0;
                    synchronized( this )
                    {
                        localDelay = this.delay;
                    }

                    Thread.sleep( localDelay );
                    parentClient.logger.debug( "Sending RESET after " + delay + " ms delay." );
                    parentClient.sendCalcKAResetRequestUDP( socket, serverAddress, serverPort,
                                                            sessionId );
                    clientNumResetTries.increment();
                    parentClient.logger.debug( "RESET - delayed thread " + clientNumResetTries.getValue() );
                    break;
                }
                catch( InterruptedException e )
                {
                    parentClient.logger.debug( this.getClass().toString() + " was interrupted!" );
                    synchronized( this )
                    {
                        if( !isScheduled )
                        {
                            break;
                        }
                        else
                        {
                            continue;
                        }
                    }
                }
                catch( IOException e )
                {
                    parentClient.logger.error( this.getClass().toString() + " IOException: " + e );
                    break;
                }
            }
            synchronized( this )
            {
                isScheduled = false;
            }
        }

        public synchronized void scheduleOrRestartReset( long delay )
        {
            this.delay = delay;

            if( isScheduled )
            {
                parentClient.logger.debug( "Restarting current thread with delay: " + delay );
                curThread.interrupt();
            }
            else
            {
                parentClient.logger.debug( "Starting new thread for scheduled time with delay: "
                            + delay );
                isScheduled = true;
                curThread = new Thread( this );
                curThread.start();
            }
        }

        public synchronized void cancelReset()
        {
            if( isScheduled )
            {
                isScheduled = false;
                curThread.interrupt();
            }
            // Otherwise do nothing as nothing is scheduled.
        }
    }

    public static void main(String[] args)
        throws UnknownHostException, SocketException, IOException, InterruptedException
    {
        if( args.length == 0 )
        {
            System.out.println( "Please enter the type of action <keepalivetest | timingtest "
                    + "| bindingreqest_udp | bindingrequest_tcp");
            return;
        }

        String action = args[0];
        String serverAddress = "127.0.0.1";

        SynchronizedTime sharedTime = new SynchronizedTime();
        AddressPortMapping mapping = new AddressPortMapping();

        if( action.equals("ka") )
        {
            Client client = new Client( "calck", sharedTime, true );
            client.doCalcKeepAliveUDP( args, false, false, false );
        }
        if( action.equals("dka") )
        {
            // First test out Constant KeepAlive by itself.
//            Client client = new Client( "const", sharedTime, false );
//            client.doConstantKeepAliveUDP( args );
//            client.testTimeout();

            // Start the KeepAlive calculation process.
            Client client = new Client( "calck", sharedTime, true );
            client.doCalcKeepAliveUDP( args, true, false, false );
        }
        else if( action.equals("rka") )
        {
//            // TEST
//            DatagramSocket socket = new DatagramSocket();
//            String addressText = "107.145.122.174";
//            int port = 8000;
//            int multiplier = 100000;
//            client.sendRandomNetworkTraffic(socket, addressText, port, sharedTime, multiplier);

            Client client = new Client( "calck", sharedTime, true );
            client.doCalcKeepAliveUDP( args, false, true, false );
        }
        else if( action.equals("tka") )
        {
            Client client = new Client( "calck", sharedTime, true );
            client.doCalcKeepAliveUDP( args, false, false, true );

//            // FOR TESTING
//            DatagramSocket socket = new DatagramSocket();
//            String address = "107.145.122.174";
//            int port = 3478;
//            String filePath = "traffic.txt";
//            client.sendPatternedNetworkTraffic(socket, address, port, filePath, false);
        }
        else if( action.equals("bs") )
        {
            Client client = new Client( "bnsrc", sharedTime, false );
            client.doKeepAliveBinarySearch( args, 1, false );
        }
        else if( action.equals( "dbs") )
        {
          // Start the Binary Search KeepAlive calculation process.
          Client client = new Client( "bnsrch", sharedTime, true );
          client.doKeepAliveBinarySearch( args, 1, true );
        }
        else if( action.equals("cbs") )
        {
            Client client = new Client( "cbnsc", sharedTime, true );
            client.doClientControlledKeepAliveUDP( args, 1, false );
        }
        else if( action.equals("dcbs") )
        {
            Client client = new Client( "cbnsc", sharedTime, true );
            client.doClientControlledKeepAliveUDP( args, 1, true );
        }
        else if( action.equals("timingtest") )
        {
            Client client = new Client( "timet", sharedTime, true );
            client.doTimingTest( args );
        }
        else if( action.equals("br_udp") )
        {
            Client client = new Client( "brudp", sharedTime, false );
            serverAddress = args[1];
            client.doBindingRequestUDP( serverAddress, net.ddp2p.common.network.stun.stun.Message.STUN_PORT, mapping, false, 0 );
        }
        else if( action.equals("br_tcp") )
        {
            Client client = new Client( "brtcp", sharedTime, false );
            serverAddress = args[1];
            client.doBindingRequestTCP( serverAddress, net.ddp2p.common.network.stun.stun.Message.STUN_PORT, mapping, false, 0 );
        }
    }

    public Client()
    {
        logger = new Logger( true, true, true, true );

        this.clientId = (int)Math.floor( Math.random() * 1000 );
    }

    public Client( String name, SynchronizedTime sharedTime, boolean isSharedTimeController )
    {
        logger = new Logger( true, true, true, true );
        logger.setAgentName( name );
        this.sharedTime = sharedTime;

        this.clientId = (int)Math.floor( Math.random() * 1000 );
        if( sharedTime != null )
        {
            if( isSharedTimeController )
            {
                this.sharedTime.setControllerId( this.clientId );
            }
            else
            {
                this.sharedTime.setObserverId( this.clientId );
            }
        }

    }

    public int getClientId() { return clientId; }
    // Needs to be generated at creation time.

    private boolean isSharedTimeController()
    {
        return ( this.sharedTime.getControllerId() == clientId );
    }

    private boolean isSharedTimeObserver()
    {
        return ( this.sharedTime.getObserverId() == clientId );
    }

    private boolean shouldUpdateFromSharedTime( State state )
    {
        if(    state == State.RUN && this.sharedTime != null
           && (this.sharedTime.didTimeChange() || this.sharedTime.shouldStopThread()) )
        {
            if( isSharedTimeObserver() )
            {
                return true;
            }
        }
        return false;
    }

    private boolean shouldUpdateSharedTime( State state, int time )
    {
        if( state == State.SEARCH && this.sharedTime != null && time > 0 )
        {
            if( isSharedTimeController() )
            {
                return true;
            }
        }
        return false;
    }

    public long pingPeer( String peerAddressText, int peerPort, AddressPortMapping mapping )
            throws SocketException, IOException, PeerCommunicationException
    {
        InetAddress address = InetAddress.getByName( peerAddressText );

        byte[] data = mapping.getExternalBytes();

        DatagramSocket socket = new DatagramSocket( mapping.getInternalPort() );
        DatagramPacket packet = new DatagramPacket( data, data.length, address, peerPort );
        long startTime = System.currentTimeMillis();

        socket.send( packet ); // Creates a new mapping.

        logger.info( "UDP Sent " + data.length + " bytes of data to "
            + address.getHostAddress() + ":" + peerPort );

        socket.setSoTimeout( SERVER_TIMEOUT_MS );

        long endTime = 0;
        try
        {
            // Parse response.
            data = new byte[ MAX_MESSAGE_SIZE ];
            DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
            socket.receive( receivedPacket );

            endTime = System.currentTimeMillis();

            // Verify sender address.
            String sourceAddress = receivedPacket.getSocketAddress().toString();
            int colonIndex = sourceAddress.indexOf( ':' ); // remove extraneous information.
            sourceAddress = sourceAddress.substring( 1, colonIndex );

            if( ! sourceAddress.equals( peerAddressText ))
            {
                String errorMessage = "Source address of peer response doesn't match peer! "
                        + sourceAddress;
                logger.error( errorMessage );
                throw new PeerCommunicationException( errorMessage );
            }

            logger.info( "Received response from peer!\n" );
        }
        catch( SocketTimeoutException e )
        {
            String errorMessage = "Timed out while waiting for peer response.";
            logger.error( errorMessage);
            throw new PeerCommunicationException( errorMessage );
        }
        finally
        {
            socket.close();
        }

        return endTime - startTime;
    }

    public void doTimingTest( String[] args )
        throws IOException, InterruptedException
    {
        // Test out by connecting laptop to router modem with a cable.

        String stunDeviceAddress= "192.168.1.1";
        int stunDevicePort = 3478;;

        int numRuns = 1;
        String outputFilePath = "output.txt";
        String protocol = "UDP";
        String peerIP = "0";
        int peerPort = 1230;

        // General setup.
        Client client = new Client();
        AddressPortMapping mapping = new AddressPortMapping();

        // If there are no command line arguments, do one test run with the defaults.
        if( args.length == 0 )
        {
            // nothing to do.
        }
        // Otherwise check for the correct number of arguments.
        else if( args.length < 6 )
        {
            System.out.println( "usage: Client timingtest <Server IP> <Server Port> <Peer IP> "
                    + "<Port to Map> <Protocol> <Output File> <Number of Runs>" );
        }
        // Otherwise parse the arguments.
        else
        {
            stunDeviceAddress = args[1];
            stunDevicePort = Integer.parseInt( args[2] );
            peerIP = args[3];
            protocol = args[4];
            outputFilePath = args[5];
            numRuns = Integer.parseInt( args[6] );
        }

        // Run the tests.
        System.out.println( "Running with the following parameters:");
        System.out.println( "STUN Device IP:\t" + stunDeviceAddress );
        System.out.println( "PeerIP Address: " + peerIP );
        System.out.println( "Protocol:\t" + protocol );
        System.out.println( "Output File:\t" + outputFilePath );
        System.out.println( "Number of Runs:\t" + numRuns );

        for( int i = 0; i < numRuns; i++ )
        {
            long stunTime = 0;
            try
            {
                if( protocol.equalsIgnoreCase( "UDP" ) )
                {
                    stunTime = doBindingRequestUDP( stunDeviceAddress, stunDevicePort, mapping,
                                                    true, 0 );
                }
                else if( protocol.equalsIgnoreCase( "TCP" ) )
                {
                    stunTime = doBindingRequestTCP( stunDeviceAddress, stunDevicePort, mapping,
                                                    true, 0);
                }
                else
                {
                    // TODO: Error.
                }
            }
            catch( SocketTimeoutException e )
            {
                // The error was logged in the appropriate method.
                Thread.sleep( 32000 );
                continue;
            }

            long peerPingTime = 0;
            if( ! peerIP.equals( "0" ) )
            {
                try
                {
                    peerPingTime = client.pingPeer( peerIP, peerPort, mapping );

                    // Uses the default file encoding.
                    FileWriter writer = new FileWriter( outputFilePath, true );
                    writer.write( i + " " + stunTime + " " + peerPingTime + " "
                            +  (stunTime + peerPingTime) + "\n" );
                    writer.close();
                }
                catch( PeerCommunicationException e )
                {
                    // The error was logged in pingPeer().
                }
            }

            if( i < numRuns - 1 )
            {
                Thread.sleep( 32000 ); // 30 seconds seems to be the minimum.
            }
        }
    }

    public int generateSessionId()
    {
        BigInteger temp = new BigInteger( 32, new Random() );
        return temp.intValue();
    }

    public void doConstantKeepAliveUDP( String[] args )
        throws SocketException, IOException
    {
        if( args.length < 8 )
        {
            System.out.println( "usage: client keepalivegeometrictest <server address> <output file> "
                    + "<num tests>");
            return;
        }

        // Set the variables.
        String serverAddressText = args[1];
        String summaryFileName = args[2];
        String rawDataFileName = args[3];
        String logFileName = args[4];
        int deltaT = Integer.parseInt( args[5] );
        int serverPort = Integer.parseInt( args[6] );
        int numTests = Integer.parseInt( args[7] );

        String usesStarterMessageText = args[8];
        boolean usesStarterMessage = usesStarterMessageText.equals("usestarter") ? true : false;

//        serverPort++;

        logger.setOutputFilePath( logFileName );
        if( serverPort == 0 ) { serverPort = net.ddp2p.common.network.stun.stun.Message.STUN_PORT; }

        logger.info( "Client ID is " + clientId + ". Using Server Port " + serverPort );

        InetAddress serverAddress = InetAddress.getByName( serverAddressText );
        DatagramSocket socket = new DatagramSocket();
        logger.info( "Starting KeepAlive Process with " + serverAddressText );

        StateRunParams paramsRun = new StateRunParams();
        paramsRun.setFailureRate( 0.99 ); // timeouts are counted in the test, so keep going on failure.
        paramsRun.setMaxRunCount( 0 ); // unlimited

        int lastGoodTime = 10000;
        int lastTimeoutTime = 0;
        boolean lastTimeoutSet = false;
        RunData runData = new RunData( lastGoodTime, State.RUN, State.NONE );

        // TEMP (use value passed in from main thread.)
        StateSearchParams paramsSearch = new StateSearchParams();
        paramsSearch.setDeltaT(      deltaT );
        paramsSearch.setMinDeltaT(   1 );
        paramsSearch.setZMax(        0 );
        paramsSearch.setZMultiplier( 2 );
        paramsSearch.setZ(           1 );
        paramsSearch.setType(        SearchType.LINEAR );

        StateRefineParams paramsRefine = new StateRefineParams();
        paramsRefine.setDeltaT(           100 );
        paramsRefine.setzMultiplier(      2 );
        paramsRefine.setZ(                1 );
        paramsRefine.setPingsPerAttempt(  5 );

        // NOTE: Only some of the RunData components are used in this case.
        Map<State, RunData> previousStateData = new HashMap<State, RunData>();
        previousStateData.put( State.RUN, new RunData( 0, State.NONE, State.NONE ) );
        while( true )
        {

            // TODO: Move the socket creation inside the loop?
            // It may not matter for this one.

            // For some reason if this is outside the loop, there is an IO error that the socket
            // closed. Need to investigate.
            // That seems to be a fluke. Can't recreate it now.
//            if( socket != null ) { socket.close(); }
//            socket = new DatagramSocket();

            if( runData.getNewState() == State.EXIT )
            {
                break;
            }

            // -- FOR TESTING PURPOSES --
            // Check if the controller thread told us to stop.
            if( this.sharedTime.shouldStopThread() )
            {
                logger.info( "STOP flag set by controller thread. Exiting." );
                this.sharedTime.resetThreadStop(); // Reset! DUH.
                break;
            }

            // If there was a timeout, revert back to the last known good time.
            if( this.sharedTime.isTimedOut() )
            {
                logger.debug( "TIMED OUT: Updating from last good shared time: " + lastGoodTime );
                lastTimeoutTime = runData.getNewTime();
                lastTimeoutSet = true;
                runData.updateNewTime( lastGoodTime );
                this.sharedTime.resetTimedOut();
            }
            // Otherwise check for a new time, update, and start a new session.
            // If the new time is greater than or equal to the last timeout time, do not update.
            else if( this.sharedTime.didTimeChange() )
            {
//                if( lastTimeoutSet && this.sharedTime.getTime() >= lastTimeoutTime && lastTimeoutTime > 0 )
//                {
//                    logger.debug( "New shared time is greater than last timeout. Not updating." );
//                    logger.data( "new shared time", this.sharedTime.getTime(), "last timeout",
//                                 lastTimeoutTime );
//                }
//                else
//                {
                    logger.debug( "Updating from shared time: " + this.sharedTime.getTime());
                    lastGoodTime = runData.getNewTime();
                    runData.updateNewTime( this.sharedTime.getTime() );
//                }
                this.sharedTime.resetTimeChanged();
            }

            // Reusing the socket may help avoid timeouts, which would be good.
            // TODO: Change to TRUE if there are issues.
            runData = runKeepAliveLoopUDP( serverAddress, serverPort, socket, usesStarterMessage,
                    paramsSearch, paramsRun, paramsRefine, runData, true, false, false );

            // Update the metric totals.
            RunData current = previousStateData.get( State.RUN );
            current.addToReceivedMessageCount( runData.getReceivedMessageCount() );
            current.addToStartMessageCount( runData.getStartMessageCount() );
            current.addToTimeoutCount( runData.getTimeoutCount() );
            current.addToSessionDuration(
                    runData.getSessionDuration() );

        }

        RunData allRuns = previousStateData.get( State.RUN );
        FileWriter rawDataWriter = new FileWriter( rawDataFileName, true );
        rawDataWriter.write( "const\n" );
        rawDataWriter.write( allRuns.getReceivedMessageCount() + "\n" );
        rawDataWriter.write( allRuns.getStartMessageCount() + "\n" );
        rawDataWriter.write( allRuns.getTimeoutCount() + "\n" );
        rawDataWriter.write( allRuns.getSessionDuration() + "\n" );
        rawDataWriter.write( runData.getNewTime() + "\n" );
        rawDataWriter.close();
    }

    // TODO: This should probably be put in a separate thread.
    public void doCalcKeepAliveUDP( String[] args, boolean usesTwoChannel,
                                    boolean generatesRandomTraffic,
                                    boolean generatesPatternedTraffic )
        throws SocketException, IOException, InterruptedException
    {
        if( args.length < 8 )
        {
            System.out.println( "usage: client keepalivetest <server address> <output file> "
                    + "<num tests>");
            return;
        }

        // Set the variables.
        String serverAddressText = args[1];
        String summaryFileName = args[2];
        String rawDataFileName = args[3];
        String logFileName = args[4];
        int deltaT = Integer.parseInt( args[5] );
        int serverPort = Integer.parseInt( args[6] );
        int numTests = Integer.parseInt( args[7] );

        String usesStarterMessageText = args[8];
        boolean usesStarterMessage = usesStarterMessageText.equals("usestarter") ? true : false;

        logger.setOutputFilePath( logFileName );
        if( serverPort == 0 ) { serverPort = net.ddp2p.common.network.stun.stun.Message.STUN_PORT; }

        logger.info( "Client ID is " + clientId + ". Using Server Port " + serverPort );

        InetAddress serverAddress = InetAddress.getByName( serverAddressText );
        DatagramSocket socket = new DatagramSocket();

        Thread constantRunner = null;
        Thread randomRunner = null;
        Thread patternedRunner = null;

        // Set the socket reuse policy based on whether or not random traffic is being generated.
        // If there is random traffic, the socket needs to remain open.
        boolean reusesSocket = (generatesRandomTraffic || generatesPatternedTraffic);

        logger.info( "Starting KeepAlive Process with " + serverAddressText );

        List<Map<State, TestParams>> testParams = getAllTestParams( deltaT, SearchType.GEOMETRIC);

        for( int j = 0; j < testParams.size(); j++ )
        {
            // Get the parameters for the current test.
            StateSearchParams paramsSearch = (StateSearchParams)testParams.get(j).get( State.SEARCH );
            StateRunParams paramsRun = (StateRunParams)testParams.get(j).get( State.RUN );
            StateRefineParams paramsRefine = (StateRefineParams)testParams.get(j).get( State.REFINE );

            // Set up the file writer with initial session information for the summary.
            FileWriter summaryWriter = new FileWriter( summaryFileName, true );

            summaryWriter.write( "----- NEW SESSION -----\n" );
            summaryWriter.write( "Number of Tests: " + numTests + "\n" );
            summaryWriter.write( "SEARCH PARAMETERS\n" );
            summaryWriter.write( "deltaT: " + paramsSearch.getDeltaT()
                    + " | zMax: " + paramsSearch.getZMax()
                    + " | zMultiplier: " + paramsSearch.getZMultiplier()
                    + " | z: " + paramsSearch.getZ()
                    + "\n" );
            summaryWriter.write( "RUN PARAMETERS\n" );
            summaryWriter.write( "failureRate: " + paramsRun.getFailureRate()
                    + " | maxRunCount: " + paramsRun.getMaxRunCount()
                    + "\n" );
            summaryWriter.write( "REFINE PARAMETERS\n" );
            summaryWriter.write( "deltaT: " + paramsRefine.getDeltaT()
                    + " | changeMultiplier: " + paramsRefine.getzMultiplier()
                    + " | maxAttempts: " + paramsRefine.getZ()
                    + " | pingsPerAttempt: " + paramsRefine.getPingsPerAttempt()
                    + "\n" );
            summaryWriter.close();

            RunData runData = null;
            for( int i = 0; i < numTests; i++ )
            {
                runData = new RunData( deltaT, State.SEARCH, State.NONE );
                List<String> stateData = new ArrayList<String>();

                // Initialize the state counts.
                Map<State, Integer> stateCounts = new HashMap<State, Integer>();
                stateCounts.put( State.SEARCH, 0 );
                stateCounts.put( State.RUN, 0 );
                stateCounts.put( State.REFINE, 0 );

                // NOTE: Only some of the RunData components are used in this case.
                Map<State, RunData> previousStateData = new HashMap<State, RunData>();
                previousStateData.put( State.SEARCH, new RunData( 0, State.NONE, State.NONE) );
                previousStateData.put( State.RUN, new RunData( 0, State.NONE, State.NONE ) );
                previousStateData.put( State.REFINE, new RunData( 0, State.NONE, State.NONE ) );

                if( usesTwoChannel )
                {
                    if( constantRunner != null && constantRunner.isAlive() )
                    {
                        // Depending on timing, it's possible that the last runner thread missed the
                        // STOP signal before the new runner thread was started and caught it
                        // instead.
                        logger.debug( "constant runner still running. stopping." );
                        sharedTime.setThreadStop();
                        constantRunner.join();
                        constantRunner = null;
                    }

                    // Start the second thread.
                    constantRunner =
                            new Thread (new Client.ConstantClientRunner( args, sharedTime ) );
                    constantRunner.start();
                }

                if( generatesRandomTraffic )
                {
                    if( randomRunner != null && randomRunner.isAlive() )
                    {
                        // Depending on timing, it's possible that the last runner thread missed the
                        // STOP signal before the new runner thread was started and caught it
                        // instead.
                        logger.debug( "random runner still running. stopping." );
                        sharedTime.setThreadStop();
                        randomRunner.join();
                        randomRunner = null;
                    }

                    // Start the second thread.
                    randomRunner =
                            new Thread( new Client.RandomClientRunner(socket, serverAddressText,
                                                                      serverPort,
                                                                      RANDOM_MS_MULTIPLIER,
                                                                      sharedTime ) );
                    randomRunner.start();
                }

                if( generatesPatternedTraffic )
                {
//                    if( patternedRunner != null && patternedRunner.isAlive() )
//                    {
//                        // Depending on timing, it's possible that the last runner thread missed the
//                        // STOP signal before the new runner thread was started and caught it
//                        // instead.
//                        logger.debug( "patterned traffic runner still running. stopping." );
//                        patternedRunner.interrupt();
//                        patternedRunner.join();
//                        logger.debug( "patterned traffic runner stopping successfully." );
//                        patternedRunner = null;
//                    }
//
//                    // Start the second thread.
//                    patternedRunner =
//                            new Thread(
//                                    new Client.PatternedTrafficClientRunner(socket,
//                                                                            serverAddressText,
//                                                                            serverPort,
//                                                                            "network_traffic//skype1.txt") );
//                    patternedRunner.start();
                    sendGenTrafStartRequest( socket, serverAddress, serverPort );
                }

                while( true )
                {
                    // Don't recycle the socket if generating random traffic.
                    if( !reusesSocket )
                    {
                        logger.debug( "In outer loop. Closing socket." );
                        socket = new DatagramSocket();
                    }


                    if( runData.getPreviousState() != State.NONE )
                    {
                        stateData.add( runData.getPreviousState().name()
                                + "\tReceived: " + runData.getReceivedMessageCount()
                                + "\tStart: " + runData.getStartMessageCount()
                                + "\tStop: " + runData.getStopMessageCount()
                                + "\tResets: " + runData.getResetMessageCount()
                                + "\tTimeouts: " + runData.getTimeoutCount()
                                + "\tTime Unreachable: " + runData.getTimeUnreachable()
                                + "\tSessionDurationAbs: " + runData.getSessionDuration()
                                + "\tTime: " + runData.getNewTime() );

                        State previousState = runData.getPreviousState();

                        // Increment the state count.
                        stateCounts.put( previousState, stateCounts.get( previousState ) + 1 );

                        // Update the total metrics for the state.
                        RunData current = previousStateData.get( previousState );
                        current.addToReceivedMessageCount( runData.getReceivedMessageCount() );
                        current.addToStartMessageCount( runData.getStartMessageCount() );
                        current.addToStopMessageCount( runData.getStopMessageCount() );
                        current.addToResetMessageCount( runData.getResetMessageCount() );
                        current.addToTimeoutCount( runData.getTimeoutCount() );
                        current.addToSessionDuration(
                                runData.getSessionDuration() );
                        current.addToTimeUnreachable(
                                runData.getTimeUnreachable() );
                    }

                    if( runData.getNewState() == State.EXIT )
                    {
                        break;
                    }

                    // DEBUG DEBUG DEBUG
                    if( runData.getNewState() == State.RUN )
                    {
                        break;
                    }

                    runData = runKeepAliveLoopUDP( serverAddress, serverPort, socket,
                                                   usesStarterMessage, paramsSearch, paramsRun,
                                                   paramsRefine, runData, reusesSocket, true, true );
                } // TEST TEST TEST LAST PARAMETER!

                // Log the totals for each state.
                summaryWriter = new FileWriter( summaryFileName, true );
                summaryWriter.write( "-----\n" );
                for( String s : stateData )
                {
                    summaryWriter.write( s + "\n");
                    System.out.println( s );
                }
                summaryWriter.write( "=====\n" );

                RunData searchTotals = previousStateData.get( State.SEARCH );
                RunData runTotals = previousStateData.get( State.RUN );
                RunData refineTotals = previousStateData.get( State.REFINE );

                String rawData = "";

                rawData +=  searchTotals.getReceivedMessageCount()
                        + "\t" + runTotals.getReceivedMessageCount()
                        + "\t" + refineTotals.getReceivedMessageCount() + "\n";
                rawData +=   searchTotals.getStartMessageCount()
                        + "\t" + runTotals.getStartMessageCount()
                        + "\t" + refineTotals.getStartMessageCount() + "\n";
                rawData +=   searchTotals.getStopMessageCount()
                        + "\t" + runTotals.getStopMessageCount()
                        + "\t" + refineTotals.getStopMessageCount() + "\n";
                rawData +=   searchTotals.getResetMessageCount()
                        + "\t" + runTotals.getResetMessageCount()
                        + "\t" + refineTotals.getResetMessageCount() + "\n";
                rawData +=   searchTotals.getTimeoutCount()
                        + "\t" + runTotals.getTimeoutCount()
                        + "\t" + refineTotals.getTimeoutCount() + "\n";
                rawData +=   searchTotals.getTimeUnreachable()
                        + "\t" + runTotals.getTimeUnreachable()
                        + "\t" + refineTotals.getTimeUnreachable() + "\n";
                rawData +=   searchTotals.getSessionDuration()
                        + "\t" + runTotals.getSessionDuration()
                        + "\t" + refineTotals.getSessionDuration() + "\n";

                rawData +=   stateCounts.get( State.SEARCH )
                        + "\t" + stateCounts.get( State.RUN )
                        + "\t" + stateCounts.get( State.REFINE ) + "\n";
                int stateCountsTotal = stateCounts.get( State.SEARCH )
                        + stateCounts.get( State.RUN ) + stateCounts.get( State.REFINE );
                rawData += (stateCountsTotal-1) + "\n" ;

                long durationTotal = searchTotals.getSessionDuration()
                        + runTotals.getSessionDuration()
                        + refineTotals.getSessionDuration();
                rawData += durationTotal + "\n";

                rawData += runData.getNewTime() + "\t";
                rawData += runData.getTimeUnreachable() + "\n";

                summaryWriter.write( rawData );
                summaryWriter.close();

                FileWriter rawDataWriter = new FileWriter( rawDataFileName, true );
                if( i == 0 )
                {
                    rawDataWriter.write( paramsSearch.getZMultiplier() + ":"
                            + paramsSearch.getZMax() + "\n" );
                }
                rawDataWriter.write( rawData );
                rawDataWriter.close();

                // -- FOR TESTING PURPOSES --
                // Tell the observer thread to stop (if one is running).
                if( this.sharedTime != null & isSharedTimeController() )
                {
                    logger.info( "Setting STOP flag for shared time." );
                    this.sharedTime.setThreadStop();
                }

                // Pause to work around server locking issue.
                // This also allows the constant thread to finish up if it's waiting on a server
                // response. It should be at least as long as the deltaT found.
                Thread.sleep( runData.getNewTime() + TIMEOUT_BUFFER_MS );

                // TODO: maybe add feature to check if time changed from state to state.
                // TODO: Try nanoTime().
            }

            // Just to make sure the other threads have ended.
            if( constantRunner != null && constantRunner.isAlive() )
            {
                // Depending on timing, it's possible that the last runner thread missed the
                // STOP signal before the new runner thread was started and caught it
                // instead.
                logger.debug( "constant runner still running after all tests. stopping." );
                sharedTime.setThreadStop();
                constantRunner.join();
                constantRunner = null;
            }
            if( randomRunner != null && randomRunner.isAlive() )
            {
                // Depending on timing, it's possible that the last runner thread missed the
                // STOP signal before the new runner thread was started and caught it
                // instead.
                logger.debug( "random runner still running after all tests. stopping." );
                sharedTime.setThreadStop();
                randomRunner.join();
                randomRunner = null;
            }
            if( patternedRunner != null && patternedRunner.isAlive() )
            {
                // Depending on timing, it's possible that the last runner thread missed the
                // STOP signal before the new runner thread was started and caught it
                // instead.
                logger.debug( "patterned traffic runner still running after all tests. stopping." );
                patternedRunner.interrupt();
                patternedRunner.join();
                logger.debug( "patterned traffic runner stopping successfully." );
                patternedRunner = null;
            }
        }
    }

    // Returns the time for use with the RUN state of the optimization.
    public RunData runKeepAliveLoopUDP( InetAddress serverAddress,
                                        int serverPort,
                                        DatagramSocket socket,
                                        boolean usesStarterMessage,
                                        StateSearchParams paramsSearch,
                                        StateRunParams paramsRun,
                                        StateRefineParams paramsRefine,
                                        RunData runData,
                                        boolean reuseSocket, // TODO: Leave this in?
                                        boolean usesRampUp,
                                        boolean compensatesForOtherTraffic )
        throws IOException
    {

        // Metrics variables.
        // ------------------
        long sessionDuration = 0;

        int numStartMessages = 0;      // The number of outgoing session start STUN messages
                                       // for experimental use.
        int numTotalInMessages = 0;    // The number of response messages from the server.
        int numSessionInMessages = 0;  // The number of response messages from the server
                                       // for the current session.
        int numTotalStopMessages = 0;  // The number of STOP messages sent to the server.
        int numTimeouts = 0;           // The number of timeouts encountered.
        int refineDelta = 0;           // The final delta for the REFINE state.
                                       // TODO: Not currently used. May not be needed.
        int numTotalResetTries = 0;    // The number of RESET messages sent by the client.
        int sumTimedOutValues = 0;     // The sum of all attempted "time" values that timed out
                                       // before the response reached the client.

        // Variables from parameters.
        // --------------------------
        int time = runData.getNewTime();
        State state = runData.getNewState();

        // Initialize base values with SEARCH parameters.
        double baseDeltaT = paramsSearch.getDeltaT();
        int z = paramsSearch.getZ();
        int zMultiplier = paramsSearch.getZMultiplier();

        // Other local variables.
        //-----------------------
        int k = 0;
        int lastGoodTime = 0;
        int maxGoodTime = 0;
        State newState = state;

        // Used with SEARCH state.
        byte serverAdjustsZ = KeepAliveTimer.FALSE;

        // Used with the SEARCH state.
        long lastResetAttempt = 0;
        SynchronizedInteger numResetTries = new SynchronizedInteger();

        // Used with the RUN state.
        int numTotalIterations = 0;
        double intervalFromServer = 0; // TODO: Should be able to remove this, as we have z. CHECK.
        int runRunIterations = 0;

        // Used with the REFINE state.
        int numRefineIterations = 0;
        int numRefineTimeouts = 0;
        boolean refineStateMinFound = false;
        boolean isInitialRefineStateValue = true;
        int zForServer = 1; // REFINE state always uses a Z of 1 for the server.

        // Used with both REFINE and RUN states.
        byte actionAfterMax = KeepAliveTimer.STAY_CONSTANT;
        boolean currentlyInRampUp = true;
        double deltaTAfterMax = 0;
        byte direction = KeepAliveTimer.DIRECTION_FORWARD;
        byte numRepeats = 0;

        // General session initiation.
        //----------------------------
        long sessionStartTimestamp = System.currentTimeMillis();
        int id = generateSessionId();
        boolean isFirstMessage = true;

        boolean shouldExitNext = false;
        boolean lastAttemptStarterSkipped = false;

        logger.info("Starting KeepAlive loop with socket reuse = \"" + reuseSocket + "\" in the "
        + state.name() + " state with time " + time );
        logger.info( "Local Port: " + socket.getLocalPort() );

        // Any additional setup needed before the main loop.
        if( state == State.RUN || state == State.REFINE )
        {
            maxGoodTime = time;               // The time to eventually get to after the build-up.
            time = paramsSearch.getDeltaT();  // Start with the initial deltaT for the build-up.
            if( !usesRampUp )
            {
                time = maxGoodTime;
            }
            logger.debug( "Setting the MAX good time to " + maxGoodTime );
            switch( state )
            {
                case RUN:
                    actionAfterMax = KeepAliveTimer.STAY_CONSTANT;
                    numRepeats = (byte)paramsRun.getMaxRunCount();
                    break;
                case REFINE:
                    actionAfterMax = KeepAliveTimer.CHANGE_BACKWARD;
                    numRepeats = (byte)paramsRefine.getPingsPerAttempt();
                    deltaTAfterMax = paramsRefine.getDeltaT();
                    z = paramsRefine.getZ();
                    break;
                default:
                    logger.error( "The client should not be in the state " + state + "! Exiting." );
                    return new RunData( 0, State.EXIT, state );
            }
        }

        // Main loop.
        while( true )
        {
            // TODO: Look into NOT exiting the loop, just updating the time and starting a new
            // session while remaining in the function.
            // Update or stop based on a SynchronizedTime shared object if appropriate.
            if( shouldUpdateFromSharedTime( state ) )
            {
                logger.info( "Received shared time notification...stopping session." );
                sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, id );
                numTotalStopMessages++;

                time = lastGoodTime; // When exiting, use the last known good time.
                                     // In the RUN state this will have no effect.
                newState = state;    // Do not change the state.
                break;
            }

            // CHECK FOR DEBUGGING.
            if( !verifyTimeIsValid( time ) )
            {
                newState = State.EXIT;
                break;
            }

            // -----------------------------------------------------------------
            // Set the timeout duration and general session variables.
            //------------------------------------------------------------------

            // Default searchType is LINEAR, so the incrementType must correspond.
            byte incrementType = KeepAliveTimer.LINEAR_INCREMENT;

            // NOTE: Needs to be done here (different from pseudocode) so one exists initially.
            // Set the appropriate timeout.
            int timeout = time + TIMEOUT_BUFFER_MS;

            // If in the SEARCH state, verify that the delta greater than the minimum. If it is,
            // exit to the RUN state since nothing will be accomplished in the SEARCH state.
            if( state == State.SEARCH )
            {
                // TODO: Add this quick check to pseudo-code.
                if( shouldExitNext )
                {
                    // Only exit if the last message received was not a "starter"
                    // message.
                    int baseNumMessages = usesStarterMessage ? 1 : 0;
                    if( numSessionInMessages > baseNumMessages || lastAttemptStarterSkipped )
                    {
                        sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, id );
                        numTotalStopMessages++;

                        sendGenTrafStopRequest( socket, serverAddress, serverPort ); // TEST

                        logger.info( "Exiting with time = " + lastGoodTime );

                        time = lastGoodTime;
                        newState = State.RUN;
                        break;
                    }
                    else
                    {
                        logger.info( "Should exit, but last message was a \"starter\"." );
                        lastAttemptStarterSkipped = true;
                    }
                }

                if( checkShouldExit( currentlyInRampUp, baseDeltaT, paramsSearch.getMinDeltatT(),
                                     deltaTAfterMax, z, time, state ) )
                {
//                    sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, id );
//                    numTotalStopMessages++;
//
//                    sendGenTrafStopRequest( socket, serverAddress, serverPort ); // TEST

//                    time = lastGoodTime;
//                    newState = State.RUN;
//                    break;
                    shouldExitNext = true;
                }

                incrementType = determineIncrementType( paramsSearch.getType(), numTimeouts );

                timeout = determineTimeout( currentlyInRampUp, time, k, baseDeltaT, baseDeltaT,
                                            z, paramsSearch.getZMultiplier(), serverAdjustsZ,
                                            incrementType, usesStarterMessage,
                                            numSessionInMessages, state );

                // SEARCH uses the client Z value.
                zForServer = z;

            }
            else if (     (state == State.RUN || state == State.REFINE)
                      && ((!isFirstMessage && intervalFromServer > 0) // intervalFromServer probably not needed.
                      || isFirstMessage ) )

            {

                timeout = determineTimeout( currentlyInRampUp, time, k, paramsSearch.getDeltaT(),
                                            deltaTAfterMax, z, paramsSearch.getZMultiplier(),
                                            serverAdjustsZ, incrementType, usesStarterMessage,
                                            numSessionInMessages, state );

                // Needed?
                // Set up the deltaT.
                // For the REFINE and RUN states the normal deltaT doesn't change.
//                if( ( baseDeltaT / z ) < paramsSearch.getMinDeltatT() )
//                {
//                    deltaTAfterMax = baseDeltaT / z;
//                }

                if( state == State.REFINE )
                {
                    // Prevent the time from the RUN state from being tested again.
                    // TODO: Verify that this is needed. Server already switches to backward.
                    if( isInitialRefineStateValue )
                    {
                        int oldMaxGoodTime = maxGoodTime;
                        maxGoodTime = maxGoodTime - 1 * (paramsRefine.getDeltaT() / z);
                        isInitialRefineStateValue = false;
                        logger.debug( "REFINE state initial time - setting from " + oldMaxGoodTime
                                + " to " + maxGoodTime );

                        if( !usesRampUp )
                        {
                        	time = maxGoodTime;
                        }
                    }

                    if( checkShouldExit( currentlyInRampUp, baseDeltaT,
                                         paramsSearch.getMinDeltatT(), deltaTAfterMax, z, time,
                                         state ) )
                    {
                        time = lastGoodTime;
                        newState = State.RUN;
                        break;
                    }
                }
            }

            socket.setSoTimeout( timeout );

            // -----------------------------------------------------------------
            // Send request message if appropriate.
            //------------------------------------------------------------------

            byte usesStarterMessageByte = usesStarterMessage ? KeepAliveTimer.USES_STARTER_MESSAGE
                    : KeepAliveTimer.NO_STARTER_MESSAGE;

            if( isFirstMessage )
            {
                // Generate a new ID if this is the first message.
                id = generateSessionId();
                logger.info( "NEW SESSION ID (new session start): " + id );

                currentlyInRampUp = true; // Resets every time.

                logger.debug("Sending first message for new session...");
                sendCalcKARequestUDP( socket, serverAddress, serverPort, id, time, baseDeltaT,
                                  incrementType, usesStarterMessageByte, direction, numRepeats,
                                  maxGoodTime, actionAfterMax, serverAdjustsZ, zForServer,
                                  zMultiplier, deltaTAfterMax, k );

                numStartMessages++;
                isFirstMessage = false;

                String searchTypeName = (paramsSearch != null) ? paramsSearch.getType().name() : "";
                logger.data( "NEW REQUEST -> time", time, "timeout", timeout,
                             "est. next time", (timeout - TIMEOUT_BUFFER_MS),
                             "lastGoodTime", lastGoodTime,
                             "baseDeltaT", baseDeltaT,
                             "z", z, "deltaTAfterMax", deltaTAfterMax,
                             "intervalFromServer", intervalFromServer );
                logger.data( "searchType", searchTypeName, "direction", direction );
                logger.data( "numRepeats", numRepeats, "numTotalIterations", numTotalIterations,
                             "numRunIterations", runRunIterations,
                             "numRefineIterations", numRefineIterations, "id", id );
            }
            else
            {
                logger.data( "time", time, "timeout", timeout,
                             "est. next time", (timeout - TIMEOUT_BUFFER_MS) );
            }

            numTotalIterations++;

            // -----------------------------------------------------------------
            // Wait for a response.
            // -----------------------------------------------------------------
            try
            {
                boolean stateChange = false;
                DelayedResetSender resetSender = new DelayedResetSender( socket, serverAddress,
                                                                         serverPort, id,
                                                                         numResetTries, this );

                // This loop is only used to allow continued listening if ID is not the one
                // expected.
                while( true )
                {
                    KeepAliveTimer timer = listenValidateAndParseTimer( socket, id,
                    		                                            compensatesForOtherTraffic,
                    		                                            serverAddress,
                    		                                            serverPort);
                    if( timer == null )
                    {
                        // In this situation the message was invalid or not applicable.
                        if( compensatesForOtherTraffic )
                        {
                            if( numResetTries.getValue() < MAX_RESET_TRIES )
                            {
                                long timeSinceLastAttempt =
                                        System.currentTimeMillis() - lastResetAttempt;
                                if( timeSinceLastAttempt > MIN_RESET_INTERVAL_MS )
                                {
                                    logger.info( "Sending reset request for current session: "
                                            + id );
                                    sendCalcKAResetRequestUDP( socket, serverAddress, serverPort,
                                                               id );
                                    numResetTries.increment();
                                    logger.debug( "RESET - main thread " + numResetTries.getValue() );
                                    lastResetAttempt = System.currentTimeMillis();
                                }
                                else
                                {
                                    logger.debug( "Not enough time since last RESET attempt. "
                                            + " Scheduling or resetting delayed message. " );
                                    resetSender.scheduleOrRestartReset( 2000 );
                                }
                            }
                            else
                            {
                                logger.info( "Max RESET tries reached. Using last good value: "
                                        + lastGoodTime );
                                sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, id );
                                numTotalStopMessages++;
                                // TODO: Try out performing the same action as when there is a
                                // timeout.

                                k++;
                                logger.debug( "After MAX resets, Updating k to " + k );

                                // TEST
                                z *= zMultiplier;
                                logger.debug( "RESET z is now "  + z );

                                // If the server was incrementing z, the client needs to make up for the
                                // lost value.
                                if( serverAdjustsZ == KeepAliveTimer.TRUE )
                                {
                                    if( numSessionInMessages > 0 )
                                    {
                                        z *= zMultiplier;
                                        logger.debug( "RESET To sync with server, z is now "  + z );
                                    }
                                }
                                // No "serverAdjustsZ" setup here.

                                if (   paramsSearch.getType() == SearchType.LINEAR
                                   || (paramsSearch.getType() == SearchType.SWITCH && numTimeouts > 1) )
                                {
                                    time = (int)(lastGoodTime + k * (baseDeltaT / z));
                                }
                                else if(    paramsSearch.getType() == SearchType.GEOMETRIC
                                        || (paramsSearch.getType() == SearchType.SWITCH
                                            && numTimeouts <= 1) )
                                {
                                    time = (int)(lastGoodTime + Math.pow( 2, k )
                                            * (baseDeltaT / z));
                                }

                                logger.debug( "RESET new time after z adjust: " + time );


                                logger.data( "z", z, "k", k, "time", time );
                                if( paramsSearch.getZMax() > 0 && z > paramsSearch.getZMax() )
                                {
                                    logger.info( "z greater than " + paramsSearch.getZMax()
                                            +" - breaking." );
                                    time = lastGoodTime; // Use the last known good time for the RUN state.
                                    stateChange = true;
                                    newState = State.RUN;
                                }

                                numTotalResetTries += numResetTries.getValue();
                                logger.debug( "RESET - Updating total reset tries: " + numTotalResetTries
                                        + " with " + numResetTries.getValue() );

                                isFirstMessage = true;
                                numResetTries.setValue( 0 );
                                numSessionInMessages = 0;
                                // END TEST

//                                stateChange = true;
//                                newState = State.RUN;
                                break;
                            }
                        }

                        continue;
                    }

                    if( compensatesForOtherTraffic )
                    {
                        numTotalResetTries += numResetTries.getValue();
                        logger.debug( "RESET - Updating total reset tries: " + numTotalResetTries
                                + " with " + numResetTries.getValue() );
                        numResetTries.setValue( 0 );
                    }

                    numTotalInMessages++;
                    numSessionInMessages++;
                    logger.debug( "(" + state + ") accepted message - numTotalInMessages: "
                            + numTotalInMessages + " | numSessionInMessages: "
                            + numSessionInMessages );

                    // If this is the first response for this session, do not change anything,
                    // as it will have been sent immediately.
                    // If time is 0, the first actual message will also be immediate.
                    if( usesStarterMessage && numSessionInMessages == 1 && time != 0 )
                    {
                        logger.data( "Received Expected ID", id, "k", k );
                        logger.debug( "--> FIRST SESSION MESSAGE <--" );
                    }
                    else
                    {
                        k = timer.getK();
                        intervalFromServer = timer.getDeltaT(); // TODO: IS this needed anymore?
                        switch( state )
                        {
                            case SEARCH:
                            case RUN:
                                z = timer.getZ();
                                lastGoodTime = timer.getTime();
                                time = lastGoodTime;
                                logger.data( "Received Expected ID", id, "k", k, "time", time,
                                             "z", z, "deltaT (local)", paramsSearch.getDeltaT()/z );

                                // Exit if z > zMax
                                if( paramsSearch.getZMax() > 0 && z > paramsSearch.getZMax() )
                                {
                                    logger.info( "z greater than " + paramsSearch.getZMax()
                                            +" - breaking." );
                                    time = lastGoodTime;
                                    stateChange = true;
                                    newState = State.RUN;
                                }

                                // Update the shared timer if appropriate.
                                if( shouldUpdateSharedTime( state, time ) )
                                {
                                    logger.debug( "Updating shared time to " + lastGoodTime );
                                    this.sharedTime.setTime( lastGoodTime );
                                    this.sharedTime.setTimeChanged();
                                }

                                if( state == State.SEARCH ) { break; }

                                if( currentlyInRampUp )
                                {
                                    if( time >= maxGoodTime )
                                    {
                                        logger.debug( "RUN ramp-up complete.");
                                        currentlyInRampUp = false;
                                        runRunIterations++;
                                    }
                                }
                                else
                                {
                                    logger.data( "numRunIterations: ", runRunIterations );
                                    runRunIterations++;

                                    // If the max run count is set to 0, then ignore it.
                                    // Otherwise, exit the loop if the max has been exceeded.
                                    if(    paramsRun.getMaxRunCount() > 0
                                        && runRunIterations >= paramsRun.getMaxRunCount() )
                                    {
                                        // Prevent the server session from continuing.
                                        sendCalcKAStopRequestUDP( socket, serverAddress, serverPort,
                                                                  id );
                                        numTotalStopMessages++;

                                        stateChange = true;
                                        newState = State.EXIT;
                                    }
                                }
                                break;
                            case REFINE:
                                logger.data( "Received Expected ID", id, "k", k,
                                        "numRefineIterations", numRefineIterations, "repeatCount",
                                        paramsRefine.getPingsPerAttempt(),
                                        "time", time, "new time", timer.getTime() );

                                direction = timer.getDirection();
                                // Switch the local state if the server switched
                                if( currentlyInRampUp )
                                {
                                    time = timer.getTime();
                                    logger.debug( "Updating time to " + time );
                                    if( time >= maxGoodTime ) // check if the time reached max
                                    {
                                        logger.debug( "REFINE ramp-up complete.");
                                        currentlyInRampUp = false;
                                        numRefineIterations++;
                                    }
                                    else
                                    {
                                        logger.debug( "REFINE still in ramp-up" );
                                    }

                                    if( !usesRampUp )
                                    {
                                    	logger.debug( "No ramp-up, so switching direction manually "
                                    			+ " based on refineStateMinFound: " + refineStateMinFound);
                                    	if( refineStateMinFound )
                                    	{
                                    		direction = KeepAliveTimer.DIRECTION_FORWARD;
                                    		actionAfterMax = KeepAliveTimer.CHANGE_FORWARD;
                                    	}
                                    	else
                                    	{
                                    		// For local use only to stop a change to the RUN state.
                                    		// If the next item times out the client doesn't know
                                    		// the direction changed to backward without a ramp-up.
                                    		direction = KeepAliveTimer.DIRECTION_BACKWARD;
                                    	}
                                    }
                                }
                                else
                                {
                                    numRefineIterations++;

                                    if( numRefineIterations == paramsRefine.getPingsPerAttempt() )
                                    {
                                        numRefineIterations = 0;
                                        lastGoodTime = timer.getTime();
                                        time = lastGoodTime;

                                        // Update the shared timer if appropriate.
                                        if( shouldUpdateSharedTime( state, time ) )
                                        {
                                            logger.debug( "Updating shared time to " + lastGoodTime );
                                            this.sharedTime.setTime( lastGoodTime );
                                            this.sharedTime.setTimeChanged();
                                        }

                                        if( direction == KeepAliveTimer.DIRECTION_BACKWARD )
                                        {
                                            logger.debug( "Switching to FORWARD direction." );
                                            refineStateMinFound = true;
                                            numRefineTimeouts = 0;

                                            // TODO: Is this needed?
                                            z *= paramsRefine.getzMultiplier();
                                            // I think the following line divides one too many times.
//                                            deltaTAfterMax =  deltaTAfterMax / z;
                                            logger.debug( "Changing deltaT -> z: " + z +
                                                    " deltaTAfterMax: " + deltaTAfterMax );
                                            // Always use LINEAR for REFINE (more precise).
                                            time = (int)(time + 1 * (deltaTAfterMax / z));

                                            numSessionInMessages = 0;
                                            isFirstMessage = true;
                                            direction = KeepAliveTimer.DIRECTION_FORWARD;
                                            actionAfterMax = KeepAliveTimer.CHANGE_FORWARD;
                                            maxGoodTime = time;

                                            // Reset time for ramp-up
                                            if( usesRampUp ) { time = paramsSearch.getDeltaT(); }

                                            // Stop the session and reverse the direction.
                                            sendCalcKAStopRequestUDP( socket, serverAddress,
                                                                      serverPort, id );
                                            numTotalStopMessages++;

                                            if( !reuseSocket )
                                            {
                                                logger.debug( "REFINE Closing socket AFTER STOP request...");
                                                if( socket != null ) { socket.close(); }
                                                socket = new DatagramSocket();
                                            }
                                        }
                                        // else (FORWARD direction) do nothing and wait for timeout
                                    }
                                    else
                                    {
                                        if( time != timer.getTime() )
                                        {
                                            logger.debug( "Time from server changed: " + time
                                                    + " : " + timer.getTime() );
                                            numRefineIterations = 0;
                                        }
                                        time = timer.getTime();
                                    }
                                }
                                break;
                            default:
                                logger.error( "Invalid client state: " + state );
                                return new RunData( 0, state, State.NONE );
                        }
                    }
                    break;
                }

                if( stateChange )
                {
                    break;
                }
            }

            // -----------------------------------------------------------------
            // Timeout.
            //------------------------------------------------------------------
            catch( SocketTimeoutException e )
            {
                // Add to the sum of times that are too large. This is only relevant for the
                // single channel SEARCH state.
                logger.debug( "Updating sum of timeout values " + sumTimedOutValues + " with " +
                    (timeout - TIMEOUT_BUFFER_MS) );
                sumTimedOutValues += timeout - TIMEOUT_BUFFER_MS;

                isFirstMessage = true;

                logger.info( "TIMEOUT waiting for ID: " + id );
                numTimeouts++;
                numRefineTimeouts++;

                // Send a STOP message to the server to avoid stray messages.
                sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, id );
                numTotalStopMessages++;

                // Skip the immediate first message if appropriate.
                int baseMessageCount = usesStarterMessage ? 1 : 0;

                if( numSessionInMessages <= baseMessageCount )
                {
                    logger.debug( "TIMEOUT on first attempt of session. Keeping k at " + k );
                }
                // Otherwise increment k to keep in sync with the server.
//                else
//                {
//                    k++;
//                }

                if( compensatesForOtherTraffic )
                {
                    numTotalResetTries += numResetTries.getValue();
                    logger.debug( "RESET - (timeout) Updating total reset tries: "
                            + numTotalResetTries + " with " + numResetTries.getValue() );
                    numResetTries.setValue( 0 );
                }

                boolean stateChange = false;
                switch( state )
                {
                    case SEARCH:
                        z *= zMultiplier;
                        logger.debug( "z is now "  + z );

                        // If the server was incrementing z, the client needs to make up for the
                        // lost value.
                        if( serverAdjustsZ == KeepAliveTimer.TRUE )
                        {
                            if( numSessionInMessages > baseMessageCount )
                            {
                                z *= zMultiplier;
                                logger.debug( "To sync with server, z is now "  + z );
                            }
                        }
                        else
                        {
                            serverAdjustsZ = KeepAliveTimer.TRUE;
                        }

                        if (   paramsSearch.getType() == SearchType.LINEAR
                           || (paramsSearch.getType() == SearchType.SWITCH && numTimeouts > 1) )
                        {
                            time = (int)(lastGoodTime + k * (baseDeltaT / z));
                        }
                        else if(    paramsSearch.getType() == SearchType.GEOMETRIC
                                || (paramsSearch.getType() == SearchType.SWITCH
                                    && numTimeouts <= 1) )
                        {
                            time = (int)(lastGoodTime + Math.pow( 2, k )
                                    * (baseDeltaT / z));
                        }

                        logger.data( "z", z, "k", k, "time", time );
                        if( paramsSearch.getZMax() > 0 && z > paramsSearch.getZMax() )
                        {
                            logger.info( "z greater than " + paramsSearch.getZMax()
                                    +" - breaking." );
                            time = lastGoodTime; // Use the last known good time for the RUN state.
                            stateChange = true;
                            newState = State.RUN;
                        }
                        break;
                    case RUN:
                        // Exit when the failures are below the acceptable threshold at the
                        // predefined deltaT. If one of the first few attempts fail this
                        // will change the state.
                        logger.debug( "RUN TIMEOUT: "
                                + ( ( (double)numTimeouts) / (numTotalIterations + 1) ) );
                        if( ((double)numTimeouts) / (numTotalIterations + 1)
                                > paramsRun.getFailureRate() )
                        {
                                stateChange = true;

                                // If this client is the shared time observer, set the timeout
                                // flag so the time can revert to the last known good time.
                                // In this case the client will stay in the RUN state.
                                if( this.sharedTime != null && isSharedTimeObserver() )
                                {
                                    this.sharedTime.setTimedOut();
                                    newState = State.RUN;
                                }
                                // Otherwise change to the REFINE state.
                                else
                                {
                                    if( currentlyInRampUp && time < maxGoodTime )
                                    {
                                        // Set the time to the "max" so the REFINE can start
                                        // from there.
                                        time = maxGoodTime;
                                    }
                                    newState = State.REFINE;
                                }
                        }
                        break;
                    case REFINE:
                        numRefineIterations = 0;
                        // In the reverse direction, do the usual calculation, but don't change
                        // direction for the new session.
                        if( direction == KeepAliveTimer.DIRECTION_BACKWARD )
                        {
                            // Keep the backwards direction going.
                            direction = KeepAliveTimer.DIRECTION_FORWARD;
                            actionAfterMax = KeepAliveTimer.CHANGE_BACKWARD;

                            // Do not adjust z since the idea is to increase until a "good" value
                            // is found, and then reverse.

                            // Always use LINEAR for REFINE (more precise).
                            // When going backward ramp-up is already done.
                            // Use the number of timeouts as a multiplier to speed up the process.
                            time = (int)(time - numRefineTimeouts * (deltaTAfterMax / z));

                            lastGoodTime = time;
                            maxGoodTime = time;

                            // Reset time for new ramp-up.
                            if( usesRampUp ) { time = paramsSearch.getDeltaT(); }

                            logger.debug( "Timeout in REVERSE direction. Continuing decrement "
                                    +" with " + maxGoodTime );

                            if( !reuseSocket )
                            {
                                logger.debug( "REFINE Closing socket AFTER STOP request...");
                                if( socket != null ) { socket.close(); }
                                socket = new DatagramSocket();
                            }
                        }
                        else if( direction == KeepAliveTimer.DIRECTION_FORWARD )
                        {
                            // If it times out during ramp-up, but a good time has already been
                            // found, fall back to that.
                            if( currentlyInRampUp && !refineStateMinFound )
                            {
                                // Stop the session and reverse.
                                direction = KeepAliveTimer.DIRECTION_FORWARD;
                                actionAfterMax = KeepAliveTimer.CHANGE_BACKWARD;

                                // Adjust backwards (from the time that timed out) using default
                                // deltaT.
                                time = maxGoodTime;
                                time = (int)(time - numRefineTimeouts * (deltaTAfterMax / z));

                                // NOTE: This doesn't count as a direction change for the timeout
                                // count purposes.

                                lastGoodTime = time;
                                maxGoodTime = time;

                                if( usesRampUp ) { time = paramsSearch.getDeltaT(); }

                                logger.debug( "Timeout in FORWARD direction during ramp-up. "
                                        +"Decrementing with " + maxGoodTime );

                                logger.debug( "Switching to REVERSE direction." );

                                if( !reuseSocket )
                                {
                                    logger.debug( "REFINE Closing socket AFTER STOP request...");
                                    if( socket != null ) { socket.close(); }
                                    socket = new DatagramSocket();
                                }
                            }
                            else
                            {
                                logger.debug( "Timeout in forward direction. Exiting to RUN with "
                                        + lastGoodTime );

                                time = lastGoodTime;
                                stateChange = true;
                                newState = State.RUN;
                            }
                        }
                        break;
                    default:
                        logger.error( "Invalid client state: " + state );
                        return new RunData( 0, state, State.NONE );
                }

                // Generate a new session ID.
                numSessionInMessages = 0;
                id = generateSessionId();
                logger.info( "NEW SESSION ID (after timeout): " + id );

                if( !reuseSocket )
                {
                    logger.debug( "Closing socket AFTER timeout and STOP request...");
                    if( socket != null ) { socket.close(); }
                    socket = new DatagramSocket();
                }

                if( stateChange )
                {
                    break;
                }
            }
        }

        // Determine the total time that the client was "unreachable".
        // Subtract the number of timeouts multiplied by the known good time from the
        // sum of the interval time values that timed out.
        int timeUnreachable = sumTimedOutValues - ( time * numTimeouts );
        logger.data( "numTimeouts", numTimeouts, "reachableTime for instances", ( time * numTimeouts ),
                     "unreachableTime", timeUnreachable );

        long sessionStopTimestamp = System.currentTimeMillis();
        sessionDuration = sessionStopTimestamp - sessionStartTimestamp;

        return new RunData( time, newState, state, numTotalInMessages, numStartMessages,
                            numTotalStopMessages, numTotalResetTries, sessionDuration, numTimeouts,
                            refineDelta, timeUnreachable );
    }

    // FOR DEBUGGING
    public boolean verifyTimeIsValid( int time )
    {
        if( time < 0 )
        {
            logger.error( "Time is invalid! (< 0) " + time );
            return false;
        }
        return true;
    }

    public boolean checkShouldExit( boolean inRampUp, double baseDeltaT, double minDeltaT,
                                    double deltaTAfterMax, int z, int time, State state )
    {
        // NOTE: RUN state does not need any of these checks so it is omitted.
        switch( state )
        {
            case SEARCH:
                if( ( baseDeltaT / z ) < minDeltaT || time < 0 )
                {
                    logger.debug( "Condition Check TRUE for SEARCH state exit - baseDeltaT: "
                            + baseDeltaT + "z: " + z + " baseDeltaT/z: " + ( baseDeltaT / z )
                            + " time: " + time );
                    return true;
                }
                return false;
            case REFINE:
                if( (baseDeltaT / z) < minDeltaT || (!inRampUp && deltaTAfterMax < minDeltaT ) )
                {
                    // TODO; This shouldn't happen.
                    logger.error( "REFINE Delta is at min or < min! " );
                    return true;
                }
                return false;
        }
        logger.error( "checkShouldExit() is in invalid state: " + state );
        return true;
    }

    public byte determineIncrementType( SearchType searchType, int numTimeouts )
    {
        byte incrementType = 0;
        if(   searchType == SearchType.LINEAR
           || (searchType == SearchType.SWITCH && numTimeouts > 0) )
        {
            incrementType = KeepAliveTimer.LINEAR_INCREMENT;
        }
        else if(   searchType == SearchType.GEOMETRIC
                || (searchType == SearchType.SWITCH && numTimeouts == 0) )
        {
            incrementType = KeepAliveTimer.GEOMETRIC_INCREMENT;
        }
        return incrementType;
    }

    public int determineTimeout( boolean inRampUp, int time, int k, double searchDeltaT,
                                 double otherDeltaT, int z, int zMultiplier,
                                 byte serverAdjustsZ, byte incrementType,
                                 boolean usesStarterMessage, int numSessionMessages, State state )
    {
        int timeout = 0;
        int baseNumMessages = usesStarterMessage ? 1 : 0;
        switch( state )
        {
            case SEARCH:
                if( numSessionMessages <= baseNumMessages )
                {
                    timeout = time + TIMEOUT_BUFFER_MS;
                }
                else
                {
                    logger.debug( "K : " + k );
                    int localZ = z;
                    if( serverAdjustsZ == KeepAliveTimer.TRUE ) { localZ *= zMultiplier; }
                    switch( incrementType )
                    {
                        case KeepAliveTimer.LINEAR_INCREMENT:
                           timeout = (int)Math.ceil( time + k * (searchDeltaT / localZ)
                                   + TIMEOUT_BUFFER_MS );
                           break;
                        case KeepAliveTimer.GEOMETRIC_INCREMENT:
                           timeout = (int)Math.ceil( time + (int)Math.pow( 2, k )
                                   * (searchDeltaT/localZ) + TIMEOUT_BUFFER_MS );
                           break;
                    }
                }
                break;
            case RUN:
            case REFINE:
                if( numSessionMessages <= baseNumMessages )
                {
                    timeout = time + TIMEOUT_BUFFER_MS;
                }
                else
                {
                    // NOTE: This may be inaccurate depending on the maxTime up
                    // until the client successfully receives a message from the
                    // server after ramp-up.
                    int tempK = numSessionMessages - 1;
                    logger.debug("TEMP K : " + tempK );
                    double tempDeltaT = searchDeltaT; // Not modified in this state.
                    if( !inRampUp )
                    {
                        tempDeltaT = otherDeltaT / z;
                        tempK = 1;
                    }
                    switch( incrementType )
                    {
                        case KeepAliveTimer.LINEAR_INCREMENT:
                           timeout = (int)Math.ceil( time + tempK * (tempDeltaT)
                                   + TIMEOUT_BUFFER_MS );
                           break;
                        case KeepAliveTimer.GEOMETRIC_INCREMENT:
                           timeout = (int)Math.ceil( time + (int)Math.pow( 2, tempK )
                                   * (tempDeltaT) + TIMEOUT_BUFFER_MS );
                           break;
                    }
                }
                break;
        }
        return timeout;
    }

    public KeepAliveTimer listenValidateAndParseTimer( DatagramSocket socket, int id,
    		                                           boolean compensatesForOtherTraffic,
    		                                           InetAddress serverAddress, int serverPort )
        throws IOException
    {
        byte[] inData = new byte[ Message.STUN_MAX_IPV4_SIZE ];
        DatagramPacket inPacket = new DatagramPacket( inData, inData.length );
        socket.receive( inPacket );
        Message receivedMessage = new Message( inData );
        KeepAliveTimer timer = null;

        // Discard messages that are not of expected response type.
        short messageType = ByteBuffer.allocate( 2 )
                                      .put( receivedMessage.getMessageType() )
                                      .getShort( 0 );
        if( messageType != Message.STUN_CALC_KEEPALIVE_RESPONSE )
        {
            logger.debug( "System Time: " + System.currentTimeMillis()
            + " - Expected different message type. Received: "
            + messageType );

            return null;
        }

        timer = (KeepAliveTimer)receivedMessage.getAttribute( "KeepAliveTimer" );

        // Discard messages that don't have matching ID's.
        // There may be stray ID's from the wheel (although very unlikely).
        if( id != timer.getId() )
        {
            logger.debug( "System Time: " + System.currentTimeMillis()
            + " - Expecting a different KeepAliveTimer session ID."
            + " (Expecting: " + id + " Received: " + timer.getId() + ")" );

            // Send a STOP request if not compensating for other traffic.
            if( !compensatesForOtherTraffic )
            {
            	// NOTE: This isn't counted in the total stop messages metric since
            	// it should not occur under normal circumstances.
                sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, timer.getId() );
            }

            return null;
        }

        return timer;
    }

    // TODO: This will be used to test the binary search method against the STUN_KA_CALC
    // method with improved parameters.
    public long doKeepAliveBinarySearch( String[] args, int minTimeChange, boolean usesTwoChannel )
        throws IOException, InterruptedException
    {
        // Parse the arguments.
        String serverAddressText = args[1];
        String summaryFileName = args[2];
        String rawDataFileName = args[3];
        String logFileName = args[4];
        int time = Integer.parseInt( args[5] );
        int serverPort = Integer.parseInt( args[6] );
        int numTests = Integer.parseInt( args[7] );

        String usesStarterMessageText = args[8];
        byte usesStarterMessage = KeepAliveTimer.NO_STARTER_MESSAGE;
        if( usesStarterMessageText.equals("usestarter") )
        {
            usesStarterMessage = KeepAliveTimer.USES_STARTER_MESSAGE;
        }

        logger.setOutputFilePath( logFileName );
        if( serverPort == 0 ) { serverPort = net.ddp2p.common.network.stun.stun.Message.STUN_PORT; }

        logger.info( "Using Server Port " + serverPort );

        Thread constantRunner = null;

        for( int i = 0; i < numTests; i++ )
        {
            time = Integer.parseInt( args[5] );
            int min = 0;
            int max = 0;
            int multiplier = 2;
            int lastTime = time;

            // TODO: Record metrics and save data to files.
            // Variables for metrics.
            int numTimeouts = 0;
            int numReceivedMessages = 0;
            int numReceivedNFMessages = 0;
            int numSentMessages = 0;

            boolean shouldExitMainLoop = false;

            InetAddress serverAddress = InetAddress.getByName( serverAddressText );
            DatagramSocket socket = null;
            logger.info( "Starting KeepAlive Process with " + serverAddressText );

            int receivedMessageCount = 0; // Two messages need to be received during each iteration
                                          // since the wheel sends one message immediately.

            if( usesTwoChannel )
            {
                if( constantRunner != null && constantRunner.isAlive() )
                {
                    // Depending on timing, it's possible that the last runner thread missed the
                    // STOP signal before the new runner thread was started and caught it
                    // instead.
                    logger.debug( "constant runner still running. stopping." );
                    sharedTime.setThreadStop();
                    constantRunner.join();
                    constantRunner = null;
                }

                // Start the second thread.
                constantRunner =
                        new Thread (new Client.ConstantClientRunner( args, sharedTime ) );
                constantRunner.start();
            }

            long startTimestamp = System.currentTimeMillis();

            while( true )
            {
                // Set up the request message.
                int id = generateSessionId();

                if( socket != null ) { socket.close(); }
                socket = new DatagramSocket();

                sendCalcKARequestUDP( socket, serverAddress, serverPort, id, time, 0,
                        KeepAliveTimer.PLACEHOLDER, usesStarterMessage,
                        KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER,
                        KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER,
                        KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER,
                        KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER,
                        KeepAliveTimer.PLACEHOLDER );
                numSentMessages++;

                // Set the timeout.
                int timeout =  time + TIMEOUT_BUFFER_MS;
                socket.setSoTimeout( timeout );
                logger.data( "NEW REQUEST -> timeout",  timeout, "time", time,
                             "min", min, "max", max, "id", id );

                // Wait for the response.
                try
                {
                    // This loop is only used to allow continued listening if ID is not the one
                    // expected, or if it is the first message.
                    while( true )
                    {
                        byte[] inData = new byte[ Message.STUN_MAX_IPV4_SIZE ];
                        DatagramPacket inPacket = new DatagramPacket( inData, inData.length );
                        socket.receive( inPacket );
                        Message receivedMessage = new Message( inData );

                        // Verify the type.
                        short messageType = ByteBuffer.allocate( 2 )
                                                      .put( receivedMessage.getMessageType() )
                                                      .getShort( 0 );
                        if( messageType != Message.STUN_CALC_KEEPALIVE_RESPONSE )
                        {
                            logger.error( "Expected a different message type than: " + messageType );
                            continue;
                        }

                        KeepAliveTimer timer =
                                (KeepAliveTimer)receivedMessage.getAttribute( "KeepAliveTimer" );
                        // Discard messages that don't have matching ID's.
                        // There may be stray ID's from the wheel (although very unlikely).
                        if( id != timer.getId() )
                        {
                            logger.debug( "System Time: " + System.currentTimeMillis()
                                + " - Expecting a different KeepAliveTimer session ID."
                                + " (Expecting: " + id + " Received: " + timer.getId() + ")" );
                            continue;
                        }

                        logger.data( "Received Expected ID", id );

                        receivedMessageCount++;
                        numReceivedMessages++; // Total for test run.

                        // Ignore the first (immediate) message for the session if "starter" messages
                        // are being used.
                        if(    usesStarterMessage == KeepAliveTimer.USES_STARTER_MESSAGE
                            && receivedMessageCount == 1 )
                        {
                            logger.debug( "Received first message for ID: " + id );
                            continue;
                        }

                        receivedMessageCount = 0;
                        numReceivedNFMessages++; // Total for test run.

                        // Send a STOP message for the current session.
                        sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, id );

                        // Set the minimum known time.
                        min = timer.getTime();
                        logger.debug( "Setting new min time: " + min );

                        if( this.sharedTime != null && isSharedTimeController() )
                        {
                            this.sharedTime.setTime( min );
                            this.sharedTime.setTimeChanged();
                        }

                        // TODO: The server should just send messages at a constant rate like for
                        // RUN and REFINE states. All calculations are done by the client.

                        // If a max has not yet been discovered, double the time deltaT.
                        if( max == 0 )
                        {
                            time = time * multiplier;
                            logger.debug( "No max yet, multiply time: " + time );
                        }
                        // Otherwise update the time deltaT based on the min and max.
                        else
                        {
                            lastTime = time;
                            time = ( min + max ) / 2;
                            // If the time deltaT does not change, exit the loop.
                            if( Math.abs(lastTime - time) < minTimeChange )
                            {
                                logger.debug( "NTO time change below min (" + minTimeChange +"). exiting loop: "
                                        + " time: " + time + " lastTime: " + lastTime );
                                shouldExitMainLoop = true;
                                break;
                            }
                            else
                            {
                                logger.debug( "Performing binary search: (" + min + ", " + max
                                        + ")" + " time: " + time );
                            }
                        }

                        if( max == min  )
                        {
                            logger.debug( "min and max are the same. exiting loop." );
                            shouldExitMainLoop = true;
                            break;
                        }

                        break;
                    }
                }
                catch( SocketTimeoutException e )
                {
                    logger.info( "Timeout waiting for ID: " + id );

                    numTimeouts++; // Total for test run.

                    // Stop the current session.
                    sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, id );

                    // Set the max based on the current test value.
                    max = time;
                    logger.debug( "Setting new max time: " + max );

                    // Adjust the deltaT to (min + max) / 2.
                    lastTime = time;
                    time = ( min + max ) / 2;
                    logger.debug( "Performing binary search: (" + min + ", " + max + ")" + " time: "
                            + time + " timeChange: " + (lastTime - time) );

                    // If the time deltaT does not change, exit the loop.
                    if( Math.abs(lastTime - time) < minTimeChange )
                    {
                        logger.debug( "TO time change below min (" + minTimeChange +"). exiting loop: "
                                + " time: " + time + " lastTime: " + lastTime );
                        shouldExitMainLoop = true;
                        break;
                    }
                }

                if( shouldExitMainLoop )
                {
                    break;
                }
            }

            long stopTimestamp = System.currentTimeMillis();
            long duration = stopTimestamp - startTimestamp;

            // Log the relevant information (messages, NFmessages, timeouts, final time, duration)
            FileWriter rawWriter = new FileWriter( rawDataFileName, true );
            rawWriter.write( numReceivedMessages + "\n"  + numReceivedNFMessages + "\n"
                    + numSentMessages + "\n" + numTimeouts + "\n" );
            rawWriter.write( duration + "\n" + min + "\n" );
            rawWriter.close();

            if( this.sharedTime != null && isSharedTimeController() )
            {
                logger.debug( "Setting STOP flag for constant thread." );
                this.sharedTime.setThreadStop();
            }

            // Pause for at least as long as the found deltaT to allow the constant thread
            // to finish up. It can't be interrupted.
            Thread.sleep(  min + TIMEOUT_BUFFER_MS );
        }

        // Just to make sure the other threads have ended.
        if( constantRunner != null && constantRunner.isAlive() )
        {
            // Depending on timing, it's possible that the last runner thread missed the
            // STOP signal before the new runner thread was started and caught it
            // instead.
            logger.debug( "constant runner still running after all tests. stopping." );
            sharedTime.setThreadStop();
            constantRunner.join();
            constantRunner = null;
        }

        return 0;
    }

    public void doClientControlledKeepAliveUDP( String[] args, int minTimeChange,
                                                boolean usesTwoChannel )
        throws UnknownHostException, SocketException, IOException, InterruptedException
    {
        // Parse the arguments.
        String serverAddressText = args[1];
//        String summaryFileName = args[2];
        String rawDataFileName = args[3];
        String logFileName = args[4];
        int time = Integer.parseInt( args[5] );
        int serverPort = Integer.parseInt( args[6] );
        int numTests = Integer.parseInt( args[7] );

        int initialTime = time;

        logger.setOutputFilePath( logFileName );
        if( serverPort == 0 ) { serverPort = net.ddp2p.common.network.stun.stun.Message.STUN_PORT; }

        logger.info( "Using Server Port " + serverPort );

        logger.info( "Starting KeepAlive Process with " + serverAddressText );

        DatagramSocket socketA = null;

        Thread constantRunner = null;

        for( int i = 0; i < numTests; i++ )
        {
            long startTimestamp = System.currentTimeMillis();

            time = initialTime;
            int min = 0;
            int max = 0;
            int multiplier = 2;
            int lastTime = time;

            // Variables for metrics.
            int numTimeouts = 0;
            int numImmediateReceivedMessagesA = 0;
            int numReceivedMessagesA = 0;
            int numSentMessages = 0;

            boolean shouldExitMainLoop = false;

            if( usesTwoChannel )
            {
                if( constantRunner != null && constantRunner.isAlive() )
                {
                    // Depending on timing, it's possible that the last runner thread missed the
                    // STOP signal before the new runner thread was started and caught it
                    // instead.
                    logger.debug( "constant runner still running. stopping." );
                    sharedTime.setThreadStop();
                    constantRunner.join();
                    constantRunner = null;
                }

                // Start the second thread.
                constantRunner =
                        new Thread (new Client.ConstantClientRunner( args, sharedTime ) );
                constantRunner.start();
            }

            while( true )
            {
                // Recreate the socket to avoid leftover bindings interfering.
                // SocketB is created/destroyed in the helper class.
                if( socketA != null ) { socketA.close(); }
                socketA = new DatagramSocket();

                // Create the initial binding on Socket A.
                try
                {
                    while( true )
                    {
                        // Build a binding request message.
                    	net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
                        message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_REQUEST );
                        message.generateTransactionID();

                        // Send UDP Request
                        byte[] udpData = message.getBytes();
                        sendUDP( udpData, serverAddressText, serverPort, socketA );
                        numSentMessages++;

                        // Set the timeout ( initial ).
                        socketA.setSoTimeout( SERVER_TIMEOUT_MS );

                        // Wait for Response.
                        // UDP must be received all at once (as far as I can tell).
                        byte[] data = new byte[ Message.STUN_MAX_IPV4_SIZE ];
                        DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
                        socketA.receive( receivedPacket );

                        InetSocketAddress originAddress = (InetSocketAddress)receivedPacket
                                                          .getSocketAddress();
                        logger.info( "UDP Received data from "
                                + originAddress.getAddress().getHostAddress() + " : "
                                + originAddress.getPort() );

                        // Process the response message.
                        Message receivedMessage = new Message( data );

                        if( ! Arrays.equals( receivedMessage.getTransactionID(),
                                             message.getTransactionID() ) )
                        {
                            logger.info( "(initial) Received unexpected STUN ID: "
                                    + message.getTransactionID() );
                            continue; // Keep listening for the correct response message.
                        }

                        logger.info( "(intitial) Received expected STUN ID: "
                                + message.getTransactionID() );

                        // This creates more message traffic as compared to the server.
                        numImmediateReceivedMessagesA++;

                        break;
                    }
                }
                catch( SocketTimeoutException e )
                {
                    logger.error( "Unable to establish initial binding on Socket A: " + e );
                    continue;
                }

                // Send a binding request on Socket B that will re-direct a response to Socket A.
                SynchronizedBindingRequestData sharedBRData = new SynchronizedBindingRequestData();
                sharedBRData.setTime( time );
                long nextMessageTime = System.currentTimeMillis() + time;
                BindingRequestHelper brh = new BindingRequestHelper( this, sharedBRData,
                                                                     nextMessageTime,
                                                                     serverAddressText, serverPort,
                                                                     socketA.getLocalPort() );
                new Thread( brh ).start();
                numSentMessages++; // The new thread sends a request.

                // Listening on Socket A.
                try
                {
                    while( true )
                    {
                        // Set the timeout ( initial ).
                        socketA.setSoTimeout( time  + TIMEOUT_BUFFER_MS );

                        logger.info( "Listening on Socket A" );

                        // Listen for a response.
                        byte[] data = new byte[ Message.STUN_MAX_IPV4_SIZE ];
                        DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
                        socketA.receive( receivedPacket );

                        // Get the transaction ID of the request from Socket B.
                        byte[] stunTransactionId = sharedBRData.getStunTransactionID();

                        logger.info( "Received Response on Socket A." );
                        Message receivedMessage = new Message( data );

                        if( ! Arrays.equals( receivedMessage.getTransactionID(),
                                             stunTransactionId ) )
                        {
                            logger.info( "(second) Received unexpected STUN ID: "
                                    + stunTransactionId );
                            continue; // Keep listening for the correct response message.
                        }

                        logger.info( "(second) Received expected STUN ID: " + stunTransactionId );
                        numReceivedMessagesA++;

                        // Set the minimum known time.
                        if( min == time )
                        {
                            logger.debug( "No change from last min time. exiting loop." );
                            shouldExitMainLoop = true;
                            break;
                        }
                        min = time;
                        logger.debug( "Setting new min time: " + min );

                        // Update the shared time object if it appropriate.
                        if( this.sharedTime != null && isSharedTimeController() )
                        {
                            logger.debug( "Updating the shared time to " + time );
                            this.sharedTime.setTime( time );
                            this.sharedTime.setTimeChanged();
                        }

                        // If a max has not yet been discovered, double the time deltaT.
                        if( max == 0 )
                        {
                            time = time * multiplier;
                            logger.debug( "No max yet, multiply time: " + time );
                        }
                        // Otherwise update the time deltaT based on the min and max.
                        else
                        {
                            lastTime = time;
                            time = ( min + max ) / 2;
                            // If the time deltaT does not change, exit the loop.
                            if( Math.abs(lastTime - time) < minTimeChange )
                            {
                                logger.debug( "NTO time change below min (" + minTimeChange +"). exiting loop: "
                                        + "min: " + min );
                                shouldExitMainLoop = true;
                                break;
                            }
                            else
                            {
                                logger.debug( "Performing binary search: (" + min + ", " + max
                                        + ")" + " time: " + time );
                            }
                        }

                        if( max == min )
                        {
                            logger.debug( "max and min are the same. exiting loop." );
                            shouldExitMainLoop = true;
                        }

                        break;
                    }
                }
                catch( SocketTimeoutException e )
                {
                    logger.info( "Timeout waiting on Socket A." );

                    numTimeouts++; // Total for test run.

                    // Set the max based on the current test value.
                    max = time;
                    logger.debug( "Setting new max time: " + max );

                    // Adjust the deltaT to (min + max) / 2.
                    lastTime = time;
                    time = ( min + max ) / 2;
                    logger.debug( "Performing binary search: (" + min + ", " + max + ")" + " time: "
                            + time + " timeChange: " + (lastTime - time) );

                    // If the time deltaT does not change, exit the loop.
                    if( Math.abs(lastTime - time) < minTimeChange )
                    {
                        logger.debug( "TO time change below min (" + minTimeChange +"). exiting loop: "
                                + "min: " + min );
                        shouldExitMainLoop = true;
                        break;
                    }
                }

                if( shouldExitMainLoop )
                {
                    break;
                }
            }

            long endTimestamp = System.currentTimeMillis();
            long duration = endTimestamp - startTimestamp;

            // Write the results to file.
            FileWriter rawDataWriter = new FileWriter( rawDataFileName, true );
            rawDataWriter.write( "client_only:" + initialTime + "\n" );
            rawDataWriter.write( numReceivedMessagesA + "\n" );
            rawDataWriter.write( numImmediateReceivedMessagesA + "\n" );
            rawDataWriter.write( numSentMessages + "\n" );
            rawDataWriter.write( numTimeouts + "\n" );
            rawDataWriter.write( duration + "\n" );
            rawDataWriter.write( min  + "\n" );
            rawDataWriter.close();

            // If the shared time object is being used, stop the other thread.
            if( this.sharedTime != null && isSharedTimeController() )
            {
                logger.debug( "Setting STOP flag for constant thread." );
                this.sharedTime.setThreadStop();
            }

            // Pause for at least as long as the found deltaT to allow the constant thread
            // to finish up. It can't be interrupted.
            Thread.sleep(  min + TIMEOUT_BUFFER_MS );
        }

        // Just to make sure the other threads have ended.
        if( constantRunner != null && constantRunner.isAlive() )
        {
            // Depending on timing, it's possible that the last runner thread missed the
            // STOP signal before the new runner thread was started and caught it
            // instead.
            logger.debug( "constant runner still running after all tests. stopping." );
            sharedTime.setThreadStop();
            constantRunner.join();
            constantRunner = null;
        }
    }

    public long doBindingRequestUDP( String serverAddress, int serverPort,
                                     AddressPortMapping mapping,
                                     boolean throwTimeoutExceptions,
                                     int responsePort )
        throws IOException, SocketTimeoutException
    {
        // Build a binding request message.
    	net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
        message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_REQUEST );
        message.generateTransactionID();

        // If a response port was specified, add the attribute.
        if( responsePort > 0 )
        {
            ResponsePort responsePortAttribute = new ResponsePort( responsePort );
            message.addAttribute( responsePortAttribute.getName(), responsePortAttribute );
        }

        // Send UDP Request
        DatagramSocket udpSocket = new DatagramSocket();
        byte[] udpData = message.getBytes();
        long startTime = sendUDP( udpData, serverAddress, serverPort, udpSocket );


        // Set the timeout.
        udpSocket.setSoTimeout( SERVER_TIMEOUT_MS );

        long endTime = 0;
        try
        {
            // Wait for Response.
            // UDP must be received all at once (as far as I can tell).
            byte[] data = new byte[ Message.STUN_MAX_IPV4_SIZE ];
            DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
            udpSocket.receive( receivedPacket );

            endTime = System.currentTimeMillis();

            InetSocketAddress originAddress = (InetSocketAddress)receivedPacket.getSocketAddress();
            logger.info( "UDP Received data from " + originAddress.getAddress().getHostAddress()
                + " : " + originAddress.getPort() );

            // Process the response message.
            Message receivedMessage = new Message( data );

            logger.debug( "Response:\n" + receivedMessage.toString() );

            if( mapping != null )
            {
                MappedAddress mappedAddress
                    = (MappedAddress)receivedMessage.getAttribute( "MappedAddress");
                byte[] externalAddress = ByteBuffer.allocate( 4 )
                                                   .putInt( mappedAddress.getAddress() ).array();
                String externalAddressText = String.format( "%d.%d.%d.%d",
                                                            externalAddress[0] & 0xff,
                                                            externalAddress[1] & 0xff,
                                                            externalAddress[2] & 0xff,
                                                            externalAddress[3] & 0xff );
                mapping.setExternalIPAddress( externalAddressText );
                mapping.setExternalPort( mappedAddress.getPort() );
                mapping.setInternalPort( mappedAddress.getPort() );
            }
        }
        catch( SocketTimeoutException e )
        {
            logger.error( "Timed out waiting for server response." );
            if( throwTimeoutExceptions )
            {
                throw e;
            }
            return 0;
        }
        finally
        {
            udpSocket.close();
        }

        return endTime - startTime;
    }

    public long doBindingRequestTCP( String serverAddress, int serverPort,
                                     AddressPortMapping mapping,
                                     boolean throwTimeoutExceptions,
                                     int responsePort )
        throws IOException, SocketTimeoutException
    {
        // Build a binding request message.
    	net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
        message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_REQUEST );
        message.generateTransactionID();

        // If a response port was specified, add the attribute.
        if( responsePort > 0 )
        {
            ResponsePort responsePortAttribute = new ResponsePort( responsePort );
            message.addAttribute( responsePortAttribute.getName(), responsePortAttribute );
        }

        // Sent the TCP Request.
        Socket tcpSocket = new Socket();
        byte[] tcpData = message.getBytes();
        long startTime = sendTCP( tcpData, serverAddress, serverPort, tcpSocket );

        // Set the timeout.
        tcpSocket.setSoTimeout( SERVER_TIMEOUT_MS );

        long endTime = 0;
        try
        {
            // Wait for the Response.
            byte[] data = new byte[ Message.STUN_MAX_IPV4_SIZE ];
            InputStream inStream = tcpSocket.getInputStream();
            int readSize = inStream.read( data  );

            endTime = System.currentTimeMillis();

            // For now only reads once.
            if( readSize > 0 )
            {
                logger.info("TCP Received data from "
                    + tcpSocket.getRemoteSocketAddress().toString() );
            }

            // Process the response message.
            Message receivedMessage = new Message( data );
            logger.debug( "Response:\n" + receivedMessage.toString() );

            if( mapping != null )
            {
                MappedAddress mappedAddress
                    = (MappedAddress)receivedMessage.getAttribute( "MappedAddress");
                byte[] externalAddress = ByteBuffer.allocate( 4 )
                                                   .putInt( mappedAddress.getAddress() ).array();
                String externalAddressText = String.format( "%d.%d.%d.%d",
                                                            externalAddress[0] & 0xff,
                                                            externalAddress[1] & 0xff,
                                                            externalAddress[2] & 0xff,
                                                            externalAddress[3] & 0xff );
                mapping.setExternalIPAddress( externalAddressText );
                mapping.setExternalPort( mappedAddress.getPort() );
            }
        }
        catch( SocketTimeoutException e )
        {
            logger.error( "Timed out waiting for server response." );
            if( throwTimeoutExceptions )
            {
                throw e;
            }
            return 0;
        }
        finally
        {
            tcpSocket.close();
        }

        return endTime - startTime;
    }

    // The socket must remain open during use.
    public void sendRandomNetworkTraffic( DatagramSocket socket, String addressText, int port,
                                          SynchronizedTime sharedTime, int multiplier )
        throws UnknownHostException, InterruptedException, IOException
    {
        String s = "some throwaway data";
        byte[] throwaway = s.getBytes();

        logger.info( "Starting random traffic sending with multiplier " + multiplier );

        while( true )
        {
            // Check if a STOP signal has been issued.
            if( sharedTime.shouldStopThread() )
            {
                logger.debug( "Received STOP signal. Exiting." );
                sharedTime.resetThreadStop();
                break;
            }

            // Generate an amount of time to wait.
            int waitTime = (int)(Math.random() * multiplier);
            logger.debug( "Waiting for " + waitTime + " milliseconds." );
            Thread.sleep( waitTime );

            // Send the message.
            // sendUDP( throwaway, addressText, port, socket );

            // Build a binding request message.
            net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
            message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_REQUEST );
            message.generateTransactionID();

            // Send UDP Request
            byte[] udpData = message.getBytes();
            long startTime = sendUDP( udpData, addressText, port, socket );

//            // Set the timeout.
//            socket.setSoTimeout( SERVER_TIMEOUT_MS );
//
//            try
//            {
//                // Wait for Response.
//                // UDP must be received all at once (as far as I can tell).
//                byte[] data = new byte[ Message.STUN_MAX_IPV4_SIZE ];
//                DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
//                socket.receive( receivedPacket );
//            }
//            catch( SocketTimeoutException e )
//            {
//                logger.debug( "Random traffic generator timed out on response." );
//            }

        }
    }

    // TODO: Maybe the random traffic generator should also just have the thread interrupted?
    public void sendPatternedNetworkTraffic( DatagramSocket socket, String addressText, int port,
                                             String patternFilePath, boolean repeatsForever )
        throws IOException, InterruptedException
    {
        List<Integer> delays = new ArrayList<Integer>();
        BufferedReader reader =  null;
        int sentCount = 0;
        try
        {
            // Load all of the values first for a more accurate recreation.
            reader = new BufferedReader( new FileReader( patternFilePath ) );
            String currentLine;
            logger.debug( "Loading patterned traffic data from \"" + patternFilePath );
            while( ( currentLine = reader.readLine() ) != null )
            {
                delays.add( Integer.parseInt( currentLine) );
            }
            logger.debug( "Traffic data size is " + delays.size() + " messages." );

            do
            {
                // Build a binding request message to use for the tests.
            	net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
                message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_REQUEST );
                message.generateTransactionID();
                byte[] udpData = message.getBytes();

                // Send the traffic at the appropriate intervals.
                for( Integer delay : delays )
                {
                    if( Thread.currentThread().isInterrupted() )
                    {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    if ( delay > 0 )
                    {
                        Thread.sleep( delay );
                    }

                    sendUDP( udpData, addressText, port, socket );
//                    logger.debug( "PATTERN - sending message after " + delay + " ms." );
                    sentCount++;
                }

                logger.debug( "End of traffic data.  Repeat: " + repeatsForever );
            } while( repeatsForever );
        }
        catch( IOException e )
        {
            logger.error( "Unexpected IO Exception: " + e );
            throw e;
        }
        finally
        {
            logger.debug( "TRAFFIC SENT COUNT: " + sentCount );
            if( reader != null )
            {
                reader.close();
            }
        }
    }

    // Generic Helper method to send a CALC_KA_REQUEST.
    public void sendCalcKARequestUDPGeneric( DatagramSocket socket, short messageType,
                                             InetAddress serverAddress, int serverPort, int id,
                                             int time, double deltaT, byte incrementType,
                                             byte usesStarterMessage, byte direction, byte repeats,
                                             int maxTime, byte actionAfterMaxTime, byte serverAdjustsZ,
                                             int z, int zMultiplier, double deltaTAfterMax, int k )
        throws IOException
    {
        // Set up the request message.
        Message request = new Message();
        request.setMessageType( messageType );
        request.generateTransactionID();

        KeepAliveTimer timer = new KeepAliveTimer();
        timer.setId( id );
        timer.setTime( time );
        timer.setDeltaT( deltaT );
        timer.setIncrementType( incrementType );
        timer.setUsesStarterMessage( usesStarterMessage );
        timer.setDirection( direction );
        timer.setRepeats( repeats );
        timer.setMaxTime( maxTime );
        timer.setActionAfterMax( actionAfterMaxTime );
        timer.setServerAdjustsZ( serverAdjustsZ );
        timer.setZ( z );
        timer.setzMultiplier( zMultiplier );
        timer.setDeltaTAfterMax( deltaTAfterMax );
        timer.setK( k );

        request.addAttribute( "KeepAliveTimer", timer );

        // Send the request message to the server.
        byte[] outData = request.getBytes();
        DatagramPacket outPacket = new DatagramPacket( outData, outData.length, serverAddress,
                                                       serverPort );
        socket.send( outPacket );
    }

    // Helper method to send a normal CALC_KA_REQUEST.
    public void sendCalcKARequestUDP( DatagramSocket socket, InetAddress serverAddress,
                                      int serverPort, int id, int time, double deltaT,
                                      byte incrementType, byte usesStarterMessage, byte direction,
                                      byte repeats, int maxTime, byte actionAfterMaxTime,
                                      byte serverAdjustsZ, int z, int zMultiplier,
                                      double deltaTAfterMax, int k )
        throws IOException
    {
        sendCalcKARequestUDPGeneric( socket, net.ddp2p.common.network.stun.stun.Message.STUN_CALC_KEEPALIVE_REQUEST,
                                     serverAddress, serverPort, id, time, deltaT, incrementType,
                                     usesStarterMessage, direction, repeats, maxTime, actionAfterMaxTime,
                                     serverAdjustsZ, z, zMultiplier, deltaTAfterMax, k );
    }

    // Helper method to send a CALC_KA_STOP_REQUEST.
    public void sendCalcKAStopRequestUDP( DatagramSocket socket, InetAddress serverAddress,
                                          int serverPort, int id )
        throws IOException
    {
        sendCalcKARequestUDPGeneric( socket, net.ddp2p.common.network.stun.stun.Message.STUN_CALC_KEEPALIVE_STOP_REQUEST,
               serverAddress, serverPort, id, 0, 0, KeepAliveTimer.PLACEHOLDER,
               KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER,
               KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER,
               KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER,
               KeepAliveTimer.PLACEHOLDER );
    }

    // Helper method to send a CALC_KA_RESET_REQUEST.
    public void sendCalcKAResetRequestUDP( DatagramSocket socket, InetAddress serverAddress,
                                          int serverPort, int id )
        throws IOException
    {
        sendCalcKARequestUDPGeneric( socket, net.ddp2p.common.network.stun.stun.Message.STUN_CALC_KEEPALIVE_RESET_REQUEST,
               serverAddress, serverPort, id, 0, 0, KeepAliveTimer.PLACEHOLDER,
               KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER,
               KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER,
               KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER, KeepAliveTimer.PLACEHOLDER,
               KeepAliveTimer.PLACEHOLDER);
    }

    public void sendGenTrafStartRequest( DatagramSocket socket, InetAddress serverAddress,
                                         int serverPort )
        throws IOException
    {
        Message request = new Message();
        request.setMessageType( Message.STUN_GENTRAF_START_REQUEST);
        request.generateTransactionID();

        // Send the request message to the server.
        byte[] outData = request.getBytes();
        DatagramPacket outPacket = new DatagramPacket( outData, outData.length, serverAddress,
                                                       serverPort );
        socket.send( outPacket );
    }

    public void sendGenTrafStopRequest( DatagramSocket socket, InetAddress serverAddress,
            int serverPort )
    throws IOException
    {
        Message request = new Message();
        request.setMessageType( Message.STUN_GENTRAF_STOP_REQUEST);
        request.generateTransactionID();

        // Send the request message to the server.
        byte[] outData = request.getBytes();
        DatagramPacket outPacket = new DatagramPacket( outData, outData.length, serverAddress,
                                  serverPort );
        socket.send( outPacket );
    }


    public long sendUDP( byte[] data, String addressText, int port, DatagramSocket socket )
        throws UnknownHostException, SocketException, IOException
    {
        InetAddress address = InetAddress.getByName( addressText );

        DatagramPacket packet = new DatagramPacket( data, data.length, address,
                                                    port );

        long sendTime = System.currentTimeMillis();
        socket.send( packet );

        logger.info( "UDP Sent " + data.length + " bytes of data to "
            + address.getHostAddress() + ":" + port );

        return sendTime;
    }

    public long sendTCP( byte[] data, String addressText, int port, Socket socket )
        throws UnknownHostException, SocketException, IOException
    {
        InetSocketAddress address = new InetSocketAddress( addressText,
                                                           port);

        socket.connect( address );
        OutputStream outStream = socket.getOutputStream();

        long sendTime = System.currentTimeMillis();
        outStream.write( data );

        logger.info( "TCP Sent " + data.length + " bytes of data to "
            + address.getHostName() + ":" + port );

        return sendTime;
    }

    // Builds the test params list. Al in one place for convenience.
    // Each entry contains the parameters for all of the states, for one test.
    public List<Map<State, TestParams>> getAllTestParams( int deltaT, SearchType type )
    {
        List<Map<State, TestParams>> allTestsParameters = new ArrayList<Map<State, TestParams>>();
        Map<State, TestParams> currentTest = null;

        StateSearchParams paramsSearch = null;

        //The RUN and REFINE parameters won't change for now.
        StateRunParams paramsRun = new StateRunParams();
        paramsRun.setFailureRate( 0.10 );
        paramsRun.setMaxRunCount( 3 );

        StateRefineParams paramsRefine = new StateRefineParams();
        paramsRefine.setDeltaT(           100 );
        paramsRefine.setzMultiplier(      2 );
        paramsRefine.setZ(                1 );
        paramsRefine.setPingsPerAttempt(  3 );

        // Set up the various tests
        //-------------------------

//        currentTest = new HashMap<State, TestParams>();
//
//        currentTest.put( State.RUN, paramsRun );
//        currentTest.put( State.REFINE, paramsRefine );
//
//        paramsSearch = new StateSearchParams();
//        paramsSearch.setDeltaT(      deltaT );
//        paramsSearch.setMinDeltaT(   100 );
//        paramsSearch.setZMax(        0 );
//        paramsSearch.setZMultiplier( 2 );
//        paramsSearch.setZ(           1 );
//        paramsSearch.setType(        type);
//
//        currentTest.put( State.SEARCH, paramsSearch );
//
//        allTestsParameters.add( currentTest );

        // ---------------------------------------------------------------------

        currentTest = new HashMap<State, TestParams>();

        currentTest.put( State.RUN, paramsRun );
        currentTest.put( State.REFINE, paramsRefine );

        paramsSearch = new StateSearchParams();
        paramsSearch.setDeltaT(      deltaT );
        paramsSearch.setMinDeltaT(   1 );
        paramsSearch.setZMax(        0 );
        paramsSearch.setZMultiplier( 2 );
        paramsSearch.setZ(           1 );
        paramsSearch.setType(        type);

        currentTest.put( State.SEARCH, paramsSearch );

        allTestsParameters.add( currentTest );

        // ---------------------------------------------------------------------

        // Return the result.
        // ------------------

        // z 4
//      zMultiplierValues.add( 2 );
//      zMaxValues.add( 16 );
//      zMultiplierValues.add( 4 );
//      zMaxValues.add( 256 );
//      zMultiplierValues.add( 6);
//      zMaxValues.add( 1296 );
//      zMultiplierValues.add( 8);
//      zMaxValues.add( 4096 );
//      zMultiplierValues.add( 10);
//      zMaxValues.add( 10000 );

//      //z 3
//      zMultiplierValues.add( 2 );
//      zMaxValues.add( 8 );
//      zMultiplierValues.add( 4 );
//      zMaxValues.add( 64 );
//      zMultiplierValues.add( 6);
//      zMaxValues.add( 216 );
//      zMultiplierValues.add( 8);
//      zMaxValues.add( 512 );
//      zMultiplierValues.add( 10);
//      zMaxValues.add( 1000 );

//      //z 2
//      zMultiplierValues.add( 2 );
//      zMaxValues.add( 8 );
//      zMultiplierValues.add( 4 );
//      zMaxValues.add( 64 );
//      zMultiplierValues.add( 6);
//      zMaxValues.add( 216 );
//      zMultiplierValues.add( 8);
//      zMaxValues.add( 512 );
//      zMultiplierValues.add( 10);
//      zMaxValues.add( 1000 );

        return allTestsParameters;
    }


}

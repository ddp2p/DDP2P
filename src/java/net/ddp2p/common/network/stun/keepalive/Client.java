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
    private class PeerCommunicationException extends Exception
    {
        public PeerCommunicationException( String message )
        {
            super( message );
        }
    }
    public enum State
    {
        SEARCH
       ,RUN
       ,REFINE
       ,EXIT
       ,NONE;
    }
    public enum SearchType
    {
        LINEAR
       ,GEOMETRIC
       ,SWITCH
    }
    public class RunData
    {
        private int newTime; 
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
    public class StateSearchParams implements TestParams
    {
        private int deltaT;
        private int zMax;
        private int zMultiplier;
        private int minDeltaT;
        private SearchType type;
        private int z;
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
        @Override
        public String getName() { return "StateSearchParams"; }
    }
    public class StateRunParams implements TestParams
    {
        private double failureRate;
        private int maxRunCount; 
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
            this.sharedTime = new SynchronizedTime(); 
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
                net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
                message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_REQUEST );
                message.generateTransactionID();
                this.sharedBRData.setStunTransactionID( message.getTransactionID() );
                if( this.otherPort > 0 )
                {
                    ResponsePort responsePortAttribute = new ResponsePort( this.otherPort );
                    message.addAttribute( responsePortAttribute.getName(), responsePortAttribute );
                }
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
                byte[] udpData = message.getBytes();
                client.sendUDP( udpData, serverAddressText, serverPort, socket );
                socket.setSoTimeout( timeout );
                byte[] data = new byte[ Message.STUN_MAX_IPV4_SIZE ];
                DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
                socket.receive( receivedPacket );
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
            Client client = new Client( "calck", sharedTime, true );
            client.doCalcKeepAliveUDP( args, true, false, false );
        }
        else if( action.equals("rka") )
        {
            Client client = new Client( "calck", sharedTime, true );
            client.doCalcKeepAliveUDP( args, false, true, false );
        }
        else if( action.equals("tka") )
        {
            Client client = new Client( "calck", sharedTime, true );
            client.doCalcKeepAliveUDP( args, false, false, true );
        }
        else if( action.equals("bs") )
        {
            Client client = new Client( "bnsrc", sharedTime, false );
            client.doKeepAliveBinarySearch( args, 1, false );
        }
        else if( action.equals( "dbs") )
        {
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
        socket.send( packet ); 
        logger.info( "UDP Sent " + data.length + " bytes of data to "
            + address.getHostAddress() + ":" + peerPort );
        socket.setSoTimeout( SERVER_TIMEOUT_MS );
        long endTime = 0;
        try
        {
            data = new byte[ MAX_MESSAGE_SIZE ];
            DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
            socket.receive( receivedPacket );
            endTime = System.currentTimeMillis();
            String sourceAddress = receivedPacket.getSocketAddress().toString();
            int colonIndex = sourceAddress.indexOf( ':' ); 
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
        String stunDeviceAddress= "192.168.1.1";
        int stunDevicePort = 3478;;
        int numRuns = 1;
        String outputFilePath = "output.txt";
        String protocol = "UDP";
        String peerIP = "0";
        int peerPort = 1230;
        Client client = new Client();
        AddressPortMapping mapping = new AddressPortMapping();
        if( args.length == 0 )
        {
        }
        else if( args.length < 6 )
        {
            System.out.println( "usage: Client timingtest <Server IP> <Server Port> <Peer IP> "
                    + "<Port to Map> <Protocol> <Output File> <Number of Runs>" );
        }
        else
        {
            stunDeviceAddress = args[1];
            stunDevicePort = Integer.parseInt( args[2] );
            peerIP = args[3];
            protocol = args[4];
            outputFilePath = args[5];
            numRuns = Integer.parseInt( args[6] );
        }
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
                }
            }
            catch( SocketTimeoutException e )
            {
                Thread.sleep( 32000 );
                continue;
            }
            long peerPingTime = 0;
            if( ! peerIP.equals( "0" ) )
            {
                try
                {
                    peerPingTime = client.pingPeer( peerIP, peerPort, mapping );
                    FileWriter writer = new FileWriter( outputFilePath, true );
                    writer.write( i + " " + stunTime + " " + peerPingTime + " "
                            +  (stunTime + peerPingTime) + "\n" );
                    writer.close();
                }
                catch( PeerCommunicationException e )
                {
                }
            }
            if( i < numRuns - 1 )
            {
                Thread.sleep( 32000 ); 
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
        logger.info( "Starting KeepAlive Process with " + serverAddressText );
        StateRunParams paramsRun = new StateRunParams();
        paramsRun.setFailureRate( 0.99 ); 
        paramsRun.setMaxRunCount( 0 ); 
        int lastGoodTime = 10000;
        int lastTimeoutTime = 0;
        boolean lastTimeoutSet = false;
        RunData runData = new RunData( lastGoodTime, State.RUN, State.NONE );
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
        Map<State, RunData> previousStateData = new HashMap<State, RunData>();
        previousStateData.put( State.RUN, new RunData( 0, State.NONE, State.NONE ) );
        while( true )
        {
            if( runData.getNewState() == State.EXIT )
            {
                break;
            }
            if( this.sharedTime.shouldStopThread() )
            {
                logger.info( "STOP flag set by controller thread. Exiting." );
                this.sharedTime.resetThreadStop(); 
                break;
            }
            if( this.sharedTime.isTimedOut() )
            {
                logger.debug( "TIMED OUT: Updating from last good shared time: " + lastGoodTime );
                lastTimeoutTime = runData.getNewTime();
                lastTimeoutSet = true;
                runData.updateNewTime( lastGoodTime );
                this.sharedTime.resetTimedOut();
            }
            else if( this.sharedTime.didTimeChange() )
            {
                    logger.debug( "Updating from shared time: " + this.sharedTime.getTime());
                    lastGoodTime = runData.getNewTime();
                    runData.updateNewTime( this.sharedTime.getTime() );
                this.sharedTime.resetTimeChanged();
            }
            runData = runKeepAliveLoopUDP( serverAddress, serverPort, socket, usesStarterMessage,
                    paramsSearch, paramsRun, paramsRefine, runData, true, false, false );
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
        boolean reusesSocket = (generatesRandomTraffic || generatesPatternedTraffic);
        logger.info( "Starting KeepAlive Process with " + serverAddressText );
        List<Map<State, TestParams>> testParams = getAllTestParams( deltaT, SearchType.GEOMETRIC);
        for( int j = 0; j < testParams.size(); j++ )
        {
            StateSearchParams paramsSearch = (StateSearchParams)testParams.get(j).get( State.SEARCH );
            StateRunParams paramsRun = (StateRunParams)testParams.get(j).get( State.RUN );
            StateRefineParams paramsRefine = (StateRefineParams)testParams.get(j).get( State.REFINE );
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
                Map<State, Integer> stateCounts = new HashMap<State, Integer>();
                stateCounts.put( State.SEARCH, 0 );
                stateCounts.put( State.RUN, 0 );
                stateCounts.put( State.REFINE, 0 );
                Map<State, RunData> previousStateData = new HashMap<State, RunData>();
                previousStateData.put( State.SEARCH, new RunData( 0, State.NONE, State.NONE) );
                previousStateData.put( State.RUN, new RunData( 0, State.NONE, State.NONE ) );
                previousStateData.put( State.REFINE, new RunData( 0, State.NONE, State.NONE ) );
                if( usesTwoChannel )
                {
                    if( constantRunner != null && constantRunner.isAlive() )
                    {
                        logger.debug( "constant runner still running. stopping." );
                        sharedTime.setThreadStop();
                        constantRunner.join();
                        constantRunner = null;
                    }
                    constantRunner =
                            new Thread (new Client.ConstantClientRunner( args, sharedTime ) );
                    constantRunner.start();
                }
                if( generatesRandomTraffic )
                {
                    if( randomRunner != null && randomRunner.isAlive() )
                    {
                        logger.debug( "random runner still running. stopping." );
                        sharedTime.setThreadStop();
                        randomRunner.join();
                        randomRunner = null;
                    }
                    randomRunner =
                            new Thread( new Client.RandomClientRunner(socket, serverAddressText,
                                                                      serverPort,
                                                                      RANDOM_MS_MULTIPLIER,
                                                                      sharedTime ) );
                    randomRunner.start();
                }
                if( generatesPatternedTraffic )
                {
                    sendGenTrafStartRequest( socket, serverAddress, serverPort );
                }
                while( true )
                {
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
                        stateCounts.put( previousState, stateCounts.get( previousState ) + 1 );
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
                    if( runData.getNewState() == State.RUN )
                    {
                        break;
                    }
                    runData = runKeepAliveLoopUDP( serverAddress, serverPort, socket,
                                                   usesStarterMessage, paramsSearch, paramsRun,
                                                   paramsRefine, runData, reusesSocket, true, true );
                } 
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
                if( this.sharedTime != null & isSharedTimeController() )
                {
                    logger.info( "Setting STOP flag for shared time." );
                    this.sharedTime.setThreadStop();
                }
                Thread.sleep( runData.getNewTime() + TIMEOUT_BUFFER_MS );
            }
            if( constantRunner != null && constantRunner.isAlive() )
            {
                logger.debug( "constant runner still running after all tests. stopping." );
                sharedTime.setThreadStop();
                constantRunner.join();
                constantRunner = null;
            }
            if( randomRunner != null && randomRunner.isAlive() )
            {
                logger.debug( "random runner still running after all tests. stopping." );
                sharedTime.setThreadStop();
                randomRunner.join();
                randomRunner = null;
            }
            if( patternedRunner != null && patternedRunner.isAlive() )
            {
                logger.debug( "patterned traffic runner still running after all tests. stopping." );
                patternedRunner.interrupt();
                patternedRunner.join();
                logger.debug( "patterned traffic runner stopping successfully." );
                patternedRunner = null;
            }
        }
    }
    public RunData runKeepAliveLoopUDP( InetAddress serverAddress,
                                        int serverPort,
                                        DatagramSocket socket,
                                        boolean usesStarterMessage,
                                        StateSearchParams paramsSearch,
                                        StateRunParams paramsRun,
                                        StateRefineParams paramsRefine,
                                        RunData runData,
                                        boolean reuseSocket, 
                                        boolean usesRampUp,
                                        boolean compensatesForOtherTraffic )
        throws IOException
    {
        long sessionDuration = 0;
        int numStartMessages = 0;      
        int numTotalInMessages = 0;    
        int numSessionInMessages = 0;  
        int numTotalStopMessages = 0;  
        int numTimeouts = 0;           
        int refineDelta = 0;           
        int numTotalResetTries = 0;    
        int sumTimedOutValues = 0;     
        int time = runData.getNewTime();
        State state = runData.getNewState();
        double baseDeltaT = paramsSearch.getDeltaT();
        int z = paramsSearch.getZ();
        int zMultiplier = paramsSearch.getZMultiplier();
        int k = 0;
        int lastGoodTime = 0;
        int maxGoodTime = 0;
        State newState = state;
        byte serverAdjustsZ = KeepAliveTimer.FALSE;
        long lastResetAttempt = 0;
        SynchronizedInteger numResetTries = new SynchronizedInteger();
        int numTotalIterations = 0;
        double intervalFromServer = 0; 
        int runRunIterations = 0;
        int numRefineIterations = 0;
        int numRefineTimeouts = 0;
        boolean refineStateMinFound = false;
        boolean isInitialRefineStateValue = true;
        int zForServer = 1; 
        byte actionAfterMax = KeepAliveTimer.STAY_CONSTANT;
        boolean currentlyInRampUp = true;
        double deltaTAfterMax = 0;
        byte direction = KeepAliveTimer.DIRECTION_FORWARD;
        byte numRepeats = 0;
        long sessionStartTimestamp = System.currentTimeMillis();
        int id = generateSessionId();
        boolean isFirstMessage = true;
        boolean shouldExitNext = false;
        boolean lastAttemptStarterSkipped = false;
        logger.info("Starting KeepAlive loop with socket reuse = \"" + reuseSocket + "\" in the "
        + state.name() + " state with time " + time );
        logger.info( "Local Port: " + socket.getLocalPort() );
        if( state == State.RUN || state == State.REFINE )
        {
            maxGoodTime = time;               
            time = paramsSearch.getDeltaT();  
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
        while( true )
        {
            if( shouldUpdateFromSharedTime( state ) )
            {
                logger.info( "Received shared time notification...stopping session." );
                sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, id );
                numTotalStopMessages++;
                time = lastGoodTime; 
                newState = state;    
                break;
            }
            if( !verifyTimeIsValid( time ) )
            {
                newState = State.EXIT;
                break;
            }
            byte incrementType = KeepAliveTimer.LINEAR_INCREMENT;
            int timeout = time + TIMEOUT_BUFFER_MS;
            if( state == State.SEARCH )
            {
                if( shouldExitNext )
                {
                    int baseNumMessages = usesStarterMessage ? 1 : 0;
                    if( numSessionInMessages > baseNumMessages || lastAttemptStarterSkipped )
                    {
                        sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, id );
                        numTotalStopMessages++;
                        sendGenTrafStopRequest( socket, serverAddress, serverPort ); 
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
                    shouldExitNext = true;
                }
                incrementType = determineIncrementType( paramsSearch.getType(), numTimeouts );
                timeout = determineTimeout( currentlyInRampUp, time, k, baseDeltaT, baseDeltaT,
                                            z, paramsSearch.getZMultiplier(), serverAdjustsZ,
                                            incrementType, usesStarterMessage,
                                            numSessionInMessages, state );
                zForServer = z;
            }
            else if (     (state == State.RUN || state == State.REFINE)
                      && ((!isFirstMessage && intervalFromServer > 0) 
                      || isFirstMessage ) )
            {
                timeout = determineTimeout( currentlyInRampUp, time, k, paramsSearch.getDeltaT(),
                                            deltaTAfterMax, z, paramsSearch.getZMultiplier(),
                                            serverAdjustsZ, incrementType, usesStarterMessage,
                                            numSessionInMessages, state );
                if( state == State.REFINE )
                {
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
            byte usesStarterMessageByte = usesStarterMessage ? KeepAliveTimer.USES_STARTER_MESSAGE
                    : KeepAliveTimer.NO_STARTER_MESSAGE;
            if( isFirstMessage )
            {
                id = generateSessionId();
                logger.info( "NEW SESSION ID (new session start): " + id );
                currentlyInRampUp = true; 
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
            try
            {
                boolean stateChange = false;
                DelayedResetSender resetSender = new DelayedResetSender( socket, serverAddress,
                                                                         serverPort, id,
                                                                         numResetTries, this );
                while( true )
                {
                    KeepAliveTimer timer = listenValidateAndParseTimer( socket, id,
                    		                                            compensatesForOtherTraffic,
                    		                                            serverAddress,
                    		                                            serverPort);
                    if( timer == null )
                    {
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
                                k++;
                                logger.debug( "After MAX resets, Updating k to " + k );
                                z *= zMultiplier;
                                logger.debug( "RESET z is now "  + z );
                                if( serverAdjustsZ == KeepAliveTimer.TRUE )
                                {
                                    if( numSessionInMessages > 0 )
                                    {
                                        z *= zMultiplier;
                                        logger.debug( "RESET To sync with server, z is now "  + z );
                                    }
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
                                logger.debug( "RESET new time after z adjust: " + time );
                                logger.data( "z", z, "k", k, "time", time );
                                if( paramsSearch.getZMax() > 0 && z > paramsSearch.getZMax() )
                                {
                                    logger.info( "z greater than " + paramsSearch.getZMax()
                                            +" - breaking." );
                                    time = lastGoodTime; 
                                    stateChange = true;
                                    newState = State.RUN;
                                }
                                numTotalResetTries += numResetTries.getValue();
                                logger.debug( "RESET - Updating total reset tries: " + numTotalResetTries
                                        + " with " + numResetTries.getValue() );
                                isFirstMessage = true;
                                numResetTries.setValue( 0 );
                                numSessionInMessages = 0;
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
                    if( usesStarterMessage && numSessionInMessages == 1 && time != 0 )
                    {
                        logger.data( "Received Expected ID", id, "k", k );
                        logger.debug( "--> FIRST SESSION MESSAGE <--" );
                    }
                    else
                    {
                        k = timer.getK();
                        intervalFromServer = timer.getDeltaT(); 
                        switch( state )
                        {
                            case SEARCH:
                            case RUN:
                                z = timer.getZ();
                                lastGoodTime = timer.getTime();
                                time = lastGoodTime;
                                logger.data( "Received Expected ID", id, "k", k, "time", time,
                                             "z", z, "deltaT (local)", paramsSearch.getDeltaT()/z );
                                if( paramsSearch.getZMax() > 0 && z > paramsSearch.getZMax() )
                                {
                                    logger.info( "z greater than " + paramsSearch.getZMax()
                                            +" - breaking." );
                                    time = lastGoodTime;
                                    stateChange = true;
                                    newState = State.RUN;
                                }
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
                                    if(    paramsRun.getMaxRunCount() > 0
                                        && runRunIterations >= paramsRun.getMaxRunCount() )
                                    {
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
                                if( currentlyInRampUp )
                                {
                                    time = timer.getTime();
                                    logger.debug( "Updating time to " + time );
                                    if( time >= maxGoodTime ) 
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
                                            z *= paramsRefine.getzMultiplier();
                                            logger.debug( "Changing deltaT -> z: " + z +
                                                    " deltaTAfterMax: " + deltaTAfterMax );
                                            time = (int)(time + 1 * (deltaTAfterMax / z));
                                            numSessionInMessages = 0;
                                            isFirstMessage = true;
                                            direction = KeepAliveTimer.DIRECTION_FORWARD;
                                            actionAfterMax = KeepAliveTimer.CHANGE_FORWARD;
                                            maxGoodTime = time;
                                            if( usesRampUp ) { time = paramsSearch.getDeltaT(); }
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
            catch( SocketTimeoutException e )
            {
                logger.debug( "Updating sum of timeout values " + sumTimedOutValues + " with " +
                    (timeout - TIMEOUT_BUFFER_MS) );
                sumTimedOutValues += timeout - TIMEOUT_BUFFER_MS;
                isFirstMessage = true;
                logger.info( "TIMEOUT waiting for ID: " + id );
                numTimeouts++;
                numRefineTimeouts++;
                sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, id );
                numTotalStopMessages++;
                int baseMessageCount = usesStarterMessage ? 1 : 0;
                if( numSessionInMessages <= baseMessageCount )
                {
                    logger.debug( "TIMEOUT on first attempt of session. Keeping k at " + k );
                }
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
                            time = lastGoodTime; 
                            stateChange = true;
                            newState = State.RUN;
                        }
                        break;
                    case RUN:
                        logger.debug( "RUN TIMEOUT: "
                                + ( ( (double)numTimeouts) / (numTotalIterations + 1) ) );
                        if( ((double)numTimeouts) / (numTotalIterations + 1)
                                > paramsRun.getFailureRate() )
                        {
                                stateChange = true;
                                if( this.sharedTime != null && isSharedTimeObserver() )
                                {
                                    this.sharedTime.setTimedOut();
                                    newState = State.RUN;
                                }
                                else
                                {
                                    if( currentlyInRampUp && time < maxGoodTime )
                                    {
                                        time = maxGoodTime;
                                    }
                                    newState = State.REFINE;
                                }
                        }
                        break;
                    case REFINE:
                        numRefineIterations = 0;
                        if( direction == KeepAliveTimer.DIRECTION_BACKWARD )
                        {
                            direction = KeepAliveTimer.DIRECTION_FORWARD;
                            actionAfterMax = KeepAliveTimer.CHANGE_BACKWARD;
                            time = (int)(time - numRefineTimeouts * (deltaTAfterMax / z));
                            lastGoodTime = time;
                            maxGoodTime = time;
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
                            if( currentlyInRampUp && !refineStateMinFound )
                            {
                                direction = KeepAliveTimer.DIRECTION_FORWARD;
                                actionAfterMax = KeepAliveTimer.CHANGE_BACKWARD;
                                time = maxGoodTime;
                                time = (int)(time - numRefineTimeouts * (deltaTAfterMax / z));
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
        int timeUnreachable = sumTimedOutValues - ( time * numTimeouts );
        logger.data( "numTimeouts", numTimeouts, "reachableTime for instances", ( time * numTimeouts ),
                     "unreachableTime", timeUnreachable );
        long sessionStopTimestamp = System.currentTimeMillis();
        sessionDuration = sessionStopTimestamp - sessionStartTimestamp;
        return new RunData( time, newState, state, numTotalInMessages, numStartMessages,
                            numTotalStopMessages, numTotalResetTries, sessionDuration, numTimeouts,
                            refineDelta, timeUnreachable );
    }
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
                    int tempK = numSessionMessages - 1;
                    logger.debug("TEMP K : " + tempK );
                    double tempDeltaT = searchDeltaT; 
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
        if( id != timer.getId() )
        {
            logger.debug( "System Time: " + System.currentTimeMillis()
            + " - Expecting a different KeepAliveTimer session ID."
            + " (Expecting: " + id + " Received: " + timer.getId() + ")" );
            if( !compensatesForOtherTraffic )
            {
                sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, timer.getId() );
            }
            return null;
        }
        return timer;
    }
    public long doKeepAliveBinarySearch( String[] args, int minTimeChange, boolean usesTwoChannel )
        throws IOException, InterruptedException
    {
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
            int numTimeouts = 0;
            int numReceivedMessages = 0;
            int numReceivedNFMessages = 0;
            int numSentMessages = 0;
            boolean shouldExitMainLoop = false;
            InetAddress serverAddress = InetAddress.getByName( serverAddressText );
            DatagramSocket socket = null;
            logger.info( "Starting KeepAlive Process with " + serverAddressText );
            int receivedMessageCount = 0; 
            if( usesTwoChannel )
            {
                if( constantRunner != null && constantRunner.isAlive() )
                {
                    logger.debug( "constant runner still running. stopping." );
                    sharedTime.setThreadStop();
                    constantRunner.join();
                    constantRunner = null;
                }
                constantRunner =
                        new Thread (new Client.ConstantClientRunner( args, sharedTime ) );
                constantRunner.start();
            }
            long startTimestamp = System.currentTimeMillis();
            while( true )
            {
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
                int timeout =  time + TIMEOUT_BUFFER_MS;
                socket.setSoTimeout( timeout );
                logger.data( "NEW REQUEST -> timeout",  timeout, "time", time,
                             "min", min, "max", max, "id", id );
                try
                {
                    while( true )
                    {
                        byte[] inData = new byte[ Message.STUN_MAX_IPV4_SIZE ];
                        DatagramPacket inPacket = new DatagramPacket( inData, inData.length );
                        socket.receive( inPacket );
                        Message receivedMessage = new Message( inData );
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
                        if( id != timer.getId() )
                        {
                            logger.debug( "System Time: " + System.currentTimeMillis()
                                + " - Expecting a different KeepAliveTimer session ID."
                                + " (Expecting: " + id + " Received: " + timer.getId() + ")" );
                            continue;
                        }
                        logger.data( "Received Expected ID", id );
                        receivedMessageCount++;
                        numReceivedMessages++; 
                        if(    usesStarterMessage == KeepAliveTimer.USES_STARTER_MESSAGE
                            && receivedMessageCount == 1 )
                        {
                            logger.debug( "Received first message for ID: " + id );
                            continue;
                        }
                        receivedMessageCount = 0;
                        numReceivedNFMessages++; 
                        sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, id );
                        min = timer.getTime();
                        logger.debug( "Setting new min time: " + min );
                        if( this.sharedTime != null && isSharedTimeController() )
                        {
                            this.sharedTime.setTime( min );
                            this.sharedTime.setTimeChanged();
                        }
                        if( max == 0 )
                        {
                            time = time * multiplier;
                            logger.debug( "No max yet, multiply time: " + time );
                        }
                        else
                        {
                            lastTime = time;
                            time = ( min + max ) / 2;
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
                    numTimeouts++; 
                    sendCalcKAStopRequestUDP( socket, serverAddress, serverPort, id );
                    max = time;
                    logger.debug( "Setting new max time: " + max );
                    lastTime = time;
                    time = ( min + max ) / 2;
                    logger.debug( "Performing binary search: (" + min + ", " + max + ")" + " time: "
                            + time + " timeChange: " + (lastTime - time) );
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
            Thread.sleep(  min + TIMEOUT_BUFFER_MS );
        }
        if( constantRunner != null && constantRunner.isAlive() )
        {
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
        String serverAddressText = args[1];
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
            int numTimeouts = 0;
            int numImmediateReceivedMessagesA = 0;
            int numReceivedMessagesA = 0;
            int numSentMessages = 0;
            boolean shouldExitMainLoop = false;
            if( usesTwoChannel )
            {
                if( constantRunner != null && constantRunner.isAlive() )
                {
                    logger.debug( "constant runner still running. stopping." );
                    sharedTime.setThreadStop();
                    constantRunner.join();
                    constantRunner = null;
                }
                constantRunner =
                        new Thread (new Client.ConstantClientRunner( args, sharedTime ) );
                constantRunner.start();
            }
            while( true )
            {
                if( socketA != null ) { socketA.close(); }
                socketA = new DatagramSocket();
                try
                {
                    while( true )
                    {
                    	net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
                        message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_REQUEST );
                        message.generateTransactionID();
                        byte[] udpData = message.getBytes();
                        sendUDP( udpData, serverAddressText, serverPort, socketA );
                        numSentMessages++;
                        socketA.setSoTimeout( SERVER_TIMEOUT_MS );
                        byte[] data = new byte[ Message.STUN_MAX_IPV4_SIZE ];
                        DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
                        socketA.receive( receivedPacket );
                        InetSocketAddress originAddress = (InetSocketAddress)receivedPacket
                                                          .getSocketAddress();
                        logger.info( "UDP Received data from "
                                + originAddress.getAddress().getHostAddress() + " : "
                                + originAddress.getPort() );
                        Message receivedMessage = new Message( data );
                        if( ! Arrays.equals( receivedMessage.getTransactionID(),
                                             message.getTransactionID() ) )
                        {
                            logger.info( "(initial) Received unexpected STUN ID: "
                                    + message.getTransactionID() );
                            continue; 
                        }
                        logger.info( "(intitial) Received expected STUN ID: "
                                + message.getTransactionID() );
                        numImmediateReceivedMessagesA++;
                        break;
                    }
                }
                catch( SocketTimeoutException e )
                {
                    logger.error( "Unable to establish initial binding on Socket A: " + e );
                    continue;
                }
                SynchronizedBindingRequestData sharedBRData = new SynchronizedBindingRequestData();
                sharedBRData.setTime( time );
                long nextMessageTime = System.currentTimeMillis() + time;
                BindingRequestHelper brh = new BindingRequestHelper( this, sharedBRData,
                                                                     nextMessageTime,
                                                                     serverAddressText, serverPort,
                                                                     socketA.getLocalPort() );
                new Thread( brh ).start();
                numSentMessages++; 
                try
                {
                    while( true )
                    {
                        socketA.setSoTimeout( time  + TIMEOUT_BUFFER_MS );
                        logger.info( "Listening on Socket A" );
                        byte[] data = new byte[ Message.STUN_MAX_IPV4_SIZE ];
                        DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
                        socketA.receive( receivedPacket );
                        byte[] stunTransactionId = sharedBRData.getStunTransactionID();
                        logger.info( "Received Response on Socket A." );
                        Message receivedMessage = new Message( data );
                        if( ! Arrays.equals( receivedMessage.getTransactionID(),
                                             stunTransactionId ) )
                        {
                            logger.info( "(second) Received unexpected STUN ID: "
                                    + stunTransactionId );
                            continue; 
                        }
                        logger.info( "(second) Received expected STUN ID: " + stunTransactionId );
                        numReceivedMessagesA++;
                        if( min == time )
                        {
                            logger.debug( "No change from last min time. exiting loop." );
                            shouldExitMainLoop = true;
                            break;
                        }
                        min = time;
                        logger.debug( "Setting new min time: " + min );
                        if( this.sharedTime != null && isSharedTimeController() )
                        {
                            logger.debug( "Updating the shared time to " + time );
                            this.sharedTime.setTime( time );
                            this.sharedTime.setTimeChanged();
                        }
                        if( max == 0 )
                        {
                            time = time * multiplier;
                            logger.debug( "No max yet, multiply time: " + time );
                        }
                        else
                        {
                            lastTime = time;
                            time = ( min + max ) / 2;
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
                    numTimeouts++; 
                    max = time;
                    logger.debug( "Setting new max time: " + max );
                    lastTime = time;
                    time = ( min + max ) / 2;
                    logger.debug( "Performing binary search: (" + min + ", " + max + ")" + " time: "
                            + time + " timeChange: " + (lastTime - time) );
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
            FileWriter rawDataWriter = new FileWriter( rawDataFileName, true );
            rawDataWriter.write( "client_only:" + initialTime + "\n" );
            rawDataWriter.write( numReceivedMessagesA + "\n" );
            rawDataWriter.write( numImmediateReceivedMessagesA + "\n" );
            rawDataWriter.write( numSentMessages + "\n" );
            rawDataWriter.write( numTimeouts + "\n" );
            rawDataWriter.write( duration + "\n" );
            rawDataWriter.write( min  + "\n" );
            rawDataWriter.close();
            if( this.sharedTime != null && isSharedTimeController() )
            {
                logger.debug( "Setting STOP flag for constant thread." );
                this.sharedTime.setThreadStop();
            }
            Thread.sleep(  min + TIMEOUT_BUFFER_MS );
        }
        if( constantRunner != null && constantRunner.isAlive() )
        {
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
    	net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
        message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_REQUEST );
        message.generateTransactionID();
        if( responsePort > 0 )
        {
            ResponsePort responsePortAttribute = new ResponsePort( responsePort );
            message.addAttribute( responsePortAttribute.getName(), responsePortAttribute );
        }
        DatagramSocket udpSocket = new DatagramSocket();
        byte[] udpData = message.getBytes();
        long startTime = sendUDP( udpData, serverAddress, serverPort, udpSocket );
        udpSocket.setSoTimeout( SERVER_TIMEOUT_MS );
        long endTime = 0;
        try
        {
            byte[] data = new byte[ Message.STUN_MAX_IPV4_SIZE ];
            DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
            udpSocket.receive( receivedPacket );
            endTime = System.currentTimeMillis();
            InetSocketAddress originAddress = (InetSocketAddress)receivedPacket.getSocketAddress();
            logger.info( "UDP Received data from " + originAddress.getAddress().getHostAddress()
                + " : " + originAddress.getPort() );
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
    	net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
        message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_REQUEST );
        message.generateTransactionID();
        if( responsePort > 0 )
        {
            ResponsePort responsePortAttribute = new ResponsePort( responsePort );
            message.addAttribute( responsePortAttribute.getName(), responsePortAttribute );
        }
        Socket tcpSocket = new Socket();
        byte[] tcpData = message.getBytes();
        long startTime = sendTCP( tcpData, serverAddress, serverPort, tcpSocket );
        tcpSocket.setSoTimeout( SERVER_TIMEOUT_MS );
        long endTime = 0;
        try
        {
            byte[] data = new byte[ Message.STUN_MAX_IPV4_SIZE ];
            InputStream inStream = tcpSocket.getInputStream();
            int readSize = inStream.read( data  );
            endTime = System.currentTimeMillis();
            if( readSize > 0 )
            {
                logger.info("TCP Received data from "
                    + tcpSocket.getRemoteSocketAddress().toString() );
            }
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
    public void sendRandomNetworkTraffic( DatagramSocket socket, String addressText, int port,
                                          SynchronizedTime sharedTime, int multiplier )
        throws UnknownHostException, InterruptedException, IOException
    {
        String s = "some throwaway data";
        byte[] throwaway = s.getBytes();
        logger.info( "Starting random traffic sending with multiplier " + multiplier );
        while( true )
        {
            if( sharedTime.shouldStopThread() )
            {
                logger.debug( "Received STOP signal. Exiting." );
                sharedTime.resetThreadStop();
                break;
            }
            int waitTime = (int)(Math.random() * multiplier);
            logger.debug( "Waiting for " + waitTime + " milliseconds." );
            Thread.sleep( waitTime );
            net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
            message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_REQUEST );
            message.generateTransactionID();
            byte[] udpData = message.getBytes();
            long startTime = sendUDP( udpData, addressText, port, socket );
        }
    }
    public void sendPatternedNetworkTraffic( DatagramSocket socket, String addressText, int port,
                                             String patternFilePath, boolean repeatsForever )
        throws IOException, InterruptedException
    {
        List<Integer> delays = new ArrayList<Integer>();
        BufferedReader reader =  null;
        int sentCount = 0;
        try
        {
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
            	net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
                message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_REQUEST );
                message.generateTransactionID();
                byte[] udpData = message.getBytes();
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
    public void sendCalcKARequestUDPGeneric( DatagramSocket socket, short messageType,
                                             InetAddress serverAddress, int serverPort, int id,
                                             int time, double deltaT, byte incrementType,
                                             byte usesStarterMessage, byte direction, byte repeats,
                                             int maxTime, byte actionAfterMaxTime, byte serverAdjustsZ,
                                             int z, int zMultiplier, double deltaTAfterMax, int k )
        throws IOException
    {
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
        byte[] outData = request.getBytes();
        DatagramPacket outPacket = new DatagramPacket( outData, outData.length, serverAddress,
                                                       serverPort );
        socket.send( outPacket );
    }
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
    public List<Map<State, TestParams>> getAllTestParams( int deltaT, SearchType type )
    {
        List<Map<State, TestParams>> allTestsParameters = new ArrayList<Map<State, TestParams>>();
        Map<State, TestParams> currentTest = null;
        StateSearchParams paramsSearch = null;
        StateRunParams paramsRun = new StateRunParams();
        paramsRun.setFailureRate( 0.10 );
        paramsRun.setMaxRunCount( 3 );
        StateRefineParams paramsRefine = new StateRefineParams();
        paramsRefine.setDeltaT(           100 );
        paramsRefine.setzMultiplier(      2 );
        paramsRefine.setZ(                1 );
        paramsRefine.setPingsPerAttempt(  3 );
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
        return allTestsParameters;
    }
}

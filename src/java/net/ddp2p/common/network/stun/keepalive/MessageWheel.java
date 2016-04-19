package net.ddp2p.common.network.stun.keepalive;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import net.ddp2p.common.network.stun.stun.KeepAliveTimer;
import net.ddp2p.common.network.stun.stun.Message;
import net.ddp2p.common.util.Logger;
public class MessageWheel
{
    public final int SESSION_LIFETIME_BUFFER_MS = 5000;
    public final int QUEUE_PAUSE_MS = 5000;
    PriorityBlockingQueue<KeepAliveData> messageQueue = null;
    Map<SessionKey, KeepAliveData> sessions = null;
    Set<SessionKey> itemsPendingRemoval = null;
    DatagramSocket serverSocket = null;
    Runner runner = null;
    net.ddp2p.common.util.Logger logger = null;
    public static void main( String[] args )
    {
    }
    public MessageWheel( Map<SessionKey, KeepAliveData> sessions, DatagramSocket serverSocket )
    {
        messageQueue = new PriorityBlockingQueue<KeepAliveData>( 10,
                                                                 new KeepAliveDataComparator() );
        itemsPendingRemoval =
            Collections.newSetFromMap( new ConcurrentHashMap<SessionKey, Boolean>() );
        this.sessions = sessions;
        this.serverSocket = serverSocket;
        runner = new Runner( serverSocket );
        logger = new Logger( true, true, true, true );
    }
    public void add( KeepAliveData session )
    {
        if( session == null )
        {
            logger.error( "Session does not exist." );
            return;
        }
        if( session.getUsesStarterMessage() == KeepAliveTimer.NO_STARTER_MESSAGE )
        {
            session.setNextSendTime( System.currentTimeMillis() + session.getCurrentTime() );
            logger.info( "Adding session *WITHOUT* start message." );
        }
        else if( session.getUsesStarterMessage() == KeepAliveTimer.USES_STARTER_MESSAGE )
        {
            session.setNextSendTime( System.currentTimeMillis() );
            logger.info( "Adding session *WITH* start message." );
        }
        messageQueue.offer( session );
        logger.info( "Adding: " + session.getSessionKey().getId()
            + " NextSendTime: " + session.getNextSendTime() );
    }
    public void remove( SessionKey key )
    {
        itemsPendingRemoval.add( key );
    }
    public Runner getRunner()
    {
        return runner;
    }
    public class Runner implements Runnable
    {
        DatagramSocket serverSocket = null;
        public Runner( DatagramSocket serverSocket )
        {
            this.serverSocket = serverSocket;
        }
        @Override
        public void run()
        {
            DatagramSocket socket = serverSocket;
            while( true )
            {
                KeepAliveData current = messageQueue.poll();
                if( current == null )
                {
                    try
                    {
                        Thread.sleep( QUEUE_PAUSE_MS );
                        continue;
                    }
                    catch( InterruptedException e )
                    {
                        logger.debug( "Sleep was interrupted during queue wait." );
                        continue;
                    }
                }
                if( itemsPendingRemoval.contains( current.getSessionKey() ) )
                {
                    itemsPendingRemoval.remove( current.getSessionKey() );
                    continue;
                }
                long difference = current.getNextSendTime() - System.currentTimeMillis();
                logger.debug( "Pulled: " + current.getSessionKey().getId()
                    + " Time Difference: " + difference );
                try
                {
                    if( difference > 0 )
                    {
                        Thread.currentThread().isInterrupted();
                        Thread.sleep( difference );
                    }
                }
                catch( InterruptedException e )
                {
                    messageQueue.offer( current );
                    current = messageQueue.peek();
                    logger.debug( "Interrupted by new session: "
                        + current.getSessionKey().getId() );
                    continue;
                }
                if( itemsPendingRemoval.contains( current.getSessionKey() ) )
                {
                    itemsPendingRemoval.remove( current.getSessionKey() );
                    continue;
                }
                Message responseMessage = new Message();
                responseMessage.setMessageType( Message.STUN_CALC_KEEPALIVE_RESPONSE );
                responseMessage.generateTransactionID();
                KeepAliveTimer timer = new KeepAliveTimer();
                timer.setId( current.getSessionKey().getId() );
                timer.setK( current.getK() );
                timer.setTime( current.getCurrentTime() );
                if( current.getMaxWasReached() )
                {
                    timer.setDeltaT( current.getDeltaTAfterMax() );
                }
                else
                {
                    timer.setDeltaT( current.getDeltaT() );
                }
                timer.setDirection( current.getDirection() );
                timer.setZ( current.getZ() );
                timer.setServerAdjustsZ( current.getServerAdjustsZ() );
                responseMessage.addAttribute( "KeepAliveTimer", timer );
                byte[] response = responseMessage.getBytes();
                try
                {
                    InetAddress clientAddress = InetAddress.getByName( current.getSessionKey()
                                                                              .getAddress() );
                    int clientPort = current.getSessionKey().getPort();
                    DatagramPacket outPacket = new DatagramPacket( response, response.length,
                                                                   clientAddress, clientPort );
                    socket.send( outPacket );
                }
                catch( UnknownHostException e )
                {
                    logger.error("UnknownHostException in MessageWheel.Runner" );
                }
                catch( IOException e )
                {
                    logger.error( "IOException in MessageWheel.Runner" );
                }
                logger.debug( "Sending Message for " + current.getSessionKey().getId()
                        + " at " + System.currentTimeMillis() );
                logger.data( "time", current.getCurrentTime(), "k", current.getK(),
                             "deltaT", current.getDeltaT(), "incrementType",
                             current.getIncrementType(), "k * deltaT",
                             current.getK() * current.getDeltaT(), "2^k * deltaT",
                             ( Math.pow( 2, current.getK() ) * current.getDeltaT() ),
                             "deltaTAfterMax", current.getDeltaTAfterMax(),
                             "max", current.getMaxTime(), "z", current.getZ() );
                logger.data( "address", current.getSessionKey().getAddress(),
                            "port", current.getSessionKey().getPort() );
                boolean noTimeAdjustment = false;
                if(    current.getUsesStarterMessage() == KeepAliveTimer.USES_STARTER_MESSAGE
                    && current.getLocalK() == 0 )
                {
                    logger.debug( "On starter message. No time adjustment." );
                    noTimeAdjustment = true;
                }
                if( current.getLocalK() == 0 && (current.getServerAdjustsZ() == KeepAliveTimer.FALSE) )
                {
                    if( current.getMaxWasReached() )
                    {
                        current.setDeltaTAfterMax( current.getInitialDeltaTAfterMax()
                                / current.getZ() );
                        logger.debug( "ONE TIME Updating deltaT to "+ current.getDeltaTAfterMax () );
                    }
                    else
                    {
                        current.setDeltaT( current.getInitialDeltaT() / current.getZ() );
                        logger.debug( "ONE TIME Updating deltaT to "+ current.getDeltaT() );
                    }
                }
                if(    current.getCurrentTime() != current.getMaxTime()
                    && !noTimeAdjustment )
                {
                    if(     current.getMaxRepeats() > 0
                        && (current.getCurrentRepeats() < current.getMaxRepeats())
                        && (current.getActionAfterMax() == KeepAliveTimer.MAINTAIN_CURRENT) )
                    {
                        logger.debug( "Currently on repeat " + current.getCurrentRepeats() + " of "
                                + current.getMaxRepeats() + ". Not updating time." );
                        current.incrementCurrentRepeats();
                    }
                    else
                    {
                        logger.debug( "Resetting current repeats and updating time." );
                        current.resetCurrentRepeats();
                        current.incrementKNoRepeat(); 
                        int kToUse = current.getK();
                        if( current.getMaxRepeats() > 0 )
                        {
                            kToUse = current.getKNoRepeat();
                        }
                        if( current.getServerAdjustsZ() == KeepAliveTimer.TRUE )
                        {
                            current.setKNoRepeat( 1 );
                            kToUse = current.getInitialK();
                            int kToCompare = 1;
                            if ( current.getIncrementType() == KeepAliveTimer.GEOMETRIC_INCREMENT )
                            {
                                kToCompare = 0;
                            }
                            if( current.getInitialK() > kToCompare )
                            {
                                current.setZ( current.getZ() * current.getzMultiplier() );
                                logger.debug( "Updating z to " + current.getZ() + "with initialK: " + current.getInitialK() );
                            }
                            if( current.getMaxWasReached() )
                            {
                                current.setDeltaTAfterMax( current.getInitialDeltaTAfterMax()
                                        / current.getZ() );
                                logger.debug( "Updating deltaT to "+ current.getDeltaTAfterMax () );
                            }
                            else
                            {
                                current.setDeltaT( current.getInitialDeltaT() / current.getZ() );
                                logger.debug( "Updating deltaT to "+ current.getDeltaT() );
                            }
                        }
                        double intervalToUse = current.getDeltaT();
                        if( current.getMaxWasReached() )
                        {
                            intervalToUse = current.getDeltaTAfterMax();
                        }
                        logger.debug( "Using deltaT: " + intervalToUse );
                        if( intervalToUse > 0 )
                        {
                            double timeDelta = 0;
                            switch( current.getIncrementType() )
                            {
                                case KeepAliveTimer.LINEAR_INCREMENT:
                                    timeDelta = kToUse * intervalToUse;
                                    break;
                                case KeepAliveTimer.GEOMETRIC_INCREMENT:
                                    timeDelta =  (int)Math.pow( 2, kToUse )
                                                 * intervalToUse;
                                    break;
                            }
                            switch( current.getDirection() )
                            {
                                case KeepAliveTimer.DIRECTION_FORWARD:
                                    current.setCurrentTime( (int)(current.getCurrentTime() + timeDelta) );
                                    break;
                                case KeepAliveTimer.DIRECTION_BACKWARD:
                                    current.setCurrentTime( (int)(current.getCurrentTime() - timeDelta) );
                                    break;
                            }
                            logger.debug( "timeDelta: " + timeDelta + " | time: "
                                    + current.getCurrentTime() );
                        }
                    }
                }
                current.setNextSendTime( System.currentTimeMillis() + current.getCurrentTime() );
                if(    (current.getServerAdjustsZ() != KeepAliveTimer.TRUE)
                    && ( !noTimeAdjustment)) // TODO: Update pseudo code if used.
                {
                	current.incrementK(); 
                }
                current.incrementLocalK(); 
                if(    current.getMaxTime() > 0
                    && current.getCurrentTime() >= current.getMaxTime()
                    && !noTimeAdjustment )
                {
                    current.setMaxWasReached( true );
                    current.resetKNoRepeat(); 
                    current.setCurrentTime( current.getMaxTime() );
                    current.setNextSendTime( System.currentTimeMillis() + current.getCurrentTime() );
                    logger.debug( "At MAX time: " + current.getMaxTime() + ". Resetting deltaT." );
                    current.setMaxTime( 0 ); 
                    switch( current.getActionAfterMax() )
                    {
                        case KeepAliveTimer.MAINTAIN_CURRENT:
                            logger.error( "Entered MAX state with MAINTAIN_CURRENT set." );
                            break;
                        case KeepAliveTimer.STAY_CONSTANT:
                            current.setK( 0 );
                            current.setDeltaT( 0 );
                            current.setActionAfterMax( KeepAliveTimer.MAINTAIN_CURRENT );
                            break;
                        case KeepAliveTimer.CHANGE_FORWARD:
                            logger.debug( "Setting direction after max time to FORWARD." );
                            current.setK( 0 );
                            current.setActionAfterMax( KeepAliveTimer.MAINTAIN_CURRENT );
                            current.setDirection( KeepAliveTimer.DIRECTION_FORWARD );
                            break;
                        case KeepAliveTimer.CHANGE_BACKWARD:
                            logger.debug( "Setting direction after max time to BACKWARD." );
                            current.setK( 0 );
                            current.setActionAfterMax( KeepAliveTimer.MAINTAIN_CURRENT );
                            current.setDirection( KeepAliveTimer.DIRECTION_BACKWARD);
                            break;
                    }
                }
                messageQueue.offer( current );
                logger.debug( "Re-queueing..." );
                logger.data( "id", current.getSessionKey().getId(), "T", current.getCurrentTime(),
                             "DeltaT", current.getDeltaT(), "k", current.getK(),
                             "Next Time", current.getNextSendTime() );
            }
        }
    }
}

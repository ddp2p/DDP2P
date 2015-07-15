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

    // TEMP For testing.
    public static void main( String[] args )
    {
//        // Test Data
//        KeepAliveData test1 = new KeepAliveData();
//        test1.setId( 1111 );
//        test1.setCurrentTime( 0 );
//        test1.setNextSendTime( System.currentTimeMillis() );
//
//        KeepAliveData test2 = new KeepAliveData();
//        test2.setId( 2222 );
//        test2.setCurrentTime( 0 );
//        test2.setNextSendTime( System.currentTimeMillis() );
//
//        Map<Integer, KeepAliveData> map
//          = Collections.synchronizedMap( new HashMap<Integer, KeepAliveData>() );
//        map.put( test1.getId(), test1 );
//        map.put( test2.getId(), test2 );
//
//        // needs a socket passed in in order to work.
//        MessageWheel wheel = new MessageWheel( map, null );
//        wheel.add( test1.getId(), 1000 );
//        wheel.add( test2.getId(), 100 );
//        new Thread( wheel.getRunner() ).start();
    }

    // The map must be synchronized.
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

    // For when the deltaT is already set.
    public void add( KeepAliveData session )
    {
        if( session == null )
        {
            logger.error( "Session does not exist." );
            return;
        }

        // Check if this session should send a "starter" message.
        // If not, the nextSendTime should be at the actual deltaT.
        if( session.getUsesStarterMessage() == KeepAliveTimer.NO_STARTER_MESSAGE )
        {
            session.setNextSendTime( System.currentTimeMillis() + session.getCurrentTime() );
            logger.info( "Adding session *WITHOUT* start message." );
        }
        // Otherwise it should be as soon as possible from the addition time.
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
                        // Can't throw exceptions out of here due to Runnable interface.
                        // In this case we'll just start the loop over since nothing is pending.
                        logger.debug( "Sleep was interrupted during queue wait." );
                        continue;
                    }
                }

                // If the next item is on on the remove list, simply continue (item already popped).
                if( itemsPendingRemoval.contains( current.getSessionKey() ) )
                {
                    itemsPendingRemoval.remove( current.getSessionKey() );
                    continue;
                }

                // TODO: Implement session removal based on count.

                // Otherwise see how much longer we need to wait.
                long difference = current.getNextSendTime() - System.currentTimeMillis();
                logger.debug( "Pulled: " + current.getSessionKey().getId()
                    + " Time Difference: " + difference );
                try
                {
                    // This ensures that if difference is negative, the send executes immediately
                    // since it is behind schedule.
                    if( difference > 0 )
                    {
                        // Clear the interrupt flag before sleeping.
                        Thread.currentThread().isInterrupted();
                        Thread.sleep( difference );
                    }
                }
                catch( InterruptedException e )
                {
                    // In the event of an interruption, add the current item back into the
                    // queue and grab the new top item.
                    messageQueue.offer( current );
                    current = messageQueue.peek();
                    logger.debug( "Interrupted by new session: "
                        + current.getSessionKey().getId() );
                    continue;
                }

                // Do another check to make sure that we have not received a STOP message.
                // We don't need to check the expiration time here since that is based on
                // the NextSendTime.
                if( itemsPendingRemoval.contains( current.getSessionKey() ) )
                {
                    itemsPendingRemoval.remove( current.getSessionKey() );
                    continue;
                }

                // Send the message and re-add to the queue for the next iteration.
                Message responseMessage = new Message();
                responseMessage.setMessageType( Message.STUN_CALC_KEEPALIVE_RESPONSE );

                responseMessage.generateTransactionID();

                // Currently the client only gets K and the time from the server message.
                // TODO: May need to add more items in as development continues.
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

                // If this sessions is using a "starter message", don't adjust the time until the
                // actual first sending.
                boolean noTimeAdjustment = false;
                if(    current.getUsesStarterMessage() == KeepAliveTimer.USES_STARTER_MESSAGE
                    && current.getLocalK() == 0 )
                {
                    logger.debug( "On starter message. No time adjustment." );
                    noTimeAdjustment = true;
                }

                // If serverAdjustsZ is false, do an initial adjustment on deltaT with z
                // to sync it with the client.
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

                // Check if the current time is already the max. If so, do not increment it.
                // This will only happen when the client sets is specifically.
                // Also no time adjustment if this is a "starter message."
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
                        current.incrementKNoRepeat(); // only updated after time value changes.

                        int kToUse = current.getK();
                        if( current.getMaxRepeats() > 0 )
                        {
                            kToUse = current.getKNoRepeat();
                        }
                        if( current.getServerAdjustsZ() == KeepAliveTimer.TRUE )
                        {
                            current.setKNoRepeat( 1 );
                            //kToUse = current.getKNoRepeat();
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
                    && (/*current.getIncrementType() == KeepAliveTimer.GEOMETRIC_INCREMENT &&*/ !noTimeAdjustment)) // TODO: Update pseudo code if used.
                {
                	current.incrementK(); // Comment this out to more easily compare timings.
                }
                current.incrementLocalK(); // Actual count of session messages. Not reset.

                // Check for the MAX time and update the deltaT if appropriate.
                if(    current.getMaxTime() > 0
                    && current.getCurrentTime() >= current.getMaxTime()
                    && !noTimeAdjustment )
                {

                    current.setMaxWasReached( true );

                    current.resetKNoRepeat(); // Reset local counter.
                    current.setCurrentTime( current.getMaxTime() );
                    // Reset the next send time if needed.
                    current.setNextSendTime( System.currentTimeMillis() + current.getCurrentTime() );

                    logger.debug( "At MAX time: " + current.getMaxTime() + ". Resetting deltaT." );
                    current.setMaxTime( 0 ); // To prevent resets every time.

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


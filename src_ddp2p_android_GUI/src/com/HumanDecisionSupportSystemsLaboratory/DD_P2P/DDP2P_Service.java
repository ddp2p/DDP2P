/* Copyright (C) 2014,2015 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
Florida Tech, Human Decision Support Systems Laboratory
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation; either the current version of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. */
/* ------------------------------------------------------------------------- */

package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.HumanDecisionSupportSystemsLaboratory.DD_P2P.AndroidChat.AndroidChatReceiver;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DD_DirectoryServer;
import net.ddp2p.common.util.P2PDDSQLException;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by msilaghi on 7/15/15.
 */
public class DDP2P_Service extends Service {
    private static final int ONGOING_NOTIFICATION_ID = 1;
    public static boolean serversStarted = false;
    int _startId;
    Intent _intent; // to use with stopSelfResult(_startId);

    private Looper mServiceLooper;


    @Override
    public void onCreate() {
        Log.d("Service", "DDP2P_Service: onCreate: start");
        super.onCreate();
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        launchForeground();
        startDDP2P(this.getApplicationContext());
        Log.d("Service", "DDP2P_Service: onCreate: stop");
    }

    public static ArrayList<ArrayList<Object>> startDDP2P(Context ctx) {
        Log.d("Service", "DDP2P_Service: startDDP2P: start");
        boolean r = ensureDatabaseIsInited(ctx);
        Log.d("Service", "DDP2P_Service: onCreate: db inited: " + r);

        ArrayList<ArrayList<Object>> peer_IDs = D_Peer.getAllPeers();
        Log.d("Service",
                "DDP2P_Service: onCreate: found peers: #" + peer_IDs.size());

        if (peer_IDs.size() == 0) {
            // testPeerCreation();
            // peer_IDs = D_Peer.getAllPeers();
            // Log.d("onCreateView", "Safe: onCreateView: re-found peers: #" +
            // peer_IDs.size());
        } else {
            DDP2P_Service.startServers();
        }
        Log.d("Service", "DDP2P_Service: startDDP2P: stop");
        return peer_IDs;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return
        super.onStartCommand(intent, flags, startId);
        Log.d("Service", "DDP2P_Service: onStartCommand: start");
        _startId = startId; _intent = intent;

        /**
         * Not sure if this serves (messages just launch foreground, which is already done in create)
         * /
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        */

        // If we get killed, after returning from here, restart
        Log.d("Service", "DDP2P_Service: onStartCommand: stop (message sent)");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Service", "DDP2P_Service: onBind: start");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("Service", "DDP2P_Service: onUnbind: start");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d("Service", "DDP2P_Service: onDestroy: start");
        Log.d("Service", "DDP2P_Service: onDestroy: in");
        Toast.makeText(this, "DDP2P service closing...", Toast.LENGTH_SHORT).show();
        terminateForeground();
        Log.d("Service", "DDP2P_Service: onDestroy: stop");
        super.onDestroy();
    }

    void launchForeground() {
        Log.d("Service", "DDP2P_Service: launchForeground: start");
        Intent notificationIntent = new Intent(this, Main.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 , notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("DDP2P")
                .setContentText("DDP2P start")
                .setSmallIcon(R.drawable.ic_launcher)//.vote_icon)
                //.setLargeIcon(aBitmap)
                .addAction(R.drawable.ic_launcher, "DDP2P", pendingIntent)
                .build();

//        Notification notification = new Notification(R.drawable.vote_icon, getText(R.string.DDP2P), System.currentTimeMillis());
//        notification.setLatestEventInfo(this, "DDP2P", "DDP2P start", pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, notification);
        Log.d("Service", "DDP2P_Service: launchForeground: stop");
    }

    void terminateForeground() {
        Log.d("Service", "DDP2P_Service: terminateForeground: start");
        stopForeground(true);
        // stop DDP2P threads
        DD.stop_servers();
        Log.d("Service", "items to save: " + net.ddp2p.common.data.SaverThreadsConstants.getNumberRunningSaverWaitingItems());
        DD.clean_before_exit();
        // stopSelfResult(_startId);
        stopSelf();
        Log.d("Service", "DDP2P_Service: terminateForeground: stop");
        //System.exit(0);

        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parent;
        while ((parent = rootGroup.getParent()) != null) {
            rootGroup = parent;
        }

        listThreads(rootGroup, "");
    }
    // List all threads and recursively list all subgroup
    public static void listThreads(ThreadGroup group, String indent) {
        System.out.println(indent + "Group[" + group.getName() +
                ":" + group.getClass()+"]");
        int nt = group.activeCount();
        Thread[] threads = new Thread[nt*2 + 10]; //nt is not accurate
        nt = group.enumerate(threads, false);

        // List every thread in the group
        for (int i=0; i<nt; i++) {
            Thread t = threads[i];
            System.out.println(indent + "  Thread[" + t.getName()
                    + ":" + t.getClass() + "]");
        }

        // Recursively list all subgroups
        int ng = group.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[ng*2 + 10];
        ng = group.enumerate(groups, false);

        for (int i=0; i<ng; i++) {
            listThreads(groups[i], indent + "  ");
        }
    }
    private static final Object monitorServers = new Object();
    public static final Object monitorDatabaseInitialization = new Object();

    public static boolean ensureDatabaseIsInited(Context activity) {
        Log.d("Service", "DDP2P_Service: ensureDatabaseIsInited: start");
        synchronized (monitorDatabaseInitialization) {
            // pull out all safes from database
            if (Application_GUI.dbmail == null)
                Application_GUI.dbmail = new Android_DB_Email(activity);
            else
                Log.d("Service", "DDP2P_Service: ensureDatabaseIsInited: dbmail was already here");

            //Safe.safeItself.getActivity());
            if (Application_GUI.gui == null)
                Application_GUI.gui = new Android_GUI();
            else
                Log.d("Service", "DDP2P_Service: ensureDatabaseIsInited: gui was already here");
            if (Application.db == null) {
                try {
                    DBInterface db = new DBInterface("deliberation-app.db");
                    Application.db = db;
                    Log.d("Service", "DDP2P_Service: ensureDatabaseIsInited: db inited");
                } catch (P2PDDSQLException e1) {
                    e1.printStackTrace();
                    return false;
                }
            } else {
                Log.d("Service", "DDP2P_Service: ensureDatabaseIsInited: db was already here");
            }
        }
        Log.d("Service", "DDP2P_Service: ensureDatabaseIsInited: stop");
        return true;
    }
    public static boolean startServers() {
        Log.d("Service", "DDP2P_Service: startServers: start, status="+serversStarted);
        synchronized (monitorServers) {
            if (serversStarted) {
                Log.d("Service", "DDP2P_Service: startServers: quit already done");
                return true;
            }
             serversStarted = true;
        }
        D_Peer myself = HandlingMyself_Peer.get_myself_or_null();
        if (myself == null) {
            Log.d("Service", "DDP2P_Service: startServers: quit myself loaded");

            //initial the server:
            Identity.init_Identity(false, true, false);
            System.out.println("Service: onCreateOptionsMenu: inited");
            HandlingMyself_Peer.loadIdentity(null);
            System.out.println("Service: loaded identity");
        }
        try {
            DD.load_listing_directories();
        } catch (NumberFormatException e){
            Log.i("Service", "some error in server initial!");
            e.printStackTrace();
        } catch (UnknownHostException e){
            Log.i("server", "some error in server initial!");
            e.printStackTrace();
        } catch (P2PDDSQLException e) {
            Log.i("server", "some error in server initial!");
            e.printStackTrace();
        }
        myself = HandlingMyself_Peer.get_myself_or_null();
        if (myself == null) {
            Log.i("Service", "Service: startServers: no myself available, no startServers!");
            //AddSafe.h Toast
            serversStarted = false;
            return false;
        } else {
            Log.i("Service", "Service: startServers: myself available. Clean Addresses. Dirs #"+Identity.getListing_directories_addr().size());

            myself = D_Peer.getPeerByPeer_Keep(myself);

            D_Peer.cleanAddressesKept(myself, true, null);
            D_Peer.cleanAddressesKept(myself, false, null);

            net.ddp2p.common.hds.Address dir0 = null;
            if (Identity.getListing_directories_addr().size() > 0) {
                dir0 = Identity.getListing_directories_addr().get(0);
                Log.i("Service", "Service: startServers: myself available. Existing first dir:"+dir0);
            } else {
                String dirdd = "DIR%B%0.9.56://163.118.78.40:10000:10000:DD";
                DD_DirectoryServer ds = new DD_DirectoryServer();
                ds.parseAddress(dirdd);
                ds.save();
                dir0 = new Address(dirdd);
                Log.i("Service", "Service: startServers: myself available. Default dir:"+dir0);
            }
            dir0.pure_protocol = Address.DIR;
            dir0.branch = DD.BRANCH;
            dir0.agent_version = DD.VERSION;
            dir0.certified = true;
            dir0.version_structure = Address.V3;
            dir0.address = dir0.domain+":"+dir0.tcp_port;
            System.out.println("Service: startServers: Adding address: "+dir0);
            Log.i("Service", "Service: startServers: myself available. Default dir added:" + dir0);
            D_Peer.addAddressKept(myself, dir0, true, null);
            System.out.println("Myself After Adding address: "+myself);
            Log.i("Service", "Service: startServers: myself="+ myself.toString());

            myself.releaseReference();


            try {
                DD.startNATServer(true);
                DD.startUServer(true, Identity.current_peer_ID);
                DD.startServer(false, Identity.current_peer_ID);
                DD.startClient(true);

            } catch (NumberFormatException e) {
            } catch (P2PDDSQLException e) {
                System.err.println("Safe: onCreateView: error");
                e.printStackTrace();
            }

            Log.i("Service", "Service: startServers: test peer...");
        }
        //initialize chat:
        try {
            net.ddp2p.common.plugin_data.PluginRegistration.loadPlugin(com.HumanDecisionSupportSystemsLaboratory.DD_P2P.AndroidChat.Main.class, HandlingMyself_Peer.getMyPeerGID(), HandlingMyself_Peer.getMyPeerName());
            com.HumanDecisionSupportSystemsLaboratory.DD_P2P.AndroidChat.Main.receiver = new AndroidChatReceiver();
        } catch (MalformedURLException e) {
            Log.i("Service", "some error in chat initial!");
            e.printStackTrace();
        }
        Log.d("Service", "DDP2P_Service: startServers: stop");
        return true;
    }


    private ServiceHandler mServiceHandler;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
            Log.d("Service", "DDP2P_Service: handleMessage: <init>");
        }
        @Override
        public void handleMessage(Message msg) {
            Log.d("Service", "DDP2P_Service: handleMessage: start");
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            launchForeground();
            //stopSelf(msg.arg1);
            Log.d("Service", "DDP2P_Service: handleMessage: stop");
        }
    }
    public class LocalBinder extends Binder {
        DDP2P_Service getService() {
            // Return this instance of LocalService so clients can call public methods
            Log.d("Service", "DDP2P_Service: handleMessage: getService");
            return DDP2P_Service.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();
}

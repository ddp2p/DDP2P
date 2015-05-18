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

import net.ddp2p.ASN1.Decoder;
import net.ddp2p.common.hds.Address;

import java.io.File;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.ddp2p.common.hds.PeerInput;
import net.ddp2p.common.util.DDP2P_ServiceThread;
import net.ddp2p.common.util.DD_DirectoryServer;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.StegoStructure;
import android.annotation.TargetApi;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.HumanDecisionSupportSystemsLaboratory.DD_P2P.AndroidChat.AndroidChatReceiver;

import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.util.Util;


public class Main extends FragmentActivity implements TabListener, LoadPK.LoadPKListener {
	
	public static boolean serversStarted = false;

	public static byte[] icon_org;

	android.app.ActionBar actionBar = null;
	
	ViewPager mViewPager;

	TabFragmentPagerAdapter mAdapter;
	
	int SELECT_PHOTO = 42;
	int SELECT_PHOTO_KITKAT = 43;
    final static int PAGES_NB = 2;
	final static int POSITION_SAFE = 0;
	final static int POSITION_ORGS = 1;
    final static int RESULT_ADD_PEER = 11;
	
	private String selectedImagePath;

	private File selectImageFile;
	public Fragment findFragmentByPosition(int position) {
		FragmentPagerAdapter fragmentPagerAdapter = mAdapter;
		return getSupportFragmentManager().findFragmentByTag(
				"android:switcher:" + mViewPager.getId() + ":"
						+ fragmentPagerAdapter.getItemId(position));
	}
	private static String makeFragmentName(int viewPagerId, int index) {
		return "android:switcher:" + viewPagerId + ":" + index;
	}
	@Override
	public void getPKResult(StegoStructure param) {
		if (param == null) return;

		new DDP2P_ServiceThread("SavingImports", true, param) {
			@Override
			public void _run() {
				StegoStructure imported_object = (StegoStructure)ctx;
				try {
					imported_object.saveSync(); //.save();
					Log.d("Import", "Main saved stego");
					Orgs.reloadOrgs();
					Safe.loadPeer();
					Log.d("Import", "Main reloaded data");
					handler.sendMessage(handler.obtainMessage(Main.REFRESH_ALL));
				} catch(Exception e) {}

			}
		}.start();

//		new AsyncTask<StegoStructure,Object, Object>(){
//			@Override
//			protected Object doInBackground(StegoStructure... param) {
//				StegoStructure imported_object = param[0];
//				try {
//					imported_object.saveSync(); //.save();
//					Log.d("Import", "Main saved stego");
//					Orgs.reloadOrgs();
//					Safe.loadPeer();
//					Log.d("Import", "Main reloaded data");
//
//				} catch(Exception e) {}
//				return null;
//			}
//			protected void onPostExecute(Long result) {
//				Toast.makeText(Main.this.getApplicationContext(), getResources().getString(R.string.SaveSuccess), Toast.LENGTH_SHORT).show();
//				//Safe.safeAdapter.notifyDataSetChanged();
//				{
//					Orgs o = (Orgs) Main.this.findFragmentByPosition(0);
//					//o.reloadOrgs();
//					Orgs.listAdapter = new Orgs.OrgAdapter(o.getActivity(),
//							Orgs.orgName);
//					o.setListAdapter(Orgs.listAdapter);
//					Orgs.OrgAdapter adapt = ((Orgs.OrgAdapter) o.getListAdapter());
//					adapt.notifyDataSetChanged();
//					Log.d("Import", "Main updated org");
//				}
//				{
//					//Safe.loadPeer();
//					Safe.safeAdapter = new Safe.SafeAdapter(Safe.safeItself.getActivity(),
//							Safe.list, Safe.imgData);
//					Safe.safeItself.setListAdapter(Safe.safeAdapter);
//					Safe.SafeAdapter adapt = ((Safe.SafeAdapter) ((Safe) Main.this.findFragmentByPosition(0)).getListAdapter());
//					adapt.notifyDataSetChanged();
//					Log.d("Import", "Main updated safe");
//				}
//				/*
//				List<Fragment> f = Main.this.getSupportFragmentManager().getFragments();
//				for (Fragment _f: f) {
//					if (_f == null) continue;
//					if (_f instanceof Orgs) {
//						Log.d("Import", "Main found org");
//						Orgs.OrgAdapter adapt = ((Orgs.OrgAdapter)((Orgs) _f).getListAdapter());
//						adapt.notifyDataSetChanged();
//					}
//					if (_f instanceof Safe) {
//						Log.d("Import", "Main found safe");
//						Safe.SafeAdapter adapt = ((Safe.SafeAdapter)((Safe) _f).getListAdapter());
//						adapt.notifyDataSetChanged();
//					}
//				}
//				*/
//				//mViewPager.getChildAt(1);
//			}
//		}.execute(param);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        {
            // Use intent in case the app was started by another app to send messages/images
            Intent intent = getIntent();
            if (intent != null) {
                Uri data = intent.getData();
                if (intent.getType() != null) {
                    // Figure out what to do based on the intent type
                    if (intent.getType().contains("image/")) {
                        // Handle intents with image data ...

                        // should probably set here a window only to select contact destination

                        // Create intent to deliver some kind of result data
                        //Intent result = new Intent("net.ddp2p.RESULT_ACTION", Uri.parse("content://result_uri"));
                        // setResult(Activity.RESULT_OK, result);
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                    } else if (intent.getType().equals("text/plain")) {
                        // Handle intents with text ...

                        // should probably set here a window only to select contact destination

                        // Create intent to deliver some kind of result data
                        //Intent result = new Intent("net.ddp2p.RESULT_ACTION", Uri.parse("content://result_uri"));
                        // setResult(Activity.RESULT_OK, result);
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                    }
                }
            }
        }

		setContentView(R.layout.main);
        
		//initial action bar
		actionBar = this.getActionBar();
        
		//set tab navigation mode
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		//initial view pager
        mViewPager = (ViewPager) this.findViewById(R.id.pager);
                        
        actionBar.setDisplayHomeAsUpEnabled(false);  
        
        //add adapter
        mAdapter = new TabFragmentPagerAdapter(getSupportFragmentManager());  
            
        mViewPager.setAdapter(mAdapter);  
            
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int position) {
                // if (position > 1) position = 1;
				actionBar.setSelectedNavigationItem(position);
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
				
			}
		
        });
        
		//add tabs
		actionBar.addTab(actionBar.newTab().setText(Util.__(getString(R.string.users)))
				.setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(Util.__(getString(R.string.organizations)))
				.setTabListener(this));
        if (PAGES_NB > 2) {
            actionBar.addTab(actionBar.newTab().setText("Acts")
                    .setTabListener(this));
        }
 
		new net.ddp2p.common.util.DDP2P_ServiceThread("loading icons", false, this) {
			@Override
			public void _run() {
				Main m = (Main) ctx;
				Bitmap bmp = PhotoUtil.decodeSampledBitmapFromResource(getResources(), R.drawable.organization_default_img, 55, 55);
				icon_org = PhotoUtil.BitmapToByteArray(bmp, 100);
			}
		}.start();
   }//end of onCreate()

		

	@Override
	public void onTabReselected(android.app.ActionBar.Tab tab,
			FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(android.app.ActionBar.Tab tab,
			FragmentTransaction ft) {
		
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(android.app.ActionBar.Tab tab,
			FragmentTransaction ft) {
		
	}

	//initial menu in action bar
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//System.out.println("Main: onCreateOptionsMenu: start");
		Log.d("onCreateOptionsMenu", "Main: onCreateOptionsMenu: start");
		
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		 //menu.add(0, MENU_PAUSE, 0, "Pause").setIcon(android.R.drawable.sym_action_email);
		/*
		//initial the server:
		//Identity.init_Identity();
		System.out.println("Main: onCreateOptionsMenu: inited");
		//HandlingMyself_Peer.loadIdentity(null);
		System.out.println("Main: loaded identity");
		
		try {
			DD.load_listing_directories();
		} catch (NumberFormatException e){
			Log.i("server", "some error in server initial!");
			e.printStackTrace();
		} catch (UnknownHostException e){
			Log.i("server", "some error in server initial!");
			e.printStackTrace();
		} catch (P2PDDSQLException e) {
			Log.i("server", "some error in server initial!");
			e.printStackTrace();
		}
		D_Peer myself = HandlingMyself_Peer.get_myself();
		myself.cleanAddresses(true, null);
		myself.cleanAddresses(false, null);
		hds.Address dir0 = Identity.listing_directories_addr.get(0);
		dir0.pure_protocol = hds.Address.DIR;
		dir0.branch = DD.BRANCH;
		dir0.agent_version = DD.VERSION;
		dir0.certified = true;
		dir0.version_structure = hds.Address.V3;
		dir0.address = dir0.domain+":"+dir0.tcp_port;
		System.out.println("Adding address: "+dir0);
		myself.addAddress(dir0, true, null);
		System.out.println("Myself After Adding address: "+myself);
		Log.i("myself", myself.toString());

		
		try {
			DD.startUServer(true, Identity.current_peer_ID);
			DD.startServer(false, Identity.current_peer_ID);
			DD.startClient(true);
			
		} catch (NumberFormatException e) {
		} catch (P2PDDSQLException e) {
			System.err.println("Safe: onCreateView: error");
			e.printStackTrace();
		}
		
		Log.i("Test peer", "test peer...");

		//initial chat:
		try {
			plugin_data.PluginRegistration.loadPlugin(com.HumanDecisionSupportSystemsLaboratory.DDP2P.AndroidChat.Main.class, HandlingMyself_Peer.getMyPeerGID(), HandlingMyself_Peer.getMyPeerName());
			com.HumanDecisionSupportSystemsLaboratory.DDP2P.AndroidChat.Main.receiver = new AndroidChatReceiver();
		} catch (MalformedURLException e) {
			Log.i("chat", "some error in chat initial!");
			e.printStackTrace();
		}
		*/
		
		Log.d("onCreateOptionsMenu", "Main: onCreateOptionsMenu: almost done");
		boolean result = super.onCreateOptionsMenu(menu);
		Log.d("onCreateOptionsMenu", "Main: onCreateOptionsMenu: done");
		return result;
	}
	
    //the plus mark, add a new safe
	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		

		if (item.getItemId() == R.id.add_new_safe_my) {
            try {
                DD.load_listing_directories();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this.getApplicationContext(),
                        "Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG)
                        .show();
                // return;
            }

            if ((Identity.getListing_directories_addr().size() <= 0)) {
				Toast.makeText(this, "To add your safe, first add a directory from the Manage Directories top-right menu!", Toast.LENGTH_LONG).show();
				return super.onOptionsItemSelected(item);
			}
			Toast.makeText(this, "add a new safe my", Toast.LENGTH_SHORT).show();
			
			Intent intent = new Intent();
			intent.setClass(this, AddSafe.class);
			startActivityForResult(intent, Main.RESULT_ADD_PEER);
		}

		if (item.getItemId() == R.id.add_new_safe_other) {
			if ((Identity.getListing_directories_addr().size() <= 0)) {
				Toast.makeText(this, "To add a safe, first add a directory from the Manage Directories top-right menu!", Toast.LENGTH_LONG).show();
				return super.onOptionsItemSelected(item);
			}
			Toast.makeText(this, "add a new safe other", Toast.LENGTH_SHORT).show();
		    
		    if (Build.VERSION.SDK_INT < 19){
		        Intent intent = new Intent(); 
		        intent.setType("image/*");
		        intent.setAction(Intent.ACTION_GET_CONTENT);
		        startActivityForResult(intent, SELECT_PHOTO);
		    } else {
		        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		        intent.setType("image/*");
		        startActivityForResult(intent, SELECT_PHOTO_KITKAT);
		    }
		}
		
		if (item.getItemId() == R.id.add_new_org) {
			Toast.makeText(this, "add a new organization", Toast.LENGTH_SHORT).show();
			
			Intent intent = new Intent();
			intent.setClass(this, AddOrg.class);
			startActivity(intent);
		}
		
		if (item.getItemId() == R.id.action_directories) {
			Toast.makeText(this, "adding a directory", Toast.LENGTH_SHORT).show();
			
			Intent intent = new Intent();
			intent.setClass(this, SelectDirectoryServer.class);
			startActivity(intent);
		}
		
		if (item.getItemId() == R.id.action_loadAddresss) {
			
			FragmentManager fm = getSupportFragmentManager();
		    LoadPK loadPKDialog = new LoadPK();
			//loadPKDialog.setTargetFragment(this,0);
		    loadPKDialog.show(fm, "fragment_send_public_key");


		}

        // not yet implemented!
		if (item.getItemId() == R.id.action_start_directory) {
			
			Intent i = new Intent();
			i.setClass(this, StartDirectoryServer.class);
			startActivity(i);
		}
		
		if (item.getItemId() == R.id.action_settings) {		
			Intent intent = new Intent();
			intent.setClass(this, Setting.class);
			startActivity(intent);
		}
		
		return super.onOptionsItemSelected(item);
	}
		
    
    //adapter
    public static class TabFragmentPagerAdapter extends FragmentPagerAdapter{  
    	  
        public TabFragmentPagerAdapter(FragmentManager fm) {  
            super(fm);  
        }  
  
        @Override  
        public Fragment getItem(int pos) {  
            Bundle bun = new Bundle();

            switch (pos) {   
            
            case POSITION_SAFE:
            	Safe mainAct = new Safe();
                bun.putInt("pageNo", pos+1);
            	mainAct.setArguments(bun);
            	
                return mainAct;
            	
            case POSITION_ORGS:
                Orgs orgs = new Orgs();       
                bun.putInt("pageNo", pos+1);
                orgs.setArguments(bun);
                return orgs;
                
            case 2:
                Acts chat = new Acts();       
                bun.putInt("pageNo", pos+1);
                chat.setArguments(bun);
                return chat;
            }  
            
            return null;
        }  
  
        @Override  
        public int getCount() {  
              
            return Main.PAGES_NB;
        }   
    }


	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == RESULT_OK && requestCode == this.RESULT_ADD_PEER) {
            byte[] _pi = resultData.getByteArrayExtra(AddSafe.PI);
            net.ddp2p.common.hds.PeerInput pi = null;
            if (_pi != null) try { pi = new PeerInput().decode(new Decoder(_pi));}catch(Exception e){e.printStackTrace();}
            if (pi == null) pi = Safe.peerInput;
            new PeerCreatingThread(pi).start();

            super.onActivityResult(requestCode, resultCode, resultData);
            return;
        }
		if (resultCode == RESULT_OK && resultData != null) {
            Uri uri = null;
            
            if (requestCode == SELECT_PHOTO) {
                uri = resultData.getData();
                Log.i("Uri", "Uri: " + uri.toString());
            } else if (requestCode == SELECT_PHOTO_KITKAT) {
                uri = resultData.getData();
                Log.i("Uri_kitkat", "Uri: " + uri.toString());
                final int takeFlags = resultData.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                // Check for the freshest data.
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
            }
            
 
            selectedImagePath = FileUtils.getPath(this,uri);
            Log.i("path", "path: " + selectedImagePath); 
                
            selectImageFile = new File(selectedImagePath);
            String error;    
            StegoStructure adr[] = DD.getAvailableStegoStructureInstances();
            int[] selected = new int[1];
            try {
            	error = DD.loadBMP(selectImageFile, adr, selected);
            	Log.i("error", "error: " + error); 
			    if (error == "") {
			        adr[selected[0]].save();
			        Toast.makeText(this, "add new safe other successfully!", Toast.LENGTH_SHORT).show();
			    }						
            }
		    catch (Exception e) {
		    	Toast.makeText(this, "Unable to load safe from this photo!", Toast.LENGTH_SHORT).show();
			    e.printStackTrace();
		    } 
                	
		}
		super.onActivityResult(requestCode, resultCode, resultData);
	}



	private static final Object monitorServers = new Object();
	public static boolean startServers() {
		synchronized (monitorServers) {
			if (serversStarted) return true;
			serversStarted = true;
		}
		//initial the server:
		Identity.init_Identity(false, true, false);
		System.out.println("Main: onCreateOptionsMenu: inited");
		HandlingMyself_Peer.loadIdentity(null);
		System.out.println("Main: loaded identity");
		
		try {
			DD.load_listing_directories();
		} catch (NumberFormatException e){
			Log.i("server", "some error in server initial!");
			e.printStackTrace();
		} catch (UnknownHostException e){
			Log.i("server", "some error in server initial!");
			e.printStackTrace();
		} catch (P2PDDSQLException e) {
			Log.i("server", "some error in server initial!");
			e.printStackTrace();
		}
		D_Peer myself = HandlingMyself_Peer.get_myself_or_null();
		if (myself == null) {
			Log.i("server", "Safe: startServers: no myself available!");
			//AddSafe.h Toast
			serversStarted = false;
			return false;
		} else {
			myself.cleanAddresses(true, null);
			myself.cleanAddresses(false, null);
            net.ddp2p.common.hds.Address dir0 = null;
			if (Identity.getListing_directories_addr().size() > 0) dir0 = Identity.getListing_directories_addr().get(0);
			else {
				String dirdd = "DIR%B%0.9.56://163.118.78.40:10000:10000:DD";
				DD_DirectoryServer ds = new DD_DirectoryServer();
				ds.parseAddress(dirdd);
				ds.save();
				dir0 = new Address(dirdd);
			}
			dir0.pure_protocol = net.ddp2p.common.hds.Address.DIR;
			dir0.branch = DD.BRANCH;
			dir0.agent_version = DD.VERSION;
			dir0.certified = true;
			dir0.version_structure = net.ddp2p.common.hds.Address.V3;
			dir0.address = dir0.domain+":"+dir0.tcp_port;
			System.out.println("Adding address: "+dir0);
			myself.addAddress(dir0, true, null);
			System.out.println("Myself After Adding address: "+myself);
			Log.i("myself", myself.toString());
		
			
			try {
				DD.startUServer(true, Identity.current_peer_ID);
				DD.startServer(false, Identity.current_peer_ID);
				DD.startClient(true);
				
			} catch (NumberFormatException e) {
			} catch (P2PDDSQLException e) {
				System.err.println("Safe: onCreateView: error");
				e.printStackTrace();
			}
			
			Log.i("Test peer", "test peer...");
		}	
		//initial chat:
		try {
            net.ddp2p.common.plugin_data.PluginRegistration.loadPlugin(com.HumanDecisionSupportSystemsLaboratory.DD_P2P.AndroidChat.Main.class, HandlingMyself_Peer.getMyPeerGID(), HandlingMyself_Peer.getMyPeerName());
			com.HumanDecisionSupportSystemsLaboratory.DD_P2P.AndroidChat.Main.receiver = new AndroidChatReceiver();
		} catch (MalformedURLException e) {
			Log.i("chat", "some error in chat initial!");
			e.printStackTrace();
		}
		return true;
	}

    class PeerCreatingThread extends Thread {
        PeerInput pi;
        final public String TAG = "PEER_ADD_TH";

        PeerCreatingThread(PeerInput _pi) {
            Log.d(TAG, "add safe: peerCreatingThread constructor");
            pi = _pi;
        }

        public void run() {

            Log.d("onCreatePeerCreatingTh",
                    "PeerCreatingThread: run: start");
            gen();

            Log.d("onCreatePeerCreatingTh",
                    "PeerCreatingThread: run: announced");
            Log.d(TAG, "add safe: run()");

            Safe.loadPeer();

            Log.d("onCreatePeerCreatingTh",
                    "PeerCreatingThread: run: generated");
            threadMsg("a");

        }

        public D_Peer gen() {
            Log.d(TAG, "add safe: gen()");
            D_Peer peer = HandlingMyself_Peer.createMyselfPeer_w_Addresses(pi,
                    true);
            D_Peer myself = HandlingMyself_Peer.get_myself_or_null();
            myself = D_Peer.getPeerByPeer_Keep(myself);
            if (myself == null) {
                Toast.makeText(Main.this, //getApplicationContext(),
                        "Could not set slogan/email", Toast.LENGTH_SHORT)
                        .show();
                return peer;
            }
            Log.d("onCreatePeerCreatingTh",
                    "PeerCreatingThread: gen: start");
            myself.setEmail(pi.email);
            myself.setSlogan(pi.slogan);
            myself.storeRequest();

            Log.d("onCreatePeerCreatingTh",
                    "PeerCreatingThread: gen: inited");
            // data.HandlingMyself_Peer.setMyself(myself, true, false);
            if (!Main.serversStarted)
                Main.startServers();
            return peer;
        }

        private void threadMsg(String msg) {
            if (! msg.equals(null) && ! msg.equals("")) {
                Log.d(TAG, "add safe: threadMsg");
                Message msgObj = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("message", msg);
                msgObj.setData(b);
                handler.sendMessage(msgObj);
                Log.d(TAG, "add safe: threadMsg finished");

				handler.sendMessage(handler.obtainMessage(Main.REFRESH_ALL));
            }
        }
    }
	void refreshOrg() {
		if (Orgs.activ == null) {
			Log.d("Main", "refresh Safe none");
			return;
		}
		Orgs o = (Orgs) Main.this.findFragmentByPosition(POSITION_ORGS);
		//o.reloadOrgs();
		if (o == null) {
			Log.d("Main", "refresh Orgs none now");
			return;
		}
		if (o != Orgs.activ) Log.d("Main", "refresh Orgs changed");

		// Without this ist does not show first
		Orgs.OrgAdapter _adapt = (Orgs.OrgAdapter) o.getListAdapter();
		if (_adapt != null) _adapt.notifyDataSetChanged();

		Orgs.OrgAdapter newAdapter = new Orgs.OrgAdapter(o.getActivity(), Orgs.orgName);
		o.setListAdapter(newAdapter);
		Orgs.OrgAdapter adapt = ((Orgs.OrgAdapter) o.getListAdapter());
		adapt.notifyDataSetChanged();
		Orgs.listAdapter = newAdapter;
	}
	void refreshSafe() {
		if (Safe.safeItself == null) {
			Log.d("Main", "refresh Safe none");
			return;
		}
		Safe s = (Safe) Main.this.findFragmentByPosition(POSITION_SAFE);
		if (s == null) {
			Log.d("Main", "refresh Safe none now");
			return;
		}
		if (s != Safe.safeItself) Log.d("Main", "refresh Safe changed");

		// Without this ist does not show first
		Safe.SafeAdapter _adapt = (Safe.SafeAdapter) s.getListAdapter();
		if (_adapt != null) _adapt.notifyDataSetChanged();

		Safe.SafeAdapter newAdapter = new Safe.SafeAdapter(s.getActivity(), Safe.list, Safe.imgData);

		s.setListAdapter(newAdapter);
		newAdapter.notifyDataSetChanged();
		Safe.safeAdapter = newAdapter;
	}
	final static int REFRESH_ALL = 10;

    private final Handler handler = new Handler() {
        final static String TAG = "Main_Handler";

        // Create handleMessage function

        public void handleMessage(Message msg) {

			if (msg.what == Main.REFRESH_ALL) {
				refreshOrg();
			}
            //String aResponse = msg.getData().getString("message");
            Log.d(TAG, "add safe: handler");
            Toast.makeText(Main.this, "Added a new safe successfully!", Toast.LENGTH_LONG).show();

		 	refreshSafe();

            /*
            Intent i = getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage( getBaseContext().getPackageName() );
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            */
        }
    };


}

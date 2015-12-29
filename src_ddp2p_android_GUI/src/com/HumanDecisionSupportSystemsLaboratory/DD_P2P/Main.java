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

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;

import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.hds.PeerInput;
import net.ddp2p.common.util.DDP2P_ServiceThread;
import net.ddp2p.common.util.StegoStructure;
import android.annotation.TargetApi;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.util.Util;


public class Main extends FragmentActivity implements TabListener, LoadPK.LoadPKListener {

	public static byte[] icon_org;

	android.app.ActionBar actionBar = null;
	
	ViewPager mViewPager;

	TabFragmentPagerAdapter mAdapter;
	
	final static int SELECT_PHOTO = 42;
	final static int SELECT_PHOTO_KITKAT = 43;
	final static int RESULT_ADD_PEER = 11;
	final static int RESULT_ADD_ORG = 12;
	final static int RESULT_SETTINGS = 13;
	final static int RESULT_STARTUP_DIRS = 20;

	final static String RESULT_ORG = "RESULT_ORG";

	final static String DD_WIZARD_SKIP = "DD_WIZARD_SKIP";

    final static int PAGES_NB = 2;
	final static int POSITION_SAFE = 0;
	final static int POSITION_ORGS = 1;

	private String selectedImagePath;

	private File selectImageFile;
	public static boolean startup_wizzard_passed = false;

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

	public static class StartUp extends DialogFragment {
		private Button butImport;
		private Button butAbandon;
		private Button butCreate;

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			switch (requestCode & 0x0FFFF) {
				case Main.RESULT_STARTUP_DIRS:
					Toast.makeText(getActivity(), "Defining My Identity", Toast.LENGTH_SHORT).show();

					Log.d("Main", "Main:StartUp:onActivityResult: create: add peer request=" + Main.RESULT_ADD_PEER);
					Intent intent = new Intent();
					intent.setClass(getActivity(), AddSafe.class);
					getActivity().startActivityForResult(intent, Main.RESULT_ADD_PEER);
					Log.d("Main", "Main:StartUp:onActivityResult: create: added peer");

					android.support.v4.app.FragmentTransaction ft = getFragmentManager()
							.beginTransaction();
					ft.detach(StartUp.this);
					ft.commit();
			}
			Main.startup_wizzard_passed = true;
			super.onActivityResult(requestCode, resultCode, data);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			Log.d("Main", "Main:StartUp:onCreateView: start");

			View view = inflater.inflate(R.layout.dialog_to_start_up_vertical, container);
			butImport = (Button) view.findViewById(R.id.dialog_startup_import);
			butAbandon = (Button) view.findViewById(R.id.dialog_startup_skip);
			butCreate = (Button) view.findViewById(R.id.dialog_startup_createNew);
			getDialog().setTitle(getString(R.string.dialog_wizard_title));

			butImport.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Log.d("Main", "Main:StartUp:onCreateView: import");

					//Intent intent = new Intent().setClass(getActivity(), ImportBrowseWebObjects.class);
					//startActivity(intent);

					FragmentManager fm = getActivity().getSupportFragmentManager();
					LoadPK loadPKDialog = new LoadPK();
					//loadPKDialog.setTargetFragment(this,0);
					loadPKDialog.show(fm, "fragment_load_public_key");

					android.support.v4.app.FragmentTransaction ft = getFragmentManager()
							.beginTransaction();
					ft.detach(StartUp.this);
					ft.commit();
				}
			});

			butCreate.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Log.d("Main", "Main:StartUp:onCreateView: create");

					try {
						DD.load_listing_directories();
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(getActivity(),
								"Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG)
								.show();
						return;
					}

					Log.d("Main", "Main:StartUp:onCreateView: create: loaded dirs");
					if ((Identity.getListing_directories_addr().size() <= 0)) {
						butCreate.setText(getString(R.string.continue_create_new_peer));
						butImport.setVisibility(View.GONE);
						butAbandon.setVisibility(View.GONE);
						//Toast.makeText(getActivity(), "To add your safe, first add a directory from the Manage Directories top-right menu!", Toast.LENGTH_LONG).show();
						Log.d("Main", "Main:StartUp:onCreateView: create: import dirs");
						Intent intent = new Intent().setClass(getActivity(), ImportBrowseWebObjects_Dirs.class);
						intent.putExtra(ImportBrowseWebObjects.PARAM_INSTRUCTION,
								getText(R.string.import_web_object_instr_dir));
						startActivityForResult(intent, Main.RESULT_STARTUP_DIRS);
						Log.d("Main", "Main:StartUp:onCreateView: create: imported dirs");
						return;
					}
					Toast.makeText(getActivity(), "Defining My Identity", Toast.LENGTH_SHORT).show();

					Log.d("Main", "Main:StartUp:onCreateView: create: add peer request=" + Main.RESULT_ADD_PEER);
					Intent intent = new Intent();
					intent.setClass(getActivity(), AddSafe.class);
					getActivity().startActivityForResult(intent, Main.RESULT_ADD_PEER);
					Log.d("Main", "Main:StartUp:onCreateView: create: added peer");

					android.support.v4.app.FragmentTransaction ft = getFragmentManager()
							.beginTransaction();
					ft.detach(StartUp.this);
					ft.commit();
				}
			});

			butAbandon.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					DD.setAppTextNoException(Main.DD_WIZARD_SKIP, "Y");
					Log.d("Main", "Main:StartUp:onCreateView: abandon");
					android.support.v4.app.FragmentTransaction ft = getFragmentManager()
							.beginTransaction();
					ft.detach(StartUp.this);
					ft.commit();
				}
			});

			Log.d("Main", "Main:StartUp:onCreateView: stop");
			//return super.onCreateView(inflater, container, savedInstanceState);
			return view;
		}
	}
	/*
	@Override
	public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
		Log.d("Main", "Main:onCreateView: start");
		View v = super.onCreateView(parent, name, context, attrs);
		Log.d("Main", "Main:onCreateView: stop");
		return v;
	}
	*/

	boolean _wizard_started = false;

	public void askStartUp() {
		_wizard_started = true;
		Log.d("Main", "Main:askStartUp: start");
		// update name dialog
		FragmentManager fm = getSupportFragmentManager();
		StartUp dialog;
		dialog = new StartUp();
		//dialog.setArguments(b);
		dialog.show(fm, "fragment_startup");
		Log.d("Main", "Main:askStartUp: stop");
	}
	BroadcastReceiver resultReceiver;
	final static public String BROADCAST_MAIN_RECEIVER = "net.ddp2p.local";
	final static public String BROADCAST_PARAM_TOAST = "toast";
	private BroadcastReceiver createBroadcastReceiver() {
		return new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String toast = intent.getStringExtra(Main.BROADCAST_PARAM_TOAST);
				Toast.makeText(Main.this, toast, Toast.LENGTH_LONG).show();
			}
		};
	}
	@Override
	protected void onDestroy() {
		if (resultReceiver != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(resultReceiver);
		}
		Log.d("Main", "Main: onDestroy: done");
		super.onDestroy();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        {
			Log.d("Main", "Main: onCreate: start");
			resultReceiver = createBroadcastReceiver();
			LocalBroadcastManager.getInstance(this).registerReceiver(resultReceiver, new IntentFilter(Main.BROADCAST_MAIN_RECEIVER));

            // Use intent in case the app was started by another app to send messages/images
            Intent intent = getIntent();
            if (intent != null) {
                Uri data = intent.getData();
				String urlData = intent.getDataString();
				Log.d("Main", "Main: onCreate: URI is: "+data+" str="+urlData);
				if (data != null) {
					String type = intent.getType();
					Log.d("Main", "Main: onCreate: intent type=" + type);
					if (type != null) {
						// Figure out what to do based on the intent type
						if (type.contains("image/")) {
							Log.d("Main", "Main: onCreate: image intent: start ImportBrowseWebObjects");
							// Handle intents with image data ...

							// should probably set here a window only to select contact destination
							Intent browser = new Intent();
							browser.setClass(this, ImportBrowseWebObjects.class);
							//browser.putExtra(ImportBrowseWebObjects.PARAM_BROWSER_URL, data);
							browser.setData(data);
							startActivity(browser);

							// Create intent to deliver some kind of result data
							//Intent result = new Intent("net.ddp2p.RESULT_ACTION", Uri.parse("content://result_uri"));
							// setResult(Activity.RESULT_OK, result);
							//// setResult(Activity.RESULT_CANCELED);
							//// finish();
						} else if (type.equals("text/plain")) {
							Log.d("Main", "Main: onCreate: text intent: start ImportBrowseWebObjects");
							// Handle intents with text ...

							// should probably set here a window only to select contact destination
							Intent browser = new Intent();
							browser.setClass(this, ImportBrowseWebObjects.class);
							//browser.putExtra(ImportBrowseWebObjects.PARAM_BROWSER_URL, data);
							browser.setData(data);
							startActivity(browser);

							//// Create intent to deliver some kind of result data
							////Intent result = new Intent("net.ddp2p.RESULT_ACTION", Uri.parse("content://result_uri"));
							//// setResult(Activity.RESULT_OK, result);

							//setResult(Activity.RESULT_CANCELED);
							//finish();
						}
					} else {
						Log.d("Main", "Main: onCreate: null type intent: start ImportBrowseWebObjects");
						Intent browser = new Intent();
						browser.setClass(this, ImportBrowseWebObjects.class);
						//browser.putExtra(ImportBrowseWebObjects.PARAM_BROWSER_URL, data);
						browser.setData(data);
						startActivity(browser);
					}
				} else {
					Log.d("Main", "Main: onCreate: no data");
				}
            }
        }

		Log.d("Main", "Main: onCreate: start service");
		Intent serviceIntent = new Intent(this, DDP2P_Service.class);
		startService(serviceIntent);

		Log.d("Main", "Main: onCreate: setContentView");
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

		Log.d("Main", "Main: onCreate: set Page Viewer");

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
		Log.d("Main", "Main: onCreate: will init actionBar");

		//add tabs
		actionBar.addTab(actionBar.newTab().setText(Util.__(getString(R.string.users)))
				.setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(Util.__(getString(R.string.organizations)))
				.setTabListener(this));
        if (PAGES_NB > 2) {
            actionBar.addTab(actionBar.newTab().setText("Acts")
                    .setTabListener(this));
        }

		Log.d("Main", "Main: onCreate: will preload icon_org");
		new net.ddp2p.common.util.DDP2P_ServiceThread("loading icons", false, this) {
			@Override
			public void _run() {
				Main m = (Main) ctx;
				Bitmap bmp = PhotoUtil.decodeSampledBitmapFromResource(getResources(), R.drawable.organization_default_img, 55, 55);
				icon_org = PhotoUtil.BitmapToByteArray(bmp, 100);
				Log.d("Main", "MainTh: onCreate: preloaded icon_org");
			}
		}.start();


		new CheckStartUp().execute();
		Log.d("Main", "Main: onCreate: stop");
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

		Log.d("onCreateOptionsMenu", "Main: onCreateOptionsMenu: almost done");
		boolean result = super.onCreateOptionsMenu(menu);
		Log.d("onCreateOptionsMenu", "Main: onCreateOptionsMenu: done");
		return result;
	}

	/** Defines callbacks for service binding, passed to bindService(). Currently not used */
	DDP2P_Service mService;
	boolean mBound = false;
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className,
									   IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			DDP2P_Service.LocalBinder binder = (DDP2P_Service.LocalBinder) service;
			mService = binder.getService();
			mBound = true;
			unbindService(this);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};


	//the plus mark, add a new safe
	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.action_quit) {
			Log.d("Main", "Main: onCreate: start service");
			Intent serviceIntent = new Intent(this, DDP2P_Service.class);
			stopService(serviceIntent);
			//bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE); // may bind to send commands
			finish();
		}

		if (item.getItemId() == R.id.import_new_web_object) {
			Log.d("Main", "Main: onOptionsItemSelected: null type intent: start ImportBrowseWebObjects");
			Intent j = new Intent().setClass(this, ImportBrowseWebObjects.class);
			this.startActivityForResult(j, Main.RESULT_ADD_PEER);
		}

		if (item.getItemId() == R.id.add_new_org) {
			Toast.makeText(this, "Adding a new organization.", Toast.LENGTH_SHORT).show();

			Intent intent = new Intent();
			intent.setClass(this, AddOrg.class);
			this.startActivityForResult(intent, Main.RESULT_ADD_ORG);
		}

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
			Toast.makeText(this, "Adding a new safe myself", Toast.LENGTH_SHORT).show();
			
			Intent intent = new Intent();
			intent.setClass(this, AddSafe.class);
			this.startActivityForResult(intent, Main.RESULT_ADD_PEER);
		}

		if (item.getItemId() == R.id.add_new_safe_other) {

//			if ((Identity.getListing_directories_addr().size() <= 0)) {
//				Toast.makeText(this, "To add a safe, first add a directory from the Manage Directories top-right menu!", Toast.LENGTH_LONG).show();
//				return super.onOptionsItemSelected(item);
//			}
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

		if (item.getItemId() == R.id.action_directories) {
			//Toast.makeText(this, "Managing directories", Toast.LENGTH_SHORT).show();
			
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

		if (item.getItemId() == R.id.action_main_refresh) {
			//threadMsg("b");
			new ReloadSafe().execute();
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
			startActivityForResult(intent, RESULT_SETTINGS);
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
		Log.d("Main", "Main: onActivityResult: start r=" + resultCode+"vs(ok="+RESULT_OK+")" + " q=" + requestCode+"(ok=" + (requestCode&0x0FFFF) + ")");
		if (resultCode != RESULT_OK) {
			Log.d("Main", "oAR: result nok");
			new ReloadSafe().execute();
			super.onActivityResult(requestCode, resultCode, resultData);
			Log.d("Main", "oAR: result nok stop");
			return;
		}
		switch (requestCode & 0x0FFFF) // string the fragment information which is in upper bits
		{
			case RESULT_STARTUP_DIRS:
				super.onActivityResult(requestCode, resultCode, resultData);
				return;
			case SELECT_PHOTO:
			case SELECT_PHOTO_KITKAT:
				if (resultData != null) {
					Uri uri = null;

					if (requestCode == SELECT_PHOTO) {
						uri = resultData.getData();
						Log.i("Main", "oAR: Uri: " + uri.toString());
					} else if (requestCode == SELECT_PHOTO_KITKAT) {
						uri = resultData.getData();
						Log.i("Main", "oAR: Uri kitkat: " + uri.toString());
						final int takeFlags = resultData.getFlags()
								& (Intent.FLAG_GRANT_READ_URI_PERMISSION
								| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
						// Check for the freshest data.
						getContentResolver().takePersistableUriPermission(uri, takeFlags);
					}

					if (uri != null) {
						selectedImagePath = FileUtils.getPath(this, uri);
						Log.i("Main", "oAR: path: " + selectedImagePath);

						selectImageFile = new File(selectedImagePath);
						String mimeType= URLConnection.guessContentTypeFromName(selectImageFile.getName());
						String error;
						StegoStructure adr[] = DD.getAvailableStegoStructureInstances();
						int[] selected = new int[1];
						try {
							//error = DD.loadBMP(selectImageFile, adr, selected);
							error = DD.loadFromMedia(selectImageFile, mimeType, adr, selected);
							Log.i("Main", "oAR: error: " + error);
							if (error == "") {
								adr[selected[0]].save();
								Toast.makeText(this, "add new safe other successfully!", Toast.LENGTH_SHORT).show();
							}
						} catch (Exception e) {
							Toast.makeText(this, "Unable to load safe from this photo!", Toast.LENGTH_SHORT).show();
							e.printStackTrace();
						}
					}
				}
				super.onActivityResult(requestCode, resultCode, resultData);
				Log.d("Main", "oAR: stop");
			case RESULT_SETTINGS:
			{
				Log.d("Main", "Main: oCW: resultSettings hidden ="+Safe.SHOW_HIDDEN);
				new ReloadSafe().execute();
				super.onActivityResult(requestCode, resultCode, resultData);
				return;
			}
			case RESULT_ADD_ORG:
			{
				byte[] _new_org = resultData.getByteArrayExtra(Main.RESULT_ORG);
				D_Organization new_org = null;
				if (_new_org != null) try {
					new_org = D_Organization.getEmpty().decode(new Decoder(_new_org));
					new OrgCreatingThread(new_org).start();
				} catch (Exception e) {
					e.printStackTrace();
				}

				super.onActivityResult(requestCode, resultCode, resultData);
				return;
			}
			case RESULT_ADD_PEER:

			default:
			{
				byte[] _pi = resultData.getByteArrayExtra(AddSafe.PI);
				net.ddp2p.common.hds.PeerInput pi = null;
				if (_pi != null) try {
					pi = new PeerInput().decode(new Decoder(_pi));
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (pi == null) pi = Safe.peerInput;
				new PeerCreatingThread(pi).start();

				super.onActivityResult(requestCode, resultCode, resultData);
				return;
			}
		}
	}

	/**
	 * Class to asynchronously create org, and to restart servers when needed
	 */
	class OrgCreatingThread extends Thread {
		D_Organization new_org;

		OrgCreatingThread(D_Organization _org) {
			new_org = _org;
		}

		public void run() {
			D_Organization o = D_Organization.storeRemote(new_org, null);

			if (! o.verifySignature()) {
				Log.d("OrgCreatingThread", "AddOrg: OrgThread: bad signature after store hash=" + o);
				Toast.makeText(null, "Bad Signature on Creation", Toast.LENGTH_SHORT).show();
				return;
			}
			o = D_Organization.getOrgByOrg_Keep(o);
			o.storeRequest_getID();
			o.releaseReference();
			Log.d("OrgCreatingThread", "AddOrg: OrgThread: got org=" + o);
			Orgs.reloadOrgs();

			//Message msgObj = handler.obtainMessage();
			//handler.sendMessage(msgObj);
			handler.sendMessage(handler.obtainMessage(Main.REFRESH_ORG));

		}
	}

	/**
	 * Class to asynchronously create myself peer, and to restart servers when needed
	 */
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
			generateMyself();

			Log.d("onCreatePeerCreatingTh",
					"PeerCreatingThread: run: announced");
			Log.d(TAG, "add safe: run()");

            Safe.loadPeer();

            Log.d("onCreatePeerCreatingTh",
                    "PeerCreatingThread: run: generated");
            threadMsg("a");

        }

        public D_Peer generateMyself() {
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
            //myself.storeRequest();
			myself.storeRequest_getID(); // should sync to guarantee visibility
			myself.releaseReference();

            Log.d("onCreatePeerCreatingTh",
                    "PeerCreatingThread: gen: inited");
            // data.HandlingMyself_Peer.setMyself(myself, true, false);
            if (!DDP2P_Service.serversStarted)
                DDP2P_Service.startServers();
            return peer;
        }

    }
	public void threadMsg(String msg) {
		if (! msg.equals(null) && ! msg.equals("")) {
			Log.d("Main", "add safe: threadMsg");
/*
			Message msgObj = handler.obtainMessage();
			Bundle b = new Bundle();
			b.putString("message", msg);
			msgObj.setData(b);
			handler.sendMessage(msgObj);
			*/

			Log.d("Main", "add safe: threadMsg finished");

			handler.sendMessage(handler.obtainMessage(Main.REFRESH_ALL));
		} else {
			Log.d("Main", "Main: threadMsg: msg empty");
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
		Log.d("Main", "Main: refreshSafe start");
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
		Log.d("Main", "Main: refreshSafe setAdapter: #"+Safe.list.size());

		s.setListAdapter(newAdapter);
		newAdapter.notifyDataSetChanged();
		Safe.safeAdapter = newAdapter;
		Log.d("Main", "Main: refreshSafe setAdapter: quit");
	}
	final static int REFRESH_ALL = 10;
	final static int REFRESH_ORG = 11;

	/**
	 * Handler to call RefreshOrg / RefreshAll / RefreshPeer
	 */
    private final Handler handler = new Handler() {
        final static String TAG = "Main_Handler";

        // Create handleMessage function

        public void handleMessage(Message msg) {
			if (msg.what == Main.REFRESH_ORG) {
				refreshOrg();
				Log.d(TAG, "add org: handler");
				Toast.makeText(Main.this, "Added a new organization successfully!", Toast.LENGTH_LONG).show();
				return;
			}

			if (msg.what == Main.REFRESH_ALL) {
				refreshOrg();
			}
            //String aResponse = msg.getData().getString("message");
            Log.d(TAG, "add safe: handler");
            //Toast.makeText(Main.this, "Added a new safe successfully!", Toast.LENGTH_LONG).show();
		 	refreshSafe();

            /*
            Intent i = getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage( getBaseContext().getPackageName() );
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            */

			if (_wizard_started) {
				_wizard_started = false;
				startup_wizzard_passed = true;

				Intent intent = new Intent().setClass(getApplicationContext(), ImportBrowseWebObjects.class);
				intent.putExtra(ImportBrowseWebObjects.PARAM_INSTRUCTION,
						getText(R.string.import_web_object_instr));
				startActivityForResult(intent, Main.RESULT_STARTUP_DIRS);
			}
        }
    };

	/**
	 * Class used to asynchronously refresh peers and orgs list.
	 * E.g after additions, etc.
	 * Should probably be called in each onResume
	 */
	public class ReloadSafe extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... voids) {
			Thread th = Thread.currentThread();
			Log.d("MainCS", "Main: ReloadSafe:doInBack: start "+th.getName());
			th.setName("Main:ReloadSafe");
			Safe.loadPeer();
			Orgs.reloadOrgs();

			Log.d("ReloadSafe",
					"PeerCreatingThread: run: generated");
			return null;
		}

		@Override
		protected void onPostExecute(Void _void) {
			refreshSafe();
			refreshOrg();
		}
	}

	/**
	 * Class used to asynchronously start the DDP2P engine service
	 */
	public class CheckStartUp extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			Thread th = Thread.currentThread();
			Log.d("MainCS", "Main: CheckStartup:doInBack: start "+th.getName());
			th.setName("Main:CheckStartUp");

			ArrayList<ArrayList<Object>> peers = DDP2P_Service.startDDP2P(getApplicationContext());
			try {
				DD.load_listing_directories();
			} catch (Exception e) {
				e.printStackTrace();
				Log.d("MainCS", "Main: CheckStartup:doInBack: fail to get dirs");
				return Boolean.TRUE;
			}
			String str = DD.getAppTextNoException("DD_WIZARD_SKIP");
			if (str != null && !"".equals(str.trim())) {
				Log.d("MainCS", "Main: CheckStartup:doInBack: str: "+str);
				return Boolean.TRUE;
			}

			if ((Identity.getListing_directories_addr().size() > 0)) {
				Log.d("MainCS", "Main: CheckStartup:doInBack: dir");
				return Boolean.TRUE;
			}
			if (peers.size() > 0) {
				Log.d("MainCS", "Main: CheckStartup:doInBack:  peers");
				return Boolean.TRUE;
			}
			Log.d("MainCS", "Main: CheckStartup:doInBack:wizzard");
			return Boolean.FALSE;
		}

		@Override
		protected void onPostExecute(Boolean o) {
			if (!o) {
				askStartUp();
			} else {
				Main.startup_wizzard_passed = true;
			}
		}
	}


}

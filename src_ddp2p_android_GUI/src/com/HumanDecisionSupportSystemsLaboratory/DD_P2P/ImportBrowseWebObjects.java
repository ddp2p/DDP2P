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
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.StegoStructure;
import net.ddp2p.common.util.Util;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by msilaghi on 7/18/15.
 */
public class ImportBrowseWebObjects extends Activity {
    public static String PARAM_BROWSER_URL = "PARAM_BROWSER_URL";
    public static String PARAM_INSTRUCTION = "PARAM_INSTRUCTION";
    //public static String DD_DOC_ROOT = "http://ddp2p.net/objects";
    TextView ibwo_url_crt;
    TextView ibwo_description, ibwo_instruction;
    WebView ibwo_webview;
    MyWebViewClient ibwo_webview_client;
    ArrayList<String> history = new ArrayList<String>();
    final public static String _ROOT_OBJECTS_URL = "ROOT_OBJECTS_URL";
    private CharSequence string_description_default;

    protected String get_DD_DOC_ROOT() {
        Log.d("ImportBrowseWebObj", "ImportBrowseWebObjects: get_DD_DOC_ROOT: start");
        return "http://ddp2p.net/objects";
    }
    protected String get_ROOT_OBJECTS_URL() {
        return _ROOT_OBJECTS_URL;
    }

    private static String getMimeType(String url) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ImportBrowseWebObjects", "IBWO: onCreate: start "+this.getClass());

        Intent intent = getIntent();
        // Uri uri = intent.getParcelableExtra(PARAM_BROWSER_URL);
        Uri uri = intent.getData();
        String instr = intent.getStringExtra(PARAM_INSTRUCTION);


        this.setContentView(R.layout.import_browse_web_object);

        Button ibwo_decode_top = (Button) findViewById(R.id.import_web_object_decode_button);
        Button ibwo_decode_bottom = (Button) findViewById(R.id.import_web_object_decode_bottom_button);
        Button ibwo_back = (Button) findViewById(R.id.import_web_object_back);
        Button ibwo_root = (Button) findViewById(R.id.import_web_object_root_button);

        Button ibwo_import_top = (Button) findViewById(R.id.import_web_object_import_top_button);
        Button ibwo_import_bottom = (Button) findViewById(R.id.import_web_object_import_bottom_button);


        ibwo_webview = (WebView) findViewById(R.id.import_web_object_webview);
        final EditText ibwo_url = (EditText) findViewById(R.id.import_web_object_url);
        ibwo_url_crt = (TextView) findViewById(R.id.import_web_object_url_crt);
        ibwo_description = (TextView) findViewById(R.id.import_web_object_description);
        ibwo_instruction = (TextView) findViewById(R.id.import_web_object_import_instructions);

        ibwo_webview.setWebViewClient(ibwo_webview_client = new MyWebViewClient());
        ibwo_webview.setWebChromeClient(new WebChromeClient());
        Log.d("ImportBrowseWebObjects", "IBWO: onCreate: class =" + this.getClass());

        if (instr != null) {
            ibwo_instruction.setText(instr);
            ibwo_instruction.setVisibility(View.VISIBLE);
            // ibwo_description.setVisibility(View.GONE);
            string_description_default = "";
            ibwo_description.setText(string_description_default);
        } else
            string_description_default = getText(R.string.help_import_web_object);

        String root_url = get_DD_DOC_ROOT();
        Log.d("ImportBrowseWebObjects", "IBWO: onCreate: root_url =" + root_url);
        ibwo_url.setText(root_url);

        String root_obj_url = get_ROOT_OBJECTS_URL();
        Log.d("ImportBrowseWebObjects", "IBWO: onCreate: DD_root_objects_url =" + root_obj_url);
        String root = DD.getAppTextNoException(root_obj_url);
        Log.d("ImportBrowseWebObjects", "IBWO: onCreate: DB url =" + root);
        if (root != null && !"".endsWith(root.trim())) ibwo_url.setText(root);
        String url = ibwo_url.getText().toString();
        Log.d("ImportBrowseWebObjects", "IBWO: onCreate: initial url=" + url);
        Log.d("ImportBrowseWebObjects", "IBWO: onCreate: initial uri=" + uri);
        if (uri == null) {
            Log.d("ImportBrowseWebObjects", "IBWO: onCreate: load uri=" + url);
            ibwo_webview.loadUrl(url);
        }
        else {
            Log.d("ImportBrowseWebObjects", "IBWO: onCreate: load uri=" + uri);
            ibwo_webview.loadUrl(uri.toString());
        }

        ibwo_root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String root;
                String crt_url = ibwo_url.getText().toString();
                Log.d("ImportBrowseWebObjects", "IBWO: onCreate: root: url =" + crt_url+" hist_sz="+history.size());

                if (history.size() == 0 || (history.get(0)).equals(crt_url)) {
                    ibwo_url.setText(root = get_DD_DOC_ROOT());
                } else {
                    Log.d("ImportBrowseWebObjects", "IBWO: onCreate: root: url =" + crt_url+" vs hist="+history.get(0));
                    ibwo_url.setText(root = history.get(0));
                }
                DD.setAppTextNoException(get_ROOT_OBJECTS_URL(), root);
            }
        });

        ibwo_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ibwo_description.setText(string_description_default);
                if (history.size() < 2) return;
                history.remove(0);
                if (history.size() == 0) return;
                String url = history.get(0);
                ibwo_webview.loadUrl(url);
            }
        });

        View.OnClickListener onDecode = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url;
                if (history.size() > 0) url = history.get(0);
                else url = ibwo_webview.getUrl();
                if (url == null) {
                    Log.d("IBWO", "IBWO: onDecode: exit null");
                    return;
                }
                Log.d("IBWO", "IBWO: onPostExecute: worker=" + url);
                new RetrieveURLDescription().execute(url);
            }
        };
        ibwo_decode_top.setOnClickListener(onDecode);
        ibwo_decode_bottom.setOnClickListener(onDecode);

        View.OnClickListener onImport = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url;
                if (history.size() > 0) url = history.get(0);
                else url = ibwo_webview.getUrl();
                if (url == null) {
                    Log.d("IBWO", "IBWO: onDecode: exit null");
                    return;
                }
                Log.d("IBWO", "IBWO: onPostExecute: worker=" + url);
                new RetrieveURLDescription().execute(url, url);
            }
        };
        ibwo_import_top.setOnClickListener(onImport);
        ibwo_import_bottom.setOnClickListener(onImport);

        ibwo_url.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                String url = textView.getText().toString();
                Log.d("ImportBrowseWebObjects", "IBWO: onCreate: url: onEditorAction url=" + url);
                ibwo_webview.loadUrl(url);
                DD.setAppTextNoException(get_ROOT_OBJECTS_URL(), url);
                return false;
            }
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        Log.d("ImportBrowseWebObjects", "IBWO: onCreate: done");
    }

    @Nullable
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    class URLBuffer {
        public byte[] client_buffer = new byte[DD.MAX_CONTAINER_SIZE];
        public int client_len;
        public String client_url;
    }
    public class MyWebViewClient extends WebViewClient {
        ArrayList<URLBuffer> client_history = new ArrayList<URLBuffer>();
        HashMap<String,URLBuffer> _client_history = new HashMap<String, URLBuffer>();
        final static int HIST_SIZE = 8;
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            // CharSequence text = ImportBrowseWebObjects.this.ibwo_url_crt.getText();
            if (history.size() > 0) {
                if (url.compareTo(history.get(0)) != 0) {
                    history.add(0, url);
                    ibwo_description.setText(string_description_default);
                }
            } else
                history.add(0, url);
            ImportBrowseWebObjects.this.ibwo_url_crt.setText(url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            Log.d("IBWO", "IBWO: :intercept start: " + url);
            if (url == null || url.trim().equals(""))
                return super.shouldInterceptRequest(view, url);
            WebResourceResponse response = old_shouldInterceptRequest(view, url);
            if (response == null) return response;
            Log.d("IBWO", "IBWO: :intercept response: " + response.getMimeType());
            if ("image/x-ms-bmp".equals(response.getMimeType()))
                response.setMimeType("image/bmp");
            if ("image/bmp".equals(response.getMimeType())) {
                InputStream is = response.getData();
            }
            try {
                URLBuffer ub;
                _client_history.clear();
                if (client_history.size() >= HIST_SIZE) ub = client_history.remove(client_history.size()-1);
                else {ub = new URLBuffer(); client_history.add(0, ub);}

                ub.client_buffer = Util.readAllBytes(response.getData(), ub.client_buffer);
                ub.client_len = ub.client_buffer.length; //Util.readAll(response.getData(), ub.client_buffer);
                ub.client_url = url;

                response.setData(new ByteArrayInputStream(ub.client_buffer, 0, ub.client_len));

                for (URLBuffer ch: client_history) _client_history.put(ch.client_url, ch);
            } catch(Exception e) {e.printStackTrace();}
            return response;
        }
        public WebResourceResponse old_shouldInterceptRequest(WebView view, String url) {

            final DefaultHttpClient httpClient = new DefaultHttpClient();
            httpClient.getConnectionManager().closeExpiredConnections();
            final HttpUriRequest httpRequest = new HttpGet(url);

            try {
                final HttpResponse response = httpClient.execute(httpRequest);
                final Header[] headers = response.getHeaders("Content-Type");
                String mimeType = "";
                String encoding = "";
                if (headers != null && headers.length > 0) {
                    final String type = headers[0].getValue();
                    final int semicolonIndex = type.indexOf(';');
                    if (semicolonIndex != -1) {
                        mimeType = type.substring(0, semicolonIndex).trim();
                        encoding = type.substring(semicolonIndex + 1).trim();
                        final int equalsIndex = encoding.indexOf('=');
                        if (equalsIndex != -1)
                            encoding = encoding.substring(equalsIndex + 1).trim();
                    } else
                        mimeType = type;
                }

                return new WebResourceResponse(mimeType, encoding, response.getEntity().getContent());
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } finally {
                httpClient.getConnectionManager().closeExpiredConnections();
            }
            return null;
        }
    }

    class RetrieveURLDescription extends AsyncTask<String, Void, String> {

        private Exception exception;
        String thread_urls[];
        StegoStructure thread_ss;
        String thread_description;

        protected String doInBackground(String... urls) {
            Thread th = Thread.currentThread();
            Log.d("IBWO", "ImportBrowseObjects: RetrieveURLDescription:doInBack: start "+th.getName());
            th.setName("ImportBrowseObjects:RetrieveURLDescription");
            try {
                thread_urls = urls;
                if (urls.length == 2
                        && browser__ss != null
                        && browser__url != null
                        && urls[0].equals(browser__url)) {
                    Log.d("IBWO", "IBWO: doInBackground: same url, save");
                    browser__ss.saveSync();
                    Log.d("IBWO", "IBWO: doInBackground: same url, saved");
                    thread_description = browser__description;
                    thread_ss = browser__ss;
                    thread_urls = urls;
                    return getString(R.string.announcement_import_successful)+ " \n"+browser__description;
                }
                if (urls.length < 1) return "No url!";
                String url = urls[0];
                URL _url = new URL(url);
                Log.d("IBWO", "IBWO: doInBackground: url=" + url);
                String mime = getMimeType(url);
                Log.d("IBWO", "IBWO: doInBackground: guessed mime=" + mime);
                if (mime == null) mime = "text/html";
                Log.d("IBWO", "IBWO: doInBackground: mime=" + mime);
                int selected[] = new int[]{-1};
                StegoStructure[] adr = DD.getAvailableStegoStructureInstances();
                //String err = DD.loadFromMedia(_url.openStream(), mime, adr, selected);
                String err;
                URLBuffer ub = ibwo_webview_client._client_history.get(url);
                if (ub != null) {
                    Log.d("IBWO", "IBWO: foInBackground: reuse: "+ub.client_buffer[0]+":"+ub.client_buffer[1]);
                    err = DD.loadFromMedia(ub.client_buffer, ub.client_len, mime, adr, selected);
                } else {
                    Log.d("IBWO", "IBWO: foInBackground: no reuse: url="+ibwo_webview_client._client_history.size());
                    err = DD.loadFromMedia(_url.openStream(), mime, adr, selected);
                }
                Log.d("IBWO", "IBWO: doInBackground: stego idx=" + selected[0]);
                if (selected[0] < 0) thread_description = "Error: " + err + "\n" + "Decoding: " + url;
                    //ibwo_description.setText("Error: " + err + "\n" + "Decoding: " + url);
                else {
                    thread_ss = adr[selected[0]];
                    if (thread_ss != null) {
                        thread_description = thread_ss.getNiceDescription();
                        if (urls.length == 2) {
                            Log.d("IBWO", "IBWO: doInBackground: same url, save");
                            thread_ss.saveSync();
                            Log.d("IBWO", "IBWO: doInBackground: same url, saved");

                            Log.d("IBWO", "IBWO: doInBackground: importing exit=" + thread_description);
                            return getString(R.string.announcement_import_successful)+ " \n"+ thread_description;
                        }
                    } else {
                        Log.d("IBWO", "IBWO: doInBackground: null stego");
                        thread_description = "No Data Type Matches";
                    }
                }
                    //ibwo_description.setText(adr[selected[0]].getNiceDescription());
                Log.d("IBWO", "IBWO: doInBackground: url=" + thread_description);
            } catch (Exception e) {
                e.printStackTrace();
                thread_description = e.getLocalizedMessage();
            }
            Log.d("IBWO", "IBWO: doInBackground: exit=" + thread_description);
            return thread_description;
        }
        protected void onPostExecute(String result) {
            Log.d("IBWO", "IBWO: onPostExecute: exit=" + thread_description);
            ibwo_description.setText(result);
            browser__ss = thread_ss;
            browser__url = thread_urls[0];
            browser__description = thread_description;
            Intent intent = new Intent();
            ImportBrowseWebObjects.this.setResult(RESULT_OK, intent);
        }
    }
    StegoStructure browser__ss = null;
    String browser__url = null;
    String browser__description = null;
}

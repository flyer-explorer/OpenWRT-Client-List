package org.openwrt.clients;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.text.format.Time;


public class HostInfo extends Activity {
	final String TAG = "openwrt-clients";
	TextView tv_host, tv_ip, tv_mac, tv_unix, tv_time;
	String[] d;
		
    @Override /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
    	/* Create window and populate items  */
        super.onCreate(savedInstanceState); //init super
        setContentView(R.layout.ip_dialog); //use ip_dialog.xml as GUI
        //grab passed parameters from passing intent
        d = getIntent().getExtras().getStringArray("opts");
        
        //create items from ip_dialog.xml
        final TextView tv_host = (TextView) findViewById(R.id.diag_host);
        final TextView tv_ip   = (TextView) findViewById(R.id.diag_ip);
    	final TextView tv_mac  = (TextView) findViewById(R.id.diag_mac);
    	final TextView tv_unix  = (TextView) findViewById(R.id.diag_unix);
    	final TextView tv_time  = (TextView) findViewById(R.id.diag_time);
    	tv_host.setText(d[0]); //set text for items passed via intent
    	tv_ip.setText(d[1]);
    	tv_mac.setText(d[2]);
    	tv_unix.setText(d[3]);
    	Time t = new Time();
    	t.set(Long.parseLong(d[3]) * 1000);
    	tv_time.setText(t.format("%Y-%m-%d %H:%M:%S"));
    	
    	//add listener for back button
    	 Button bt_back = (Button) findViewById(R.id.back_btn);
         bt_back.setOnClickListener(new View.OnClickListener()  {
        	 public void onClick(View arg0) {
                 setResult(RESULT_OK);
                 finish(); 
                 }
         });
         //clicking SSH should open connect bot
         Button bt_ssh = (Button) findViewById(R.id.bt_ssh);
         bt_ssh.setOnClickListener(new View.OnClickListener()  {
        	 public void onClick(View arg0) {
     		     //uri : ssh://user@host:port/#nickname
        		 SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        		 String username = prefs.getString("username", null);
        		 if (username == "")
        			 username = "root";
        		 int port;
        		 try {
        			 port = prefs.getInt("ssh_port", 23);
        		 } catch (Exception e) {
        			 port = 22;
        		 }
        		 String url = "ssh://" + username + "@" + d[0] + ":" + port + "/#" + d[0];
        		 Log.v(TAG, "ssh url: " + url);
        		 startActivity( new Intent(Intent.ACTION_VIEW, Uri.parse(url)) );
                 setResult(RESULT_OK);
                 finish(); 
                 }
         });
         Button bt_telnet = (Button) findViewById(R.id.bt_telnet);
         bt_telnet.setOnClickListener(new View.OnClickListener()  {
        	 public void onClick(View arg0) {
     		     //uri : telnet://host:port/#nickname
        		 SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        		 int port;
        		 try {
        			 port = prefs.getInt("telnet_port", 23);
        		 } catch (Exception e) {
        			 port = 23;
        		 }
        		 String url = "telnet://" +  d[0] + ":" + port + "/#" + d[0];
        		 Log.v(TAG, "Telnet url: " + url);
        		 startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                 setResult(RESULT_OK);
                 finish(); 
                 }
         });
         //Call as a webpage
         Button bt_http = (Button) findViewById(R.id.bt_http);
         bt_http.setOnClickListener(new View.OnClickListener()  {
        	 public void onClick(View arg0) {
        		 String url = "http://" + d[0];
        		 Intent iopenURL = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        		 startActivity(iopenURL);
                 setResult(RESULT_OK);
                 finish();                 
                 }
         });
         //Call as a webpage w/ ssl
         Button bt_https = (Button) findViewById(R.id.bt_https);
         bt_https.setOnClickListener(new View.OnClickListener()  {
        	 public void onClick(View arg0) {
        		 String url = "https://" + d[0];
        		 Intent iopenURL = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        		 startActivity(iopenURL);
                 setResult(RESULT_OK);
                 finish(); 
                 }
         });
    }
}
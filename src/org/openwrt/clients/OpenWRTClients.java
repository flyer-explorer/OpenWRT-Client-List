package org.openwrt.clients;


import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;


import android.util.Log;
import android.view.View;
import android.os.Bundle;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;
import android.widget.ListView;
import android.content.Context;
import android.net.NetworkInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.net.ConnectivityManager;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class OpenWRTClients extends Activity {
	private static final String TAG = "openwrt-clients";
    
	static Context context; //for toasting
	static ListView listView;
	ArrayList<Map<String, String>> lst;
		
    @Override /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState); //init super
    	Log.v(TAG, "Starting Program");
    	//entry point of app
        setContentView(R.layout.main); //set content from main.xml
        listView = (ListView) findViewById(R.id.attached_clients); //get listview
        lst = new ArrayList<Map<String, String>>(); //init place holder for net returns
        context = getApplicationContext(); //initialize context for other elements of program
        listView.setClickable(true); //tell android we want the list to be clickable
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() { //add listener
        	public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
        		String o = ((Object) listView.getItemAtPosition(position)).toString(); //get title of listing
        		Intent intent = new Intent(view.getContext(), HostInfo.class); //create intent
        		for (int i=0; i<lst.size(); i++) {
        			if (lst.get(i).get("hostname") == o) {//look for match in lst
        				String put[] = {lst.get(i).get("hostname"), lst.get(i).get("ip"), lst.get(i).get("mac"), lst.get(i).get("timestamp")};
        				intent.putExtra("opts", put); //pass opts intent
        				startActivity(intent); //call intent
        			}
        		} 
        	}
        	});
        Query(null); //start first query
    }
	public static String probeNetwork() {
		/* First check if network is active */
		//actually pokes the network to find what is out there
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (!mWifi.isConnected()) { //make sure wifi is connected in the first place
			Toast.makeText(context, "Turn on Wifi First", Toast.LENGTH_SHORT).show();
			return "";
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String t = prefs.getString("con_type", null);
		int port = Integer.parseInt(prefs.getString("port", "2000"));
		String host = prefs.getString("ip", null);
		String cmd = prefs.getString("cmd", "");
		if (cmd.equals("")) {
			Log.v(TAG, "Command blank, setting to null");
			cmd = null;
		}
		if ( (t == null) | (port < 1024)) {
			Log.v(TAG, "con_type is null, user needs to check configuration");
			Toast.makeText(context, "Could not tell what network to ping check configuration", Toast.LENGTH_LONG).show();
			return "";
		}
		if (t.equals("broadcast")) {
			//UDP message.  Treat as true broadcast.  Anyone yelling on a port will get grabbed
			Log.v(TAG, "Picking up Broadcast Message");
			return probeUDP(null, port, cmd);
		}
		if (t.equals("unicast")) {
			//UDP message.  Treat as single cast, eg, listen only from a single host
			Log.v(TAG, "Picking up Unicast Message");
			return probeUDP(host, port, cmd);
		} 
		if (t.equals("tcp")) {
			Log.v(TAG, "Using TCP to get Message");
			return probeTCP(host, port, cmd);
		}
		if (t.equals("debug")) {
			Log.v(TAG, "Debug Data");
			return "1234567890 FF:FF:FF:FF:FF:FF 1.2.3.4 dbg *\n1234567899 FF:FF:FF:FF:FF:FE 1.2.3.4 dbg2 *\n";
		}
		Toast.makeText(context, "You need to configure this program for it to be of any use!\nSelect UDP Broadcast, UDP Unicast, or TCP as well as provide host information", Toast.LENGTH_LONG).show();
		Log.e(TAG, "I should not be getting here.  Something is wrong");
		return "";
	}
	public static String probeTCP(String host, int port, String cmd) {
		Socket sock = null;
		BufferedReader input = null;
		String rtn = "";
	    try {
            sock = new Socket(host, port);
            if (cmd != null) {
            	//Send cmd to host
            	try {
            		PrintWriter output = new PrintWriter(sock.getOutputStream());
            		output.println(cmd);
            		output.close();
            	} catch (Exception e){
            		Toast.makeText(context, "Could not send command '" + cmd + "' to host.  Proceeding anyway", Toast.LENGTH_SHORT).show();
            	}
            }
            input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            while(true) { //read line(s)
            	String t = input.readLine();
            	if (t == null) //done reading
            		break;
            	rtn += t + "\n";
            }
            input.close();
            sock.close();
        } catch (UnknownHostException e) {
        	Log.v(TAG, "Could not find host:" + host);
        } catch (IOException e) {
        	Toast.makeText(context, "Could not connect to remote host '" + host + "' on port '" + port + "'" , Toast.LENGTH_SHORT).show();
        	Log.v(TAG, "Exception: " + e.toString());
        } finally {
        	if (sock != null) {
        		try {
					sock.close(); //Close connection
				} catch (Exception e)  {}
        	}
        }
		return rtn;
	}
	public static String probeUDP(String host, int port, String cmd) {
		//Look for /tmp/dhcp.leases being broadcast or singlecast.
		// if host==null, we will assume broadcast, and skip try
		byte[] buf = new byte[1024];
    	DatagramPacket pkt = new DatagramPacket(buf, buf.length);
    	pkt.setPort(port);
    	InetAddress addr = null;
    	try {
    		addr = InetAddress.getAllByName(host)[0];    		
    	} catch (Exception e) {
    		Log.v(TAG, "Could not get remote hostname");
    	}
    	DatagramSocket sock = null;
    	try { //try to get data
    		sock = new DatagramSocket(port);
    		if (host != null) { //listen only from specified host
    			Log.v(TAG, "Going Singlecast Route: ");
    			pkt.setAddress(addr);
    		} else { //listen for broadcast messages
    			Log.v(TAG, "Going Broadcast Route");
    			sock.setBroadcast(true); //MUST do this first
    		}
    		sock.setReuseAddress(true);
    		sock.setSoTimeout(1500);
    		if (cmd != null) {
    			//send a command before listen for a packet
    			DatagramPacket send_pkt = new DatagramPacket(cmd.getBytes(), cmd.length());
    			send_pkt.setPort(port);
    			send_pkt.setAddress(addr);
    			sock.send(send_pkt);
    			sock.receive(pkt);
    		}
    		sock.receive(pkt);
    		sock.close();
    		return new String(pkt.getData(), 0, pkt.getLength());    		
    	} catch (UnknownHostException e) {
    		Log.v(TAG, "Could not resolve host:" + host + e.toString());
    		Toast.makeText(context, "I could not resolve host '" + host + "'  Check your configuration", Toast.LENGTH_SHORT).show();
    		return "";
    	} catch (Exception e) {
    		Log.v(TAG, "Socket Error:" + e.toString());
    		Toast.makeText(context, "I did not seem to find any connected hosts", Toast.LENGTH_SHORT).show();
    		return "";
    	} finally {
    		if (sock != null) {
    			Log.v(TAG, "Closing Socket");
    			sock.close(); //close socket so we are not waiting for it to close by timeout or fc
    		}
    	}
	}
    public void Query(View view) {
    	//this is called by the button and when started.  Poke network, and refresh view
    	try {
    		lst = decodeData(); //get data from network
    	} catch (Exception e) {
    		Log.e(TAG, "Uncaught Exception: " + e.toString());
    		return;
    	}
		String[] values = new String[lst.size()];
		for(int i=0; i<lst.size(); i++) {
			values[i] = lst.get(i).get("hostname"); //add hostnames to list
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, values);
		listView.setAdapter(adapter); // Assign adapter to ListView
    }
	private static ArrayList<Map<String, String>> decodeData() {
		//Returns an array of maps that contains the timestamp, mac, IP, and
		//this host names of the clients info that is broadcast over the network
		ArrayList<Map<String, String>> rtn = new ArrayList<Map<String, String>>();
		String p = probeNetwork();
		if ( (p == null) | (p.length() == 0))
			return rtn;
		String[] lines = p.split("\n");
    	if (lines.length == 0)
    		return rtn;
    	for(int i=0; i<lines.length; i++) {
    		//line contains the dhcp leases.  Format:
        	//time_t<space>mac<space>ip<space>hostname<space>I do not know / ignore
    		HashMap<String, String> item = new HashMap<String, String>();
    		String[] t = lines[i].split(" ");
    		if (t.length != 5)
    			continue;
    		item.put("timestamp", t[0]);
    		item.put("mac", t[1]);
    		item.put("ip", t[2]);
    		item.put("hostname", t[3]);
    		rtn.add(item);
    	}
    	return rtn;
	}
	public void ShowConfig(View view) {
		Intent s = new Intent(context, PrefsActivity.class);
		startActivity(s);
	}
    public void Quit(View view) {
    	finish();
    }
}
package dk.cphbusiness.droirc;

import java.io.IOException;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import dk.cphbusiness.droirc.adapter.TabsPagerAdapter;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener, ServerListenerFragment.TaskCallbacks {

	private Connection connection;
	private ViewPager viewPager;
	private TabsPagerAdapter pagerAdapter;
	private ActionBar actionBar;
	private ServerListenerFragment serverListenerFragment;
	private FragmentManager serverListenerManager;
	private boolean resumed = true;
	private ArrayList<String> tabs;
	private String currentChannel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Information from startmenu activity
		
		Intent intent = getIntent();
		String hostName = intent.getStringExtra("HOSTNAME");
		String servername = intent.getStringExtra("SERVERNAME");
		User user = new User(0, intent.getStringExtra("NICKNAME")); // Always create user with ID 0
		
		// Initilization
		tabs = new ArrayList<String>();
		tabs.add("server");
		viewPager = (ViewPager) findViewById(R.id.pager);
		actionBar = getActionBar();
		pagerAdapter = new TabsPagerAdapter(getSupportFragmentManager(), this);
				
		viewPager.setAdapter(pagerAdapter);     
		actionBar.setHomeButtonEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);       

		// Fragment Manager (to interact with server listener)
		serverListenerManager = getFragmentManager();
		serverListenerFragment = (ServerListenerFragment) serverListenerManager.findFragmentByTag("listenertask");
		
		// If fragment is null create new
		if (serverListenerFragment == null) {
			resumed = false;
			serverListenerFragment = new ServerListenerFragment();
			serverListenerManager.beginTransaction().add(serverListenerFragment, "listenertask").commit();
		}
		
		if (!resumed) { // Only connect if new instance of program
			Toast.makeText(this, "Connecting to " + servername, Toast.LENGTH_SHORT).show();
			String[] serverInfo = { hostName, String.valueOf(6667), user.getNickname() };
			new ServerConnecter().execute(serverInfo);
		}
		else
			serverListenerManager.beginTransaction().commit();
		
		// Adding Tabs
		for (String tab_name : tabs) {
			actionBar.addTab(actionBar.newTab().setText(tab_name)
					.setTabListener(this));
		}

		// Listener for swiping the main window
		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				// on changing the page
				// make respected tab selected
				actionBar.setSelectedNavigationItem(position);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});

	}
	
	// Outgoing message method
	public void message(View view) {
		try {			
			EditText editText = (EditText) findViewById(R.id.editText1);
			TextView chatArea = (TextView) findViewById(R.id.chatwindow);
			// TODO: fix sending msg to right fragment 
			String message = editText.getText().toString();
			
			if (message.equals("")) return; // If message is an empty string don't write to server. Blocks onClick from sending empty lines when clicking EditText field
			
			if (message.length() > 7 && message.substring(0, 5).equalsIgnoreCase("/join")) { // check if user writes /join
				String channelName = message.substring(6);
				if (!isInChannel(channelName)) {
					connection.joinChannel(channelName);
					tabs.add(channelName); // adding the tab
					actionBar.addTab(actionBar.newTab().setText(channelName).setTabListener(this));
					pagerAdapter.notifyDataSetChanged();
					chatArea.append("Joining channel " + channelName);
				}
				else
					chatArea.append("Already in channel");
			}
			
			else if (actionBar.getSelectedTab() == null) {
				chatArea.append("You must join a channel before sending messages!");
			}
			
			else {
				String currentChannel = actionBar.getSelectedTab().getText().toString();
				connection.getWriter().write("PRIVMSG " + currentChannel + " :" + message + "\r\n");
				chatArea.append("<" + connection.getUser().getNickname() + "> " + message + "\n");
				editText.setText("");
				connection.getWriter().flush();
			}
			//scrollToBottom();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private boolean isInChannel(String channelName) {
		ArrayList<String> channels = connection.getChannels();
		for (String channel : channels) {
			if (channel.equals(channelName))
				return true;
		}
		return false;
	}
	
	// Tab Change Listener
	@Override
	public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) {

	}

	@Override
	public void onTabSelected(Tab tab, android.app.FragmentTransaction ft) {
		viewPager.setCurrentItem(tab.getPosition());
		currentChannel = connection.getChannels().get(tab.getPosition());
		System.out.println("Current channel changed to: " + currentChannel);
	}

	@Override
	public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) {

	}

	// ServerListenerFragment callbacks
	@Override
	public void onProgressUpdate(String... values) {
		TextView chatArea = (TextView) findViewById(R.id.chatwindow);
		if (chatArea != null)
			chatArea.append(values[0] + "\n");
	}
	
	@Override
	public void onPreExecute() {}

	@Override
	public void onCancelled() {}

	@Override
	public void onPostExecute() {}
	// -----------
	
	private class ServerConnecter extends AsyncTask<String, String, Void> {
		@Override
		protected Void doInBackground(String... params) {
			// String[] serverInfo = { hostName, String.valueOf(6667), user.getNickname() }
			String hostName = params[0];
			int port = Integer.parseInt(params[1]);
			User user = new User(0, params[2]);
			connection = new Connection(hostName, port, user);
			connection.connect();
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			serverListenerManager.beginTransaction().commit();
		}
	}

	public Connection getConnection() {
		return connection;
	}
}
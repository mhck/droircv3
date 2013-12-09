package dk.cphbusiness.droirc;


import java.io.IOException;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;

public class ServerListenerFragment extends Fragment {
	private TaskCallbacks callbacks;
	private ListenerTask task;
	private MainActivity chat;

	static interface TaskCallbacks {
		void onPreExecute();
		void onProgressUpdate(String... values);
		void onCancelled();
		void onPostExecute();
	}

	/**
	 * Hold a reference to the parent Activity so we can report the
	 * task's current progress and results. The Android framework
	 * will pass us a reference to the newly created Activity after
	 * each configuration change.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		callbacks = (TaskCallbacks) activity;
	}

	/**
	 * This method will only be called once when the retained
	 * Fragment is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Getting the parent activity
		chat = (MainActivity) getActivity();
		
		// Retain this fragment across configuration changes.
		setRetainInstance(true);

		// Create and execute the background task.
		task = new ListenerTask();
		task.execute();
	}

	/**
	 * Set the callback to null so we don't accidentally leak the
	 * Activity instance.
	 */
	@Override
	public void onDetach() {
		super.onDetach();
		callbacks = null;
	}

	/**
	 * A dummy task that performs some (dumb) background work and
	 * proxies progress updates and results back to the Activity.
	 *
	 * Note that we need to check if the callbacks are null in each
	 * method in case they are invoked after the Activity's and
	 * Fragment's onDestroy() method have been called.
	 */
	private class ListenerTask extends AsyncTask<Void, String, Void> {

		@Override
		protected void onPreExecute() {
			if (callbacks != null) {
				callbacks.onPreExecute();
			}
		}

		/**
		 * Note that we do NOT call the callback object's methods
		 * directly from the background thread, as this could result
		 * in a race condition.
		 */
		@Override
		protected Void doInBackground(Void... ignore) {
			Connection connection = chat.getConnection();
			try {
				connection.setLine(connection.getReader().readLine()); // reads first line
				while (connection.getLine() != null) {
					System.err.println(connection.getLine());
					if (connection.getLine().startsWith("PING ")) {
						// We must respond to PINGs to avoid being disconnected.
						connection.getWriter().write("PONG " + connection.getLine().substring(5) + "\r\n");
						connection.getWriter().flush();
					}
					else {
						publishProgress(StringProcessor.processLine(connection.getLine(), connection.getHostName(), connection.getUser().getNickname()));
					}
					connection.setLine(connection.getReader().readLine()); // reads line
				}
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return null;
		}

		protected void onProgressUpdate(String... values) {
			/*super.onProgressUpdate(values);
			TextView chatArea = (TextView) chat.findViewById(R.id.textView1);
			chatArea.append(values[0] + "\n");*/
			chat.onProgressUpdate(values);
		}

		@Override
		protected void onCancelled() {
			if (callbacks != null) {
				callbacks.onCancelled();
			}
		}

		@Override
		protected void onPostExecute(Void ignore) {
			if (callbacks != null) {
				callbacks.onPostExecute();
			}
		}
	}
}
package com.cortxt.app.corelib.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.cortxt.app.corelib.R;
import com.cortxt.app.utillib.DataObjects.Carrier;
import com.cortxt.app.utillib.DataObjects.EventType;
import com.cortxt.app.utillib.Reporters.ReportManager;
import com.cortxt.app.utillib.Reporters.WebReporter.WebReporter;
import com.cortxt.app.utillib.Utils.Constants;
import com.cortxt.app.utillib.Utils.Global;
import com.cortxt.app.utillib.Utils.LoggerUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

// TODO: use Compatibility Package
@SuppressLint("NewApi")
public class CallHistoryFragment extends ListFragment {
	private static final HashSet<Integer> EVENTS_TO_DISPLAY = new HashSet<Integer>();
	static {
		EVENTS_TO_DISPLAY.add(EventType.EVT_DROP.getIntValue());
		EVENTS_TO_DISPLAY.add(EventType.EVT_CALLFAIL.getIntValue());
	}

	// private ListView mListView;
	private TextView mEmptyMessage;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.frag_eventhistory, null, false);

		//TextView mapHistoryTitle = (TextView) view.findViewById(R.id.actionbartitle);

		Intent intent = this.getActivity().getIntent();
		boolean bFromStats = false;
		boolean bFromNerd = false;
		int eventtype = 0;
		if (intent.hasExtra("eventtype"))
			eventtype = intent.getIntExtra("eventtype",0);

		mEmptyMessage = (TextView) getActivity().findViewById(R.id.eventhistory_emptymessage);

		HashSet<Integer> eventsToDisplay = new HashSet<Integer>();
		if (eventtype > 0)
			eventsToDisplay.add(eventtype);
		else {
			eventsToDisplay.add(EventType.EVT_DROP.getIntValue());
			eventsToDisplay.add(EventType.EVT_CALLFAIL.getIntValue());
			if (!bFromStats) {
				eventsToDisplay.add(EventType.COV_VOD_NO.getIntValue());
				eventsToDisplay.add(EventType.COV_VOD_YES.getIntValue());
				eventsToDisplay.add(EventType.COV_DATA_YES.getIntValue());
				eventsToDisplay.add(EventType.COV_3G_YES.getIntValue());
				eventsToDisplay.add(EventType.COV_4G_YES.getIntValue());
				eventsToDisplay.add(EventType.COV_DATA_NO.getIntValue());
				eventsToDisplay.add(EventType.COV_3G_NO.getIntValue());
				eventsToDisplay.add(EventType.COV_4G_NO.getIntValue());
				eventsToDisplay.add(EventType.MAN_SPEEDTEST.getIntValue());
				eventsToDisplay.add(EventType.VIDEO_TEST.getIntValue());
				eventsToDisplay.add(EventType.AUDIO_TEST.getIntValue());
				eventsToDisplay.add(EventType.WEBPAGE_TEST.getIntValue());
				eventsToDisplay.add(EventType.YOUTUBE_TEST.getIntValue());
				eventsToDisplay.add(EventType.SMS_TEST.getIntValue());
				eventsToDisplay.add(EventType.EVT_VQ_CALL.getIntValue());

				// eventsToDisplay.add(DataNetworkChangeEvent.TYPE_DATA_NETWORK_CHANGE);
				if (bFromNerd) {
					eventsToDisplay = null;
					//				eventsToDisplay.add(EventType.EVT_CONNECT.getIntValue());
					//				eventsToDisplay.add(EventType.EVT_DISCONNECT.getIntValue());
					//				eventsToDisplay.add(EventType.EVT_UNANSWERED.getIntValue());
					//				eventsToDisplay.add(EventType.COV_UPDATE.getIntValue());
					//				eventsToDisplay.add(EventType.MAN_TRACKING.getIntValue());
					//				eventsToDisplay.add(EventType.TT_DROP.getIntValue());
					//				eventsToDisplay.add(EventType.TT_FAIL.getIntValue());
					//				eventsToDisplay.add(EventType.TT_DATA.getIntValue());
					//				eventsToDisplay.add(EventType.TT_NO_SVC.getIntValue());
					//				eventsToDisplay.add(EventType.APP_MONITORING.getIntValue());
					//				eventsToDisplay.add(EventType.MAN_PLOTTING.getIntValue());
					//				eventsToDisplay.add(EventType.LATENCY_TEST.getIntValue());
					//				eventsToDisplay.add(EventType.WIFI_CONNECT.getIntValue());
					//				eventsToDisplay.add(EventType.WIFI_DISCONNECT.getIntValue());
					//				eventsToDisplay.add(EventType.SMS_TEST.getIntValue());
					//				eventsToDisplay.add(EventType.VIDEO_TEST.getIntValue());
					//				// if (!MMCLogger.isDebuggable())
					//				{
					//					eventsToDisplay.add(EventType.TRAVEL_CHECK.getIntValue());
					//					eventsToDisplay.add(EventType.EVT_FILLIN.getIntValue());
					//				}
				} else {
					// Dashboard.customizeTitleBar(this.getActivity(), view,
					// R.string.dashboard_eventhistory,
					// R.string.dashcustom_eventhistory);
				}
			} else { // compare screen might call this
				// Dashboard.customizeTitleBar(this.getActivity(), view,
				// R.string.dashboard_eventhistory,
				// R.string.dashcustom_eventhistory);
			}
		}

		/*
		 * mListView.setOnItemClickListener(new OnItemClickListener() {
		 * 
		 * @Override public void onItemClick(AdapterView<?> parent, View view,
		 * int position, long id) { int eventId = (Integer) view.getTag();
		 * Intent intent = new Intent(EventHistoryFragment.this.getActivity(),
		 * EventDetail.class); intent.putExtra(EventDetail.EXTRA_EVENT_ID,
		 * eventId); startActivity(intent); } });
		 */
		
		//load lat/lons for locations before trying to geocode
		preGeocode(eventsToDisplay);
		
		return view;
	}

	public void parseAddress(String response,  List<HashMap<String, String>> events) {
		JSONObject json = null;
		
		if(response == null)
			return;
		
		try {
			json = new JSONObject(response);
			
			if(json.has("error")) {
				String error = json.getString("error");
			}
			else {
				json = json.getJSONObject("address");
				HashMap<String, String> event = events.get(0);
				String number = "";
				if(json.has("house_number"))
					number = json.getString("house_number");
				String road = json.getString("road");
				String suburb = "";
				//if(json.has("suburb"))
				//	suburb = ", " + json.getString("suburb");
				String address = number + " " + road + suburb;
				event.put(KEY_ADDRESS, address.trim());
				/*
=======
		TaskHelper.execute(
		new AsyncTask<Void, Void, List<HashMap<String, String>>>() {
			@Override
			protected List<HashMap<String, String>> doInBackground(Void... params) {
				List<HashMap<String, String>> results = ReportManager.getInstance(getActivity()).getEvents(eventsToDisplay);
				for (HashMap<String, String> event : results) {
					try
					{
						if (event == null)
							continue;
						if (!(event.containsKey(EventKeys.LATITUDE) && event.containsKey(EventKeys.LONGITUDE))) {
							event.put(KEY_ADDRESS, getActivity().getString(R.string.mycoverage_unknownlocation));
							continue;
						}
						Double lat = Double.parseDouble(event.get(EventKeys.LATITUDE));
						Double lon = Double.parseDouble(event.get(EventKeys.LONGITUDE));
						String address = String.format("%.4f, %.4f", lat, lon);
						event.put(KEY_ADDRESS, address);
						// Geocoder geocoder = new Geocoder(EventHistory.this);
						// String addressString = "";
						// try {
						// List<Address> addresses = geocoder.getFromLocation(lat,
						// lon, 1);
						// if(addresses == null || addresses.size() <= 0){
						// continue;
						// }
						// Address address = addresses.get(0);
						// for(int i=0; i<=address.getMaxAddressLineIndex(); i++) {
						// addressString += address.getAddressLine(i) + " ";
						// }
						// event.put(KEY_ADDRESS, addressString);
						// } catch (Exception e) {
						// String address = String.format("%.4f, %.4f", lat, lon);
						// event.put(KEY_ADDRESS, address);
						// }
					}
					catch (Exception e){}
				}
				return results;
>>>>>>> mmc_white_GM_feature_transit_sampling
*/
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
			return;
		}
	} 
	
	public void parseAddresses(String response, List<HashMap<String, String>> events) {
		final String KEY_ADDRESS = "address";
		JSONObject json = null;
		JSONArray jsonArray = null;

		if(response == null)
			return;
		
		try {
			jsonArray = new JSONArray(response);
		} catch (JSONException e) {
			//Right now the server will give back a JSONObject if there is only 1 location, so if that happens here catch it 
			//This will be fixed soon so revisit this TODO 
			parseAddress(response, events);
			return;
		}
		try {
			for(int i = 0; i < jsonArray.length(); i++) {
				json = jsonArray.getJSONObject(i);
				HashMap<String, String> event = events.get(i);
				
				if(json.has("error")) {
					String error = json.getString("error");
				}
				else {
					json = json.getJSONObject("address");
					String number = "";
					if(json.has("house_number"))
						number = json.getString("house_number");
					String road = json.getString("road");
					String suburb = "";
					//if(json.has("suburb"))
					//	suburb = ", " + json.getString("suburb");
					String address = number + " " + road + suburb;
					event.put(KEY_ADDRESS, address.trim());
				}
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
			return;
		}
/*
			@Override
			protected void onPostExecute(List<HashMap<String, String>> results) {
				try{
					if (mEmptyMessage == null) {
						if(getActivity() == null) {
							return;
						}
						mEmptyMessage = (TextView) getActivity().findViewById(R.id.eventhistory_emptymessage);
					}
					if (!results.isEmpty()) {
						EventListAdapter adapter = new EventListAdapter(EventHistoryFragment.this.getActivity(), results);
						// mListView.setAdapter(adapter);
						setListAdapter(adapter);
						mEmptyMessage.setVisibility(View.INVISIBLE);
						// ScalingUtility.getInstance(EventHistory.this).scaleView(mListView);
						// Toast.makeText(getActivity(), "not empty", Toast.LENGTH_SHORT).show();
					} else {
						mEmptyMessage.setVisibility(View.VISIBLE);
						// Toast.makeText(getActivity(), "empty", Toast.LENGTH_SHORT).show();
					}
				} catch (Exception e){} // in case window was closed
			}

		});
		//.execute((Void[]) null);
		
		return view;
*/
	}
	
	public void toggleHistory() {

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// Toast.makeText(this.getActivity(), "Selected: " + position, Toast.LENGTH_SHORT).show();
//		v.setBackgroundColor(Color.WHITE);

		Integer eventId = (Integer) v.getTag();
//		openEventShare (eventId);

		Intent intent = new Intent(CallHistoryFragment.this.getActivity(), CallDetailWeb.class);
		intent.putExtra(CallDetailWeb.EXTRA_EVENT_ID, eventId);
		startActivity(intent);
	}



	// TODO: on back finish the parent activity
	/*
	 * public void MapBackActionClicked(View view) { this.finish(); }
	 */
	private static final String KEY_ADDRESS = "address";

	class EventListAdapter extends ArrayAdapter<HashMap<String, String>> {
		public EventListAdapter(Context context, List<HashMap<String, String>> objects) {
			super(context, R.layout.eventhistory_listitem, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			try {
				HashMap<String, String> event = getItem(position);

				if (row == null) {
					LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					row = inflater.inflate(R.layout.eventhistory_listitem, parent, false);
					//ScalingUtility.getInstance(EventHistoryFragment.this.getActivity()).scaleView(row);
				}

				EventType eventType = EventType.get(Integer.parseInt(event.get(ReportManager.EventKeys.TYPE)));
				TextView date = (TextView) row.findViewById(R.id.eventhistory_date);
				TextView time = (TextView) row.findViewById(R.id.eventhistory_time);
				TextView type = (TextView) row.findViewById(R.id.eventhistory_type);
				TextView loc = (TextView) row.findViewById(R.id.eventhistory_location);

//				FontsUtil.applyFontToTextView(Constants.font_Regular, type, EventHistoryFragment.this.getActivity());
//				FontsUtil.applyFontToTextView(Constants.font_Regular, time, EventHistoryFragment.this.getActivity());
//				FontsUtil.applyFontToTextView(Constants.font_Regular, date, EventHistoryFragment.this.getActivity());
//				FontsUtil.applyFontToTextView(Constants.font_Regular, loc, EventHistoryFragment.this.getActivity());

				int iconResource = eventType.getImageResource();
				int nameResource = eventType.getEventString();

				ImageView icon = (ImageView) row.findViewById(R.id.eventhistory_icon);
				if (iconResource != 0)
					icon.setImageResource(iconResource);

				String title = "";
				if (nameResource > 0)
					title = getContext().getString (nameResource);


				type.setText(title);

				long timeStamp = Long.parseLong(event.get(ReportManager.EventKeys.TIMESTAMP));
				DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
				DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
				Date d = new Date(timeStamp);
				date.setText(" / " + dateFormat.format(d));
				time.setText(timeFormat.format(d));

				if (event.containsKey(KEY_ADDRESS)) {
					loc.setText(event.get(KEY_ADDRESS));
				}
				int id = Integer.parseInt(event.get(ReportManager.EventKeys.ID));
				row.setTag(id);
			} catch (Exception e) {
				LoggerUtil.logToFile(LoggerUtil.Level.DEBUG, "EventHistory", "getView", "", e);
			}
			return row;
		}
	}

	public void preGeocode(final HashSet<Integer> eventsToDisplay) {
		new AsyncTask<Void, Void, List<HashMap<String, String>>>() {
			@Override
			protected List<HashMap<String, String>> doInBackground(Void... params) {
				List<HashMap<String, String>> results = ReportManager.getInstance(getActivity()).getEvents(eventsToDisplay);
				
				for (HashMap<String, String> event : results) {
					try {
						if (event == null) {
							continue;
						}
						if (!(event.containsKey(ReportManager.EventKeys.LATITUDE) && event.containsKey(ReportManager.EventKeys.LONGITUDE))) {
							event.put(KEY_ADDRESS, getActivity().getString(R.string.mycoverage_unknownlocation));
							continue;
						}
						Double lat = Double.parseDouble(event.get(ReportManager.EventKeys.LATITUDE));
						Double lon = Double.parseDouble(event.get(ReportManager.EventKeys.LONGITUDE));
						String address = "@" + String.format("%.4f, %.4f", lat, lon);
						event.put(KEY_ADDRESS, address);
					}
					catch (Exception e){
						System.out.println(e);
					}
				}	
				
				return results;
			}

			@Override
			protected void onPostExecute(List<HashMap<String, String>> results) {
				if(getActivity() == null) {
					return;
				}
				
				if (mEmptyMessage == null) {
					
					mEmptyMessage = (TextView) getActivity().findViewById(R.id.eventhistory_emptymessage);
				}
				if (!results.isEmpty()) {
					EventListAdapter adapter = new EventListAdapter(CallHistoryFragment.this.getActivity(), results);
					setListAdapter(adapter);
					mEmptyMessage.setVisibility(View.INVISIBLE);
				}
				else {
					mEmptyMessage.setVisibility(View.VISIBLE);
				}
				
				geocode(eventsToDisplay);
			}

		}.execute((Void[]) null);
	}
	
	public void geocode(final HashSet<Integer> eventsToDisplay) {
		new AsyncTask<Void, Void, List<HashMap<String, String>>>() {
			@Override
			protected List<HashMap<String, String>> doInBackground(Void... params) {
				List<HashMap<String, String>> results = ReportManager.getInstance(getActivity()).getEvents(eventsToDisplay);
				
				String locations = "";
				for (HashMap<String, String> event : results) {
					try
					{
						if (event == null) {
							locations += "&location=0&location=0"; //This keeps order for parseAddresses()
							continue;
						}
						if (!(event.containsKey(ReportManager.EventKeys.LATITUDE) && event.containsKey(ReportManager.EventKeys.LONGITUDE))) {
							event.put(KEY_ADDRESS, getActivity().getString(R.string.mycoverage_unknownlocation));
							locations += "&location=0&location=0"; //This keeps order for parseAddresses()
							continue;
						}
						Double lat = Double.parseDouble(event.get(ReportManager.EventKeys.LATITUDE));
						Double lon = Double.parseDouble(event.get(ReportManager.EventKeys.LONGITUDE));
						locations += "&location=" + lat + "&location=" + lon;
						String address = String.format("%.4f, %.4f", lat, lon);
						event.put(KEY_ADDRESS, "@" + address);
					}
					catch (Exception e){
						System.out.println(e);
					}

				}	
				
				if(locations.equals(""))
					return null;
				
				try {
					String apiKey = Global.getApiKey(getActivity());
					String server = Global.getApiUrl(getActivity());
					String url = server + "/api/osm/location?apiKey=" + apiKey + locations;
					String response = WebReporter.getHttpURLResponse(url, false);

					parseAddresses(response, results);
				}
				catch (Exception e){
					System.out.println(e);
				}
				
				return results;
			}
			
			@Override
			protected void onPostExecute(List<HashMap<String, String>> results) {
				if(getActivity() == null || results == null) {
					return;
				}
				try{
				if (mEmptyMessage == null) {
					
					mEmptyMessage = (TextView) getActivity().findViewById(R.id.eventhistory_emptymessage);
				}
				if (!results.isEmpty() && CallHistoryFragment.this.getActivity() != null) {
					EventListAdapter adapter = new EventListAdapter(CallHistoryFragment.this.getActivity(), results);
					// mListView.setAdapter(adapter);
					setListAdapter(adapter);
					mEmptyMessage.setVisibility(View.INVISIBLE);

				} else {
					mEmptyMessage.setVisibility(View.VISIBLE);
				}
				} catch (Exception e) {} // in case window closed
			}

		}.execute((Void[]) null);
	}
}

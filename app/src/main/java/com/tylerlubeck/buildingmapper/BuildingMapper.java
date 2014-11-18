package com.tylerlubeck.buildingmapper;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class BuildingMapper extends Activity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private Spinner point_picker;
    private Spinner room_picker;
    private Button whereAmI;
    private Button getAPs;
    private Button saveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final Context global_ctx = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_building_mapper);
        getAPs = (Button) findViewById(R.id.get_access_points_btn);
        whereAmI = (Button) findViewById(R.id.where_am_i_btn);
        saveBtn = (Button) findViewById(R.id.save_access_points_btn);
        room_picker = (Spinner) findViewById(R.id.room_picker_spinner);
        point_picker = (Spinner) findViewById(R.id.location_picker_spinner);

        FloorListAdapter room_adapter = new FloorListAdapter(this, new ArrayList<Floor>());
        room_picker.setAdapter(room_adapter);

        GetFloorsAsyncTask fill_room_drop_down = new GetFloorsAsyncTask(getString(R.string.get_room_names_url),
                                                                           room_adapter,
                                                                           null /* params */);
        fill_room_drop_down.execute();

        room_picker.setOnItemSelectedListener(this);

        saveBtn.setOnClickListener(this);
        getAPs.setOnClickListener(this);
        whereAmI.setOnClickListener(this);
    }

    JSONArray getAccessPoints(ListView listView, int numberOfPolls) {
        AccessPointManager ap = new AccessPointManager(this);
        AccessPointListAdapter accessPointListAdapter = new AccessPointListAdapter(this, ap.getAccessPoints());
        listView.setAdapter(accessPointListAdapter);
        JSONArray all_objects = ap.pollAccessPoints(numberOfPolls);

        return all_objects;
    }

    void postToServer(JSONArray data, int id, Context ctx) {
        JSONObject send_data = new JSONObject();
        try {
            send_data.put("APS", data);
            send_data.put("lid", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        PostAccessPointsTask post_access_points = new PostAccessPointsTask(getString(R.string.post_access_points), send_data, ctx);
        post_access_points.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.building_mapper, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        ListView items = (ListView) findViewById(R.id.list_of_access_points);
        JSONArray all_objects; //TODO: can I just fill this now?
        Location selected_location;
        switch (view.getId()) {
            case R.id.where_am_i_btn:
                all_objects = getAccessPoints(items, 10);
                Log.d("BUILDINGMAPPER", all_objects.toString());
                //postToServer(all_objects, -1, this);
                break;
            case R.id.get_access_points_btn:
                getAccessPoints(items, 1);
                break;
            case R.id.save_access_points_btn:
                int nums[] = {2, 10, 30, 60, 100};
                int ids[]  = {100, 101, 102, 103, 104};
                for (int i = 0; i < 5; i++) {
                    all_objects = getAccessPoints(items, nums[i]);
                    // selected_location = (Location) point_picker.getSelectedItem();
                    postToServer(all_objects, ids[i], this);
                   // Log.d("BUILDINGMAPPER", all_objects.toString());
                    Toast.makeText(this, "Done with pass for: " + nums[i], Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Floor room = (Floor)adapterView.getItemAtPosition(i);
        ArrayAdapter<String> adapter = (ArrayAdapter)point_picker.getAdapter();
        if (adapter != null) {
            adapter.clear();
            adapter.notifyDataSetChanged();
        }
        ArrayAdapter<String> points_adapter  = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1, new ArrayList<String>());
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("building", room.getBuilding()));
        params.add(new BasicNameValuePair("floor", String.valueOf(room.getFloor())));
        String url = getString(R.string.get_access_points_url) + "/" + room.getBuilding() + "/" + String.valueOf(room.getFloor());

        point_picker.setAdapter(points_adapter);
        //GetPointsAsyncTask fill_points_drop_down = new GetPointsAsyncTask(getString(R.string.get_access_points_url), points_adapter, params);
        GetPointsAsyncTask fill_points_drop_down = new GetPointsAsyncTask(url, points_adapter, null);
        fill_points_drop_down.execute();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        Log.d("BUILDINGMAPPER", "NOTHING SELECTED");
        Spinner point_picker = (Spinner) findViewById(R.id.location_picker_spinner);
        ArrayAdapter<String> adapter = (ArrayAdapter)point_picker.getAdapter();
        adapter.clear();
        adapter.notifyDataSetChanged();
    }
}

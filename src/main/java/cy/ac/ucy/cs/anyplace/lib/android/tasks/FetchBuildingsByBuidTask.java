/*
* Anyplace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Timotheos Constambeys
* 
* Supervisor: Demetrios Zeinalipour-Yazti
*
* URL: http://anyplace.cs.ucy.ac.cy
* Contact: anyplace@cs.ucy.ac.cy
*
* Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
* All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of
* this software and associated documentation files (the "Software"), to deal in the
* Software without restriction, including without limitation the rights to use, copy,
* modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
* and to permit persons to whom the Software is furnished to do so, subject to the
* following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*/

package cy.ac.ucy.cs.anyplace.lib.android.tasks;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;


import cy.ac.ucy.cs.anyplace.lib.Anyplace;
import cy.ac.ucy.cs.anyplace.lib.android.nav.BuildingModel;
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkUtils;

import static android.content.Context.MODE_PRIVATE;

public class FetchBuildingsByBuidTask extends AsyncTask<Void, Void, String> {

	public interface FetchBuildingsByBuidTaskListener {
		void onErrorOrCancel(String result);

		void onSuccess(String result, BuildingModel building);
	}

	private FetchBuildingsByBuidTaskListener mListener;
	private Context ctx;

	private BuildingModel building;
	private boolean success = false;
	private ProgressDialog dialog;
	private Boolean showDialog = true;
	private String json_req;
	private String mbuid;

	public FetchBuildingsByBuidTask(FetchBuildingsByBuidTaskListener fetchBuildingsTaskListener, Context ctx, String buid) {
		this.mListener = fetchBuildingsTaskListener;
		this.ctx = ctx;
        mbuid = buid;
		// create the JSON object for the navigation API call
		JSONObject j = new JSONObject();
		try {
			j.put("username", "username");
			j.put("password", "pass");
			// insert the destination POI and the user's coordinates
			j.put("buid", buid);
			this.json_req = j.toString();

		} catch (JSONException e) {

		}
	}

	public FetchBuildingsByBuidTask(FetchBuildingsByBuidTaskListener fetchBuildingsTaskListener, Context ctx, String buid, Boolean showDialog) {
		this(fetchBuildingsTaskListener, ctx, buid);
		this.showDialog = showDialog;
	}

	@Override
	protected void onPreExecute() {
		if (showDialog) {
			dialog = new ProgressDialog(ctx);
			dialog.setIndeterminate(true);
			dialog.setTitle("Fetching Building");
			dialog.setMessage("Please be patient...");
			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(false);
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					FetchBuildingsByBuidTask.this.cancel(true);
				}
			});
			dialog.show();
		}

	}

	@Override
	protected String doInBackground(Void... params) {

		if (!NetworkUtils.isOnline(ctx)) {
			return "No connection available!";
		}

		try {

			if (json_req == null)
				return "Error creating the request!";

			String response;
          SharedPreferences pref = ctx.getSharedPreferences("LoggerPreferences", MODE_PRIVATE);

          String host = pref.getString("server_ip_address", "ap.cs.ucy.ac.cy");
          String port = pref.getString("server_port", "443");

          Anyplace client = new Anyplace(host, port, ctx.getCacheDir().getAbsolutePath());
            response = client.buildingsByBuildingCode(mbuid);
			JSONObject json = new JSONObject(response);

			if (json.has("status") && json.getString("status").equalsIgnoreCase("1")) {
				return "Error Message: " + json.getString("message");
			}

			// process the buildings received
			BuildingModel b;
			b = new BuildingModel();
			b.setPosition(json.getString("coordinates_lat"), json.getString("coordinates_lon"));
			b.buid = json.getString("buid");
			b.name = json.getString("name");

			building = b;

			success = true;
			return "Successfully fetched buildings";

		}  catch (Exception e) {
			return "Error fetching buildings. [ " + e.getMessage() + " ]";
		}
	}

	@Override
	protected void onPostExecute(String result) {
		if (showDialog)
			dialog.dismiss();

		if (success) {
			mListener.onSuccess(result, building);
		} else {
			// there was an error during the process
			mListener.onErrorOrCancel(result);
		}

	}

	@Override
	protected void onCancelled(String result) {
		if (showDialog)
			dialog.dismiss();
		mListener.onErrorOrCancel("Buildings Fetch cancelled...");
	}

	@Override
	protected void onCancelled() { // just for < API 11
		if (showDialog)
			dialog.dismiss();
		mListener.onErrorOrCancel("Buildings Fetch cancelled...");
	}

}

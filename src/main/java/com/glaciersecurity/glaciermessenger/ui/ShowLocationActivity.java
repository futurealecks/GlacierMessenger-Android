package com.glaciersecurity.glaciermessenger.ui;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.osmdroid.util.GeoPoint;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.ActivityShowLocationBinding;
import com.glaciersecurity.glaciermessenger.ui.util.LocationHelper;
import com.glaciersecurity.glaciermessenger.ui.util.UriHelper;
import com.glaciersecurity.glaciermessenger.ui.widget.Marker;
import com.glaciersecurity.glaciermessenger.ui.widget.MyLocation;


public class ShowLocationActivity extends LocationActivity implements LocationListener {

	private GeoPoint loc = Config.Map.INITIAL_POS;
	private ActivityShowLocationBinding binding;


	private Uri createGeoUri() {
		return Uri.parse("geo:" + this.loc.getLatitude() + "," + this.loc.getLongitude());
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.binding = DataBindingUtil.setContentView(this,R.layout.activity_show_location);
		setSupportActionBar((Toolbar) binding.toolbar);

		configureActionBar(getSupportActionBar());
		setupMapView(this.binding.map, this.loc);

		this.binding.fab.setOnClickListener(view -> startNavigation());

		final Intent intent = getIntent();
		if (intent != null) {
			final String action = intent.getAction();
			if (action == null) {
				return;
			}
			switch (action) {
				case "com.glaciersecurity.glaciermessenger.location.show":
					if (intent.hasExtra("longitude") && intent.hasExtra("latitude")) {
						final double longitude = intent.getDoubleExtra("longitude", 0);
						final double latitude = intent.getDoubleExtra("latitude", 0);
						this.loc = new GeoPoint(latitude, longitude);
					}
					break;
				case Intent.ACTION_VIEW:
					final Uri geoUri = intent.getData();

					// Attempt to set zoom level if the geo URI specifies it
					if (geoUri != null) {
						final HashMap<String, String> query = UriHelper.parseQueryString(geoUri.getQuery());

						// Check for zoom level.
						final String z = query.get("z");
						if (z != null) {
							try {
								mapController.setZoom(Double.valueOf(z));
							} catch (final Exception ignored) {
							}
						}

						// Check for the actual geo query.
						boolean posInQuery = false;
						final String q = query.get("q");
						if (q != null) {
							final Pattern latlng = Pattern.compile("/^([-+]?[0-9]+(\\.[0-9]+)?),([-+]?[0-9]+(\\.[0-9]+)?)(\\(.*\\))?/");
							final Matcher m = latlng.matcher(q);
							if (m.matches()) {
								try {
									this.loc = new GeoPoint(Double.valueOf(m.group(1)), Double.valueOf(m.group(3)));
									posInQuery = true;
								} catch (final Exception ignored) {
								}
							}
						}

						final String schemeSpecificPart = geoUri.getSchemeSpecificPart();
						if (schemeSpecificPart != null && !schemeSpecificPart.isEmpty()) {
							try {
								final GeoPoint latlong = LocationHelper.parseLatLong(schemeSpecificPart);
								if (latlong != null && !posInQuery) {
									this.loc = latlong;
								}
							} catch (final NumberFormatException ignored) {
							}
						}
					}

					break;
			}
			updateLocationMarkers();
		}
	}

	@Override
	protected void gotoLoc(final boolean setZoomLevel) {
		if (this.loc != null && mapController != null) {
			if (setZoomLevel) {
				mapController.setZoom(Config.Map.FINAL_ZOOM_LEVEL);
			}
			mapController.animateTo(new GeoPoint(this.loc));
		}
	}

	@Override
	public void onRequestPermissionsResult(final int requestCode,
										   @NonNull final String[] permissions,
										   @NonNull final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		updateUi();
	}

	@Override
	protected void setMyLoc(final Location location) {
		this.myLoc = location;
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_show_location, menu);
		updateUi();
		return true;
	}

	@Override
	protected void updateLocationMarkers() {
		super.updateLocationMarkers();
		if (this.myLoc != null) {
			this.binding.map.getOverlays().add(new MyLocation(this, null, this.myLoc));
		}
		this.binding.map.getOverlays().add(new Marker(this.marker_icon, this.loc));
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
//
		}
		return super.onOptionsItemSelected(item);
	}

	private void startNavigation() {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
				"google.navigation:q=" +
						String.valueOf(this.loc.getLatitude()) + "," + String.valueOf(this.loc.getLongitude())
		)));
	}

	@Override
	protected void updateUi() {
		final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=0,0"));
		final ComponentName component = i.resolveActivity(getPackageManager());
		this.binding.fab.setVisibility(component == null ? View.GONE : View.VISIBLE);
	}

	@Override
	public void onLocationChanged(final Location location) {
		if (LocationHelper.isBetterLocation(location, this.myLoc)) {
			this.myLoc = location;
			updateLocationMarkers();
		}
	}

	@Override
	public void onStatusChanged(final String provider, final int status, final Bundle extras) {

	}

	@Override
	public void onProviderEnabled(final String provider) {

	}

	@Override
	public void onProviderDisabled(final String provider) {

	}
}

package eu.starcross.ukmaps;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import com.qozix.tileview.TileView;
import com.qozix.tileview.widgets.ZoomPanLayout;
import com.qozix.tileview.widgets.ZoomPanLayout.ZoomPanListener;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.OSRef;

    public class MainActivity extends AppCompatActivity
            implements NavigationView.OnNavigationItemSelectedListener {


        /** Interval for location updates. Inexact. Updates may be more or less frequent  */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    /**The fastest rate for active location updates. Updates will never be more frequent */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static final String TAG = "TileProject";

    // Use unclear - code appears to be arbitrary
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private Task<Location> mLocation;
    private boolean mMapMoved;

    private TileView mTileView;

    // Location marker
    private ImageView mNavImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView( R.layout.activity_main );
        mTileView = (TileView) findViewById(R.id.tile_view);
        mTileView.setSize( 280000, 520000 );  // the original size of the untiled image
        mTileView.defineBounds( 0, 0, 700000, 1300000);
        mTileView.addDetailLevel( 1f, "%d_%d.gif", 200, 200);
        mTileView.setMinimumScaleMode(ZoomPanLayout.MinimumScaleMode.NONE);
        mTileView.setScaleLimits(0.2f, 3);
        mTileView.setShouldRenderWhilePanning(true);
        mTileView.setBitmapProvider( new BitmapProviderPicasso());
        mMapMoved = false; // Track movement away from gps centre

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createLocationCallback();
        createLocationRequest();
        createPanListener();
        disableScreenLock();

        mNavImageView = new ImageView( this );
        mNavImageView.setImageResource(R.drawable.blue_circle_40);
        mTileView.addMarker(mNavImageView, 0, 0, -0.5f, -0.5f);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                updateLocation();
            }
        };
    }

    /**
     * Set up the location request for real time location updates
     * Requires ACCESS_FINE_LOCATION defined in AndroidManifest.xml.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Set the desired interval for active location updates.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Set the fastest rate for active location updates.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    public void updateLocation() {

        if (mCurrentLocation != null) {
            LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            //LatLng latLng = new LatLng(51.764341, -0.213588);
            latLng.toOSGB36();
            OSRef osRef = latLng.toOSRef();

            double x = osRef.getEasting();
            double y = 1300000 - osRef.getNorthing();

            // If user has not moved the map already, centre on gps
            if (!mMapMoved) {
                mTileView.scrollToAndCenter(x, y);
            }
            mTileView.moveMarker(mNavImageView, x, y);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove location updates to save battery.
        stopLocationUpdates();
    }



    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }


    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }


    /**
     * Requests location updates from the FusedLocationApi. Don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        } catch (SecurityException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }
    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    private void createPanListener() {
        mTileView.addZoomPanListener(new ZoomPanListener() {
            public void onPanBegin(int x, int y, Origination origin) {
                mMapMoved = true;
            }
            public void onPanUpdate(int x, int y, Origination origin) {}
            public void onPanEnd(int x, int y, Origination origin) {}
            public void onZoomBegin(float scale, Origination origin) {}
            public void onZoomUpdate(float scale, Origination origin) {}
            public void onZoomEnd(float scale, Origination origin) {}
        });
    }

    public void disableScreenLock() {
        KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();
    }

        @SuppressWarnings("StatementWithEmptyBody")
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            // Handle navigation view item clicks here.
            int id = item.getItemId();

            if (id == R.id.settings_button) {
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
            }

            return true;
        }

}
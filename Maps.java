package cg.paridel.mazone.fragments;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.paridel.mazone.R;
import com.paridel.mazone.api.API;
import com.paridel.mazone.cache.DatabaseHelper;
import com.paridel.mazone.cache.Hotspot;
import com.paridel.mazone.api.ProximityIntentReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class Maps extends Fragment implements OnMapReadyCallback,
        GoogleMap.InfoWindowAdapter, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final long POINT_RADIUS = 50;
    private static final long PROX_ALERT_EXPIRATION = 5000; //-1 It will never expire
    private static final String PROX_ALERT_INTENT = "com.paridel.mazone.fragments";
    private static final int REQUEST_LOCATION_CODE = 99;
    private static final int REQUEST_CHECK_SETTINGS_GPS = 0x1;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 0x2;

    private DatabaseHelper databaseHelper;
    private List<Hotspot> markerArrayList;
    private GoogleMap mMap;
    private ProgressBar progressBar;
    private RelativeLayout viewMaps;
    private static final int LOCATION_REQ_CODE = 10;
    private static final NumberFormat nf = new DecimalFormat("##");
    private GoogleApiClient mGoogleApiClient;
    private Location mylocation;
    private Double myLati, myLong;
    private Context context;

    public Maps() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions();
        }

        bulidGoogleApiClient();

        /*Database use here*/
        databaseHelper = new DatabaseHelper(getContext());

        viewMaps = (RelativeLayout) view.findViewById(R.id.viewMaps);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        //show error dialog if Google Play Services not available
        if (!isGooglePlayServicesAvailable()) {
            Log.d("onCreate", "Google Play Services not available. Ending Test case.");
            getActivity().finish();
        } else {
            Log.d("onCreate", "Google Play Services available. Continuing.");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return view;
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(getContext());
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(getActivity(), result,
                        0).show();
            }
            return false;
        }
        return true;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            //bulidGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }


        mMap.clear();
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setInfoWindowAdapter(this);

        if (dataInternet()) {
            getMaZone();
        } else {
            getDatabaseMaZone();
            Snackbar.make(viewMaps, getString(R.string.volley_network_error), Snackbar.LENGTH_LONG).show();
        }
    }

    protected synchronized void bulidGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .enableAutoManage(getActivity(), 0, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.stopAutoManage(getActivity());
            mGoogleApiClient.disconnect();
        }
    }


    private void getMaZone() {
        progressBar.setVisibility(View.VISIBLE);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, API.GET_HOTSPOTS,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONArray jsonArray = jsonObject.getJSONArray("hotspots");

                            if (jsonArray.length() == 0) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Pas de hotspots wifi disponibles !", Toast.LENGTH_LONG).show();
                            } else {
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject object = jsonArray.getJSONObject(i);

                                    int idClient = object.getInt("id_client");
                                    String nomClient = object.getString("nom_client");
                                    String linkClient = object.getString("link_client");
                                    String latitudeClient = object.getString("latitude_client");
                                    String longitudeClient = object.getString("longitude_client");
                                    String cityClient = object.getString("city_client");

                                    Hotspot hotspot = new Hotspot(idClient, nomClient, linkClient, latitudeClient, longitudeClient, cityClient);
                                    if (databaseHelper.checkId(hotspot)){
                                        progressBar.setVisibility(View.GONE);
                                        Log.e("id existant", String.valueOf(hotspot.getIdClient()));
                                        getDatabaseMaZone();
                                    }
                                    else {

                                        databaseHelper.insertMaps(hotspot);

                                        markerArrayList = databaseHelper.getAllMazone();

                                        if (markerArrayList.size() == 0) {
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(getContext(), "Les points ne sont pas disponibles", Toast.LENGTH_LONG).show();
                                        }
                                        else {
                                            getDatabaseMaZone();
                                            progressBar.setVisibility(View.GONE);
                                        }
                                        Log.e("DATA maps : ", latitudeClient + ", " + longitudeClient);
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error instanceof NetworkError) {
                            progressBar.setVisibility(View.GONE);
                            getDatabaseMaZone();
                            Snackbar.make(viewMaps, getString(R.string.volley_network_error), Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof ServerError) {
                            progressBar.setVisibility(View.GONE);
                            getDatabaseMaZone();
                            Snackbar.make(viewMaps, getString(R.string.volley_server_error), Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof AuthFailureError) {
                            progressBar.setVisibility(View.GONE);
                            getDatabaseMaZone();
                            Snackbar.make(viewMaps, getString(R.string.volley_authfail), Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof ParseError) {
                            progressBar.setVisibility(View.GONE);
                            getDatabaseMaZone();
                            Snackbar.make(viewMaps, getString(R.string.volley_parse_error), Snackbar.LENGTH_LONG).show();
                        } else if (error instanceof TimeoutError) {
                            progressBar.setVisibility(View.GONE);
                            getDatabaseMaZone();
                            Snackbar.make(viewMaps, getString(R.string.volley_time_out_error), Snackbar.LENGTH_LONG).show();
                        }
                    }
                }) {

        };

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(stringRequest);
    }


    private void getDatabaseMaZone() {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        markerArrayList = databaseHelper.getAllMazone();

        if (markerArrayList.size() == 0) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Les points ne sont pas disponibles", Toast.LENGTH_LONG).show();
        }
        else {
            progressBar.setVisibility(View.GONE);
            Marker[] markers = new Marker[markerArrayList.size()];

            for (int i = 0; i < markerArrayList.size(); i++) {
                Double lat = Double.valueOf(markerArrayList.get(i).getLatClient());
                Double lon = Double.valueOf(markerArrayList.get(i).getLongClient());
                LatLng latLng = new LatLng(lat, lon);

                /*Proximity Alert methode*/
                setProximity(latLng.latitude, latLng.longitude, i + 1, i, markerArrayList.get(i).getNomClient());

                builder.include(latLng);

                if (mMap != null) {
                    markers[i] = mMap.addMarker(new MarkerOptions().position(latLng)
                            .title(markerArrayList.get(i).getNomClient())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.yattoomaps)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
                }
            }

            markerArrayList.clear();
            databaseHelper.close();
        }
    }


    /*Proximity Alert methode*/
    private void setProximity(double lat, double lon, final long eventID, int requestCode, String name) {
        if (ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity)getContext(), new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQ_CODE);
        }

        LocationManager locManager = (LocationManager)this.getActivity().getSystemService(Context.LOCATION_SERVICE);

        Intent intent = new Intent(PROX_ALERT_INTENT);
        intent.putExtra(ProximityIntentReceiver.EVENT_ID_INTENT_EXTRA, eventID);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        assert locManager != null;
        locManager.addProximityAlert(lat, lon, POINT_RADIUS, PROX_ALERT_EXPIRATION, pendingIntent);

        IntentFilter filter = new IntentFilter(PROX_ALERT_INTENT);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(new ProximityIntentReceiver(), filter);
        //getContext().registerReceiver(new ProximityIntentReceiver(), filter);

        //locManager.removeProximityAlert(pendingIntent);
    }


    public boolean dataInternet() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Activity.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isAvailable() && netInfo.isConnectedOrConnecting();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        checkPermissions();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mylocation = location;

        if (mylocation != null) {
            myLati = mylocation.getLatitude();
            myLong = mylocation.getLongitude();
        }

        /*if(mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
        }*/
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getInfoContents(Marker marker) {

        View view = LayoutInflater.from(getContext()).inflate(R.layout.info_maps, null);
        TextView nomClient = (TextView) view.findViewById(R.id.nomClient);
        TextView distanceClient = (TextView) view.findViewById(R.id.distanceClient);
        nomClient.setText(marker.getTitle());

        try {
            Location loc1 = new Location("Point mazone");
            loc1.setLatitude(marker.getPosition().latitude);
            loc1.setLongitude(marker.getPosition().longitude);


            Location loc2 = new Location("Utilisateur");
            loc2.setLatitude(myLati);
            loc2.setLongitude(myLong);

            float distanceInMeters = loc1.distanceTo(loc2);
            distanceClient.setText(nf.format(distanceInMeters) + "m");

        }
        catch (Exception e){
            e.printStackTrace();
            Log.e("error", e.toString());
        }


        return view;
    }

    private void checkPermissions(){
        int permissionLocation = ContextCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(getActivity(),
                        listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            }
        }
        else{
            getMyLocation();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS_GPS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        getMyLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        getActivity().finish();
                        break;
                }
                break;
        }
    }

    /*OK*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_CODE: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else {
                    Toast.makeText(getContext(), getString(R.string.permissiondenied), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /*OK*/
    private void getMyLocation(){
        if(mGoogleApiClient!=null) {
            if (mGoogleApiClient.isConnected()) {
                int permissionLocation = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION);

                if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                    mylocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setInterval(20000);
                    locationRequest.setFastestInterval(10000);
                    //locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
                    builder.setAlwaysShow(true);
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
                    PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                            .checkLocationSettings(mGoogleApiClient, builder.build());
                    result.setResultCallback(new ResultCallback<LocationSettingsResult>() {

                        @Override
                        public void onResult(@NonNull LocationSettingsResult result) {
                            final Status status = result.getStatus();
                            switch (status.getStatusCode()) {
                                case LocationSettingsStatusCodes.SUCCESS:
                                    // All location settings are satisfied.
                                    // You can initialize location requests here.
                                    int permissionLocation = ContextCompat
                                            .checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
                                    if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                                        mylocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                                        mMap.setMyLocationEnabled(true);
                                    }
                                    break;

                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    try {
                                        status.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS_GPS);
                                        mMap.setMyLocationEnabled(true);
                                    }
                                    catch (IntentSender.SendIntentException e) {
                                        // Ignore the error.
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    break;
                            }
                        }
                    });
                }
            }
        }
    }
}

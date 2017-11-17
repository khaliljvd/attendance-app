package khalil.csc131attendance;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.sheets.v4.SheetsScopes;

import com.google.api.services.sheets.v4.model.*;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaCas;
import android.media.tv.TvInputService;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.service.textservice.SpellCheckerService;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Main activity for the attendance check in application. This
 * activity hosts the check-in form.
 */
public class MainActivity extends Activity
        implements EasyPermissions.PermissionCallbacks {

    /////////////////////////////////////
    //////// CHANGE THESE VALUES ////////
    /////////////////////////////////////////////////////
    private String mEmail = "EMAIL_ADDRESS_HERE";
    private String mEmailPass = "EMAIL_PASSWORD_HERE";
    public String mSpreadsheetID = "https://docs.google.com/spreadsheets/d/YOUR_SPREADSHEET_ID/edit#gid=0";
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////

    GoogleAccountCredential mCredential;
    public LocationManager locationManager;
    private android.location.LocationListener locationListener;
    private static final int REQUEST_FINE_LOCATION = 0;
    private ProgressBar gpsProgress;
    private Button mCallApiButton;
    private String sid;
    private String name;
    private String date_time;

    private String rec, subject, textMessage;

    private Session session;
    private String key;
    private AutoCompleteTextView sid_entry;
    public TextView distanceText;
    private AutoCompleteTextView key_entry;
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    public double lat1;
    public double lon1;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {SheetsScopes.SPREADSHEETS};

    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sid_entry = (AutoCompleteTextView) findViewById(R.id.sid);
        key_entry = (AutoCompleteTextView) findViewById(R.id.password);
        //mCallApiButton.setBackgroundResource(R.drawable.selector);
        mCallApiButton = (Button) findViewById(R.id.check_in_button);
        mCallApiButton.setBackgroundResource(R.drawable.selector);

        ImageView window = (ImageView) findViewById(R.id.window);
        ImageView logo = (ImageView) findViewById(R.id.banner);
        distanceText = (TextView) findViewById(R.id.Distance);
        gpsProgress = (ProgressBar) findViewById(R.id.loading_panel);
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Attempting Check In ...");

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        StartAnimations(logo, window);

        locationListener = new android.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                new spinProgressBar().execute();
                //Toast.makeText(MainActivity.this, "Updated Location", Toast.LENGTH_LONG).show();
                double dist = distance(lat1, lon1, locationManager.getLastKnownLocation("gps").getLatitude(), locationManager.getLastKnownLocation("gps").getLongitude(), "m");
                distanceText.setText(String.format("Distance from class: %.2f meters", dist));
                setButton(dist);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getPermissions();
            return;
        }

        Button settingsButton = (Button) findViewById(R.id.settings);


        settingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        sid_entry.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        key_entry.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, locationListener);

        getSettings();

        double[] latlng = {locationManager.getLastKnownLocation("gps").getLatitude(),
                locationManager.getLastKnownLocation("gps").getLongitude()};

        double dist = distance(lat1, lon1, latlng[0], latlng[1], "m");
        setButton(dist);
        distanceText.setText(String.format("Distance from class: %.2f meters", dist));
        gpsProgress.setVisibility(View.GONE);
    }

    /**
     * Hides the keyboard if the user touches anywhere outside the text fields.
     */
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Grabs the latitude and longitude values from the SharedPreferences.
     * Used for testing purposes when resetting the location from the
     * settings tab.
     */
    private void getSettings(){
        SharedPreferences mSharedLat = getSharedPreferences("latitude", MODE_PRIVATE);
        SharedPreferences mSharedLong = getSharedPreferences("longitude", MODE_PRIVATE);
        SharedPreferences.Editor mEditLat = mSharedLat.edit();
        SharedPreferences.Editor mEditLong = mSharedLong.edit();
        lat1 = mSharedLat.getFloat("latitude", 0);
        lon1 = mSharedLong.getFloat("longitude", 0);
    }

    /**
     * Grabs the URL from SharedPreferences. If the value is not changed
     * then the sheet defaults to the original Google Sheet the application
     * was hardcoded with.
     * @return the Google Spreadsheet ID that the API will call on
     */
    private String getURL(){
        String full_URL;
        String ID;
        SharedPreferences mSharedURL = getSharedPreferences("url", MODE_PRIVATE);
        SharedPreferences.Editor mEditURL =  mSharedURL.edit();
        mEditURL.apply();
        full_URL = mSharedURL.getString("url", mSpreadsheetID);
        String[] split_URL = full_URL.split("/");
        ID = split_URL[split_URL.length-2];
        return ID;
    }

    /**
     * Sets the button to perform the sign in action notify the user
     * that he/she is too far from the classroom. Check-in button is
     * orange if within close proximity of the classrom and gray
     * otherwise.
     * @param dist indicating the distance between the longitude and latitude
     *             pairs.
     */
    public void setButton(Double dist) {
        //Check to see if too far from the classroom

        if (dist > 15) {
            mCallApiButton.setBackgroundResource(R.drawable.check_in_button_grey);
            mCallApiButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Too far from classroom!", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            mCallApiButton.setBackgroundResource(R.drawable.selector);
            mCallApiButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    sid = sid_entry.getText().toString().trim();
                    key = key_entry.getText().toString().trim();
                    if (sid.trim().equals("") && key.trim().equals("")) {
                        sid_entry.setError("Student ID is required!");
                        key_entry.setError("Key is required!");
                        //sid_entry.setHint("please enter username");
                    } else if (sid.trim().equals("")) {
                        sid_entry.setError("Student ID is required!");
                    } else if (key.trim().equals("")) {
                        key_entry.setError("Key is required!");
                    } else {
                        // Close the keyboard
                        View view = getCurrentFocus();
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }

                        //
                        getResultsFromApi();
                    }
                }
            });
        }
    }

    /**
     * Calculates the distance between two sets of longitude and latitude
     * coordinates in Kilometers, Miles, or Meters.
     * @param lat1 indicating the classroom latitude value.
     * @param lon1 indicating the classroom longitude value.
     * @param lat2 indicating the user's latitude value.
     * @param lon2 indicating the user's longitude value.
     * @param unit indicating the units distance is to be returned in.
     * @return the distance between the pairs of coordinates

     */
    public static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == "K") {
            dist = dist * 1.609344;
        } else if (unit == "N") {
            dist = dist * 0.8684;
        } else if (unit == "m"){
            dist = dist*1.609344*1000;
        }

        return (dist);
    }

    /**
     * Converts degrees to radians for the purposes of distance calculation
     * between two sets of longitude and latitude coordinates
     * @param deg indicating the degree value which will be converted.
     * @return the calculated value of radians
     */
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /**
     * Converts radians to degrees for the purposes of distance calculation
     * between two sets of longitude and latitude coordinates
     * @param rad indicating the radian value which will be converted.
     * @return the calculated value of degrees
     */
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    /**
     * Performs a permission check for location, contacts, and internet.
     * If user has not granted permissions, this will request them.
     */
    public void getPermissions(){
        int permissionCheck = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showMessageOKCancel("This application requires some permissions to operate properly.",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                                    Manifest.permission.INTERNET},
                                            REQUEST_FINE_LOCATION);
                                }
                            }
                        });
                return;
            }
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.INTERNET},
                    REQUEST_FINE_LOCATION);
        }

    }

    /**
     * Displays a message to the user asking to proceed with giving application
     * permissions
     * @param message indicating the message to be displayed to the user.
     * @param okListener indicating the positive action on the click of OK.
     */
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new android.support.v7.app.AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * Called from the method onCreate. Begins the animations for the application.
     * Animates and fades in the CSUS Logo and the check-in fields.
     * @param logo indicates the CSUS logo.
     * @param window indicating the check-in window with text.
     */
    private void StartAnimations(ImageView logo, ImageView window) {
        Animation fade_in = AnimationUtils.loadAnimation(this, R.anim.alpha);
        Animation slide_in = AnimationUtils.loadAnimation(this, R.anim.translate);
        //Animation slide_in = AnimationUtils.loadAnimation(this, R.anim.translate);
        RelativeLayout login_form = (RelativeLayout) findViewById(R.id.login_form);

        login_form.startAnimation(fade_in);

        sid_entry.startAnimation(slide_in);
        key_entry.startAnimation(slide_in);
        distanceText.startAnimation(fade_in);
        window.startAnimation(slide_in);
        //distanceText.startAnimation(slide_in);
        mCallApiButton.startAnimation(slide_in);

    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            System.out.println("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    System.out.println(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    System.out.println("Account Name: " + accountName);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Displays a Toast message to the user.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private void toastMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * Async task which spins a progress circle for 1 second when the distance
     * has been recalculated due to an updated user location.
     */
    private class spinProgressBar extends AsyncTask<Void, Void, Void> {

        /**
         * Background task to sleep a thread for 1 second while the
         * progress circle spins.
         * upon successful check-in.
         * @param args no parameters needed for this task.
         * @return null
         */
        @Override
        protected Void doInBackground(Void... args) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * After waiting one second, hide the progress circle.
         */
        @Override
        protected void onPostExecute(Void result) {
            gpsProgress.setVisibility(View.INVISIBLE);
            super.onPostExecute(result);
        }

        /**
         * Make the progress circle animation visible to
         * the user.
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            gpsProgress.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Async task which sends an email receipt using SMTP and SSL
     * upon successful check-in.
     */
    private class emailReceipt extends AsyncTask<Void, Void, Void> {

        /**
         * Background task to email the receipt to the user's email
         * upon successful check-in.
         * @param args no parameters needed for this task.
         * @return null
         */
        @Override
        protected Void doInBackground(Void... args) {
            try{
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress("csc131attendance@gmail.com"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(rec));
                message.setSubject(subject);
                message.setContent(textMessage, "text/html; charset=utf-8");
                Transport.send(message);

            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Displays a Toast message after email is sent to notify
         * the user.
         * @param result is the result of the email send
         */
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //pdialog.hide();
            Toast.makeText(MainActivity.this, "Email Sent!", Toast.LENGTH_SHORT).show();
        }

        /**
         * Action to take before the email is sent.
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            subject = "Subject Line";
        }
    }

    /**
     * Determines what actions are performed when the application is
     * paused such as when the user switches to another application.
     */
    @Override
    public void onPause(){
        super.onPause();
        locationManager.removeUpdates(locationListener);
        //this.finishAffinity();
    }

    /**
     * Determines what actions are performed when the application is
     * resumed such as when the user switches from another application
     * to this one.
     */
    @Override
    public void onResume(){
        super.onResume();
        getSettings();

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, locationListener);

        double[] latlng = {locationManager.getLastKnownLocation("gps").getLatitude(),
                locationManager.getLastKnownLocation("gps").getLongitude()};
        double dist = distance(lat1, lon1, latlng[0], latlng[1], "m");
        setButton(dist);
        distanceText.setText(String.format("Distance from class: %.2f meters", dist));
    }

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private String val;
        private Exception mLastError = null;
        private Object date;
        private Object time;
        private boolean updateSuccess = false;
        private int errorCode = 0;
        private java.text.DateFormat dateFormat;
        private java.text.DateFormat timeFormat;
        MakeRequestTask(GoogleAccountCredential credential) {
            System.out.println("HERE ARE THE CREDS: " + credential);
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Sheets API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Sheets API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                System.out.println(mLastError);
                cancel(true);
                return null;
            }
        }

        /**
         * Grabs the values from the Google Sheet and checks the student-id and key.
         * If both match the sheet, the writeSheet() function is called to update
         * the Google Sheet. Otherwise, the error code is updated depending on the
         * type of error.
         * @return The contents of the Google Sheet
         * @throws IOException if unable to grab data from the Sheet.
         */
        private List<String> getDataFromApi() throws IOException {
            Boolean found = false;
            String spreadsheetId = mSpreadsheetID;
            String range = "Class Data!A2:J32";
            List<String> results = new ArrayList<String>();
            ValueRange response = this.mService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            int count = 1;
            boolean firstRun = true;
            if (values != null) {
                for (List row : values) {
                    if(firstRun && (row.get(9).toString().equals(key))){
                        firstRun = false;
                    } else if (firstRun) {
                        System.out.println("KEY: " + row.get(9).toString());
                        errorCode = 1;
                        break;
                    }
                    count++;
                    val = row.get(0).toString();
                    System.out.println(val);
                    if(val.equals(sid)) {
                        found = true;
                        System.out.println("FOUND");
                        //Grab the current attendance state
                        String currentAtt = row.get(3).toString();
                        System.out.println("FOUND2");
                        //If already checked in, notify and break
                        if(currentAtt.equals("Yes")) {
                            errorCode = 3;
                            break;
                        }
                        //Write to the attendance section of the sheet based on found row
                        rec = row.get(6).toString().trim();
                        name = row.get(1).toString() + " " + row.get(2).toString();
                        System.out.println(rec);
                        String writeRange = "Class Data!D" + count + ":F" + count;
                        writeSheet(spreadsheetId, writeRange);
                        break;
                    }
                }
                if((errorCode == 0) && !found){
                    errorCode = 2;
                }
            }

            return results;
        }

        /**
         * Updates the Google Sheet with the attendance mark, date, and
         * time the user checks in.
         * @param spreadsheetId is the Google Sheet with the attendance roster.
         * @param writeRange contains the row that the function will mark
         *                   the attendance check-in on.
         */
        private void writeSheet(String spreadsheetId, String writeRange){
            System.out.println("Updating Sheet");
            dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            timeFormat = new SimpleDateFormat("hh:mm:ss a");
            Date MM_dd_yyyy = new Date();
            Date HH_mm_ss = new Date();

            Object attending = "Yes";
            date = dateFormat.format(MM_dd_yyyy);
            time = timeFormat.format(HH_mm_ss);
            date_time = date + " at " + time;

            List<List<Object>> setRow = Arrays.asList(
                    Arrays.asList(
                            attending,date,time
                    )
                    // Additional rows ...
            );
            ValueRange body = new ValueRange()
                    .setValues(setRow);
            try {
                UpdateValuesResponse result =
                        mService.spreadsheets().values().update(spreadsheetId, writeRange, body)
                                .setValueInputOption("USER_ENTERED")
                                .execute();
                updateSuccess = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Called before the Async task starts to give a visual
         * notification to the user that the application is contacting
         * the Google Sheets API.
         */
        @Override
        protected void onPreExecute() {
            System.out.println("");
            mProgress.show();
            mSpreadsheetID = getURL();
        }

        /**
         * Called after the Async task starts to take action and notify the user
         * on the result of the check in attempt.
         * @param output indicating the output from the attempted check in.
         *               1 = Invalid Key Entry,
         *               2 = Student ID not Found,
         *               3 = Already Checked In,
         *               else, Check In Successful.
         */
        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if(errorCode == 1){
                toastMessage("Invalid Key!");
            } else if (errorCode == 2){
                toastMessage("Student ID not Found!");
            } else if (errorCode == 3){
                toastMessage("Already Checked in!");
            } else {
                //output.add(0, "Data retrieved using the Google Sheets API:");
                if (updateSuccess) {
                    setMessage(val);
                    toastMessage("Successfully Checked In!\nEmailing Receipt...");
                    Properties props = new Properties();
                    props.put("mail.smtp.host","smtp.gmail.com");
                    props.put("mail.smtp.socketFactory.port", "465");
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.port", "465");

                    session = Session.getDefaultInstance(props, new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication(){
                            return new PasswordAuthentication(mEmail, mEmailPass);
                        }
                    });

                    //pdialog = ProgressDialog.show(MainActivity.this, "", "Sending Mail...", true);
                    new emailReceipt().execute();
                }
            }
        }

        /**
         * Cancels the Google Sheets API call if there is an IO Exception.
         * This occurs due to an incorrect URL entry.
         */
        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    System.out.println("The following error occurred:\n"
                            + mLastError.getMessage());
                    Toast.makeText(MainActivity.this, "Unable to get Google Sheets Data. Did you enter the URL correctly?", Toast.LENGTH_LONG).show();
                }
            } else {
                System.out.println("Request cancelled.");
            }
        }
    }

    /**
     * Called in onPostExecute of the MakeRequest Asynctask upon
     * successful check-in. Puts the student info and check-in info
     * into an email template and updates the global variables.
     */
    private void setMessage(String student_id) {
        textMessage = "Here is your receipt:<br /><br />" +
                "------------------------------------------------<br />" +
                "Name: " + name + "<br />" +
                "Student ID: " + student_id + "<br />" +
                "Checked in on " + date_time + "<br />" +
                "----------------------------------------------------";
        subject = "Subject Line";
    }


}
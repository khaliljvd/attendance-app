package khalil.csc131attendance;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by khalil on 11/23/17.
 */

public class ListActivity extends MainActivity{

    ProgressDialog mProgress;
    Spinner mySpinner;
    private Button mCallApiButton;private ListView listView;
    private String mSection;
    private String rec,name, date_time;
    private Session session;
    private String mClassSize;
    private String mStartTime;
    private String mEndTime;
    private String mCourseKey;
    private String mAppPassword;
    private List<String> mStudentList;
    private static final String[] SCOPES = {SheetsScopes.SPREADSHEETS};
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private String deviceID = "ADMIN";
    ArrayList<String> selectedItems = new ArrayList<>();
    ArrayList<String> mFirstName = new ArrayList<>();
    ArrayList<String> mLastName = new ArrayList<>();
    ArrayList<String> mAttending = new ArrayList<>();
    String[] mFullName = new String[2];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        //mCallApiButton.setBackgroundResource(R.drawable.selector);
        mCallApiButton = (Button) findViewById(R.id.check_in_button);
        mCallApiButton.setBackgroundResource(R.drawable.selector_check_in_admin);
        Button settingsButton = (Button) findViewById(R.id.nav_settings);
        settingsButton.setBackgroundResource(R.drawable.selector_nav_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Intent intent = new Intent(ListActivity.this, SettingsActivity.class);
                startActivity(intent);
                //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        Button CheckinButton = (Button) findViewById(R.id.nav_check_in);
        CheckinButton.setBackgroundResource(R.drawable.selector_nav_check);
        CheckinButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Intent intent = new Intent(ListActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(selectedItems.isEmpty())
                    toastMessage("No students selected!");
                else {
                    Collections.sort(selectedItems, String.CASE_INSENSITIVE_ORDER);
                    mFirstName.clear();
                    mLastName.clear();
                /*
                for (int i = 0; i < listView.getCount(); i++) {
                    item = listView.getChildAt(i);
                    selectedItem = ((TextView) item).getText().toString();
                    System.out.println("Student " + i + ": " + selectedItem);
                    //listView.isItemChecked(i)
                    if(false) {
                        item = listView.getChildAt(i);
                        System.out.println("Getting the name from the list at location: " + i);
                        selectedItem = ((TextView) item).getText().toString();
                        selectedItems.add(selectedItem);
                    }
                }
                */

                    for (String row : selectedItems) {
                        mFirstName.add(row.split(", ")[1]);
                        mLastName.add(row.split(", ")[0]);
                        System.out.println("Checking in " + row);
                    }

                    new MakeRequestTask(mCredential).execute();
                }
            }
        });

        listView = (ListView) findViewById(R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                String selectedItem = ((TextView) view).getText().toString();

                if(listView.isItemChecked(position)){
                    System.out.println("Adding item to list");
                    selectedItems.add(selectedItem);
                } else {
                    System.out.println("Removing item from list");
                    selectedItems.remove(selectedItem);
                }
                /*
                if(selectedItems.contains(selectedItem)) {
                    selectedItems.remove(selectedItem); //remove deselected item from the list of selected items
                    System.out.println("Removing item from list");
                }
                else {
                    selectedItems.add(selectedItem); //add selected item to the list of selected items
                    System.out.println("Adding item to list");
                }
                */
            }
        });

        mySpinner = (Spinner) findViewById(R.id.spinner1);
        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(ListActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array
                .Courses));

        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(myAdapter);
        mySpinner.setAlpha(0.85f);
        listView.setAlpha(0.85f);

        mySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                getResultsFromApi();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Updating Class List ...");

        // Initialize credentials and service object.

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getPermissions();
            return;
        }
        getResultsFromApi();
        //new GetListTask(mCredential).execute();
        startAnimations();
    }
    private void startAnimations() {
        AnimationSet animationSet = new AnimationSet(true);
        final Animation fade_in = AnimationUtils.loadAnimation(this, R.anim.alpha);
        final Animation slide_in = AnimationUtils.loadAnimation(this, R.anim.translate);

        animationSet.addAnimation(fade_in);
        animationSet.addAnimation(slide_in);

        mySpinner.startAnimation(animationSet);
        listView.startAnimation(animationSet);
        mCallApiButton.startAnimation(animationSet);
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
            preventFinish = false;
            new GetGlobalVars(mCredential).execute();
            new GetListTask(mCredential).execute();
        }
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
        preventFinish = true;
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
     * Displays a Toast message to the user.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private void toastMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(!preventFinish)
            finish();
    }

    private class GetGlobalVars extends AsyncTask<Void, Void, Void> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;
        GetGlobalVars(GoogleAccountCredential credential) {
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
        protected Void doInBackground(Void... params) {
            try {
                getDataFromApi();
                return null;
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
        private void getDataFromApi() throws IOException {
            String spreadsheetId = mSpreadsheetID;
            List<String> results = new ArrayList<String>();
            mSection = mySpinner.getSelectedItem().toString();
            String range = mSection + "!B1:E3";

            ValueRange response = this.mService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();

            if (values != null) {
                mClassSize = values.get(0).get(1).toString();
                mStartTime = values.get(0).get(3).toString();
                mEndTime = values.get(1).get(3).toString();
                mCourseKey = values.get(1).get(1).toString();
                mAppPassword = values.get(2).get(3).toString();
            }
        }

        /**
         * Called before the Async task starts to give a visual
         * notification to the user that the application is contacting
         * the Google Sheets API.
         */

        @Override
        protected void onPreExecute(){
            System.out.println("");
            mProgress.show();
            mSpreadsheetID = getURL();
        }

        /**
         * Called after the Async task starts to take action and notify the user
         * on the result of the check in attempt.
         */
        @Override
        protected void onPostExecute(Void params) {
            mProgress.hide();
            System.out.println("Class Size: " + mClassSize);
            System.out.println("Start Time: " + mStartTime);
            System.out.println("End Time: " + mEndTime);
            System.out.println("Course Key: " + mCourseKey);
            System.out.println("Password: " + mAppPassword);
            //storeVariables();
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
                            ListActivity.REQUEST_AUTHORIZATION);
                } else {
                    System.out.println("The following error occurred:\n"
                            + mLastError.getMessage());
                    Toast.makeText(ListActivity.this, "Unable to get Google Sheets Data. Did you enter the URL correctly?", Toast.LENGTH_LONG).show();
                }
            } else {
                System.out.println("Request cancelled.");
            }
        }
    }

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class GetListTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;
        GetListTask(GoogleAccountCredential credential) {
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
            Boolean isUnique;
            String spreadsheetId = mSpreadsheetID;
            List<String> results = new ArrayList<String>();
            mSection = mySpinner.getSelectedItem().toString();
            String range = mSection + "!B6:F" + (mClassSize+5);
            System.out.println(range);
            System.out.println(mSpreadsheetID);
            ValueRange response = this.mService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();

            int count = 1 + 4;
            boolean firstRun = true;
            if (values != null) {
                System.out.println("MADE IT HERE\n");
                for (List row : values) {
                    count++;
                    mAttending.add(row.get(4).toString());
                    results.add(row.get(1) + ", " + row.get(0));
                }
            }
            return results;
        }

        /**
         * Called before the Async task starts to give a visual
         * notification to the user that the application is contacting
         * the Google Sheets API.
         */

        @Override
        protected void onPreExecute(){
            mAttending.clear();
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
            selectedItems.clear();
            mProgress.hide();
            if (output == null || output.size() == 0) {
                System.out.println("No results returned.");
            } else {
                System.out.println("Data retrieved using the Google Sheets API:");
                Collections.sort(output, String.CASE_INSENSITIVE_ORDER);
                buildList(output);
                System.out.println(TextUtils.join("\n", output));
                setChecked();
            }
        }

        private void setChecked(){
            for(int i = 0; i < listView.getCount(); i++){
                System.out.println("Attending: " + mAttending.get(i));
                if(mAttending.get(i).equals("Yes")){
                    listView.setItemChecked(i, true);
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
                            ListActivity.REQUEST_AUTHORIZATION);
                } else {
                    System.out.println("The following error occurred:\n"
                            + mLastError.getMessage());
                    Toast.makeText(ListActivity.this, "Unable to get Google Sheets Data. Did you enter the URL correctly?", Toast.LENGTH_LONG).show();
                }
            } else {
                System.out.println("Request cancelled.");
            }
        }

        private void buildList(List<String> output){
            ArrayAdapter<String> adapter= new ArrayAdapter<String>(ListActivity.this,
                    R.layout.row_layout, output);
            listView.setAdapter(adapter);
        }
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private String fName;
        private String lName;
        private StringBuilder postMessage = new StringBuilder();
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
            mSection = mySpinner.getSelectedItem().toString();
            String range = mSection + "!A5:J" + (mClassSize+5);
            System.out.println(range);
            System.out.println(mSpreadsheetID);
            List<String> results = new ArrayList<String>();
            ValueRange response = this.mService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();

            //if(!isAdmin)
            //isUnique = isUnique(response, values);
            //else
            //   isUnique = true;

            int count = 1 + 4;
            int selIndex = 0;
            boolean getHeaders = true;
            boolean firstRun = true;
            if (values != null) {
                System.out.println("MADE IT HERE\n");
                for (List row : values) {
                    if(getHeaders) {
                        getHeaders = false;
                        continue;
                    }

                    count++;
                    fName = row.get(1).toString();
                    lName = row.get(2).toString();

                    if(selIndex+1 > mFirstName.size())
                        break;

                    if(fName.equals(mFirstName.get(selIndex)) && lName.equals(mLastName.get(selIndex))) {
                        //Grab the current attendance state
                        String currentAtt = row.get(5).toString();
                        //If already checked in, notify and break
                        if(currentAtt.equals("Yes")) {
                            postMessage.append("\n" + mFirstName.get(selIndex) + " " + mLastName.get(selIndex)
                                    + ": " + "\tAlready Checked In");
                            System.out.println("Skipping " + mLastName.get(selIndex) + "...");
                            selIndex++;
                            continue; //Skip this check in
                        }

                        postMessage.append("\n" + mFirstName.get(selIndex) + " " + mLastName.get(selIndex)
                                        + ": " + "\tSuccess");
                        //Write to the attendance section of the sheet based on found row
                        //rec = row.get(7).toString().trim();
                        //name = row.get(1).toString() + " " + row.get(2).toString();
                        //System.out.println(rec);
                        String writeRange = mSection + "!E" + count + ":H" + count;
                        System.out.println("Writing to " + writeRange);
                        writeSheet(spreadsheetId, writeRange);
                        selIndex++;
                    }
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
                            deviceID,attending,date,time
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
            if(isAdmin)
                deviceID = "ADMIN";
            else
                deviceID = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ANDROID_ID);
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
            toastMessage(postMessage.toString());
            if(errorCode == 1){
                toastMessage("Invalid Key!");
            } else if (errorCode == 2){
                toastMessage("Student ID not Found!");
            } else if (errorCode == 3){
                toastMessage("Already Checked in!");
            } else if (errorCode == 4){
                toastMessage("Already Checked in!");
            } else {
                //output.add(0, "Data retrieved using the Google Sheets API:");
                if (updateSuccess) {
                    //setMessage(val);
                    toastMessage(postMessage.toString());
                    //toastMessage("Successfully Checked In!\nEmailing Receipt...");
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
                    //new emailReceipt().execute();
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
                            ListActivity.REQUEST_AUTHORIZATION);
                } else {
                    System.out.println("The following error occurred at line " + mLastError.getStackTrace()[0].getLineNumber() + ":\n"
                            + mLastError.getMessage());
                    //Toast.makeText(ListActivity.this, "Unable to get Google Sheets Data. Did you enter the URL correctly?", Toast.LENGTH_LONG).show();
                }
            } else {
                System.out.println("Request cancelled.");
            }
        }
    }
}

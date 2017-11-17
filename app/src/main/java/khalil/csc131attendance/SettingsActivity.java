package khalil.csc131attendance;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

/**
 * Settings activity for the CSC 131 attendance app.
 * This activity allows the Google Sheet URL to be
 * edited and allows the app to reset the class location
 * to the current location. This is only for testing and
 * demonstration purposes.
 */

public class SettingsActivity extends MainActivity {
    private AutoCompleteTextView url;

    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_settings);
        Button updateLocation = (Button) findViewById(R.id.update_location);
        url = (AutoCompleteTextView) findViewById(R.id.url);
        url.setText(getURL());
        updateLocation.setBackgroundResource(R.drawable.selector_location_reset);

        url.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        updateLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double[] latlng = {locationManager.getLastKnownLocation("gps").getLatitude(),
                        locationManager.getLastKnownLocation("gps").getLongitude()};
                Toast.makeText(SettingsActivity.this, "Setting current location as class location...", Toast.LENGTH_SHORT).show();
                storeSettings((float)latlng[0], (float)latlng[1]);
            }
        });
    }

    /**
     * Sets the latitude and longitude for the classroom in SharedPreferences.
     * If the updateLocation button is clicked, the current location is set as
     * the classroom location.
     * @param latitude indicating the updated latitude of current location.
     * @param longitude indicating the updated longitude of current location.
     */
    private void storeSettings(float latitude, float longitude){
        SharedPreferences mSharedLat = getSharedPreferences("latitude", MODE_PRIVATE);
        SharedPreferences mSharedLong = getSharedPreferences("longitude", MODE_PRIVATE);
        SharedPreferences.Editor mEditLat = mSharedLat.edit();
        SharedPreferences.Editor mEditLong = mSharedLong.edit();
        mEditLat.putFloat("latitude", latitude);
        mEditLong.putFloat("longitude", longitude);
        mEditLat.apply();
        mEditLong.apply();
    }

    /**
     * Sets the URL for the Google Sheet in SharedPreferences. If the value
     * is not changed, it will default to Sheet ID
     * 1KcmLXB7PB4LPOy1GEN-fW86GKE0GSc8hFL7ANfvFuGk.
     */
    private void setURL(){
        SharedPreferences mSharedURL = getSharedPreferences("url", MODE_PRIVATE);
        SharedPreferences.Editor mEditURL = mSharedURL.edit();
        mEditURL.putString("url", url.getText().toString());
        mEditURL.apply();
    }

    /**
     * Gets the URL for the Google Sheet in SharedPreferences. If the value
     * is not changed, it will default to Sheet ID
     * 1KcmLXB7PB4LPOy1GEN-fW86GKE0GSc8hFL7ANfvFuGk.
     */
    private String getURL(){
        SharedPreferences mSharedURL = getSharedPreferences("url", MODE_PRIVATE);
        return mSharedURL.getString("url", mSpreadsheetID);
    }

    /**
     * When the user exits the settings pane, update the URL
     * that was entered in the URL text field.
     */
    @Override
    public void onPause(){
        super.onPause();
        setURL();
    }

    /**
     * Hides the keyboard if the user touches anywhere outside the text fields.
     * @param view indicating the current application view.
     */
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}


package khalil.csc131attendance;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
    private Toast toastObj = null;


    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_settings);
        Button updateLocation = (Button) findViewById(R.id.update_location);
        Button editSheet = (Button) findViewById(R.id.edit_sheet);
        final Button adminMode = (Button) findViewById(R.id.admin_mode);
        final Button password = (Button) findViewById(R.id.password);
        updateLocation.setBackgroundResource(R.drawable.selector_location_reset);
        editSheet.setBackgroundResource(R.drawable.selector_edit_sheet);

        if(isAdmin)
            adminMode.setBackgroundResource(R.drawable.selector_disable_admin_mode);
        else
            adminMode.setBackgroundResource(R.drawable.selector_enable_admin_mode);

        if(isPassDisabled)
            password.setBackgroundResource(R.drawable.selector_enable_pass);
        else
            password.setBackgroundResource(R.drawable.selector_disable_pass);

        Button listButton = (Button) findViewById(R.id.nav_multiple);
        listButton.setBackgroundResource(R.drawable.selector_nav_list);
        listButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Intent intent = new Intent(SettingsActivity.this, ListActivity.class);
                startActivity(intent);
                //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        Button CheckinButton = (Button) findViewById(R.id.nav_check_in);
        CheckinButton.setBackgroundResource(R.drawable.selector_nav_check);
        CheckinButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        url = (AutoCompleteTextView) findViewById(R.id.url);
        url.setText(getURL());
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
                toastMessage("Setting current location as class location...");
                storeSettings((float)latlng[0], (float)latlng[1]);
            }
        });

        editSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toastMessage("Redirecting to Google Sheet...");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(getURL()));
                startActivity(browserIntent);

            }
        });

        adminMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isAdmin) {
                    toastMessage("Disabling Admin Mode...");
                    adminMode.setBackgroundResource(R.drawable.selector_enable_admin_mode);
                    isAdmin = false;
                    storeAdminSettings();
                } else {
                    toastMessage("Enabling Admin Mode...");
                    adminMode.setBackgroundResource(R.drawable.selector_disable_admin_mode);
                    isAdmin = true;
                    storeAdminSettings();
                }
            }
        });

        password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPassDisabled) {
                    toastMessage("Enabling Password Protection...");
                    password.setBackgroundResource(R.drawable.selector_disable_pass);
                    isPassDisabled = false;
                    storePassSettings();
                } else {
                    toastMessage("Disabling Password Protection...");
                    password.setBackgroundResource(R.drawable.selector_enable_pass);
                    isPassDisabled = true;
                    storePassSettings();
                }
            }
        });
        startAnimations(updateLocation, editSheet, adminMode, password);
    }

    private void startAnimations(Button updateLocation, Button editSheet, Button adminMode, Button password) {
        AnimationSet animationSet = new AnimationSet(true);
        final Animation fade_in = AnimationUtils.loadAnimation(this, R.anim.alpha);
        final Animation slide_in = AnimationUtils.loadAnimation(this, R.anim.translate);
        final ImageView disclaimer = (ImageView) findViewById(R.id.disclaimer);
        final ImageView banner = (ImageView) findViewById(R.id.banner);
        final TextView settings = (TextView) findViewById(R.id.textView);
        final TextView url_label = (TextView) findViewById(R.id.textView2);
        //login_form.startAnimation(fade_in);
        animationSet.addAnimation(fade_in);
        animationSet.addAnimation(slide_in);

        settings.startAnimation(fade_in);
        url_label.startAnimation(animationSet);
        url.startAnimation(animationSet);
        updateLocation.startAnimation(animationSet);
        editSheet.startAnimation(animationSet);
        adminMode.startAnimation(animationSet);
        password.startAnimation(animationSet);


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

    private void storeAdminSettings(){
            SharedPreferences mSharedAdmin = getSharedPreferences("admin", MODE_PRIVATE);
            SharedPreferences.Editor mEditAdmin = mSharedAdmin.edit();
            mEditAdmin.putBoolean("admin", isAdmin);
            mEditAdmin.apply();
    }

    private void storePassSettings(){
        SharedPreferences mSharedPass = getSharedPreferences("pass", MODE_PRIVATE);
        SharedPreferences.Editor mEditPass = mSharedPass.edit();
        mEditPass.putBoolean("pass", isPassDisabled);
        mEditPass.apply();
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
        finish();
    }

    /**
     * Hides the keyboard if the user touches anywhere outside the text fields.
     * @param view indicating the current application view.
     */
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void toastMessage(String message){
        if(toastObj!=null)
            toastObj.cancel();
        toastObj = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toastObj.setGravity(Gravity.CENTER_HORIZONTAL,0,800);
        toastObj.show();
    }
}


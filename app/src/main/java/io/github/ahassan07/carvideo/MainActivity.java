package io.github.ahassan07.carvideo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

/** Phone screen: settings, setup steps, and a box for sending an address to the car screen. */
public final class MainActivity extends Activity {
    private static final String CAR_SPEED = "com.google.android.gms.permission.CAR_SPEED";
    private static final int REQUEST_PERMISSIONS = 1;

    private EditText sendInput;
    private EditText homeInput;
    private EditText sitesInput;
    private RadioGroup scaleGroup;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendInput = findViewById(R.id.send_input);
        homeInput = findViewById(R.id.home_input);
        sitesInput = findViewById(R.id.sites_input);
        scaleGroup = findViewById(R.id.scale_group);
        status = findViewById(R.id.status);

        homeInput.setText(Prefs.home(this));
        sitesInput.setText(Prefs.sitesText(this));
        float scale = Prefs.scale(this);
        if (scale < 1.15f) {
            scaleGroup.check(R.id.scale_small);
        } else if (scale < 1.45f) {
            scaleGroup.check(R.id.scale_medium);
        } else {
            scaleGroup.check(R.id.scale_large);
        }

        findViewById(R.id.send_button).setOnClickListener(v -> sendToCar());
        findViewById(R.id.phone_button).setOnClickListener(v -> openOnPhone());
        findViewById(R.id.save_button).setOnClickListener(v -> save());
        findViewById(R.id.perm_button).setOnClickListener(v -> requestPermissions(
                new String[] {
                        CAR_SPEED,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQUEST_PERMISSIONS));
        sendInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                sendToCar();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        showStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        showStatus();
    }

    private void showStatus() {
        boolean speed = checkSelfPermission(CAR_SPEED) == PackageManager.PERMISSION_GRANTED;
        boolean gps = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        String text = getString(CarBrowser.isCarConnected()
                        ? R.string.status_connected : R.string.status_disconnected)
                + "\n" + getString(speed ? R.string.status_speed_yes : R.string.status_speed_no)
                + "\n" + getString(gps ? R.string.status_gps_yes : R.string.status_gps_no);
        status.setText(text);
    }

    private String typedAddress() {
        String url = Prefs.fromInput(sendInput.getText().toString());
        if (url == null) {
            toast(R.string.nothing_to_open);
        }
        return url;
    }

    private void sendToCar() {
        String url = typedAddress();
        if (url == null) {
            return;
        }
        toast(CarBrowser.loadOnCar(url) ? R.string.sent : R.string.not_connected);
        showStatus();
    }

    private void openOnPhone() {
        String url = typedAddress();
        if (url == null) {
            return;
        }
        startActivity(new Intent(this, PhoneBrowserActivity.class)
                .putExtra(PhoneBrowserActivity.EXTRA_URL, url));
    }

    private void save() {
        int checked = scaleGroup.getCheckedRadioButtonId();
        float scale = checked == R.id.scale_small ? Prefs.SCALE_SMALL
                : checked == R.id.scale_large ? Prefs.SCALE_LARGE
                : Prefs.SCALE_MEDIUM;
        Prefs.save(this, homeInput.getText().toString(), sitesInput.getText().toString(), scale);
        homeInput.setText(Prefs.home(this));
        toast(R.string.saved);
    }

    private void toast(int message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}

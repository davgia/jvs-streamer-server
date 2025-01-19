package unibo.JVS.Streamer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import veg.mediacapture.sdk.MediaCapture;
import veg.mediacapture.sdk.MediaCapture.CaptureNotifyCodes;
import veg.mediacapture.sdk.MediaCaptureCallback;
import veg.mediacapture.sdk.MediaCaptureConfig;
import veg.mediacapture.sdk.MediaCaptureConfig.CaptureModes;
import veg.mediacapture.sdk.MediaCaptureConfig.CaptureVideoResolution;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import static veg.mediacapture.sdk.MediaCaptureConfig.*;

public class MainActivity extends Activity implements MediaCaptureCallback {

    private static final String TAG = "JVSStreamer";

    private static final int MY_REQUEST_CODE = 100;
    private static final int MY_OVERLAY_PERMISSION_REQUEST_CODE = 103;

    public static final String MY_SUPPORTED_RES_LIST_ENTRIES = "jvs.extra.supported.res.entries";
    public static final String MY_SUPPORTED_RES_LIST_VALUES = "jvs.extra.supported.res.values";

    public static final String STREAM_STATUS_KEY = "jvs.stream.status.key";
    public static final String INTENT_UPDATE_UI = "jvs.update.ui";

    private SharedPreferences settings = null;

    private MediaCapture capturer = null;
    private boolean misAudioEnabled = true;
    private boolean misSurfaceCreated = false;
    private boolean alreadySendRequest = false;
    private boolean SendingRequest = false;
    private boolean opened = false;

    private ImageView led = null;
    private TextView captureStatusLine1 = null;
    private TextView captureStatusLine2 = null;
    private TextView captureStatusLine3 = null;
    private TextView statusJVS = null;
    private ImageButton buttonRed = null;

    private MulticastLock multicastLock = null;
    private PowerManager.WakeLock mWakeLock;

    private Toast toastShot = null;
    private IntentFilter intentFilter;

    @Override
    @SuppressLint("WifiManagerLeak")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        intentFilter = new IntentFilter();
        intentFilter.addAction(INTENT_UPDATE_UI);

        setTitle(R.string.app_name);

        askPermissions();

        // Prevents the phone to go to sleep mode
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "unibo.rtspserver.mediastream");
        } else {
            Log.d(TAG, "Unable to get wake lock.");
            finish();
        }

        WifiManager wifi = (WifiManager) getSystemService(WIFI_SERVICE);
        if (wifi != null) {
            multicastLock = wifi.createMulticastLock("multicastLock");
            multicastLock.setReferenceCounted(true);
            multicastLock.acquire();
        } else {
            Log.d(TAG, "Unable to get multicast lock.");
            finish();
        }

        setContentView(R.layout.main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        ActionBar bar = getActionBar();
        if (bar != null)
            bar.hide();

        led = findViewById(R.id.led);
        led.setImageResource(R.drawable.led_green);

        captureStatusLine1 = findViewById(R.id.statusRec);
        captureStatusLine1.setText("");
        captureStatusLine2 = findViewById(R.id.statusStat);
        captureStatusLine2.setText("");
        captureStatusLine3 = findViewById(R.id.statusRec2);
        captureStatusLine3.setText("");

        statusJVS = findViewById(R.id.statusJVS);
        statusJVS.setText("");

        /*
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        capturer = findViewById(R.id.captureView);
        loadCapturer();
        */

        ImageButton buttonSettings = findViewById(R.id.imageButtonMenu);
        buttonSettings.setSoundEffectsEnabled(false);
        buttonSettings.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Log.d(TAG,"Settings button pressed!");

                        if (checkPermissions()) {
                            Intent intent;
                            if (isRec()) {
                                Toast.makeText(MainActivity.this, "Press 'Stop Streaming' button  first", Toast.LENGTH_LONG).show();
                            } else {
                                intent = new Intent(MainActivity.this, SettingsActivity.class);

                            /*
                            MediaCaptureConfig config = capturer.getConfig();
                            CharSequence[] resEntries = getResources().getStringArray(R.array.videoResArray);
                            CharSequence[] resEntriesValues = getResources().getStringArray(R.array.videoResValues);
                            ArrayList<CharSequence> resFilteredEntries = new ArrayList<>();
                            ArrayList<CharSequence> resFilteredValues = new ArrayList<>();

                            for (MediaCaptureConfig.CaptureVideoResolution vr : config.getVideoSupportedRes()) {
                                String svr = "" + ((config.getVideoOrientation() == 0) ? config.getVideoWidth(vr) : config.getVideoHeight(vr));

                                for (int i = 0; i < resEntries.length; i++) {
                                    if (resEntriesValues[i].toString().equals(svr)) {
                                        resFilteredEntries.add(resEntries[i]);
                                        resFilteredValues.add(resEntriesValues[i]);
                                        break;
                                    }
                                }
                            }

                            intent.putCharSequenceArrayListExtra(MY_SUPPORTED_RES_LIST_ENTRIES, resFilteredEntries);
                            intent.putCharSequenceArrayListExtra(MY_SUPPORTED_RES_LIST_VALUES, resFilteredValues);
                            */

                                startActivityForResult(intent, 0);
                            }
                        } else {
                            Log.d(TAG, "Unable to start service: some permissions have not been granted");
                            checkPermissions();
                        }
                    }
                }
        );

        buttonRed = findViewById(R.id.button_capture);
        buttonRed.setSoundEffectsEnabled(false);
        buttonRed.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Log.d(TAG,"Recording/stop button pressed!");

                        //if (capturer == null)
                        //    return;

                        if (!isRec()) {

                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                            Intent intent = new Intent(MainActivity.this, CameraService.class);
                            startService(intent);

                            //String sRecStatus = misAudioEnabled ? "00:00" : "00:00 - Audio OFF";
                            String sRecStatus = "00:00";
                            captureStatusLine1.setText(sRecStatus);
                            captureStatusLine3.setText("");
                            captureStatusLine2.setText("");
                            statusJVS.setText("");
                            led.setImageResource(R.drawable.led_red);
                            buttonRed.setImageResource(R.drawable.ic_stop);

                            /*
                           loadConfig();
                           alreadySendRequest = false;
                           capturer.StartStreaming();

                            //start postponed rec
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG,"Capturer starts recording!");
                                    capturer.StartRecording();
                                     if (!alreadySendRequest && !sendingRequest) {
                                        sendAddNewStreamRequest();
                                    }
                                }
                            }, 1000);

                            //start postponed transcoding
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG,"Capturer starts transcoding!");
                                    capturer.StartTranscoding();
                                }
                            }, 2000);
                            */

                        } else {
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                            Intent intent = new Intent(MainActivity.this, CameraService.class);
                            stopService(intent);

                            //capturer.Stop();

                            captureStatusLine1.setText("");
                            captureStatusLine3.setText("");
                            captureStatusLine2.setText("");
                            statusJVS.setText("");
                            led.setImageResource(R.drawable.led_green);
                            buttonRed.setImageResource(R.drawable.ic_fiber_manual_record_red);
                        }
                    }
                }
        );
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG,"onPause");

        unregisterReceiver(receiver);

        if (capturer != null)
            capturer.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG,"onResume");

        registerReceiver(receiver, intentFilter);

        if (capturer != null)
            capturer.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG,"onStart");

        // Lock screen
        mWakeLock.acquire(10000);

        if (capturer != null)
            capturer.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG,"onStop");

        if (capturer != null)
            capturer.onStop();

        statusJVS.setVisibility(View.INVISIBLE);
        statusJVS.setText("");

        // A WakeLock should only be released when isHeld() is true !
        if (mWakeLock.isHeld())
            mWakeLock.release();

        if (toastShot != null)
            toastShot.cancel();

        if (misSurfaceCreated)
            finish();
    }

    @Override
    public void onBackPressed() {

        Log.d(TAG,"onBackPressed");

        if (toastShot != null)
            toastShot.cancel();

        if (capturer != null)
            capturer.Close();

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {

        Log.d(TAG,"onDestroy");

        if (toastShot != null)
            toastShot.cancel();

        if (capturer != null)
            capturer.onDestroy();

        System.gc();

        if (multicastLock != null) {
            multicastLock.release();
            multicastLock = null;
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        Log.d(TAG,"onWindowFocusChanged: " + hasFocus);

        if (capturer != null)
            capturer.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        Log.d(TAG,"onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.d(TAG,"onOptionsItemSelected");

        Intent intent;

        switch (item.getItemId()) {
            case R.id.menu_settings:
                if (isRec()) {
                    Toast.makeText(this, "Press 'Stop Streaming' button  first", Toast.LENGTH_LONG).show();
                } else {
                    intent = new Intent(this.getBaseContext(), SettingsActivity.class);
                    startActivityForResult(intent, 0);
                }
                return true;

            case R.id.menu_exit:
                if (isRec()) {
                    Toast.makeText(this, "Press 'Stop Streaming' button  first", Toast.LENGTH_LONG).show();
                } else {
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG,"onActivityResult");

        if (requestCode != MediaCapture.PERMISSION_CODE) {
            Toast.makeText(this, "Unknown request code: " + requestCode, Toast.LENGTH_SHORT).show();
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "Get media projection with the new permission");

        if (!opened) {
            capturer.SetPermissionRequestResults(resultCode, data);
            capturer.Open(null, this);
            opened = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_REQUEST_CODE: {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission not granted for: " + permissions[i], Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Log.d(TAG, "Permission granted for: " + permissions[i]);
                    }
                }
                break;
            }
            case MY_OVERLAY_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_LONG).show();
                } else {

                    Toast.makeText(this, "Overlay permission denied!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    @Override
    public int OnCaptureStatus(int arg) {
        Log.d(TAG,"OnCaptureStatus: " + arg);

        CaptureNotifyCodes status = CaptureNotifyCodes.forValue(arg);

        if (status != null) {

            String strText = "";

            switch (status) {
                case CAP_OPENED:
                    strText = "Opened";
                    break;
                case CAP_SURFACE_CREATED:
                    strText = "Camera surface created";
                    misSurfaceCreated = true;
                    break;
                case CAP_SURFACE_DESTROYED:
                    strText = "Camera surface destroyed";
                    misSurfaceCreated = false;
                    break;
                case CAP_STARTED:
                    strText = "Started";
                    break;
                case CAP_STOPPED:
                    strText = "Stopped";
                    alreadySendRequest = false;
                    SendingRequest = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            captureStatusLine1.setText("");
                            captureStatusLine3.setText("");
                            captureStatusLine2.setText("");
                            statusJVS.setText("");
                        }
                    });
                    break;
                case CAP_CLOSED:
                    strText = "Closed";
                    break;
                case CAP_ERROR:
                    strText = "Error";
                case CAP_TIME:
                    if (isRec()) {
                        int rtmpStatus = capturer.getRTMPStatus();
                        int rec_status = capturer.getRECStatus();
                        String[] clients = capturer.getRTSPClients(0);
                        int v_cnt = capturer.getVideoPackets();
                        int a_cnt = capturer.getAudioPackets();
                        int nReconnects = capturer.getStatReconnectCount();

                        int dur = (int) (long) capturer.getDuration() / 1000;
                        int min = dur / 60;
                        int sec = dur - (min * 60);

                        String statLine1 = String.format( Locale.ITALY, "%02d:%02d", min, sec);
                        String statLine2 = "";
                        String statLine3 = "";

                        if (!misAudioEnabled) {
                            statLine1 += " - Audio OFF";
                        }

                        if (rtmpStatus != -1) {
                            statLine1 += " - RTSP ON (" + capturer.getRTSPAddr() + ")";
                            statLine2 += "PTS - v:" + v_cnt + " a:" + a_cnt + " rcc:" + nReconnects + " - clients: " + clients.length;

//                            if (!alreadySendRequest && !SendingRequest) {
//                                sendAddNewStreamRequest();
//                            }
                        } else {
                            statLine1 += " - Connecting ...";
                        }

                        if (rec_status != -1) {
                            if (rec_status != 0 && rec_status != -999 && rec_status != -5) {
                                statLine3 += "REC Err: " + rec_status;
                            } else {
                                statLine3 += "Current fps: " + capturer.getRTSPFps(0);
                            }
                        }

                        final String text1 = statLine1;
                        final String text2 = statLine2;
                        final String text3 = statLine3;

                        runOnUiThread(new Runnable() {
                            public void run() {
                                captureStatusLine1.setText(text1);
                                captureStatusLine2.setText(text2);
                                captureStatusLine3.setText(text3);
                            }
                        });
                    }
                    break;
                default:
                    break;
            }

            Log.d(TAG, "onCaptureStatus: " + strText);
        }

        return 0;
    }

    @Override
    public int OnCaptureReceiveData(ByteBuffer buffer, int type, int size, long pts) {
        return 0;
    }

    /**
     * Asks for permissions to user
     */
    private void askPermissions() {

        List permissionToRequest = new ArrayList<String>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionToRequest.add(Manifest.permission.CAMERA);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionToRequest.add(Manifest.permission.RECORD_AUDIO);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (permissionToRequest.size() > 0) {
            String[] arrayRequests = (String[])permissionToRequest.toArray(new String[permissionToRequest.size()]);
            ActivityCompat.requestPermissions(this, arrayRequests, MY_REQUEST_CODE);
        }

        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MY_OVERLAY_PERMISSION_REQUEST_CODE);
            }
        }
    }

    /**
     * Checks permissions
     * @return True if all required permissions are granted.
     */
    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        return Settings.canDrawOverlays(this);
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Loads the capturer if all required permissions are granted
     */
    private void loadCapturer() {

        Log.d(TAG, "loadCapturer");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            capturer = findViewById(R.id.captureView);

            loadConfig();

            capturer.RequestPermission(this);

            if (capturer.getConfig().getCaptureSource() == CaptureSources.PP_MODE_CAMERA.val())
                capturer.Open(null, this);
        }
    }

    /**
     * Sends a POST HTTP request to the JVS service
     */
    @SuppressLint("SetTextI18n")
    public synchronized void sendAddNewStreamRequest() {

        if (SendingRequest) { return;}

        SendingRequest = true;

        Log.d(TAG,"sendAddNewStreamRequest");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusJVS.setVisibility(View.VISIBLE);
                statusJVS.setText("JVS init...");
            }
        });

        Boolean jvssend_needed = settings.getBoolean("jvsEnable", false);
        int jvstype = Integer.parseInt(settings.getString("jvsType", "0"));

        String jvs_server_address = settings.getString("jvsAddress", "");
        int jvs_server_port = Integer.parseInt(settings.getString("jvsPort", "0"));

        if (!jvssend_needed) {
            Log.d(TAG, "Skipped sending request to JVS server.");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusJVS.setText("JVS is disabled");
                }
            });
            return;
        }

        if (jvs_server_address.isEmpty() || jvs_server_port <= 0) {
            Log.d(TAG, "Cannot send request to server if there is no declared url and/or port.");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusJVS.setText("JVS disabled (bad url or port)");
                }
            });
            return;
        }

        Log.d(TAG, "Sending request to JVS server...");

        Date todayDate = Calendar.getInstance().getTime();

        JSONObject o = null;
        try {
            MediaCaptureConfig mcg = capturer.getConfig();

            o = new JSONObject()
                    .put("url", "rtsp://" + getLocalIpAddress() + ":" + mcg.getPort() + "/ch0")
                    .put("encType", jvstype)
                    .put("title", "Dummy stream #" + jvstype + "-" + todayDate.getSeconds());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, "http://" +
                jvs_server_address + ":" + jvs_server_port + "/streams", o, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getInt("status") == 0) {
                        Log.d(TAG, "Succedeed.");
                        alreadySendRequest = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                statusJVS.setText("JVS conversion started");
                            }
                        });
                    } else {
                        Log.d(TAG, "Failed: " + response.get("message"));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                statusJVS.setText("JVS init failed");
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                SendingRequest = false;

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusJVS.setText("");
                    }
                });
                Log.d(TAG, "Failed: " + error.getLocalizedMessage());
                SendingRequest = false;
            }
        });
        queue.add(req);
    }

    /**
     * Gets the local ip address of the device
     * @return The IP Address
     */
    public String getLocalIpAddress() {
        Log.d(TAG,"getLocalIpAddress");

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address && !inetAddress.isAnyLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
        }
        return null;
    }

    /**
     * Determine whether the capturer is recording or not
     * @return True if it's recording; otherwise false.
     */
    public boolean isRec() {
        //return (capturer != null && capturer.getState() == CaptureState.Started);
        return CameraService.isRunning();
    }

    /**
     * Load capturer configuration
     */
    private void loadConfig() {

        Log.d(TAG,"loadConfig");

        if (capturer == null)
            return;

        MediaCaptureConfig config = capturer.getConfig();

        int videoResWidth = Integer.parseInt(settings.getString("videoRes", "1280"));
        int videoBitrate = Integer.parseInt(settings.getString("videoBitrate", "700"));
        int videoFrameRate = Integer.parseInt(settings.getString("videoFps", "30"));

        int audioBitrate = Integer.parseInt(settings.getString("audioBitrate", "64"));
        int audioSampleRate = Integer.parseInt(settings.getString("audioSampling", "44100"));
        int audioChannels = Integer.parseInt(settings.getString("audioChannels", "2"));
        String audioCodec = settings.getString("audioCodec", getString(R.string.settingsDefaultAudioCodec));

        misAudioEnabled = settings.getBoolean("audio_enable", true);

        int streamerType = CaptureModes.PP_MODE_ALL.val();

        if (!misAudioEnabled) {
            streamerType = CaptureModes.PP_MODE_VIDEO.val();
        }

        config.setUseAVSync(true);
        config.setStreaming(true);
        config.setCaptureMode(streamerType);
        config.setStreamType(StreamerTypes.STREAM_TYPE_RTSP_SERVER.val());

        config.setAudioFormat(audioCodec);
        config.setAudioBitrate(audioBitrate);
        config.setAudioSamplingRate(audioSampleRate);
        config.setAudioChannels(audioChannels);

        String rtsp_url = "rtsp://@:" + settings.getString("rtsp_port", "5540");
        config.setUrl(rtsp_url);

        config.setvideoOrientation(0); //landscape

        config.setVideoFramerate(videoFrameRate);
        config.setVideoKeyFrameInterval(1);
        config.setVideoBitrate(videoBitrate);

        List<CaptureVideoResolution> listRes = config.getVideoSupportedRes();
        if (listRes != null && listRes.size() > 0) {
            int w = (config.getVideoOrientation() == 0)? config.getVideoWidth(listRes.get(0)):config.getVideoHeight(listRes.get(0));
            if (w < videoResWidth) {
                videoResWidth = w;
            }
        }

        switch (videoResWidth) {
            case 3840:
                config.setVideoResolution(CaptureVideoResolution.VR_3840x2160);
                break;
            case 1920:
                config.setVideoResolution(CaptureVideoResolution.VR_1920x1080);
                break;
            case 1280:
                config.setVideoResolution(CaptureVideoResolution.VR_1280x720);
                break;
            case 721:
                config.setVideoResolution(CaptureVideoResolution.VR_720x576);
                break;
            case 720:
                config.setVideoResolution(CaptureVideoResolution.VR_720x480);
                break;
            case 640:
                config.setVideoResolution(CaptureVideoResolution.VR_640x480);
                break;
            case 352:
                config.setVideoResolution(CaptureVideoResolution.VR_352x288);
                break;
            case 320:
                config.setVideoResolution(CaptureVideoResolution.VR_320x240);
                break;
            case 176:
                config.setVideoResolution(CaptureVideoResolution.VR_176x144);
                break;
        }

        config.setTranscoding(false);
        config.setRecording(false);
        config.setUseSec(false);
        config.setUseSecRecord(false);

        if (settings.getBoolean("capture_screen", false)) {
            config.setCaptureSource(CaptureSources.PP_MODE_VIRTUAL_DISPLAY.val());
        } else {
            config.setCaptureSource(CaptureSources.PP_MODE_CAMERA.val());
        }
    }

    /**
     * Broadcast receiver to let main activity to receive messages from the background service
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction() != null && intent.getAction().equals(INTENT_UPDATE_UI)) {
                Log.d(TAG, "Received broadcast intent.");

                StreamStatus streamStatus = intent.getParcelableExtra(STREAM_STATUS_KEY);

                int dur = (int) streamStatus.getCurrentTime();
                int min = dur / 60;
                int sec = dur - (min * 60);

                String statLine1 = String.format( Locale.ITALY, "%02d:%02d", min, sec);
                String statLine2 = "";
                String statLine3 = "";

                if (!misAudioEnabled) {
                    statLine1 += " - Audio OFF";
                }

                if (streamStatus.isStreamingStatus() != -1) {
                    statLine1 += " - RTSP ON (" + streamStatus.getAddress() + ")";
                    statLine2 += "PTS - v:" + streamStatus.getSentVideoPackets() + " a:" +
                            streamStatus.getSentAudioPackets() + " rcc:" + streamStatus.getNumberOfReconnection() +
                            " - clients: " + streamStatus.getClients().size();
                } else {
                    statLine1 += " - Connecting ...";
                }

                if (streamStatus.isRecordingStatus() != -1) {
                    if (streamStatus.isRecordingStatus() != 0 &&
                            streamStatus.isRecordingStatus() != -999 &&
                            streamStatus.isRecordingStatus() != -5) {
                        statLine3 += "REC Err: " + streamStatus.isRecordingStatus();
                    } else {
                        statLine3 += "Current fps: " + streamStatus.getCurrentFramerate();
                    }
                }

                captureStatusLine1.setText(statLine1);
                captureStatusLine2.setText(statLine2);
                captureStatusLine3.setText(statLine3);
            }
        }
    };
}

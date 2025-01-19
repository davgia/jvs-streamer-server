package unibo.JVS.Streamer;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import veg.mediacapture.sdk.MediaCapture;
import veg.mediacapture.sdk.MediaCaptureCallback;
import veg.mediacapture.sdk.MediaCaptureConfig;

public class CameraService extends Service implements MediaCaptureCallback {

    public static final String TAG = "JVSService";

    public static boolean started = false;

    private SharedPreferences settings = null;
    private MediaCapture mediaCapture = null;

    private boolean alreadySendRequest = false;
    private boolean sendingRequest = false;

    private RelativeLayout layout;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate ");

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        LayoutInflater inflater =(LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        layout = (RelativeLayout) inflater.inflate(R.layout.dummy_layout,null);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        wm.addView(layout, params);

        mediaCapture = layout.findViewById(R.id.surfaceView_fake);

        loadConfig();

        mediaCapture.RequestPermission(this);

        if (mediaCapture.getConfig().getCaptureSource() == MediaCaptureConfig.CaptureSources.PP_MODE_CAMERA.val())
            mediaCapture.Open(null, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        alreadySendRequest = false;
        loadConfig();
        mediaCapture.StartStreaming();
        started = true;

        //start postponed rec
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mediaCapture.StartRecording();
                if (!alreadySendRequest && !sendingRequest) {
                    sendAddNewStreamRequest();
                }
            }
        }, 1000);

        //start postponed transcoding
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mediaCapture.StartTranscoding();
            }
        }, 2000);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        started = false;

        if (layout != null) {
            layout.removeAllViews();
        }

        if (mediaCapture != null)
            mediaCapture.onDestroy();

        super.onDestroy();
    }

    @Override
    public int OnCaptureStatus(int i) {

        MediaCapture.CaptureNotifyCodes status = MediaCapture.CaptureNotifyCodes.forValue(i);

        if (status != null) {
            switch (status) {
                case CAP_OPENED:
                    Log.d(TAG,"Capture status: Opened");
                    break;
                case CAP_SURFACE_CREATED:
                    Log.d(TAG,"Capture status: Camera surface created");
                    break;
                case CAP_SURFACE_DESTROYED:
                    Log.d(TAG,"Capture status: Camera surface destroyed");
                    break;
                case CAP_STARTED:
                    Log.d(TAG,"Capture status: Started");
                    break;
                case CAP_STOPPED:
                    Log.d(TAG,"Capture status: Stopped");
                    alreadySendRequest = false;
                    sendingRequest = false;
                    break;
                case CAP_CLOSED:
                    Log.d(TAG,"Capture status: Closed");
                    break;
                case CAP_ERROR:
                    Log.d(TAG,"Capture status: Error");
                case CAP_TIME:
                    Log.d(TAG,"Capture status: Time");

                    if (isRec()) {
                        int bitrate = mediaCapture.getRTSPBitRate(0);
                        int rtmpStatus = mediaCapture.getRTMPStatus();
                        int recStatus = mediaCapture.getRECStatus();
                        int videoPackets = mediaCapture.getVideoPackets();
                        int audioPackets = mediaCapture.getAudioPackets();
                        int statReconnectCount = mediaCapture.getStatReconnectCount();
                        int duration = (int) mediaCapture.getDuration() / 1000;
                        int framerate = mediaCapture.getRTSPFps(0);
                        String address = mediaCapture.getRTSPAddr();
                        List<String> clients = Arrays.asList(mediaCapture.getRTSPClients(0));

                        StreamStatus ss = new StreamStatus();
                        ss.setClients(clients);
                        ss.setCurrentBitrate(bitrate);
                        ss.setCurrentFramerate(framerate);
                        ss.setCurrentTime(duration);
                        ss.setNumberOfReconnection(statReconnectCount);
                        ss.setSentAudioPackets(audioPackets);
                        ss.setSentVideoPackets(videoPackets);
                        ss.setRecordingStatus(recStatus);
                        ss.setStreamingStatus(rtmpStatus);
                        ss.setAddress(address);

                        Intent intent = new Intent();
                        intent.putExtra(MainActivity.STREAM_STATUS_KEY, ss);
                        intent.setAction(MainActivity.INTENT_UPDATE_UI);
                        sendBroadcast(intent);
                    }
                    break;
                default:
                    Log.d(TAG,"Capture status: unknown");
                    break;
            }
        }

        return 0;
    }

    @Override
    public int OnCaptureReceiveData(ByteBuffer byteBuffer, int i, int i1, long l) {
        return 0;
    }

    /**
     * Determine whether the capturer is recording or not
     * @return True if it's recording; otherwise false.
     */
    public boolean isRec() {
        return (mediaCapture != null && mediaCapture.getState() == MediaCapture.CaptureState.Started);
    }

    /**
     * Load capturer configuration
     */
    private void loadConfig() {

        if (mediaCapture == null)
            return;

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        MediaCaptureConfig config = mediaCapture.getConfig();

        int videoResWidth = Integer.parseInt(settings.getString("videoRes", "1280"));
        int videoBitrate = Integer.parseInt(settings.getString("videoBitrate", "700"));
        int videoFrameRate = Integer.parseInt(settings.getString("videoFps", "30"));

        int audioBitrate = Integer.parseInt(settings.getString("audioBitrate", "64"));
        int audioSampleRate = Integer.parseInt(settings.getString("audioSampling", "44100"));
        int audioChannels = Integer.parseInt(settings.getString("audioChannels", "2"));
        String audioCodec = settings.getString("audioCodec", getString(R.string.settingsDefaultAudioCodec));

        boolean audioEnabled = settings.getBoolean("audio_enable", true);

        int streamerType = MediaCaptureConfig.CaptureModes.PP_MODE_ALL.val();

        if (!audioEnabled) {
            streamerType = MediaCaptureConfig.CaptureModes.PP_MODE_VIDEO.val();
        }

        config.setUseAVSync(true);
        config.setStreaming(true);
        config.setCaptureMode(streamerType);
        config.setStreamType(MediaCaptureConfig.StreamerTypes.STREAM_TYPE_RTSP_SERVER.val());

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

        List<MediaCaptureConfig.CaptureVideoResolution> listRes = config.getVideoSupportedRes();
        if (listRes != null && listRes.size() > 0) {
            for (MediaCaptureConfig.CaptureVideoResolution vr : listRes) {
                int w = (config.getVideoOrientation() == 0) ? config.getVideoWidth(vr) : config.getVideoHeight(vr);
                if (w < videoResWidth) {
                    videoResWidth = w;
                    break;
                }
            }
        }

        switch (videoResWidth) {
            case 3840:
                config.setVideoResolution(MediaCaptureConfig.CaptureVideoResolution.VR_3840x2160);
                break;
            case 1920:
                config.setVideoResolution(MediaCaptureConfig.CaptureVideoResolution.VR_1920x1080);
                break;
            case 1280:
                config.setVideoResolution(MediaCaptureConfig.CaptureVideoResolution.VR_1280x720);
                break;
            case 721:
                config.setVideoResolution(MediaCaptureConfig.CaptureVideoResolution.VR_720x576);
                break;
            case 720:
                config.setVideoResolution(MediaCaptureConfig.CaptureVideoResolution.VR_720x480);
                break;
            case 640:
                config.setVideoResolution(MediaCaptureConfig.CaptureVideoResolution.VR_640x480);
                break;
            case 352:
                config.setVideoResolution(MediaCaptureConfig.CaptureVideoResolution.VR_352x288);
                break;
            case 320:
                config.setVideoResolution(MediaCaptureConfig.CaptureVideoResolution.VR_320x240);
                break;
            case 176:
                config.setVideoResolution(MediaCaptureConfig.CaptureVideoResolution.VR_176x144);
                break;
        }

        config.setTranscoding(false);
        config.setRecording(false);
        config.setUseSec(false);
        config.setUseSecRecord(false);

        if (settings.getBoolean("capture_screen", false)) {
            config.setCaptureSource(MediaCaptureConfig.CaptureSources.PP_MODE_VIRTUAL_DISPLAY.val());
        } else {
            config.setCaptureSource(MediaCaptureConfig.CaptureSources.PP_MODE_CAMERA.val());
        }
    }

    /**
     * Sends a POST HTTP request to the JVS service
     */
    @SuppressLint("SetTextI18n")
    public void sendAddNewStreamRequest() {

        if (sendingRequest) { return;}

        sendingRequest = true;

        Log.d(TAG, "Try to send request to JVS service...");

        Boolean jvssend_needed = settings.getBoolean("jvsEnable", false);
        int jvstype = Integer.parseInt(settings.getString("jvsType", "0"));

        String jvs_server_address = settings.getString("jvsAddress", "");
        int jvs_server_port = Integer.parseInt(settings.getString("jvsPort", "0"));

        if (!jvssend_needed) {
            Log.d(TAG, "Skipped sending request to JVS service.");
            return;
        }

        if (jvs_server_address.isEmpty() || jvs_server_port <= 0) {
            Log.d(TAG, "Cannot send request to server if there is no declared url and/or port.");
            return;
        }

        Log.d(TAG, "Sending request to JVS server...");

        Date todayDate = Calendar.getInstance().getTime();

        JSONObject o = null;
        try {
            MediaCaptureConfig mcg = mediaCapture.getConfig();

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
                        Log.d(TAG, "JVS service replied with OK.");
                        alreadySendRequest = true;
                    } else {
                        Log.d(TAG, "JVS service replied with: " + response.get("message"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                sendingRequest = false;

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "JVS service request failed: " + error.getLocalizedMessage());
                sendingRequest = false;
            }
        });

        queue.add(req);

    }

    /**
     * Gets the local ip address of the device
     * @return The IP Address
     */
    public String getLocalIpAddress() {
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
     * Determine whether the service is running or not.
     * @return True, if the service is running; otherwise false.
     */
    public static boolean isRunning() { return started; }
}

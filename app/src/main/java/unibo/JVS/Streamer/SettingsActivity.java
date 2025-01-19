package unibo.JVS.Streamer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import java.util.List;

@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity {

    static final public String TAG = "SettingsActivity";
    public static SettingsActivity sThis;

    SharedPreferences settings = null;

    /* RTSP Settings */
    EditTextPreference record_rtsp_port = null;

    /* Streaming settings */
    CheckBoxPreference record_capture_screen = null;
    ListPreference record_videoRes = null;
    ListPreference record_videoBitrate = null;
    ListPreference record_videoFps = null;
    ListPreference record_videoCodec = null;

    CheckBoxPreference record_audio_enable = null;
    ListPreference record_audioBitrate = null;
    ListPreference record_audioSampling = null;
    ListPreference record_audioChannels = null;
    ListPreference record_audioCodec = null;

    /* JVS Settings */
    CheckBoxPreference jvs_enable = null;
    ListPreference jvs_type = null;
    EditTextPreference jvs_port = null;
    EditTextPreference jvs_address = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sThis = this;

        addPreferencesFromResource(R.layout.preferences);
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

            String AppVersion = pInfo.versionName;
            String AppName = (String) getText(R.string.app_name);
            setTitle("Settings " + AppName + " " + AppVersion);
        } catch (NameNotFoundException e1) {
            e1.printStackTrace();
        }

		/* init controls */
        record_rtsp_port = (EditTextPreference) findPreference("rtsp_port");
        record_capture_screen = (CheckBoxPreference) findPreference("capture_screen");
        record_videoCodec = (ListPreference) findPreference("videoCodec");
        record_videoRes = (ListPreference) findPreference("videoRes");
        record_videoBitrate = (ListPreference) findPreference("videoBitrate");
        record_videoFps = (ListPreference) findPreference("videoFps");
        record_audio_enable = (CheckBoxPreference) findPreference("audio_enable");
        record_audioCodec = (ListPreference) findPreference("audioCodec");
        record_audioBitrate = (ListPreference) findPreference("audioBitrate");
        record_audioSampling = (ListPreference) findPreference("audioSampling");
        record_audioChannels = (ListPreference) findPreference("audioChannels");
        jvs_enable = (CheckBoxPreference) findPreference("jvsEnable");
        jvs_type = (ListPreference) findPreference("jvsType");
        jvs_port = (EditTextPreference) findPreference("jvsPort");
        jvs_address = (EditTextPreference) findPreference("jvsAddress");

        /* Filter possible res */
        List<CharSequence> resFilteredEntries = getIntent().getCharSequenceArrayListExtra(MainActivity.MY_SUPPORTED_RES_LIST_ENTRIES);
        List<CharSequence> resFilteredValues = getIntent().getCharSequenceArrayListExtra(MainActivity.MY_SUPPORTED_RES_LIST_VALUES);

        if (resFilteredEntries != null && resFilteredValues != null) {
            record_videoRes.setEntries(resFilteredEntries.toArray(new CharSequence[resFilteredEntries.size()]));
            record_videoRes.setEntryValues(resFilteredValues.toArray(new CharSequence[resFilteredValues.size()]));
        }

        /* Restore values */
        boolean isCaptureScreen = settings.getBoolean("capture_screen", false);
        updateRecordCaptureScreen(isCaptureScreen);

        String videoCodec = settings.getString("videoCodec", getString(R.string.settingsDefaultVideoCodec));
        int videoBitrate = Integer.parseInt(settings.getString("videoBitrate", "700"));
        int videoRes = Integer.parseInt(settings.getString("videoRes", "1280"));
        int videoFps = Integer.parseInt(settings.getString("videoFps", "30"));
        record_videoBitrate.setSummary(videoBitrate + " kbps");
        record_videoRes.setSummary(getStringResolution(videoRes));
        record_videoFps.setSummary(videoFps + " fps");
        record_videoCodec.setSummary(getVideoCodecString(videoCodec));

        boolean isAudioEnabled = settings.getBoolean("audio_enable", true);
        updateRecordAudioEnable(isAudioEnabled);

        String audioCodec = settings.getString("audioCodec", getString(R.string.settingsDefaultAudioCodec));
        int sAudioBitrate = Integer.parseInt(settings.getString("audio_bitrate", "64"));
        int sAudioSamplingRate = Integer.parseInt(settings.getString("audio_sampling", "44100"));
        int sAudioChannels = Integer.parseInt(settings.getString("audio_channels", "2"));
        record_audioBitrate.setSummary(sAudioBitrate + " kbps");
        record_audioSampling.setSummary(sAudioSamplingRate + " Khz");
        record_audioChannels.setSummary(sAudioChannels + " channels");
        record_audioCodec.setSummary(getAudioCodecString(audioCodec));

        int sUrlPort = Integer.parseInt(settings.getString("rtsp_port", "5540"));
        record_rtsp_port.setSummary(sUrlPort + "");

        boolean jvsEnabled = settings.getBoolean("jvsEnable", false);

        int jvsType = Integer.parseInt(settings.getString("jvsType", "0"));
        int jvsPort = Integer.parseInt(settings.getString("jvsPort", "0"));
        String jvsAddress = settings.getString("jvsAddress", "");
        jvs_type.setSummary(getJvsType(jvsType));

        if (jvsPort > 0) {
            jvs_port.setSummary(jvsPort + "");
        }
        if (!jvsAddress.isEmpty()) {
            jvs_address.setSummary(jvsAddress);
        }

        if (jvsEnabled && !jvsAddress.isEmpty() && jvsPort > 0) {
            updateJvsEnable(true);
        }

        /* Reset button */
        final PreferenceScreen button_reset = (PreferenceScreen) findPreference("reset_settings");
        button_reset.bind(getListView());
        button_reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                checkAlertReset();
                return true;
            }

        });

        /* Add onChange listeners */
        record_rtsp_port.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                return true;
            }
        });
        record_capture_screen.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateRecordCaptureScreen((boolean) newValue);
                return true;
            }
        });
        record_videoCodec.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(getVideoCodecString((String)newValue));
                return true;
            }
        });
        record_videoRes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(getStringResolution(Integer.parseInt(newValue.toString())));
                return true;
            }
        });
        record_videoBitrate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue + " kbps");
                return true;
            }
        });
        record_videoFps.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue + " fps");
                return true;
            }
        });
        record_audio_enable.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateRecordAudioEnable((boolean) newValue);
                return true;
            }
        });
        record_audioCodec.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(getAudioCodecString((String)newValue));
                return true;
            }
        });
        record_audioBitrate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue + " kbps");
                return true;
            }
        });
        record_audioSampling.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue + " Khz");
                return true;
            }
        });
        record_audioChannels.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue + " channels");
                return true;
            }
        });
        jvs_enable.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateJvsEnable((boolean) newValue);
                return true;
            }
        });
        jvs_type.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(getJvsType(Integer.parseInt(newValue.toString())));
                return true;
            }
        });
        jvs_port.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue.toString());
                return true;
            }
        });
        jvs_address.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue.toString());
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /* HELPER METHODS */
    private void updateRecordAudioEnable(boolean isEnabled) {
        record_audio_enable.setSummary(isEnabled ? "audio ON" : "audio OFF");
        record_audioBitrate.setEnabled(isEnabled);
        record_audioChannels.setEnabled(isEnabled);
        record_audioSampling.setEnabled(isEnabled);
        record_audioCodec.setEnabled(isEnabled);
    }

    private void updateRecordCaptureScreen(boolean isEnabled) {
        record_capture_screen.setSummary(isEnabled ? "Screen Capture" : "Camera Capture");
    }

    private void updateJvsEnable(boolean isEnabled) {
        jvs_type.setEnabled(isEnabled);
        jvs_port.setEnabled(isEnabled);
        jvs_address.setEnabled(isEnabled);
    }

    private boolean checkAlertReset() {
        String message = "Are you sure reset settings to defaults?";
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage(message)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, "YES");

                        onResetSettings();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, "NO");
                    }
                }).show();

        return true;
    }

    private void onResetSettings() {
        Editor ed = settings.edit();

        ed.clear();
        ed.putString("urlport", "5540");
        ed.putString("jvsType", "0");
        ed.putString("jvsPort", "8081");
        ed.putString("jvsAddress", "");
        ed.apply();

        Intent intent = new Intent(this.getBaseContext(), SettingsActivity.class);
        startActivityForResult(intent, 0);

        finish();
    }

    private String getStringResolution(final int val) {
        String s = "";
        switch (val) {
            case 3840:
                s = "3840x2160";
                break;
            case 1920:
                s = "1920x1080";
                break;
            case 1280:
                s = "1280x720";
                break;
            case 721:
                s = "720x576";
                break;
            case 720:
                s = "720x480";
                break;
            case 640:
                s = "640x480";
                break;
            case 352:
                s = "352x288";
                break;
            case 320:
                s = "320x240";
                break;
            case 176:
                s = "176x144";
                break;
        }
        return s;
    }

    private String getJvsType(final int val) {
        String s = "";
        switch (val) {
            case 0:
                s = "MP4-DASH (H264, AAC)";
                break;
            case 1:
                s = "WEBM-DASH (VP8, Vorbis)";
                break;
            case 2:
                s = "WEBM-DASH (VP9, Opus)";
                break;
        }
        return s;
    }

    private String getVideoCodecString(final String val) {
        switch (val) {
            case "video/avc":
                return "AVC";
            case "video/hevc":
                return "HEVC";
            case "video/x-vnd.on2.vp8":
                return "VP8";
            case "video/x-vnd.on2.vp9":
                return "VP9";
        }
        return "unknown";
    }

    private String getAudioCodecString(final String val) {
        switch (val) {
            case "audio/mp4a-latm":
                return "AAC";
            case "audio/ac3": {
                return "AC3";
            }
            case "audio/flac": {
                return "FLAC";
            }
            case "audio/opus": {
                return "Opus";
            }
            case "audio/vorbis": {
                return "Vorbis";
            }
        }
        return "unknown";
    }
}

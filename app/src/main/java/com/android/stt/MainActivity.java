package com.android.stt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.util.Locale;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    Button mic;
    EditText result;
    TextView cmd;
    TextToSpeech myTts;
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted ) finish();

    }
    private SpeechAPI speechAPI;
    private VoiceRecorder mVoiceRecorder;
    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            if (speechAPI != null) {
                speechAPI.startRecognizing(mVoiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (speechAPI != null) {
                speechAPI.recognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            if (speechAPI != null) {
                speechAPI.finishRecognizing();
                stopVoiceRecorder();
                mic.setClickable(true);
            }
        }

    };

    private final SpeechAPI.Listener mSpeechServiceListener =
            new SpeechAPI.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {
                    if (isFinal) {
                        stopVoiceRecorder();
                    }
                    if (result != null && !TextUtils.isEmpty(text)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinal) {
                                    Toast.makeText(MainActivity.this, "Recognizing Complete!", Toast.LENGTH_SHORT).show();
                                    String value = result.getText().toString();
                                    processResult(value);
                                    Log.d("STT","RECOGNIZED TEXT-->"+value);
                                } else {
                                    result.setText(text);
                                }
                            }
                        });
                    }
                }
            };

    private void processResult(String value) {
        value = value.toLowerCase();

       if(value.contains("open")){
           if(value.contains("whatsapp")){
               speak("Opening Whatsapp");
               Toast.makeText(MainActivity.this, "OPENING WHATSAPP", Toast.LENGTH_LONG).show();
               startNewActivity(MainActivity.this, "com.whatsapp");
           }
           else if(value.contains("browser")||value.contains("chrome")){
               speak("Opening browser");
               Toast.makeText(MainActivity.this, "OPENING BROWSER", Toast.LENGTH_LONG).show();
               startNewActivity(MainActivity.this, "com.android.chrome");
           }
           else if(value.contains("instagram")||value.contains("insta")){
               speak("Opening instagram");
               Toast.makeText(MainActivity.this, "OPENING INSTAGRAM", Toast.LENGTH_LONG).show();
               startNewActivity(MainActivity.this, "com.instagram.android");
           }
       }
       else if(value.contains("exit from app")|| value.contains("close app")){
           speak("Closing App");
           Toast.makeText(MainActivity.this, "EXITING FROM APPLICATION", Toast.LENGTH_LONG).show();
           finishAffinity();
           System.exit(0);
       }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        result=findViewById(R.id.result);

        mic= findViewById(R.id.mic);
        cmd=findViewById(R.id.cmnd);

        ButterKnife.bind(this);
        speechAPI = new SpeechAPI(MainActivity.this);

        myTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                    int result = myTts.setLanguage(Locale.getDefault());
                    if(result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED){
                        Toast.makeText(MainActivity.this, "Device Do not Support Language",Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        myTts.setPitch(1.0f);
                        myTts.setSpeechRate(0.8f);
                        speak("Text To speech ready");
                    }

                }

            }
        });




        mic.setOnClickListener(view -> {

            mic.setClickable(false);
            startVoiceRecorder();
            result.setText(null);
            result.setHint("Recognizing...");
            Log.e("STT","Mic button is Disable");
        });

    }

    private void speak(String text) {

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            myTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
        else
        {
            myTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        speechAPI.addListener(mSpeechServiceListener);
    }
    @Override
    protected void onStop() {
        // Stop Cloud Speech API

        speechAPI.removeListener(mSpeechServiceListener);
        speechAPI.destroy();
        speechAPI = null;

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(myTts==null){
            myTts.stop();
            myTts.shutdown();
        }
        super.onDestroy();
    }

    private void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }
    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
            Log.e("STT", "Voice recorder stopped!");
        }
    }
    public void startNewActivity(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            // We found the activity now start the activity
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            // Bring user to the market or let them choose an app?
            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            context.startActivity(intent);
        }
    }
}
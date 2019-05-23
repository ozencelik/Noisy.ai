package com.zen.noisyai.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cleveroad.audiovisualization.DbmHandler;
import com.cleveroad.audiovisualization.GLAudioVisualizationView;
import com.zen.noisyai.Constant;
import com.zen.noisyai.R;
import com.zen.noisyai.Util;
import com.zen.noisyai.VisualizerHandler;
import com.zen.noisyai.database.DatabaseHelper;
import com.zen.noisyai.database.model.Record;
import com.zen.noisyai.model.AudioChannel;
import com.zen.noisyai.model.AudioSampleRate;
import com.zen.noisyai.model.AudioSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import omrecorder.AudioChunk;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.Recorder;
import pl.bclogic.pulsator4droid.library.PulsatorLayout;

import static com.zen.noisyai.Constant.DATE_FORMAT;

public class MainActivity extends AppCompatActivity
        implements PullTransport.OnAudioChunkPulledListener, MediaPlayer.OnCompletionListener {

    private static final int PERMISSON_CODE = 1240;
    private static final String[] APP_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WAKE_LOCK
    };

    private AudioSource source;
    private AudioChannel channel;
    private AudioSampleRate sampleRate;
    private int color;
    private boolean autoStart;
    private boolean keepDisplayOn;

    private MediaPlayer player;
    private Recorder recorder;
    private VisualizerHandler visualizerHandler;

    private Timer timer;
    private MenuItem listMenuItem;
    private int recorderSecondsElapsed;
    private int playerSecondsElapsed;
    private boolean isRecording;

    private RelativeLayout contentLayout;
    private GLAudioVisualizationView visualizerView;
    private TextView statusView;
    private TextView timerView;
    private ImageButton restartView;
    private ImageButton recordView;
    private ImageButton playView;


    private static ProgressDialog progressDialog;
    private static int NUMBER_OF_DENOISE;
    //private static ImageBadgeView imageBadgeView;
    private static PulsatorLayout imageBadgeView;

    private static boolean IS_DENOISED = false;
    private DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    private static String currentDate;

    private DatabaseHelper db;
    private static double percentage;

    private static boolean IS_INIT_APP = false;



    // Used to load the 'rnnoise_demo' library on application startup.
    static {
        System.loadLibrary("rnnoise_demo");
    }


    /*
    * 1- onPostCreate
    * 2- onResume
    *
    *   Record Button Click
    *   1- toggleRecording
    *   2- stopPlaying
    *   3- resumeRecording
    *
    *   Pause Button Click
    *   1- toggleRecording
    *   2- stopPlaying
    *   3- pauseRecording
    *
    *   X Button Click
    *   1- restartRecording
    *   2- onPause
    *   3- restartRecording
    *   4- onDestroy
    * */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(checkAndRequesttPermission()){
            initApp();
        }
    }


    public void initApp(){

        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(ContextCompat.getColor(this, R.color.colorPrimaryDark)));
        }

        db = new DatabaseHelper(this);

        imageBadgeView = findViewById(R.id.denoise_badge_view);
        imageBadgeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectAudio();
            }
        });
        imageBadgeView.start();

        //imageBadgeView.setBadgeValue(NUMBER_OF_DENOISE);

        source = AudioSource.MIC;
        channel = AudioChannel.STEREO;
        sampleRate = AudioSampleRate.HZ_48000;
        color = ContextCompat.getColor(this, R.color.colorPrimary);
        autoStart = false;
        keepDisplayOn = true;
        /*
        if(savedInstanceState != null) {

        } else {

            filePath = getIntent().getStringExtra(AndroidAudioRecorder.EXTRA_FILE_PATH);
            source = (AudioSource) getIntent().getSerializableExtra(AndroidAudioRecorder.EXTRA_SOURCE);
            channel = (AudioChannel) getIntent().getSerializableExtra(AndroidAudioRecorder.EXTRA_CHANNEL);
            sampleRate = (AudioSampleRate) getIntent().getSerializableExtra(AndroidAudioRecorder.EXTRA_SAMPLE_RATE);
            color = getIntent().getIntExtra(AndroidAudioRecorder.EXTRA_COLOR, Color.BLACK);
            autoStart = getIntent().getBooleanExtra(AndroidAudioRecorder.EXTRA_AUTO_START, false);
            keepDisplayOn = getIntent().getBooleanExtra(AndroidAudioRecorder.EXTRA_KEEP_DISPLAY_ON, false);
        }*/

        if(keepDisplayOn){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setElevation(0);
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(Util.getDarkerColor(color)));
            getSupportActionBar().setHomeAsUpIndicator(
                    ContextCompat.getDrawable(this, R.drawable.aar_ic_clear));
        }

        visualizerView = new GLAudioVisualizationView.Builder(this)
                .setLayersCount(1)
                .setWavesCount(6)
                .setWavesHeight(R.dimen.aar_wave_height)
                .setWavesFooterHeight(R.dimen.aar_footer_height)
                .setBubblesPerLayer(20)
                .setBubblesSize(R.dimen.aar_bubble_size)
                .setBubblesRandomizeSize(true)
                .setBackgroundColor(Util.getDarkerColor(color))
                .setLayerColors(new int[]{color})
                .build();

        contentLayout = findViewById(R.id.content);
        statusView = findViewById(R.id.status);
        timerView = findViewById(R.id.timer);
        restartView = findViewById(R.id.restart);
        recordView = findViewById(R.id.record);
        playView = findViewById(R.id.play);

        contentLayout.setBackgroundColor(Util.getDarkerColor(color));
        contentLayout.addView(visualizerView, 0);
        restartView.setVisibility(View.INVISIBLE);
        playView.setVisibility(View.INVISIBLE);

        if(Util.isBrightColor(color)) {
            ContextCompat.getDrawable(this, R.drawable.aar_ic_clear)
                    .setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            ContextCompat.getDrawable(this, R.drawable.playlist)
                    .setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            statusView.setTextColor(Color.BLACK);
            timerView.setTextColor(Color.BLACK);
            restartView.setColorFilter(Color.BLACK);
            recordView.setColorFilter(Color.BLACK);
            playView.setColorFilter(Color.BLACK);
        }

        IS_INIT_APP = true;
    }

    public String getFileName(){
        currentDate = dateFormat.format(new Date());
        Constant.setInputFileName(currentDate);

        Constant.setOutputFileExtension(1);
        Constant.setOutputFileName(currentDate);

        return Constant.getInputFileName();
    }

    /**
     * Inserting new record in db
     * and refreshing the list
     */
    private long createRecord(Record rec) {
        // inserting note in db and getting
        // newly inserted note id
        long id = db.insertRecord(rec);

        /*
        // get the newly inserted note from db
        Record n = db.getRecord(id);

        if (n != null) {
            Toast.makeText(MainActivity.this, "Succesfully added...", Toast.LENGTH_SHORT).show();
        }*/

        return id;
    }

    //Background Task for Noisy.ai operation
    public class BgTask extends android.os.AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {

            Log.i("AHMET", Constant.getInputFileName());
            Log.i("AHMETT", Constant.getOutputFileName());
            percentage = rnnoise_demo(Constant.getInputFileName(), Constant.getOutputFileName());
            //what = combine(filePath, filePath);

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Record rec = new Record();
            rec.setName(Constant.INPUT_FILE_NAME);
            rec.setFilePath(Constant.getInputFileName());
            rec.setRootFilePath(null);
            rec.setType(0);
            rec.setTimestamp(currentDate);
            long id = createRecord(rec);

            Log.i("ID", ""+id);

            Record rec1 = new Record();
            rec1.setName(Constant.OUTPUT_FILE_NAME);
            rec1.setFilePath(Constant.getOutputFileName());
            rec1.setRootFilePath(rec.getFilePath());
            rec1.setType(1);
            rec1.setPercentage(percentage);
            rec1.setTimestamp(currentDate);
            createRecord(rec1);

            Log.i("AHMETT", ""+percentage);

            NUMBER_OF_DENOISE++;
            //if(imageBadgeView != null) imageBadgeView.setBadgeValue(NUMBER_OF_DENOISE);
            progressDialog.dismiss();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

        }
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public static native double rnnoise_demo(String inputFName, String outputFName);
    public static native boolean combine(String mOne, String mTwo);


    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if(autoStart && !isRecording){
            toggleRecording(null);
        }
        //Toast.makeText(MainActivity.this, "onPostCreate", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            visualizerView.onResume();
            //Toast.makeText(MainActivity.this, "onResume", Toast.LENGTH_SHORT).show();
        } catch (Exception e){ }
    }

    @Override
    protected void onPause() {
        restartRecording(null);
        try {
            visualizerView.onPause();
            //Toast.makeText(MainActivity.this, "onPause", Toast.LENGTH_SHORT).show();
        } catch (Exception e){ }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        restartRecording(null);
        setResult(RESULT_CANCELED);
        try {
            visualizerView.release();
            //Toast.makeText(MainActivity.this, "onDestroy", Toast.LENGTH_SHORT).show();
        } catch (Exception e){ }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.aar_audio_recorder, menu);
        listMenuItem = menu.findItem(R.id.action_list);
        listMenuItem.setIcon(ContextCompat.getDrawable(this, R.drawable.playlist));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            finish();
        } else if (i == R.id.action_list) {
            // launch ignore activity
            startActivity(new Intent(MainActivity.this, ListActivity.class));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAudioChunkPulled(AudioChunk audioChunk) {
        float amplitude = isRecording ? (float) audioChunk.maxAmplitude() : 0f;
        visualizerHandler.onDataReceived(amplitude);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopPlaying();
    }

    private void selectAudio() {
        //Toast.makeText(MainActivity.this, "selectAudio", Toast.LENGTH_SHORT).show();
        IS_DENOISED = true;
        
        stopRecording();
        setResult(RESULT_OK);

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Your sound will be denoised.\nPlease wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new BgTask().execute();
        //restartRecording(null);
        //finish();
    }

    public void toggleRecording(View v) {
        //Toast.makeText(MainActivity.this, "toggleRecording", Toast.LENGTH_SHORT).show();
        stopPlaying();
        Util.wait(100, new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    pauseRecording();
                } else {
                    resumeRecording();
                }
            }
        });
    }

    public void togglePlaying(View v){
        //Toast.makeText(MainActivity.this, "togglePlaying", Toast.LENGTH_SHORT).show();
        pauseRecording();
        Util.wait(100, new Runnable() {
            @Override
            public void run() {
                if(isPlaying()){
                    stopPlaying();
                } else {
                    startPlaying();
                }
            }
        });
    }

    public void restartRecording(View v){
        if(IS_INIT_APP){
            //Toast.makeText(MainActivity.this, "restartRecording", Toast.LENGTH_SHORT).show();
            NUMBER_OF_DENOISE = 0;
            //imageBadgeView.setBadgeValue(NUMBER_OF_DENOISE);
            if(isRecording) {
                stopRecording();
            } else if(isPlaying()) {
                stopPlaying();
            } else {
                visualizerHandler = new VisualizerHandler();
                visualizerView.linkTo(visualizerHandler);
                visualizerView.release();
                if(visualizerHandler != null) {
                    visualizerHandler.stop();
                }
            }
            listMenuItem.setVisible(true);
            if(imageBadgeView != null) imageBadgeView.setVisibility(View.INVISIBLE);
            statusView.setVisibility(View.INVISIBLE);
            restartView.setVisibility(View.INVISIBLE);
            playView.setVisibility(View.INVISIBLE);
            recordView.setImageResource(R.drawable.aar_ic_rec);
            timerView.setText("00:00:00");
            recorderSecondsElapsed = 0;
            playerSecondsElapsed = 0;
        }
    }

    private void resumeRecording() {
        if(IS_INIT_APP){
            //Toast.makeText(MainActivity.this, "resumeRecording", Toast.LENGTH_SHORT).show();
            isRecording = true;
            listMenuItem.setVisible(true);
            if(imageBadgeView != null) imageBadgeView.setVisibility(View.INVISIBLE);
            statusView.setText(R.string.aar_recording);
            statusView.setVisibility(View.VISIBLE);
            restartView.setVisibility(View.INVISIBLE);
            playView.setVisibility(View.INVISIBLE);
            recordView.setImageResource(R.drawable.aar_ic_pause);
            playView.setImageResource(R.drawable.aar_ic_play);

            visualizerHandler = new VisualizerHandler();
            visualizerView.linkTo(visualizerHandler);

            if(recorder == null) {
                timerView.setText("00:00:00");

                recorder = OmRecorder.wav(
                        new PullTransport.Default(Util.getMic(source, channel, sampleRate), MainActivity.this),
                        new File(getFileName()));
            }
            recorder.resumeRecording();

            startTimer();
        }
    }

    private void pauseRecording() {
        if(IS_INIT_APP){
            //Toast.makeText(MainActivity.this, "pauseRecording", Toast.LENGTH_SHORT).show();
            isRecording = false;
            if(!isFinishing()) {
                listMenuItem.setVisible(true);
                if(imageBadgeView != null) imageBadgeView.setVisibility(View.VISIBLE);
            }
            statusView.setText(R.string.aar_paused);
            statusView.setVisibility(View.VISIBLE);
            restartView.setVisibility(View.VISIBLE);
            playView.setVisibility(View.VISIBLE);
            recordView.setImageResource(R.drawable.aar_ic_rec);
            playView.setImageResource(R.drawable.aar_ic_play);

            visualizerView.release();
            if(visualizerHandler != null) {
                visualizerHandler.stop();
            }

            if (recorder != null) {
                recorder.pauseRecording();
            }

            stopTimer();
        }
    }

    private void stopRecording(){
        if(IS_INIT_APP){
            //Toast.makeText(MainActivity.this, "stopRecording", Toast.LENGTH_SHORT).show();
            visualizerView.release();
            if(visualizerHandler != null) {
                visualizerHandler.stop();
            }

            recorderSecondsElapsed = 0;
            if (recorder != null) {
                recorder.stopRecording();
                recorder = null;
            }

            stopTimer();
        }
    }

    private void startPlaying(){
        if(IS_INIT_APP){
            //Toast.makeText(MainActivity.this, "startPlaying", Toast.LENGTH_SHORT).show();
            try {
                stopRecording();
                player = new MediaPlayer();

                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(Constant.getInputFileName());
                    player.setDataSource(fis.getFD());
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    player.prepare();
                }   finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException ignore) {
                        }
                    }

                }
                player.start();

                visualizerView.linkTo(DbmHandler.Factory.newVisualizerHandler(this, player));
                visualizerView.post(new Runnable() {
                    @Override
                    public void run() {
                        player.setOnCompletionListener(MainActivity.this);
                    }
                });

                timerView.setText("00:00:00");
                statusView.setText(R.string.aar_playing);
                statusView.setVisibility(View.VISIBLE);
                playView.setImageResource(R.drawable.aar_ic_stop);

                playerSecondsElapsed = 0;
                startTimer();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void stopPlaying(){
        if(IS_INIT_APP){
            //Toast.makeText(MainActivity.this, "stopPlaying", Toast.LENGTH_SHORT).show();
            statusView.setText("");
            statusView.setVisibility(View.INVISIBLE);
            playView.setImageResource(R.drawable.aar_ic_play);

            visualizerView.release();
            if(visualizerHandler != null) {
                visualizerHandler.stop();
            }

            if(player != null){
                try {
                    player.stop();
                    player.reset();
                } catch (Exception e){ }
            }

            stopTimer();
        }
    }

    private boolean isPlaying(){
        try {
            return player != null && player.isPlaying() && !isRecording;
        } catch (Exception e){
            return false;
        }
    }

    private void startTimer(){
        stopTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateTimer();
            }
        }, 0, 1000);
    }

    private void stopTimer(){
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private void updateTimer() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(isRecording) {
                    recorderSecondsElapsed++;
                    timerView.setText(Util.formatSeconds(recorderSecondsElapsed));
                } else if(isPlaying()){
                    playerSecondsElapsed++;
                    timerView.setText(Util.formatSeconds(playerSecondsElapsed));
                }
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSON_CODE){

            HashMap<String, Integer> permissionResult = new HashMap<>();
            int deniedCount = 0;

            for(int i=0; i<grantResults.length; i++){
                if(grantResults[i] == PackageManager.PERMISSION_DENIED){
                    permissionResult.put(permissions[i], grantResults[i]);
                    deniedCount++;
                }
            }
            if(deniedCount == 0){
                initApp();
            }
        }
    }


    private boolean checkAndRequesttPermission(){

        List<String> listPermissions = new ArrayList<>();
        for(String perm : APP_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED){
                listPermissions.add(perm);
            }
        }

        if(!listPermissions.isEmpty()){
            ActivityCompat.requestPermissions(this,
                    listPermissions.toArray(new String[listPermissions.size()]),
                    PERMISSON_CODE
            );
            return false;
        }
        return true;
    }
}

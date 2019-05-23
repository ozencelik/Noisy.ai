package com.zen.noisyai.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zen.noisyai.Constant;
import com.zen.noisyai.R;
import com.zen.noisyai.adapter.RecordAdapter;
import com.zen.noisyai.database.DatabaseHelper;
import com.zen.noisyai.database.model.Record;
import com.zen.noisyai.utils.MyDividerItemDecoration;
import com.zen.noisyai.utils.RecyclerTouchListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity implements Runnable{


    private RecordAdapter mAdapter;
    private List<Record> recList = new ArrayList<>();
    private CoordinatorLayout coordinatorLayout;
    private RecyclerView recyclerView;
    private TextView noRecordsView;

    //Media Player Variables
    private static MediaPlayer mediaPlayer = new MediaPlayer();
    private SeekBar seekBar;
    private Button buttonPlayPause, dismissButton;
    private TextView txt;
    private boolean wasPlaying = false;

    private static ProgressDialog progressDialog;
    public static Record RECORD;
    private DatabaseHelper db;
    private static double percentage;

    // Used to load the 'rnnoise_demo' library on application startup.
    static {
        System.loadLibrary("rnnoise_demo");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        setupActionBar();

        coordinatorLayout = findViewById(R.id.coordinator_layout);
        recyclerView = findViewById(R.id.recycler_view);
        noRecordsView = findViewById(R.id.empty_notes_view);

        db = new DatabaseHelper(this);

        recList = db.getAllRecords();

        for(Record rec : recList){
            Log.i("ID", ""+rec.getFilePath());
            Log.i("ROOT_ID", ""+rec.getRootFilePath());
        }

        mAdapter = new RecordAdapter(this, recList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(ListActivity.this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new MyDividerItemDecoration(this, LinearLayoutManager.VERTICAL, 16));
        recyclerView.setAdapter(mAdapter);

        toggleEmptyRecords();

        /**
         * On long press on RecyclerView item, open alert dialog
         * with options to choose
         * Edit and Delete
         * */
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, final int position) {

                final String filePath = recList.get(position).getFilePath();

                final Dialog dialog = new Dialog(ListActivity.this);
                dialog.setContentView(R.layout.popup_audios);
                dialog.setTitle("Title...");
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


                buttonPlayPause = dialog.findViewById(R.id.playButton);
                dismissButton = dialog.findViewById(R.id.dismissButton);
                seekBar = dialog.findViewById(R.id.seekbar);
                txt = dialog.findViewById(R.id.audio_name);
                txt.setText(recList.get(position).getName());
                final TextView seekBarHint = dialog.findViewById(R.id.textView);

                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                        seekBarHint.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                        seekBarHint.setVisibility(View.VISIBLE);
                        int x = (int) Math.ceil(progress / 1000f);

                        if (x < 10)
                            seekBarHint.setText("0:0" + x);
                        else
                            seekBarHint.setText("0:" + x);

                        double percent = progress / (double) seekBar.getMax();
                        int offset = seekBar.getThumbOffset();
                        int seekWidth = seekBar.getWidth();
                        int val = (int) Math.round(percent * (seekWidth - 2 * offset));
                        int labelWidth = seekBarHint.getWidth();
                        seekBarHint.setX(offset + seekBar.getX() + val
                                - Math.round(percent * offset)
                                - Math.round(percent * labelWidth / 2));

                        if (progress > 0 && mediaPlayer != null && !mediaPlayer.isPlaying()) {
                            clearMediaPlayer();
                            buttonPlayPause.setBackground(ContextCompat.getDrawable(ListActivity.this, R.drawable.play));
                            ListActivity.this.seekBar.setProgress(0);
                        }

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {


                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            mediaPlayer.seekTo(seekBar.getProgress());
                        }
                    }
                });

                dismissButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        clearMediaPlayer();
                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        clearMediaPlayer();
                    }
                });
                buttonPlayPause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playSong(filePath);
                    }
                });

                dialog.show();


                playSong(filePath);

            }

            @Override
            public void onLongClick(View view, int position) {
                showActionsDialog(position);
            }
        }));
    }



    public void playSong(String filePath) {
        //Get the filePath of the audio and play it.
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                clearMediaPlayer();
                seekBar.setProgress(0);
                wasPlaying = true;
                buttonPlayPause.setBackground(ContextCompat.getDrawable(ListActivity.this, R.drawable.play));
            }
            if (!wasPlaying) {

                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                }

                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(filePath);
                    mediaPlayer.setDataSource(fis.getFD());
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.prepare();
                }   finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException ignore) {
                        }
                    }

                }

                buttonPlayPause.setBackground(ContextCompat.getDrawable(ListActivity.this, R.drawable.pause));

                //mediaPlayer.setDataSource(filePath);

                //mediaPlayer.prepare();
                //mediaPlayer.setVolume(100, 100);
                mediaPlayer.setLooping(false);
                seekBar.setMax(mediaPlayer.getDuration());

                mediaPlayer.start();
                new Thread(this).start();
            }

            wasPlaying = false;
        } catch (Exception e) {
            e.printStackTrace();

        }

    }


    private void clearMediaPlayer() {
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = new MediaPlayer();
    }

    public void run() {
        int currentPosition = mediaPlayer.getCurrentPosition();
        int total = mediaPlayer.getDuration();


        while (mediaPlayer != null && mediaPlayer.isPlaying() && currentPosition < total) {
            try {
                Thread.sleep(1000);
                currentPosition = mediaPlayer.getCurrentPosition();
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                return;
            }

            seekBar.setProgress(currentPosition);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(ListActivity.this, MainActivity.class));
        finish();
    }

    private void setupActionBar() {
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    /**
     * Updating record in db and updating
     * item in the list by its position
     */
    private void updateRecord(String name, int position) {
        Record n = recList.get(position);
        // updating note text
        n.setName(name);

        // updating note in db
        db.updateRecord(n);

        // refreshing the list
        recList.set(position, n);
        mAdapter.notifyItemChanged(position);

        toggleEmptyRecords();
    }

    /**
     * Deleting note from SQLite and removing the
     * item from the list by its position
     */
    private void deleteRecord(int position) {
        // deleting the record from db
        db.deleteRecord(recList.get(position));

        // removing the note from the list
        recList.remove(position);
        mAdapter.notifyItemRemoved(position);

        toggleEmptyRecords();
    }

    /**
     * Opens dialog with Edit - Delete options
     * Edit - 0
     * Delete - 0
     */
    private void showActionsDialog(final int position) {

        RECORD = recList.get(position);
        CharSequence[] colors = new CharSequence[]{"Edit", "Delete"};
        if(RECORD.getType() > 0){
           colors = new CharSequence[]{"Edit", "Delete", "Denoise Again"};
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose option");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showNoteDialog(true, recList.get(position), position);
                } else if (which == 1) {
                    deleteRecord(position);
                } else if (which == 2) {

                    progressDialog = new ProgressDialog(ListActivity.this);
                    progressDialog.setMessage("Your sound will be denoised.\nPlease wait...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    new BgTask().execute();

                }
            }
        });
        builder.show();
    }

    /**
     * Shows alert dialog with EditText options to enter / edit
     * a note.
     * when shouldUpdate=true, it automatically displays old note and changes the
     * button text to UPDATE
     */
    private void showNoteDialog(final boolean shouldUpdate, final Record rec, final int position) {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        View view = layoutInflaterAndroid.inflate(R.layout.records_dialog, null);

        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(ListActivity.this);
        alertDialogBuilderUserInput.setView(view);

        final EditText inputNote = view.findViewById(R.id.note);
        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        dialogTitle.setText(!shouldUpdate ? getString(R.string.lbl_new_note_title) : getString(R.string.lbl_edit_note_title));

        if (shouldUpdate && rec != null) {
            inputNote.setText(rec.getName());
        }
        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton(shouldUpdate ? "update" : "save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {

                    }
                })
                .setNegativeButton("cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                dialogBox.cancel();
                            }
                        });

        final AlertDialog alertDialog = alertDialogBuilderUserInput.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show toast message when no text is entered
                if (TextUtils.isEmpty(inputNote.getText().toString())) {
                    Toast.makeText(ListActivity.this, "Enter note!", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    alertDialog.dismiss();
                }

                // check if user updating note
                if (shouldUpdate && rec != null) {
                    // update note by it's id
                    updateRecord(inputNote.getText().toString(), position);
                } /*else {
                    // create new note
                    createNote(inputNote.getText().toString());
                }*/
            }
        });
    }

    /**
     * Toggling list and empty notes view
     */
    private void toggleEmptyRecords() {
        // you can check notesList.size() > 0

        if (db.getRecordsCount() > 0) {
            noRecordsView.setVisibility(View.GONE);
        } else {
            noRecordsView.setVisibility(View.VISIBLE);
        }
    }

    //Background Task for Noisy.ai operation
     public class BgTask extends android.os.AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Constant.setInputFileName(RECORD.getTimestamp());
            Constant.setOutputFileName(RECORD.getTimestamp());
            Constant.setOutputFileExtension(RECORD.getType() + 1);
            Log.i("AHMET", Constant.getInputFileName());
            Log.i("AHMETT", Constant.getOutputFileName());
            percentage = rnnoise_demo(RECORD.getFilePath(), RECORD.getRootFilePath(), Constant.getOutputFileName());
            Log.i("AHMETT", ""+percentage);
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
            rec.setName(Constant.OUTPUT_FILE_NAME);
            rec.setFilePath(Constant.getOutputFileName());
            rec.setRootFilePath(RECORD.getRootFilePath());
            rec.setType(RECORD.getType() + 1);
            rec.setPercentage(percentage);
            rec.setTimestamp(RECORD.getTimestamp());
            db.insertRecord(rec);

            progressDialog.dismiss();

            recList = db.getAllRecords();
            Log.i("TAGGGGG", recList.get(1).getName());
            mAdapter = new RecordAdapter(getApplicationContext(), recList);
            recyclerView.setAdapter(mAdapter);


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
    public static native double rnnoise_demo(String inputFName, String rootFName, String outputFName);
}

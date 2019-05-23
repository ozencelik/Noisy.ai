package com.zen.noisyai;

import android.os.Environment;

public class Constant {
    public static final String INPUT_FILE_EXTENSION = "(I)";
    public static String OUTPUT_FILE_EXTENSION = "(O)";
    public static String INPUT_FILE_NAME;
    public static String OUTPUT_FILE_NAME;
    public static String FOLDER_NAME = "/Noisy.ai/";
    public static String AUDIO_FILE_PATH = Environment.getExternalStorageDirectory().getPath();
    public static String DOWNLOADS_FILE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";

    public static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";



    public static String getInputFileName() {
        return AUDIO_FILE_PATH + FOLDER_NAME + INPUT_FILE_EXTENSION + INPUT_FILE_NAME;
    }

    public static void setInputFileName(String inputFileName) {
        INPUT_FILE_NAME = inputFileName + ".wav";
    }

    public static String getOutputFileName() {
        return AUDIO_FILE_PATH + FOLDER_NAME + OUTPUT_FILE_EXTENSION + OUTPUT_FILE_NAME;
    }

    public static void setOutputFileName(String outputFileName) {
        OUTPUT_FILE_NAME = outputFileName + ".wav";
    }

    public static void setOutputFileExtension(int type) {
        OUTPUT_FILE_EXTENSION = "(O" + type + ")";
    }

    public static String getFolderName() {
        return FOLDER_NAME;
    }

    public static void setFolderName(String folderName) {
        FOLDER_NAME = folderName;
    }

    public static String getAudioFilePath() {
        return AUDIO_FILE_PATH;
    }

    public static void setAudioFilePath(String audioFilePath) {
        AUDIO_FILE_PATH = audioFilePath;
    }



}

package com.zen.noisyai.database.model;

public class Record {

    public static final String TABLE_NAME = "records";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_FILE_PATH = "path";
    public static final String COLUMN_ROOT_FILE_PATH = "root_file_path";
    public static final String COLUMN_TYPE = "type";// noisy or clean sound
    public static final String COLUMN_PERCENTAGE = "percentage";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private long id;
    private String name;
    private String filePath;
    private String rootFilePath;
    private int type = -1;// -1 -> not determined ||| 0 -> noisy ||| 1 -> clean ||| 2 -> cleaned twice
    private double percentage;
    private String duration;
    private String timestamp;


    // Create table SQL query
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_NAME + " TEXT,"
                    + COLUMN_FILE_PATH + " TEXT,"
                    + COLUMN_ROOT_FILE_PATH + " TEXT,"
                    + COLUMN_TYPE + " INTEGER,"
                    + COLUMN_DURATION + " TEXT,"
                    + COLUMN_PERCENTAGE + " REAL,"
                    + COLUMN_TIMESTAMP + " TEXT"
                    + ")";


    public Record() {
    }

    public Record(long id, String name, String filePath, String rootFilePath, int type, double percentage, String duration, String timestamp) {
        this.id = id;
        this.name = name;
        this.filePath = filePath;
        this.rootFilePath = rootFilePath;
        this.type = type;
        this.percentage = percentage;
        this.duration = duration;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }

    public void setId(long id) {
        this.id = id;
    }

    public String getRootFilePath() {
        return rootFilePath;
    }

    public void setRootFilePath(String rootFilePath) {
        this.rootFilePath = rootFilePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getType() { return type; }

    public void setType(int type) { this.type = type; }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}

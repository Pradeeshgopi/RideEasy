package com.rideeasy.camera;

public class AppConfig {
    // IMPORTANT: Change this to your Windows WiFi IP when network changes!
    public static final String SERVER_URL   = "http://10.115.159.7:3000";
    public static final String CAMERA_URL   = SERVER_URL + "/api/buses/camera";
    public static final String LOGIN_URL    = SERVER_URL + "/api/auth/login";

    // Prefs
    public static final String PREFS_NAME   = "CameraPrefs";
    public static final String KEY_CONDUCTOR = "conductorId";
    public static final String KEY_BUS       = "busNumber";
    public static final String KEY_TOKEN     = "token";

    // Detection
    public static final float CONF_THRESHOLD = 0.45f;  // YOLOv8 confidence
    public static final float LINE_POSITION  = 0.5f;   // Virtual line at 50% of frame height
    public static final int   MODEL_INPUT    = 640;     // YOLOv8n input size
}

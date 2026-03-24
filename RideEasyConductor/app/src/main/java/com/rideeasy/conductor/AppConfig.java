package com.rideeasy.conductor;

public class AppConfig {

    // ─── Network ─────────────────────────────────────────────────────────────
    // IMPORTANT: Change this to your Windows WiFi IP whenever network changes!
    // Get it with: ipconfig  (look for IPv4 under Wi-Fi adapter)
    public static final String SERVER_URL = "http://10.115.159.7:3000";

    // ─── API Endpoints ────────────────────────────────────────────────────────
    public static final String LOGIN_URL      = SERVER_URL + "/api/auth/login";
    public static final String BUS_UPDATE_URL = SERVER_URL + "/api/buses/update";
    public static final String LOCATION_URL   = SERVER_URL + "/api/buses/location";
    public static final String END_SHIFT_URL  = SERVER_URL + "/api/buses/end-shift";

    // ─── Bus Constants ────────────────────────────────────────────────────────
    public static final int TOTAL_SEATS              = 44;
    public static final int CROWD_FREE_THRESHOLD     = 40;   // % below this = FREE
    public static final int CROWD_CROWDED_THRESHOLD  = 75;   // % above this = CROWDED

    // ─── SharedPreferences keys ───────────────────────────────────────────────
    public static final String PREFS_NAME     = "ConductorPrefs";
    public static final String KEY_TOKEN      = "token";
    public static final String KEY_CONDUCTOR  = "conductorId";
    public static final String KEY_BUS        = "busNumber";
    public static final String KEY_PLATE      = "numberPlate";
    public static final String KEY_ROUTE      = "route";
}
package com.rideeasy.passenger;

public class AppConfig {

    // ─── Network ─────────────────────────────────────────────────────────────
    // IMPORTANT: Update this to your Windows WiFi IP (run ipconfig) whenever network changes!
    public static final String SERVER_URL  = "http://10.115.159.7:3000";
    public static final String SOCKET_URL  = SERVER_URL;   // Socket.io real-time

    // ─── API Endpoints ────────────────────────────────────────────────────────
    public static final String BUSES_URL   = SERVER_URL + "/api/buses";
    public static final String BOOK_URL    = SERVER_URL + "/api/bookings/book";

    // ─── Bus Constants ────────────────────────────────────────────────────────
    public static final int TOTAL_SEATS             = 44;
    public static final int CROWD_FREE_THRESHOLD    = 40;   // % → FREE
    public static final int CROWD_CROWDED_THRESHOLD = 75;   // % → CROWDED

    // ─── Routes ───────────────────────────────────────────────────────────────
    // 99A stops (display names)
    public static final String[] STOPS_99A = {
            "Tambaram West", "Tambaram East", "Convent Stop",
            "Mahalakshmi Nagar", "SIVET College", "Pallavan Nagar",
            "Perumbakkam", "Medavakkam", "Sholinganallur"
    };

    // 119 stops (display names)
    public static final String[] STOPS_119 = {
            "Guindy TVK Estate", "Concorde", "Jn. of Race Course Rd",
            "Vijaya Nagar", "IRT Road Jn.", "Kandanchavadi",
            "Thorappakkam", "Karapakkam", "Sholinganallur"
    };

    // 99A fares (per stop, boarding from start)
    public static final int[] FARES_99A = {0, 5, 8, 10, 13, 16, 20, 25, 30};

    // 119 fares
    public static final int[] FARES_119 = {0, 5, 8, 11, 14, 17, 20, 23, 27};
}
package com.rideeasy.conductor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable navigateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate logo card — scale up from centre
        CardView logoCard = findViewById(R.id.logoCard);
        ScaleAnimation scaleAnim = new ScaleAnimation(
                0.4f, 1f, 0.4f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnim.setDuration(500);
        scaleAnim.setFillAfter(true);
        logoCard.startAnimation(scaleAnim);

        // Fade in tagline
        View tagline = findViewById(R.id.tagline);
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(700);
        fadeIn.setStartOffset(350);
        fadeIn.setFillAfter(true);
        tagline.startAnimation(fadeIn);

        // Animate loading dots
        animateDots();

        // Navigate after 2 seconds
        navigateRunnable = () -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        };
        handler.postDelayed(navigateRunnable, 2000);
    }

    private void animateDots() {
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        Runnable cycle = new Runnable() {
            int step = 0;
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;
                step = (step + 1) % 3;
                animateDot(dot1, step == 0);
                animateDot(dot2, step == 1);
                animateDot(dot3, step == 2);
                handler.postDelayed(this, 400);
            }
        };
        handler.postDelayed(cycle, 400);
    }

    private void animateDot(View dot, boolean active) {
        int targetWidth = dpToPx(active ? 24 : 8);
        dot.getLayoutParams().width = targetWidth;
        dot.setAlpha(active ? 1f : 0.3f);
        dot.requestLayout();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Fix: cancel pending callbacks to prevent leak
        if (navigateRunnable != null) {
            handler.removeCallbacks(navigateRunnable);
        }
        handler.removeCallbacksAndMessages(null);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
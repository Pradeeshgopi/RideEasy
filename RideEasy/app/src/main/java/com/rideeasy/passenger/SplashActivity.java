package com.rideeasy.passenger;

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

        CardView logoCard = findViewById(R.id.logoCard);
        ScaleAnimation scaleAnim = new ScaleAnimation(
                0.4f, 1f, 0.4f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnim.setDuration(500);
        scaleAnim.setFillAfter(true);
        logoCard.startAnimation(scaleAnim);

        View tagline = findViewById(R.id.tagline);
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(700);
        fadeIn.setStartOffset(350);
        fadeIn.setFillAfter(true);
        tagline.startAnimation(fadeIn);

        animateDots();

        navigateRunnable = () -> {
            startActivity(new Intent(this, HomeActivity.class));
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
            @Override public void run() {
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
        dot.getLayoutParams().width = (int)((active ? 24 : 8) * getResources().getDisplayMetrics().density);
        dot.setAlpha(active ? 1f : 0.3f);
        dot.requestLayout();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (navigateRunnable != null) handler.removeCallbacks(navigateRunnable);
        handler.removeCallbacksAndMessages(null);
    }
}
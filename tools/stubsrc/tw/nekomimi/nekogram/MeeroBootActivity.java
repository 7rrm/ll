package tw.nekomimi.nekogram;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;

public class MeeroBootActivity extends Activity {

    private static final long DEADLINE_MS = 180 * 1000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ProgressBar progressBar;
    private TextView percentText;
    private TextView appNameText;
    private FrameLayout borderContainer;
    private long bornAt;
    private int currentProgress = 0;
    private ValueAnimator progressAnimator;
    private ValueAnimator rotationAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bornAt = System.currentTimeMillis();
        setupWindow();
        setContentView(createRootView());
        startBorderAnimation();
        handler.post(tick);
    }

    private void setupWindow() {
        Window window = getWindow();
        if (window != null) {
            window.setStatusBarColor(Color.BLACK);
            window.setNavigationBarColor(Color.BLACK);
        }
    }

    private View createRootView() {
        float dp = getResources().getDisplayMetrics().density;
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(android.view.Gravity.CENTER);
        container.setPadding((int)(20*dp), (int)(40*dp), (int)(20*dp), (int)(40*dp));

        // --- الإطار المتحرك (المربع حول الاسم) ---
        borderContainer = new FrameLayout(this);
        int size = (int)(200 * dp);
        FrameLayout.LayoutParams borderLp = new FrameLayout.LayoutParams(size, size);
        borderLp.gravity = android.view.Gravity.CENTER;
        borderContainer.setLayoutParams(borderLp);

        // خلفية الإطار (شفافة مع حدود متحركة)
        GradientDrawable borderDrawable = new GradientDrawable();
        borderDrawable.setShape(GradientDrawable.RECTANGLE);
        borderDrawable.setStroke((int)(4*dp), Color.parseColor("#FFD700"));
        borderDrawable.setColor(Color.TRANSPARENT);
        borderDrawable.setCornerRadius(30 * dp);
        borderContainer.setBackground(borderDrawable);

        // --- اسم التطبيق داخل الإطار ---
        appNameText = new TextView(this);
        appNameText.setText("aRRaS");
        appNameText.setTextColor(Color.WHITE);
        appNameText.setTextSize(32);
        appNameText.setTypeface(Typeface.DEFAULT_BOLD);
        appNameText.setGravity(android.view.Gravity.CENTER);
        FrameLayout.LayoutParams nameLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        nameLp.gravity = android.view.Gravity.CENTER;
        borderContainer.addView(appNameText, nameLp);
        container.addView(borderContainer);

        // --- شريط التقدم ---
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFD700")));
        progressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2C2C30));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int)(6*dp)
        );
        barLp.setMargins(0, (int)(30*dp), 0, (int)(20*dp));
        container.addView(progressBar, barLp);

        // --- النسبة المئوية ---
        percentText = new TextView(this);
        percentText.setText("0%");
        percentText.setTextColor(Color.parseColor("#FFD700"));
        percentText.setTextSize(24);
        percentText.setTypeface(Typeface.DEFAULT_BOLD);
        percentText.setGravity(android.view.Gravity.CENTER);
        container.addView(percentText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // --- نص "جار التحميل" ---
        TextView loadingText = new TextView(this);
        loadingText.setText("جار التحميل...");
        loadingText.setTextColor(Color.parseColor("#AAAAAA"));
        loadingText.setTextSize(18);
        loadingText.setGravity(android.view.Gravity.CENTER);
        loadingText.setTypeface(Typeface.DEFAULT_BOLD);
        loadingText.setPadding(0, (int)(30*dp), 0, 0);
        container.addView(loadingText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        root.addView(container);
        return root;
    }

    private void startBorderAnimation() {
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f);
        rotationAnimator.setDuration(8000);
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.addUpdateListener(animation -> {
            float angle = (float) animation.getAnimatedValue();
            if (borderContainer != null) {
                borderContainer.setRotation(angle);
            }
        });
        rotationAnimator.start();

        // تغيير لون الإطار بشكل دوري
        ValueAnimator colorAnim = ValueAnimator.ofInt(Color.parseColor("#FFD700"), Color.parseColor("#FF6B9D"), Color.parseColor("#00D4FF"), Color.parseColor("#FFD700"));
        colorAnim.setDuration(6000);
        colorAnim.setRepeatCount(ValueAnimator.INFINITE);
        colorAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        colorAnim.addUpdateListener(animation -> {
            int color = (int) animation.getAnimatedValue();
            GradientDrawable drawable = (GradientDrawable) borderContainer.getBackground();
            if (drawable != null) {
                drawable.setStroke((int)(4 * getResources().getDisplayMetrics().density), color);
            }
        });
        colorAnim.start();
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            try {
                File dir = new File(getFilesDir(), "vaultdex");
                if (new File(dir, ".done").exists() || System.currentTimeMillis() - bornAt > DEADLINE_MS) {
                    finishSmoothly();
                    return;
                }

                String prep = readSmall(new File(dir, ".prep"));
                int newProgress = 0;
                if (prep.length() > 0) {
                    try {
                        newProgress = Integer.parseInt(prep.trim());
                    } catch (Throwable t) {}
                } else {
                    long elapsed = System.currentTimeMillis() - bornAt;
                    newProgress = (int) Math.min(95, (elapsed * 100) / DEADLINE_MS);
                }
                newProgress = Math.max(0, Math.min(100, newProgress));

                if (newProgress != currentProgress) {
                    if (progressAnimator != null) progressAnimator.cancel();
                    progressAnimator = ValueAnimator.ofInt(currentProgress, newProgress);
                    progressAnimator.setDuration(200);
                    progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                    progressAnimator.addUpdateListener(anim -> {
                        int val = (int) anim.getAnimatedValue();
                        progressBar.setProgress(val);
                        percentText.setText(val + "%");
                    });
                    progressAnimator.start();
                    currentProgress = newProgress;
                }
            } catch (Throwable ignored) {}
            handler.postDelayed(this, 150);
        }
    };

    private void finishSmoothly() {
        try {
            overridePendingTransition(0, android.R.anim.fade_out);
            finish();
        } catch (Throwable ignored) {}
    }

    @Override
    public void onBackPressed() {}

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(tick);
        if (progressAnimator != null) progressAnimator.cancel();
        if (rotationAnimator != null) rotationAnimator.cancel();
        super.onDestroy();
    }

    private static String readSmall(File f) {
        FileInputStream in = null;
        try {
            if (!f.exists()) return "";
            in = new FileInputStream(f);
            byte[] b = new byte[8];
            int n = in.read(b);
            return n <= 0 ? "" : new String(b, 0, n, "UTF-8").trim();
        } catch (Throwable t) {
            return "";
        } finally {
            if (in != null) {
                try { in.close(); } catch (Throwable ignored) {}
            }
        }
    }
}

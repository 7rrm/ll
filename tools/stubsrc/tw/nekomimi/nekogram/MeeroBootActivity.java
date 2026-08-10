package tw.nekomimi.nekogram;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MeeroX Ultra Premium - دوائر شفافة متحركة في الخلفية
 * تصميم مستوحى من Apple و Google Pixel و Nothing
 */
public class MeeroBootActivity extends Activity {

    // نظام ألوان متطور - أزرق بنفسجي مع تدرجات
    private static final int PRIMARY = 0xFF6C63FF;
    private static final int PRIMARY_DARK = 0xFF4A47A3;
    private static final int PRIMARY_LIGHT = 0xFF8B83FF;
    private static final int PRIMARY_GLOW = 0xFF3D3A7A;
    private static final int ACCENT = 0xFFA8A3FF;
    private static final int DARK_BG = 0xFF0F0E1A;
    
    private static final long DEADLINE_MS = 180 * 1000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<CircleView> floatingCircles = new ArrayList<>();
    
    // عناصر الواجهة
    private View gradientBackground;
    private FrameLayout circlesContainer;
    private View logoContainer;
    private View ringGlow;
    private TextView brandText;
    private TextView percentageText;
    private TextView loadingText;
    private TextView dotsText;
    private ProgressBar progressBar;
    private View glowEffect;
    
    private boolean isArabic;
    private long bornAt;
    private int currentProgress = 0;
    private int dotCount = 0;
    private AnimatorSet logoAnim;
    private ValueAnimator progressAnim;
    private ObjectAnimator glowAnim;
    private Handler dotsHandler = new Handler(Looper.getMainLooper());
    private List<Animator> circleAnimators = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bornAt = System.currentTimeMillis();
        
        detectLanguage();
        setupImmersiveWindow();
        setContentView(createUltraLayout());
        
        startAllAnimations();
        handler.post(updateProgress);
        startDotsAnimation();
    }

    private void detectLanguage() {
        try {
            isArabic = "ar".equals(getResources().getConfiguration().locale.getLanguage());
        } catch (Throwable t) {
            isArabic = false;
        }
    }

    private void setupImmersiveWindow() {
        Window window = getWindow();
        if (window != null) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                );
            }
        }
    }

    private View createUltraLayout() {
        FrameLayout root = new FrameLayout(this);
        float dp = getResources().getDisplayMetrics().density;
        
        // 1. الخلفية المتدرجة
        gradientBackground = createGradientBackground();
        root.addView(gradientBackground, createMatchParams());
        
        // 2. حاوية الدوائر العائمة
        circlesContainer = new FrameLayout(this);
        root.addView(circlesContainer, createMatchParams());
        
        // 3. إنشاء الدوائر الشفافة المتحركة
        createFloatingCircles(dp);
        
        // 4. تأثير التوهج المركزي
        glowEffect = createCentralGlow(dp);
        root.addView(glowEffect, createMatchParams());
        
        // 5. المحتوى الرئيسي
        FrameLayout content = createContentLayout(dp);
        root.addView(content, createMatchParams());
        
        return root;
    }

    private View createGradientBackground() {
        View bg = new View(this);
        GradientDrawable gradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{DARK_BG, PRIMARY_GLOW, DARK_BG}
        );
        gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        bg.setBackground(gradient);
        return bg;
    }

    private void createFloatingCircles(float dp) {
        // دوائر بأحجام وشفافيات مختلفة
        int[] sizes = {120, 180, 250, 90, 150, 200, 300, 100, 160, 220};
        float[] alphas = {0.03f, 0.05f, 0.04f, 0.06f, 0.03f, 0.05f, 0.02f, 0.04f, 0.06f, 0.03f};
        int[] colors = {PRIMARY, PRIMARY_LIGHT, ACCENT, PRIMARY_DARK, PRIMARY};
        
        for (int i = 0; i < 10; i++) {
            CircleView circle = new CircleView(this);
            int size = (int)(sizes[i] * dp);
            circle.setSize(size);
            circle.setAlpha(alphas[i]);
            circle.setColor(colors[i % colors.length]);
            
            // مواقع عشوائية
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.leftMargin = (int)(Math.random() * getResources().getDisplayMetrics().widthPixels);
            params.topMargin = (int)(Math.random() * getResources().getDisplayMetrics().heightPixels);
            params.rightMargin = -size / 2;
            params.bottomMargin = -size / 2;
            
            circlesContainer.addView(circle, params);
            floatingCircles.add(circle);
        }
    }

    private View createCentralGlow(float dp) {
        View glow = new View(this);
        GradientDrawable glowDrawable = new GradientDrawable();
        glowDrawable.setShape(GradientDrawable.OVAL);
        glowDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        glowDrawable.setColors(new int[]{
            ColorUtils.setAlphaComponent(PRIMARY_LIGHT, 20),
            ColorUtils.setAlphaComponent(PRIMARY, 10),
            Color.TRANSPARENT
        });
        glowDrawable.setGradientRadius((int)(600 * dp));
        glow.setBackground(glowDrawable);
        return glow;
    }

    private FrameLayout createContentLayout(float dp) {
        FrameLayout content = new FrameLayout(this);
        
        // حاوية عمودية
        LinearLayout verticalLayout = new LinearLayout(this);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);
        verticalLayout.setGravity(Gravity.CENTER);
        verticalLayout.setPadding(0, 0, 0, (int)(30 * dp));
        
        // ---- قسم الشعار ----
        LinearLayout logoSection = createLogoSection(dp);
        verticalLayout.addView(logoSection);
        
        // ---- النسبة المئوية ----
        percentageText = createPercentageText(dp);
        LinearLayout.LayoutParams pctParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        pctParams.topMargin = (int)(15 * dp);
        pctParams.gravity = Gravity.CENTER;
        verticalLayout.addView(percentageText, pctParams);
        
        // ---- شريط التقدم ----
        progressBar = createProgressBar(dp);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
            (int)(220 * dp),
            (int)(3 * dp)
        );
        barParams.topMargin = (int)(12 * dp);
        barParams.gravity = Gravity.CENTER;
        verticalLayout.addView(progressBar, barParams);
        
        // ---- نص التحميل مع النقاط ----
        LinearLayout loadingSection = createLoadingSection(dp);
        LinearLayout.LayoutParams loadingParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        loadingParams.topMargin = (int)(35 * dp);
        loadingParams.gravity = Gravity.CENTER;
        verticalLayout.addView(loadingSection, loadingParams);
        
        content.addView(verticalLayout, createMatchParams());
        
        return content;
    }

    private LinearLayout createLogoSection(float dp) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        
        // حاوية الشعار
        FrameLayout logoWrapper = new FrameLayout(this);
        
        // حلقة توهج خارجية متحركة
        ringGlow = new View(this);
        GradientDrawable ringDrawable = new GradientDrawable();
        ringDrawable.setShape(GradientDrawable.OVAL);
        ringDrawable.setStroke((int)(2 * dp), ColorUtils.setAlphaColor(PRIMARY_LIGHT, 60));
        ringDrawable.setColor(Color.TRANSPARENT);
        ringDrawable.setSize((int)(140 * dp), (int)(140 * dp));
        ringGlow.setBackground(ringDrawable);
        
        // حلقة ثانية داخلية
        View innerRing = new View(this);
        GradientDrawable innerRingDrawable = new GradientDrawable();
        innerRingDrawable.setShape(GradientDrawable.OVAL);
        innerRingDrawable.setStroke((int)(1.5f * dp), ColorUtils.setAlphaColor(ACCENT, 30));
        innerRingDrawable.setColor(Color.TRANSPARENT);
        innerRingDrawable.setSize((int)(120 * dp), (int)(120 * dp));
        innerRing.setBackground(innerRingDrawable);
        
        // الشعار الرئيسي
        logoContainer = new View(this);
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setShape(GradientDrawable.OVAL);
        logoBg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        logoBg.setColors(new int[]{PRIMARY, PRIMARY_DARK});
        logoBg.setSize((int)(90 * dp), (int)(90 * dp));
        logoContainer.setBackground(logoBg);
        logoContainer.setElevation(20 * dp);
        logoContainer.setTranslationZ(20 * dp);
        
        // حرف K
        TextView logoText = new TextView(this);
        logoText.setText("K");
        logoText.setTextColor(Color.WHITE);
        logoText.setTextSize(44);
        logoText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        logoText.setGravity(Gravity.CENTER);
        logoText.setShadowLayer(20, 0, 0, ColorUtils.setAlphaColor(PRIMARY, 120));
        
        // إضافة العناصر
        FrameLayout logoInner = new FrameLayout(this);
        logoInner.addView(logoContainer, new FrameLayout.LayoutParams(
            (int)(90 * dp), (int)(90 * dp)
        ));
        logoInner.addView(logoText, new FrameLayout.LayoutParams(
            (int)(90 * dp), (int)(90 * dp)
        ));
        
        logoWrapper.addView(ringGlow, new FrameLayout.LayoutParams(
            (int)(140 * dp), (int)(140 * dp)
        ));
        logoWrapper.addView(innerRing, new FrameLayout.LayoutParams(
            (int)(120 * dp), (int)(120 * dp)
        ));
        logoWrapper.addView(logoInner, new FrameLayout.LayoutParams(
            (int)(90 * dp), (int)(90 * dp)
        ));
        
        container.addView(logoWrapper);
        
        // اسم التطبيق مع توهج
        brandText = new TextView(this);
        brandText.setText("aRRaS");
        brandText.setTextColor(Color.WHITE);
        brandText.setTextSize(30);
        brandText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        brandText.setGravity(Gravity.CENTER);
        brandText.setLetterSpacing(0.15f);
        brandText.setShadowLayer(12, 0, 0, ColorUtils.setAlphaColor(PRIMARY, 100));
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        brandParams.topMargin = (int)(12 * dp);
        container.addView(brandText, brandParams);
        
        return container;
    }

    private TextView createPercentageText(float dp) {
        TextView pct = new TextView(this);
        pct.setText("0%");
        pct.setTextColor(PRIMARY_LIGHT);
        pct.setTextSize(20);
        pct.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        pct.setGravity(Gravity.CENTER);
        pct.setShadowLayer(10, 0, 0, ColorUtils.setAlphaColor(PRIMARY, 80));
        return pct;
    }

    private ProgressBar createProgressBar(float dp) {
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PRIMARY_LIGHT));
            bar.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(ColorUtils.setAlphaColor(Color.WHITE, 15))
            );
        }
        return bar;
    }

    private LinearLayout createLoadingSection(float dp) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        
        // أيقونة تحميل صغيرة
        View dot = new View(this);
        GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setShape(GradientDrawable.OVAL);
        dotDrawable.setColor(PRIMARY_LIGHT);
        dot.setBackground(dotDrawable);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
            (int)(6 * dp), (int)(6 * dp)
        );
        dotParams.rightMargin = (int)(10 * dp);
        container.addView(dot, dotParams);
        
        // نص التحميل
        loadingText = new TextView(this);
        loadingText.setText(isArabic ? "جارٍ التحميل" : "Loading");
        loadingText.setTextColor(ColorUtils.setAlphaColor(Color.WHITE, 200));
        loadingText.setTextSize(16);
        loadingText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.MEDIUM));
        container.addView(loadingText);
        
        // النقاط المتحركة
        dotsText = new TextView(this);
        dotsText.setText("...");
        dotsText.setTextColor(PRIMARY_LIGHT);
        dotsText.setTextSize(16);
        dotsText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        container.addView(dotsText);
        
        return container;
    }

    private void startAllAnimations() {
        startCircleAnimations();
        startLogoAnimation();
        startGlowAnimation();
        startProgressAnimation();
    }

    private void startCircleAnimations() {
        for (int i = 0; i < floatingCircles.size(); i++) {
            CircleView circle = floatingCircles.get(i);
            
            // حركة أفقية
            float startX = 0;
            float endX = (float)(Math.random() * 600 - 300);
            ObjectAnimator moveX = ObjectAnimator.ofFloat(circle, "translationX", startX, endX);
            moveX.setDuration(8000 + (int)(Math.random() * 4000));
            moveX.setRepeatCount(ValueAnimator.INFINITE);
            moveX.setRepeatMode(ValueAnimator.REVERSE);
            moveX.setInterpolator(new AccelerateDecelerateInterpolator());
            
            // حركة عمودية
            float startY = 0;
            float endY = (float)(Math.random() * 600 - 300);
            ObjectAnimator moveY = ObjectAnimator.ofFloat(circle, "translationY", startY, endY);
            moveY.setDuration(10000 + (int)(Math.random() * 5000));
            moveY.setRepeatCount(ValueAnimator.INFINITE);
            moveY.setRepeatMode(ValueAnimator.REVERSE);
            moveY.setInterpolator(new AccelerateDecelerateInterpolator());
            
            // دوران
            ObjectAnimator rotation = ObjectAnimator.ofFloat(circle, "rotation", 0f, 360f);
            rotation.setDuration(15000 + (int)(Math.random() * 10000));
            rotation.setRepeatCount(ValueAnimator.INFINITE);
            rotation.setInterpolator(new LinearInterpolator());
            
            // تغيير الحجم (نبض)
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(circle, "scaleX", 1f, 1.3f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(circle, "scaleY", 1f, 1.3f, 1f);
            scaleX.setDuration(3000 + (int)(Math.random() * 2000));
            scaleY.setDuration(3000 + (int)(Math.random() * 2000));
            scaleX.setRepeatCount(ValueAnimator.INFINITE);
            scaleY.setRepeatCount(ValueAnimator.INFINITE);
            scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
            
            // تغيير الشفافية
            ObjectAnimator alpha = ObjectAnimator.ofFloat(circle, "alpha", 0.02f, 0.08f, 0.02f);
            alpha.setDuration(4000 + (int)(Math.random() * 3000));
            alpha.setRepeatCount(ValueAnimator.INFINITE);
            alpha.setRepeatMode(ValueAnimator.REVERSE);
            
            AnimatorSet circleAnim = new AnimatorSet();
            circleAnim.playTogether(moveX, moveY, rotation, scaleX, scaleY, alpha);
            circleAnim.start();
            
            circleAnimators.add(circleAnim);
        }
    }

    private void startLogoAnimation() {
        logoAnim = new AnimatorSet();
        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logoContainer, "scaleX", 0.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logoContainer, "scaleY", 0.2f, 1f);
        scaleX.setDuration(800);
        scaleY.setDuration(800);
        scaleX.setInterpolator(new DecelerateInterpolator());
        scaleY.setInterpolator(new DecelerateInterpolator());
        
        ObjectAnimator alpha = ObjectAnimator.ofFloat(logoContainer, "alpha", 0f, 1f);
        alpha.setDuration(600);
        
        ObjectAnimator rotation = ObjectAnimator.ofFloat(logoContainer, "rotation", -20f, 0f);
        rotation.setDuration(700);
        rotation.setInterpolator(new DecelerateInterpolator());
        
        logoAnim.playTogether(scaleX, scaleY, alpha, rotation);
        logoAnim.start();
    }

    private void startGlowAnimation() {
        if (ringGlow != null) {
            glowAnim = ObjectAnimator.ofFloat(ringGlow, "alpha", 0.1f, 0.8f, 0.1f);
            glowAnim.setDuration(2000);
            glowAnim.setRepeatCount(ValueAnimator.INFINITE);
            glowAnim.setRepeatMode(ValueAnimator.REVERSE);
            glowAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            glowAnim.start();
        }
    }

    private void startProgressAnimation() {
        progressAnim = ValueAnimator.ofFloat(0f, 1f);
        progressAnim.setDuration(1000);
        progressAnim.setRepeatCount(ValueAnimator.INFINITE);
        progressAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnim.start();
    }

    private void startDotsAnimation() {
        dotsHandler.post(new Runnable() {
            @Override
            public void run() {
                dotCount = (dotCount + 1) % 4;
                StringBuilder dots = new StringBuilder();
                for (int i = 0; i < dotCount; i++) {
                    dots.append(".");
                }
                dotsText.setText(dots.toString());
                dotsHandler.postDelayed(this, 350);
            }
        });
    }

    private final Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            try {
                File dir = new File(getFilesDir(), "vaultdex");
                
                if (new File(dir, ".done").exists() || 
                    System.currentTimeMillis() - bornAt > DEADLINE_MS) {
                    finishWithAnimation();
                    return;
                }
                
                String progressStr = readSmall(new File(dir, ".prep"));
                if (progressStr.length() > 0) {
                    try {
                        currentProgress = Integer.parseInt(progressStr.trim());
                        currentProgress = Math.max(0, Math.min(100, currentProgress));
                    } catch (Throwable t) {
                        currentProgress = 0;
                    }
                }
                
                updateUI(currentProgress);
                
            } catch (Throwable ignored) {
            }
            
            handler.postDelayed(this, 80);
        }
    };

    private void updateUI(int progress) {
        if (percentageText != null) {
            percentageText.setText(progress + "%");
            
            int color;
            if (progress < 30) {
                color = PRIMARY_LIGHT;
            } else if (progress < 60) {
                color = ColorUtils.blendARGB(PRIMARY_LIGHT, ACCENT, 0.5f);
            } else if (progress < 90) {
                color = ACCENT;
            } else {
                color = Color.WHITE;
            }
            percentageText.setTextColor(color);
        }
        
        if (progressBar != null) {
            ObjectAnimator.ofInt(progressBar, "progress", progress)
                .setDuration(300)
                .start();
        }
        
        if (loadingText != null && progress >= 100) {
            loadingText.setText(isArabic ? "✓ تم التحميل" : "✓ Loaded");
            loadingText.setTextColor(PRIMARY_LIGHT);
            dotsText.setVisibility(View.GONE);
        }
    }

    private void finishWithAnimation() {
        handler.removeCallbacks(updateProgress);
        dotsHandler.removeCallbacksAndMessages(null);
        if (logoAnim != null) logoAnim.cancel();
        if (glowAnim != null) glowAnim.cancel();
        if (progressAnim != null) progressAnim.cancel();
        for (Animator anim : circleAnimators) {
            if (anim != null) anim.cancel();
        }
        
        View root = getWindow().getDecorView().findViewById(android.R.id.content);
        if (root != null) {
            AnimatorSet exitAnim = new AnimatorSet();
            
            ObjectAnimator alpha = ObjectAnimator.ofFloat(root, "alpha", 1f, 0f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(root, "scaleX", 1f, 0.92f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(root, "scaleY", 1f, 0.92f);
            
            exitAnim.setDuration(500);
            exitAnim.setInterpolator(new DecelerateInterpolator());
            exitAnim.playTogether(alpha, scaleX, scaleY);
            
            exitAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    finish();
                    overridePendingTransition(0, 0);
                }
            });
            
            exitAnim.start();
        } else {
            finish();
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public void onBackPressed() {
        // منع الرجوع
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(updateProgress);
        dotsHandler.removeCallbacksAndMessages(null);
        if (logoAnim != null) logoAnim.cancel();
        if (glowAnim != null) glowAnim.cancel();
        if (progressAnim != null) progressAnim.cancel();
        for (Animator anim : circleAnimators) {
            if (anim != null) anim.cancel();
        }
        super.onDestroy();
    }

    private FrameLayout.LayoutParams createMatchParams() {
        return new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
    }

    private String readSmall(File f) {
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

    // ==============================
    // كلاس الدائرة المخصصة
    // ==============================
    private static class CircleView extends View {
        private Paint paint;
        private int size;
        private float alpha = 0.05f;
        private int color = PRIMARY;

        public CircleView(Activity context) {
            super(context);
            init();
        }

        private void init() {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ColorUtils.setAlphaComponent(color, (int)(alpha * 255)));
            paint.setAntiAlias(true);
        }

        public void setSize(int size) {
            this.size = size;
            requestLayout();
        }

        public void setColor(int color) {
            this.color = color;
            updatePaint();
        }

        public void setAlpha(float alpha) {
            this.alpha = alpha;
            updatePaint();
        }

        private void updatePaint() {
            paint.setColor(ColorUtils.setAlphaComponent(color, (int)(alpha * 255)));
            invalidate();
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            super.onDraw(canvas);
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) / 2f;
            canvas.drawCircle(centerX, centerY, radius, paint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(size, size);
        }
    }

    // ==============================
    // LinearInterpolator للدوران المنتظم
    // ==============================
    private static class LinearInterpolator implements android.view.animation.Interpolator {
        @Override
        public float getInterpolation(float input) {
            return input;
        }
    }
}

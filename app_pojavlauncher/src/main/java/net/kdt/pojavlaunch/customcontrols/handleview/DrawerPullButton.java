package net.kdt.pojavlaunch.customcontrols.handleview;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;

import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import net.kdt.pojavlaunch.R;

/**
 * Ceyo: Disli (menu) butonu surukle-birak ile istenen yere tasinabilir.
 * Konum kaydedilir, varsayilan: ust orta (kamera cukuru varsa altinda).
 */
public class DrawerPullButton extends View {
    private static final String PREFS = "ceyo_gear";
    private static final String KEY_X = "fx";
    private static final String KEY_Y = "fy";

    public DrawerPullButton(Context context) {super(context); init();}
    public DrawerPullButton(Context context, @Nullable AttributeSet attrs) {super(context, attrs); init();}

    private final Paint mBackgroundPaint = new Paint();
    private VectorDrawableCompat mDrawable;
    private int mSlop;
    private float mDownRawX, mDownRawY, mStartTx, mStartTy;
    private boolean mDragging;

    private void init(){
        mDrawable = VectorDrawableCompat.create(getContext().getResources(), R.drawable.ic_sharp_settings_24, null);
        setAlpha(0.33f);
        mBackgroundPaint.setColor(Color.BLACK);
        mBackgroundPaint.setAntiAlias(true);
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    private boolean isFloating() {
        return getTranslationY() > 0.5f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean floating = isFloating();
        if(floating) {
            canvas.drawOval(0, 0, getWidth(), getHeight(), mBackgroundPaint);
        } else {
            canvas.drawArc(getPaddingLeft(),-getHeight() + getPaddingBottom(),getWidth() - getPaddingRight(), getHeight() - getPaddingBottom(), 0, 180, true, mBackgroundPaint);
        }

        mDrawable.setBounds(getPaddingLeft()/2, getPaddingTop()/2, getHeight() - getPaddingRight()/2, getHeight() - getPaddingBottom()/2);
        canvas.save();
        canvas.translate((getWidth()-getHeight())/2f, floating ? 0 : -getPaddingBottom()/2f);
        mDrawable.draw(canvas);
        canvas.restore();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if(changed && !mDragging) restorePosition();
    }

    private void restorePosition() {
        if(!(getParent() instanceof View)) return;
        View parent = (View) getParent();
        int pw = parent.getWidth(), ph = parent.getHeight();
        if(pw == 0 || ph == 0) return;

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if(prefs.contains(KEY_X) && prefs.contains(KEY_Y)) {
            setTranslationX(clampX(prefs.getFloat(KEY_X, 0.5f) * pw - getLeft(), parent));
            setTranslationY(clampY(prefs.getFloat(KEY_Y, 0f) * ph - getTop(), parent));
        } else {
            // Varsayilan: ust orta. Kamera cukuru varsa onun altina in.
            int inset = 0;
            if(Build.VERSION.SDK_INT >= 28) {
                WindowInsets insets = getRootWindowInsets();
                DisplayCutout cutout = insets != null ? insets.getDisplayCutout() : null;
                if(cutout != null) inset = cutout.getSafeInsetTop();
            }
            setTranslationX(0);
            setTranslationY(clampY(inset, parent));
        }
        invalidate();
    }

    private float clampX(float tx, View parent) {
        float min = -getLeft();
        float max = parent.getWidth() - getWidth() - getLeft();
        return Math.max(min, Math.min(max, tx));
    }

    private float clampY(float ty, View parent) {
        float min = -getTop();
        float max = parent.getHeight() - getHeight() - getTop();
        return Math.max(min, Math.min(max, ty));
    }

    private void savePosition() {
        if(!(getParent() instanceof View)) return;
        View parent = (View) getParent();
        if(parent.getWidth() == 0 || parent.getHeight() == 0) return;
        getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putFloat(KEY_X, (getLeft() + getTranslationX()) / parent.getWidth())
                .putFloat(KEY_Y, (getTop() + getTranslationY()) / parent.getHeight())
                .apply();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!(getParent() instanceof View)) return super.onTouchEvent(event);
        View parent = (View) getParent();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownRawX = event.getRawX();
                mDownRawY = event.getRawY();
                mStartTx = getTranslationX();
                mStartTy = getTranslationY();
                mDragging = false;
                parent.requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - mDownRawX;
                float dy = event.getRawY() - mDownRawY;
                if(!mDragging && Math.hypot(dx, dy) > mSlop) {
                    mDragging = true;
                    setAlpha(0.8f);
                }
                if(mDragging) {
                    setTranslationX(clampX(mStartTx + dx, parent));
                    setTranslationY(clampY(mStartTy + dy, parent));
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if(mDragging) {
                    mDragging = false;
                    setAlpha(0.33f);
                    savePosition();
                } else {
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                if(mDragging) {
                    mDragging = false;
                    setAlpha(0.33f);
                    savePosition();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}

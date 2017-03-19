package com.example.rychan.fyp.perspective_transform;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.rychan.fyp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rychan on 17年3月2日.
 */

public class RegionSelector extends FrameLayout {
    protected Context context;
    private Paint paint;
    private ImageView pointer1;
    private ImageView pointer2;
    private ImageView pointer3;
    private ImageView pointer4;
    private RegionSelector regionSelector;

    public RegionSelector(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public RegionSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public RegionSelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    private void init() {
        regionSelector = this;
        pointer1 = getImageView(0, 0);
        pointer2 = getImageView(getWidth(), 0);
        pointer3 = getImageView(0, getHeight());
        pointer4 = getImageView(getWidth(), getHeight());

        addView(pointer1);
        addView(pointer2);
        addView(pointer3);
        addView(pointer4);
        initPaint();
    }

    private void initPaint() {
        paint = new Paint();
        paint.setColor(getResources().getColor(R.color.blue));
        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
    }

    public void setPoints(List<PointF> pointList, int maxWidth, int maxHeight) {
        pointer1.setX(within(pointList.get(0).x, maxWidth));
        pointer1.setY(within(pointList.get(0).y, maxHeight));

        pointer2.setX(within(pointList.get(1).x, maxWidth));
        pointer2.setY(within(pointList.get(1).y, maxHeight));

        pointer3.setX(within(pointList.get(2).x, maxWidth));
        pointer3.setY(within(pointList.get(2).y, maxHeight));

        pointer4.setX(within(pointList.get(3).x, maxWidth));
        pointer4.setY(within(pointList.get(3).y, maxHeight));
    }

    private float within(float f, float max) {
        if (f < 0) {
            return 0;
        } else if (f > max) {
            return max;
        } else {
            return f;
        }
    }

    public List<PointF> getPoints() {
        List<PointF> pointsList = new ArrayList<>(4);

        pointsList.add(new PointF(pointer1.getX(), pointer1.getY()));
        pointsList.add(new PointF(pointer2.getX(), pointer2.getY()));
        pointsList.add(new PointF(pointer3.getX(), pointer3.getY()));
        pointsList.add(new PointF(pointer4.getX(), pointer4.getY()));

        return pointsList;
    }

    @Override
    protected void attachViewToParent(View child, int index, ViewGroup.LayoutParams params) {
        super.attachViewToParent(child, index, params);
    }

    private ImageView getImageView(int x, int y) {
        ImageView imageView = new ImageView(context);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageResource(R.drawable.circle);
        imageView.setX(x);
        imageView.setY(y);
        imageView.setOnTouchListener(new TouchListenerImpl());
        return imageView;
    }

    private class TouchListenerImpl implements OnTouchListener {

        PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
        PointF StartPT = new PointF(); // Record Start Position of 'img'

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int eid = event.getAction();
            switch (eid) {
                case MotionEvent.ACTION_MOVE:
                    PointF mv = new PointF(event.getX() - DownPT.x, event.getY() - DownPT.y);
                    if ((StartPT.x + mv.x + v.getWidth()) < regionSelector.getWidth() &&
                            (StartPT.y + mv.y + v.getHeight() < regionSelector.getHeight()) &&
                            (StartPT.x + mv.x) > 0 &&
                            (StartPT.y + mv.y) > 0) {
                        v.setX((int) (StartPT.x + mv.x));
                        v.setY((int) (StartPT.y + mv.y));
                        StartPT = new PointF(v.getX(), v.getY());
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    DownPT.x = event.getX();
                    DownPT.y = event.getY();
                    StartPT = new PointF(v.getX(), v.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("Position", StartPT.toString());
                    break;
                default:
                    break;
            }
            regionSelector.invalidate();
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.drawLine(pointer1.getX() + (pointer1.getWidth() / 2), pointer1.getY() + (pointer1.getHeight() / 2), pointer3.getX() + (pointer3.getWidth() / 2), pointer3.getY() + (pointer3.getHeight() / 2), paint);
        canvas.drawLine(pointer1.getX() + (pointer1.getWidth() / 2), pointer1.getY() + (pointer1.getHeight() / 2), pointer2.getX() + (pointer2.getWidth() / 2), pointer2.getY() + (pointer2.getHeight() / 2), paint);
        canvas.drawLine(pointer2.getX() + (pointer2.getWidth() / 2), pointer2.getY() + (pointer2.getHeight() / 2), pointer4.getX() + (pointer4.getWidth() / 2), pointer4.getY() + (pointer4.getHeight() / 2), paint);
        canvas.drawLine(pointer3.getX() + (pointer3.getWidth() / 2), pointer3.getY() + (pointer3.getHeight() / 2), pointer4.getX() + (pointer4.getWidth() / 2), pointer4.getY() + (pointer4.getHeight() / 2), paint);
    }
}

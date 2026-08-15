package com.rojama.pianoshelf;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ZoomControls;

/**
 * 继承 ImageView 实现多点触控拖动和缩放。
 *
 * 修复 (Modernization):
 *  - FloatMath.sqrt (API 17 废弃) → Math.sqrt
 *  - 屏幕尺寸获取从 deprecated Display.getWidth/Height → DisplayMetrics
 *  - 增加边界 null-safe / index-safe 检查
 */
public class TouchView extends ImageView {
	static final int NONE = 0;
	static final int DRAG = 1;    // 拖动中
	static final int ZOOM = 2;    // 缩放中
	static final int BIGGER = 3;  // 放大ing
	static final int SMALLER = 4; // 缩小ing
	private int mode = NONE;

	private float beforeLenght;
	private float afterLenght;
	private float scale = 0.2f;

	private int screenW;
	private int screenH;

	public int imgW;
	public int imgH;

	private int start_x;
	private int start_y;
	private int stop_x;
	private int stop_y;

	private TranslateAnimation trans;

	public TouchView(Context context, int w, int h) {
		super(context);
		this.setPadding(0, 0, 0, 0);
		screenW = w;
		screenH = h;

		ZoomControls zoom = null;
		try {
			zoom = (ZoomControls) ((Activity) context).findViewById(R.id.zoomControls);
		} catch (Throwable ignored) { /* view may not exist in some contexts */ }
		if (zoom != null) {
			zoom.setIsZoomInEnabled(true);
			zoom.setIsZoomOutEnabled(true);
			zoom.setOnZoomInClickListener(new OnClickListener() {
				public void onClick(View v) { setScale(scale, BIGGER); }
			});
			zoom.setOnZoomOutClickListener(new OnClickListener() {
				public void onClick(View v) { setScale(scale, SMALLER); }
			});
		}
	}

	/** Compute distance between first two pointer coordinates. */
	private float spacing(MotionEvent event) {
		final int pcount = event.getPointerCount();
		if (pcount < 2) return 0f;
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		// NOTE: FloatMath removed in API 23+. Use java.lang.Math.sqrt.
		return (float) Math.sqrt(x * x + y * y);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			mode = DRAG;
			stop_x = (int) event.getRawX();
			stop_y = (int) event.getRawY();
			start_x = (int) event.getX();
			start_y = stop_y - this.getTop();
			if (event.getPointerCount() >= 2) beforeLenght = spacing(event);
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			if (event.getPointerCount() >= 2 && spacing(event) > 10f) {
				mode = ZOOM;
				beforeLenght = spacing(event);
			}
			break;
		case MotionEvent.ACTION_UP:
			processOut();
			mode = NONE;
			break;
		case MotionEvent.ACTION_POINTER_UP:
			mode = NONE;
			break;
		case MotionEvent.ACTION_MOVE:
			if (mode == DRAG) {
				if (Math.abs(stop_x - start_x - getLeft()) < 88
						&& Math.abs(stop_y - start_y - getTop()) < 85) {
					this.setPosition(stop_x - start_x, stop_y - start_y, stop_x + this.getWidth()
							- start_x, stop_y - start_y + this.getHeight());
					stop_x = (int) event.getRawX();
					stop_y = (int) event.getRawY();
				}
			} else if (mode == ZOOM) {
				if (event.getPointerCount() >= 2 && spacing(event) > 10f) {
					afterLenght = spacing(event);
					float gapLenght = afterLenght - beforeLenght;
					if (gapLenght == 0) break;
					if (Math.abs(gapLenght) > 5f) {
						if (gapLenght > 0) {
							this.setScale(scale, BIGGER);
						} else {
							this.setScale(scale, SMALLER);
						}
						beforeLenght = afterLenght;
					}
				}
			}
			break;
		}
		return true;
	}

	private void processOut() {
		int disX = 0;
		int disY = 0;
		if (getHeight() <= screenH) {
			if (this.getTop() < 0) {
				int dis = getTop();
				this.layout(this.getLeft(), 0, this.getRight(), 0 + this.getHeight());
				disY = dis - getTop();
			} else if (this.getBottom() > screenH) {
				disY = getHeight() - screenH + getTop();
				this.layout(this.getLeft(), screenH - getHeight(), this.getRight(), screenH);
			}
		}
		if (getWidth() <= screenW) {
			if (this.getLeft() < 0) {
				disX = getLeft();
				this.layout(0, this.getTop(), 0 + getWidth(), this.getBottom());
			} else if (this.getRight() > screenW) {
				disX = getWidth() - screenW + getLeft();
				this.layout(screenW - getWidth(), this.getTop(), screenW, this.getBottom());
			}
		}
		if (disX != 0 || disY != 0) {
			trans = new TranslateAnimation(disX, 0, disY, 0);
			trans.setDuration(500);
			this.startAnimation(trans);
		}
	}

	private void setScale(float temp, int flag) {
		if (flag == BIGGER) {
			if (imgW > 0 && this.getWidth() >= this.imgW * 1.4F) return;
			this.setFrame(this.getLeft()  - (int) (temp * this.getWidth()),
						 this.getTop()   - (int) (temp * this.getHeight()),
						 this.getRight() + (int) (temp * this.getWidth()),
						 this.getBottom()+ (int) (temp * this.getHeight()));
		} else if (flag == SMALLER) {
			if (imgW > 0 && this.getWidth() <= this.imgW * 0.4F) return;
			this.setFrame(this.getLeft()  + (int) (temp * this.getWidth()),
						 this.getTop()   + (int) (temp * this.getHeight()),
						 this.getRight() - (int) (temp * this.getWidth()),
						 this.getBottom()- (int) (temp * this.getHeight()));
		}
		processOut();
	}

	private void setPosition(int left, int top, int right, int bottom) {
		this.layout(left, top, right, bottom);
	}

	/** Helper to read display metrics without deprecated APIs. */
	public static DisplayMetrics getDisplayMetrics(Context ctx) {
		return ctx.getResources().getDisplayMetrics();
	}
}

package com.rojama.pianoshelf;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView.ScaleType;

/**
 * 乐谱滚动容器（替代原 AbsoluteLayout）。
 *
 * AbsoluteLayout 在 API 23+ 标记为 deprecated，现代 Android 推荐使用
 * FrameLayout + Gravity/Margin 来实现子视图的定位。这里保留了原始
 * 「按图片尺寸居中并适配屏幕」的视觉行为。
 */
public class ViewScroll extends FrameLayout {
	private int screenW;
	private int screenH;
	private int imgW;
	private int imgH;
	public TouchView tv;

	@SuppressWarnings("deprecation")
	public ViewScroll(Context context, Bitmap img, View topView) {
		super(context);
		// Prefer DisplayMetrics over deprecated Display#getWidth / getHeight
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		screenW = dm.widthPixels;
		int availableScreenH = dm.heightPixels;
		int topOffset = (topView == null) ? 0 : (topView.getBottom() + 50);
		screenH = Math.max(availableScreenH - topOffset, 1);

		tv = new TouchView(context, screenW, screenH);
		tv.setImageBitmap(img);
		imgW = img.getWidth();
		imgH = img.getHeight();
		tv.imgW = imgW;
		tv.imgH = imgH;

		int layout_w = (imgW > screenW) ? screenW : imgW;
		int layout_h = (imgH > screenH) ? screenH : imgH;

		if (layout_w == screenW || layout_h == screenH) {
			tv.setScaleType(ScaleType.CENTER_INSIDE);
		}

		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(layout_w, layout_h);
		// Center within parent (equivalent to AbsoluteLayout centering)
		lp.gravity = Gravity.CENTER;
		tv.setLayoutParams(lp);

		this.addView(tv);
	}
}

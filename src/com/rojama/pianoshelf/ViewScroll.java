package com.rojama.pianoshelf;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.ImageView.ScaleType;

/**
 * 一个绝对布局
 * 
 * @author Administrator
 * 
 */
@SuppressWarnings("deprecation")
public class ViewScroll extends AbsoluteLayout {
	private int screenW; // 可用的屏幕宽
	private int screenH; // 可用的屏幕高 总高度-上面组件的总高度
	private int imgW; // 图片原始宽
	private int imgH; // 图片原始高
	public TouchView tv;

	public ViewScroll(Context context, Bitmap img, View topView) {
		super(context);
		screenW = ((Activity) context).getWindowManager().getDefaultDisplay().getWidth();
		screenH = ((Activity) context).getWindowManager().getDefaultDisplay().getHeight()
				- (topView == null ? 0 : topView.getBottom() + 50);
		tv = new TouchView(context, screenW, screenH);
		tv.setImageBitmap(img);
		imgW = img.getWidth();
		imgH = img.getHeight();
		tv.imgW = imgW;
		tv.imgH = imgH;
		int layout_w = imgW > screenW ? screenW : imgW; // 实际显示的宽
		int layout_h = imgH > screenH ? screenH : imgH; // 实际显示的高
		if (layout_w == screenW || layout_h == screenH)
			tv.setScaleType(ScaleType.CENTER_INSIDE);
		tv.setLayoutParams(new AbsoluteLayout.LayoutParams(layout_w, layout_h,
				layout_w == screenW ? 0 : (screenW - layout_w) / 2, layout_h == screenH ? 0
						: (screenH - layout_h) / 2));
		this.addView(tv);
	}
	
	
}

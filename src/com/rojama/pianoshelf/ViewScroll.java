package com.rojama.pianoshelf;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.ImageView.ScaleType;

/**
 * һ�����Բ���
 * 
 * @author Administrator
 * 
 */
@SuppressWarnings("deprecation")
public class ViewScroll extends AbsoluteLayout {
	private int screenW; // ���õ���Ļ��
	private int screenH; // ���õ���Ļ�� �ܸ߶�-����������ܸ߶�
	private int imgW; // ͼƬԭʼ��
	private int imgH; // ͼƬԭʼ��
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
		int layout_w = imgW > screenW ? screenW : imgW; // ʵ����ʾ�Ŀ�
		int layout_h = imgH > screenH ? screenH : imgH; // ʵ����ʾ�ĸ�
		if (layout_w == screenW || layout_h == screenH)
			tv.setScaleType(ScaleType.CENTER_INSIDE);
		tv.setLayoutParams(new AbsoluteLayout.LayoutParams(layout_w, layout_h,
				layout_w == screenW ? 0 : (screenW - layout_w) / 2, layout_h == screenH ? 0
						: (screenH - layout_h) / 2));
		this.addView(tv);
	}
	
	
}

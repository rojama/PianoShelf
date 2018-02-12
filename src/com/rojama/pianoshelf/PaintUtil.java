package com.rojama.pianoshelf;

import com.xenoage.zong.musicxml.types.MxlFormattedText;
import com.xenoage.zong.musicxml.types.attributes.MxlPrintStyle;
import com.xenoage.zong.musicxml.types.enums.MxlFontStyle;
import com.xenoage.zong.musicxml.types.enums.MxlFontWeight;
import com.xenoage.zong.musicxml.types.enums.MxlLeftCenterRight;

import android.graphics.Paint;
import android.graphics.Paint.Align;

public class PaintUtil {

	public static Paint getPaint(MxlFormattedText ft, Paint defaltPaint) {
		Paint paint = PaintUtil.getPaint(ft.getPrintStyle(), defaltPaint);
		if (ft.getJustify() == MxlLeftCenterRight.Left) {
			paint.setTextAlign(Align.LEFT);
		} else if (ft.getJustify() == MxlLeftCenterRight.Center) {
			paint.setTextAlign(Align.CENTER);
		} else if (ft.getJustify() == MxlLeftCenterRight.Right) {
			paint.setTextAlign(Align.RIGHT);
		}
		return paint;
	}

	public static Paint getPaint(MxlPrintStyle ps, Paint defaltPaint) {
		Paint paint = defaltPaint;
		paint.setTextSize(ps.getFont().getFontSize().getValuePt());
		if (ps.getFont().getFontStyle() == MxlFontStyle.Italic) {
			paint.setTextSkewX(-0.25F);
		}
		if (ps.getFont().getFontWeight() == MxlFontWeight.Bold) {
			paint.setTextScaleX(1.25F);
		}
		if (ps.getColor() != null) {
			paint.setColor(ps.getColor().getValue());
		}
		return paint;
	}

}

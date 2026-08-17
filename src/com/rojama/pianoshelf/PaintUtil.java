package com.rojama.pianoshelf;

import com.xenoage.zong.musicxml.types.MxlFormattedText;
import com.xenoage.zong.musicxml.types.attributes.MxlFont;
import com.xenoage.zong.musicxml.types.attributes.MxlPrintStyle;
import com.xenoage.zong.musicxml.types.attributes.MxlFontSize;
import com.xenoage.zong.musicxml.types.enums.MxlFontStyle;
import com.xenoage.zong.musicxml.types.enums.MxlFontWeight;
import com.xenoage.zong.musicxml.types.enums.MxlLeftCenterRight;

import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class PaintUtil {

	public static Paint getPaint(MxlFormattedText ft, Paint defaltPaint) {
		Paint paint = PaintUtil.getPaint(ft.getPrintStyle(), defaltPaint, null);
		if (ft.getJustify() == MxlLeftCenterRight.Left) {
			paint.setTextAlign(Align.LEFT);
		} else if (ft.getJustify() == MxlLeftCenterRight.Center) {
			paint.setTextAlign(Align.CENTER);
		} else if (ft.getJustify() == MxlLeftCenterRight.Right) {
			paint.setTextAlign(Align.RIGHT);
		}
		return paint;
	}

	/** @deprecated 直接调用会缺少 STAFF_LINE_SPACING 联动；统一走带 CommonTransfer 参数的重载 */
	@Deprecated
	public static Paint getPaint(MxlPrintStyle ps, Paint defaltPaint) {
		return getPaint(ps, defaltPaint, null);
	}

	/**
	 * @param ct CommonTransfer（允许 null），用于把 MusicXML pt/默认字号 与 STAFF_LINE_SPACING 按比例联动
	 */
	public static Paint getPaint(MxlFormattedText ft, Paint defaltPaint, CommonTransfer ct) {
		Paint paint = PaintUtil.getPaint(ft.getPrintStyle(), defaltPaint, ct);
		if (ft.getJustify() == MxlLeftCenterRight.Left) {
			paint.setTextAlign(Align.LEFT);
		} else if (ft.getJustify() == MxlLeftCenterRight.Center) {
			paint.setTextAlign(Align.CENTER);
		} else if (ft.getJustify() == MxlLeftCenterRight.Right) {
			paint.setTextAlign(Align.RIGHT);
		}
		return paint;
	}

	public static Paint getPaint(MxlPrintStyle ps, Paint defaltPaint, CommonTransfer ct) {
		Paint paint = new Paint(defaltPaint); // copy 一份，避免改全局 paint 影响乐谱绘制
		float textSizePx = defaltPaint.getTextSize(); // 兜底：沿用默认 乐谱字号 (约 SP*1.6)
		int SP = (ct != null && ct.STAFF_LINE_SPACING > 0) ? ct.STAFF_LINE_SPACING : 0;
		if (ps != null && ps.getFont() != null) {
			MxlFont f = ps.getFont();
			MxlFontSize fs = f.getFontSize();
			if (fs != null && fs.getValuePt() != null && fs.getValuePt() > 0) {
				// MusicXML font-size 单位为 pt (1/72 inch)，Android setTextSize 默认 px，需用 applyDimension COMPLEX_UNIT_PT 转换
				float pt = fs.getValuePt();
				textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PT, pt,
						new DisplayMetrics() {
							{
								// 兜底 320dpi → 1pt≈4.44px；若 ct 有 mmToPx，则 mmToPx*25.4/72=pt→px 更准确
								this.density = 2.0f;
								this.densityDpi = 320;
								this.scaledDensity = 2.0f;
								this.xdpi = 320;
								this.ydpi = 320;
							}
						});
				// 与五线谱比例联动：默认 tenths→px = SP/10，基础字号相对 SP 缩放
				if (SP > 0) {
					// 10pt 在标准密度下≈ baseline 字号；若乐谱放大 1.4x，标题也放大 1.4x
					float relScale = SP / 10f;
					textSizePx *= relScale;
				}
			} else {
				// 无 font-size：默认相对 STAFF_LINE_SPACING 设 1.5 SP 的字号，保证可读
				if (SP > 0) {
					textSizePx = Math.max(10, SP * 1.5f);
				}
			}
			paint.setTextSize(textSizePx);
			if (f.getFontStyle() == MxlFontStyle.Italic) {
				paint.setTextSkewX(-0.25F);
			}
			if (f.getFontWeight() == MxlFontWeight.Bold) {
				paint.setFakeBoldText(true); // 比 setTextScaleX(1.25) 更像"加粗"
			}
		} else {
			// ps 或 ps.getFont() == null 时：同样按 SP 联动，把默认字号放至合理范围
			if (SP > 0) {
				float relScale = SP / 10f;
				if (relScale > 0f) textSizePx *= relScale;
			}
			paint.setTextSize(textSizePx);
		}
		if (ps != null && ps.getColor() != null) {
			paint.setColor(ps.getColor().getValue());
		}
		return paint;
	}

}

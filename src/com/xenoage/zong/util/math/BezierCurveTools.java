package com.xenoage.zong.util.math;

import com.xenoage.util.enums.VSide;
import com.xenoage.util.math.Point2f;
import com.xenoage.util.math.QuadraticCurve;

public class BezierCurveTools
{
  public static CubicBezierCurve computeBezierFrom(QuadraticCurve curve, float startX, float endX)
  {
    Point2f p1 = Point2f.p(startX, curve.getY(startX));
    Point2f p2 = Point2f.p(endX, curve.getY(endX));

    float hx = (startX + endX) / 2.0F;
    Point2f h = Point2f.p(hx, curve.getY(hx));

    float t = 0.5F;

    Point2f c = h.sub(p1.scale((1.0F - t) * (1.0F - t))).sub(p2.scale(t * t)).scale(1.0F / (2.0F * t * (1.0F - t)));

    Point2f c1 = p1.scale(0.3333333F).add(c.scale(0.6666667F));
    Point2f c2 = p2.scale(0.3333333F).add(c.scale(0.6666667F));
    return new CubicBezierCurve(p1, c1, c2, p2);
  }

  public static CubicBezierCurve correctBezier(CubicBezierCurve curve, VSide side)
  {
    Point2f p1c1 = curve.getC1().sub(curve.getP1());
    Point2f p1p2 = curve.getP2().sub(curve.getP1());
    float angleLeft = (float)Math.acos(p1c1.dotProduct(p1p2) / (p1c1.length() * p1p2.length()));

    if ((angleLeft < 0.3D) || (Float.isNaN(angleLeft)))
    {
      float slurDown = -1 * side.getDir() * 1;
      Point2f p1 = curve.getP1().add(0.0F, slurDown);
      Point2f c1 = curve.getC1().add(0.0F, slurDown).rotate(p1, 1 * side.getDir() * 0.5F);
      Point2f p2 = curve.getP2().add(0.0F, slurDown);
      Point2f c2 = curve.getC2().add(0.0F, slurDown).rotate(p2, -1 * side.getDir() * 0.5F);
      return new CubicBezierCurve(p1, c1, c2, p2);
    }

    return curve;
  }
}
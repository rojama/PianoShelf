package com.xenoage.zong.util.math;

import com.xenoage.util.math.Point2f;

public class CubicBezierCurve
{
  private final Point2f p1;
  private final Point2f c1;
  private final Point2f c2;
  private final Point2f p2;

  public CubicBezierCurve(Point2f p1, Point2f c1, Point2f c2, Point2f p2)
  {
    this.p1 = p1;
    this.c1 = c1;
    this.c2 = c2;
    this.p2 = p2;
  }

  public Point2f getP1()
  {
    return this.p1;
  }

  public Point2f getC1()
  {
    return this.c1;
  }

  public Point2f getC2()
  {
    return this.c2;
  }

  public Point2f getP2()
  {
    return this.p2;
  }
}
package com.xenoage.zong.util.math;

import com.xenoage.util.enums.VSide;
import com.xenoage.util.math.ConvexHull;
import com.xenoage.util.math.Gauss;
import com.xenoage.util.math.Point2f;
import com.xenoage.util.math.QuadraticCurve;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class QuadraticCurvesTools
{
  public static List<QuadraticCurve> computeOverConvexHull(ConvexHull convexHull, float leftArea, float rightArea)
  {
    LinkedList ret = new LinkedList();
    ArrayList points = convexHull.getPoints();
    VSide side = convexHull.getSide();
    int sideDir = side.getDir();
    int n = points.size();

    Point2f[] startPoints = { (Point2f)points.get(0), ((Point2f)points.get(0)).add(0.0F, sideDir * leftArea) };
    Point2f[] endPoints = { (Point2f)points.get(n - 1), ((Point2f)points.get(n - 1)).add(0.0F, sideDir * rightArea) };

    int m = n - 2;
    Point2f[] eq = new Point2f[4 + m];
    eq[0] = startPoints[0];
    eq[1] = startPoints[1];
    eq[2] = endPoints[0];
    eq[3] = endPoints[1];
    for (int i = 0; i < m; i++)
    {
      eq[(4 + i)] = ((Point2f)points.get(1 + i));
    }

    int[][] subsets = getAllCombinationsOf3(m + 4);
    for (int i = 0; i < subsets.length; i++)
    {
      int[] eqIndices = subsets[i];

      if (((eqIndices[0] == 0) && (eqIndices[1] == 1)) || ((eqIndices[0] == 2) && (eqIndices[1] == 3)) || ((eqIndices[1] == 2) && (eqIndices[2] == 3)))
      {
        continue;
      }

      double[][] A = new double[3][3];
      double[] b = new double[3];
      for (int iy = 0; iy < 3; iy++)
      {
        Point2f p = eq[eqIndices[iy]];
        A[iy][0] = (p.x * p.x);
        A[iy][1] = p.x;
        A[iy][2] = 1.0D;
        b[iy] = p.y;
      }
      double[] params = Gauss.solve(A, b);

      boolean ok = true;
      ok &= sideDir * getY(startPoints[0].x, params) >= sideDir * startPoints[0].y;
      ok &= sideDir * getY(startPoints[1].x, params) <= sideDir * startPoints[1].y;
      ok &= sideDir * getY(endPoints[0].x, params) >= sideDir * endPoints[0].y;
      ok &= sideDir * getY(endPoints[1].x, params) <= sideDir * endPoints[1].y;
      ok &= sideDir * params[0] <= 0.0D;
      for (int im = 0; (ok) && (im < m); im++)
      {
        ok &= sideDir * getY(((Point2f)points.get(1 + im)).x, params) >= sideDir * ((Point2f)points.get(1 + im)).y;
      }
      if (!ok) {
        continue;
      }
      ret.add(new QuadraticCurve((float)params[0], (float)params[1], (float)params[2]));
    }

    if (ret.size() == 0)
    {
      double[][] A = { { ((Point2f)points.get(0)).x, 1.0D }, { ((Point2f)points.get(n - 1)).x, 1.0D } };
      double[] b = { ((Point2f)points.get(0)).y, ((Point2f)points.get(n - 1)).y };
      double[] params = Gauss.solve(A, b);
      ret.add(new QuadraticCurve(0.0F, (float)params[0], (float)params[1]));
    }

    return ret;
  }

  public static QuadraticCurve getBestCurve(List<QuadraticCurve> curves, ConvexHull hull)
  {
    if (curves.size() > 1)
    {
      int minIndex = 0;
      float minArea = (1.0F / 1.0F);
      for (int i = 0; i < curves.size(); i++)
      {
        float area = getAreaBetween(curves.get(i), hull);
        if (area >= minArea)
          continue;
        minArea = area;
        minIndex = i;
      }

      return curves.get(minIndex);
    }
    if (curves.size() == 1)
    {
      return curves.get(0);
    }

    return null;
  }

  private static int[][] getAllCombinationsOf3(int slotsCount)
  {
    ArrayList ret = new ArrayList();
    int[] subsetIndices = new int[slotsCount];
    int max = pow(2, slotsCount);
    for (int i = 0; i < max; i++)
    {
      if (sum(subsetIndices) == 3)
      {
        ret.add(getIndicesWith1(subsetIndices));
      }
      increment(subsetIndices);
    }
    return (int[][])ret.toArray(new int[0][]);
  }

  private static int pow(int a, int b)
  {
    int ret = 1;
    for (int i = 0; i < b; i++)
    {
      ret *= a;
    }
    return ret;
  }

  private static void increment(int[] binaryNumber)
  {
    int digit = binaryNumber.length - 1;
    do
    {
      binaryNumber[digit] = (1 - binaryNumber[digit]);
      digit--;
    }

    while ((binaryNumber[(digit + 1)] == 0) && (digit >= 0));
  }

  private static int sum(int[] number)
  {
    int ret = 0;
    for (int i = 0; i < number.length; i++)
    {
      ret += number[i];
    }
    return ret;
  }

  private static int[] getIndicesWith1(int[] binaryNumber)
  {
    int[] ret = new int[3];
    int index = 0;
    for (int i = 0; (i < binaryNumber.length) && (index < 3); i++)
    {
      if (binaryNumber[i] != 1)
        continue;
      ret[index] = i;
      index++;
    }

    return ret;
  }

  private static float getY(float x, double[] params)
  {
    return (float)(params[0] * x * x + params[1] * x + params[2]);
  }

  private static float getAreaBetween(QuadraticCurve curve, ConvexHull convexHull)
  {
    float sumArea = 0.0F;
    ArrayList polygon = convexHull.getPoints();
    for (int i = 0; i < polygon.size() - 1; i++)
    {
      float x1 = ((Point2f)polygon.get(i)).x;
      float x2 = ((Point2f)polygon.get(i + 1)).x;
      float curveArea = curve.getArea(x1, x2);
      float polyArea = (x2 - x1) * (((Point2f)polygon.get(i)).y + ((Point2f)polygon.get(i + 1)).y) / 2.0F;
      sumArea += convexHull.getSide().getDir() * (curveArea - polyArea);
    }
    return sumArea;
  }
}
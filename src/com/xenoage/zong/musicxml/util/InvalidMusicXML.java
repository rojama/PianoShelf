package com.xenoage.zong.musicxml.util;

import com.xenoage.pdlib.PVector;
import com.xenoage.pdlib.Vector;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class InvalidMusicXML extends RuntimeException
{
  private final Element element;

  public InvalidMusicXML(Element element)
  {
    if (element == null)
      throw new IllegalArgumentException();
    this.element = element;
  }

  public static InvalidMusicXML invalid(Element element)
  {
    return new InvalidMusicXML(element);
  }

  public static <T> T throwNull(T o, Element e)
  {
    if (o != null)
    {
      return o;
    }
    throw invalid(e);
  }

  public Vector<Node> getStack()
  {
    Node n = this.element;
    PVector stack = PVector.pvec();
    do
    {
      stack = stack.plus(0, n);
      n = n.getParentNode();
    }
    while (n != null);
    return stack;
  }

  @Override
public String getMessage()
  {
    StringBuilder ret = new StringBuilder();
    for (Node node : getStack())
    {
      ret.append(new StringBuilder().append("/").append(node.getNodeName()).toString());
    }
    return ret.toString();
  }
}
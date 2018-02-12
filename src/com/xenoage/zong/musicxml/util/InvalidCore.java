package com.xenoage.zong.musicxml.util;

public final class InvalidCore extends RuntimeException
{
  private final Object element;

  public InvalidCore(Object element)
  {
    this.element = element;
  }

  public static InvalidCore invalidCore(Object element)
  {
    return new InvalidCore(element);
  }

  public Object getElement()
  {
    return this.element;
  }
}
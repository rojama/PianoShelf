package com.xenoage.zong.musicxml.util;

import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.types.enums.EnumWithXMLNames;
import org.w3c.dom.Element;

public final class Parse
{
  public static <T> T getEnumValue(String text, T[] values)
  {
    if ((text != null) && (text.length() > 0))
    {
      if (Character.isDigit(text.charAt(0)))
        text = "_" + text;
      for (T value : values)
      {
        if (text.equals(value.toString().toLowerCase()))
        {
          return value;
        }
      }
    }
    return null;
  }

  public static <T extends EnumWithXMLNames> T getEnumValueNamed(String text, T[] values)
  {
    if (text != null)
    {
      for (T value : values)
      {
        if (text.equals(value.getXMLName()))
        {
          return value;
        }
      }
    }
    return null;
  }

  public static float parseChildFloat(Element parent, String eName)
  {
    Element e = XMLReader.element(parent, eName);
    if (e != null)
    {
      return parseFloat(e);
    }
    throw InvalidMusicXML.invalid(e);
  }

  public static int parseChildInt(Element parent, String eName)
  {
    Element e = XMLReader.element(parent, eName);
    if (e != null)
    {
      return parseInt(e);
    }
    throw InvalidMusicXML.invalid(parent);
  }

  public static Integer parseChildIntNull(Element parent, String eName)
  {
    Element e = XMLReader.element(parent, eName);
    if (e != null)
    {
      return Integer.valueOf(parseInt(e));
    }
    return null;
  }

  public static Float parseAttrFloat(Element e, String attrName)
  {
    return InvalidMusicXML.throwNull(parseAttrFloatNull(e, attrName), e);
  }

  public static Float parseAttrFloatNull(Element e, String attrName)
  {
    try
    {
      String value = XMLReader.attribute(e, attrName);
      return value != null ? Float.valueOf(Float.parseFloat(value)) : null;
    }
    catch (NumberFormatException ex) {
    }
    throw InvalidMusicXML.invalid(e);
  }

  public static int parseAttrInt(Element e, String attrName)
  {
    return (InvalidMusicXML.throwNull(parseAttrIntNull(e, attrName), e)).intValue();
  }

  public static Integer parseAttrIntNull(Element e, String attrName)
  {
    try
    {
      String value = XMLReader.attribute(e, attrName);
      return value != null ? Integer.valueOf(parseInt(value)) : null;
    }
    catch (NumberFormatException ex) {
    }
    throw InvalidMusicXML.invalid(e);
  }

  public static float parseFloat(Element e)
  {
    try
    {
      return Float.parseFloat(XMLReader.text(e));
    }
    catch (NumberFormatException ex) {
    }
    throw InvalidMusicXML.invalid(e);
  }

  public static float parseFloatInt(Element parent, String eName)
  {
    Element e = XMLReader.element(parent, eName);
    if (e != null)
    {
      return parseFloat(e);
    }
    throw InvalidMusicXML.invalid(e);
  }

  public static Float parseChildFloatNull(Element parent, String eName)
  {
    Element e = XMLReader.element(parent, eName);
    if (e != null)
    {
      return Float.valueOf(parseFloat(e));
    }
    return null;
  }

  public static int parseInt(Element e)
  {
    try
    {
      return parseInt(XMLReader.text(e));
    }
    catch (NumberFormatException ex) {
    }
    throw InvalidMusicXML.invalid(e);
  }

  private static int parseInt(String value)
  {
    try
    {
      return Integer.parseInt(value);
    }
    catch (NumberFormatException ex)
    {
      float v = Float.parseFloat(value);
      if (v == (int)v)
        return (int)v;
    }
    throw new NumberFormatException("No integer");
  }
}
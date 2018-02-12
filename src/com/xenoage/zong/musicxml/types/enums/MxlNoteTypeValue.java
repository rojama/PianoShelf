package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.math.Fraction;
import com.xenoage.zong.musicxml.util.Parse;

public enum MxlNoteTypeValue
{
  _256th(Fraction.fr(1, 256)), 
  _128th(Fraction.fr(1, 128)), 
  _64th(Fraction.fr(1, 64)), 
  _32nd(Fraction.fr(1, 32)), 
  _16th(Fraction.fr(1, 16)), 
  Eighth(Fraction.fr(1, 8)), 
  Quarter(Fraction.fr(1, 4)), 
  Half(Fraction.fr(1, 2)), 
  Whole(Fraction.fr(1, 1)), 
  Breve(Fraction.fr(1, 1)), 
  Long(Fraction.fr(1, 1));

  public final Fraction duration;

  private MxlNoteTypeValue(Fraction duration)
  {
    this.duration = duration;
  }

  @MaybeNull
  public static MxlNoteTypeValue read(String s) {
    return Parse.getEnumValue(s, values());
  }

  public String write()
  {
    String ret = toString().toLowerCase();
    if (ret.charAt(0) == '_')
      ret = ret.substring(1);
    return ret;
  }
}
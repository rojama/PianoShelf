package com.xenoage.zong.musicxml.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({java.lang.annotation.ElementType.TYPE})
public @interface IncompleteMusicXML
{
  public abstract String missing() default "";

  public abstract String partly() default "";

  public abstract String children() default "";
}
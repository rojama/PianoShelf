package com.xenoage.zong.util.event;

import com.xenoage.util.Range;
import com.xenoage.zong.core.Score;

public final class ScoreChangedEvent
{
  public final Score oldScore;
  public final Score newScore;
  public final Range measures;

  public ScoreChangedEvent(Score oldScore, Score newScore)
  {
    this.oldScore = oldScore;
    this.newScore = newScore;
    this.measures = null;
  }

  public ScoreChangedEvent(Score oldScore, Score newScore, Range measures)
  {
    this.oldScore = oldScore;
    this.newScore = newScore;
    this.measures = measures;
  }
}
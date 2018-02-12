package com.xenoage.zong.musicxml.types;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.pdlib.PVector;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverEmpty;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlMusicDataContent;
import com.xenoage.zong.musicxml.types.enums.MxlPlacement;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="offset,editorial-voice-direction,directive", partly="direction-type", children="direction-type,sound")
public final class MxlDirection
  implements MxlMusicDataContent
{
  public static final String ELEM_NAME = "direction";

  @NeverNull
  private final PVector<MxlDirectionType> directionTypes;

  @MaybeNull
  private final Integer staff;

  @MaybeNull
  private final MxlSound sound;

  @MaybeNull
  private final MxlPlacement placement;

  public MxlDirection(PVector<MxlDirectionType> directionTypes, Integer staff, MxlSound sound, MxlPlacement placement)
  {
    this.directionTypes = directionTypes;
    this.staff = staff;
    this.sound = sound;
    this.placement = placement;
  }

  @NeverEmpty
  public PVector<MxlDirectionType> getDirectionTypes() {
    return this.directionTypes;
  }

  @MaybeNull
  public Integer getStaff() {
    return this.staff;
  }

  @MaybeNull
  public MxlSound getSound() {
    return this.sound;
  }

  @MaybeNull
  public MxlPlacement getPlacement() {
    return this.placement;
  }

  @Override
public MxlMusicDataContent.MxlMusicDataContentType getMusicDataContentType()
  {
    return MxlMusicDataContent.MxlMusicDataContentType.Direction;
  }

  @MaybeNull
  public static MxlDirection read(Element e)
  {
    PVector directionTypes = PVector.pvec();
    MxlSound sound = null;
    for (Element child : XMLReader.elements(e))
    {
      String n = child.getNodeName();
      if (n.equals("direction-type"))
      {
        MxlDirectionType directionType = MxlDirectionType.read(child);
        if (directionType != null)
          directionTypes = directionTypes.plus(directionType);
      }
      else if (n.equals("sound"))
      {
        sound = MxlSound.read(child);
      }
    }
    if (directionTypes.size() > 0)
    {
      return new MxlDirection(directionTypes, Parse.parseChildIntNull(e, "staff"), sound, MxlPlacement.read(e));
    }

    return null;
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("direction", parent);
    for (MxlDirectionType directionType : this.directionTypes)
      directionType.write(e);
    XMLWriter.addAttribute(e, "staff", this.staff);
    if (this.sound != null)
      this.sound.write(e);
    if (this.placement != null)
      this.placement.write(e);
  }

@Override
public void print(PaintTransfer pt) {
	// TODO Auto-generated method stub
	
}
}
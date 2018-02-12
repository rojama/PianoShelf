package com.xenoage.zong.symbols.common;

import com.xenoage.zong.core.music.chord.Accidental;
import com.xenoage.zong.core.music.chord.Articulation;
import com.xenoage.zong.core.music.clef.ClefType;
import com.xenoage.zong.core.music.direction.DynamicsType;
import com.xenoage.zong.core.music.direction.Pedal;
import java.util.ArrayList;

public enum CommonSymbol {
	AccidentalDoubleflat("accidental-doubleflat"), AccidentalDoublesharp("accidental-doublesharp"), AccidentalFlat(
			"accidental-flat"), AccidentalNatural("accidental-natural"), AccidentalSharp(
			"accidental-sharp"), ArticulationAccent("articulation-accent"), ArticulationStaccato(
			"articulation-staccato"), ArticulationStaccatissimo("articulation-staccatissimo"), ArticulationStrongAccent(
			"articulation-strongaccent"), ArticulationTenuto("articulation-tenuto"), BracketBrace(
			"bracket-brace"), BracketBracketLine("bracket-bracketline"), BracketBracketEnd(
			"bracket-bracketend"), ClefG("clef-g"), ClefF("clef-f"), Digit0("digit-0"), Digit1(
			"digit-1"), Digit2("digit-2"), Digit3("digit-3"), Digit4("digit-4"), Digit5("digit-5"), Digit6(
			"digit-6"), Digit7("digit-7"), Digit8("digit-8"), Digit9("digit-9"), DynamicsF(
			"dynamics-f"), DynamicsM("dynamics-m"), DynamicsP("dynamics-p"), DynamicsS("dynamics-s"), DynamicsZ(
			"dynamics-z"), NoteDot("note-dot"), NoteFlag("note-flag"), NoteHalf("note-half"), NoteQuarter(
			"note-quarter"), NoteWhole("note-whole"), PedalSustainDown1("pedal-sustain-down-1"), PedalSustainUp(
			"pedal-sustain-up"), Rest16th("rest-16th"), Rest32th("rest-32th"), Rest64th("rest-64th"), Rest128th(
			"rest-128th"), Rest256th("rest-256th"), RestEighth("rest-eighth"), RestHalf("rest-half"), RestQuarter(
			"rest-quarter"), RestWhole("rest-whole"), TextNoteHalf("text-note-half"), TextNoteQuarter(
			"text-note-quarter"), TimeCommon("time-common");

	private String id;

	private CommonSymbol(String id) {
		this.id = id;
	}

	public String getID() {
		return this.id;
	}

	public static CommonSymbol getDigit(int digit) {
		switch (digit) {
		case 0:
			return Digit0;
		case 1:
			return Digit1;
		case 2:
			return Digit2;
		case 3:
			return Digit3;
		case 4:
			return Digit4;
		case 5:
			return Digit5;
		case 6:
			return Digit6;
		case 7:
			return Digit7;
		case 8:
			return Digit8;
		case 9:
			return Digit9;
		}
		throw new IllegalArgumentException("digit must be a number between 0 and 9.");
	}

	public static CommonSymbol getArticulation(Articulation.Type type) {
		switch (type) {
		case Accent:
			return ArticulationAccent;
		case Staccato:
			return ArticulationStaccato;
		case Staccatissimo:
			return ArticulationStaccatissimo;
		case StrongAccent:
			return ArticulationStrongAccent;
		case Tenuto:
			return ArticulationTenuto;
		}
		throw new IllegalArgumentException("unsupported articulation");
	}

	public static CommonSymbol getAccidental(Accidental.Type accType) {
		switch (accType) {
		case DoubleFlat:
			return AccidentalDoubleflat;
		case Flat:
			return AccidentalFlat;
		case Sharp:
			return AccidentalSharp;
		case DoubleSharp:
			return AccidentalDoublesharp;
		}
		return AccidentalNatural;
	}

	public static CommonSymbol getClef(ClefType clefType) {
		switch (clefType) {
		case F:
			return ClefF;
		case G:
			return ClefG;
		}
		throw new IllegalArgumentException("unsupported clef");
	}

	public static ArrayList<CommonSymbol> getDynamics(DynamicsType dynamicsType) {
		String name = dynamicsType.name();
		ArrayList ret = new ArrayList(name.length());
		for (int i = 0; i < name.length(); i++) {
			switch (name.charAt(i)) {
			case 'f':
				ret.add(DynamicsF);
				break;
			case 'm':
				ret.add(DynamicsM);
				break;
			case 'p':
				ret.add(DynamicsP);
				break;
			case 's':
				ret.add(DynamicsS);
				break;
			case 'z':
				ret.add(DynamicsZ);
			}
		}
		return ret;
	}

	public static CommonSymbol getPedal(Pedal.Type pedalType) {
		switch (pedalType) {
		case Start:
			return PedalSustainDown1;
		case Stop:
			return PedalSustainUp;
		}
		return null;
	}
}
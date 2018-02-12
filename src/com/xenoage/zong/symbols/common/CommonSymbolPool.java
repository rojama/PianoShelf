package com.xenoage.zong.symbols.common;

import com.xenoage.zong.symbols.Symbol;
import com.xenoage.zong.symbols.SymbolPool;
import com.xenoage.zong.symbols.WarningSymbol;

public class CommonSymbolPool {
	private Symbol[] symbols = new Symbol[0];

	public void update(SymbolPool pool) {
		this.symbols = new Symbol[CommonSymbol.values().length];
		int i = 0;
		for (CommonSymbol commonSymbol : CommonSymbol.values()) {
			this.symbols[i] = pool.getSymbol(commonSymbol.getID());
			i++;
		}
	}

	public Symbol getSymbol(CommonSymbol commonSymbol) {
		if (commonSymbol.ordinal() < this.symbols.length) {
			return this.symbols[commonSymbol.ordinal()];
		}

		return new WarningSymbol();
	}
}
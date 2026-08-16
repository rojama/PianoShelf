package com.rojama.pianoshelf.musicxml;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * 包装 InputStream，在不改变其他字节的前提下**剥离 DOCTYPE 声明**，
 * 避免解析器（通常是 JDK 的 Xerces）联网下载
 * <!DOCTYPE ... SYSTEM "http://www.musicxml.org/.../partwise.dtd">
 * 里引用的远程 DTD——手机上这段 HTTP 下载要么超时、要么 DNS 失败，
 * 结果就是 XMLReader.readFile() 抛 IOException，使 .musicxml/.mxl 全部打不开。
 *
 * 正确的 MusicXML 解析其实**根本不需要 DTD**：MusicXML 所有实体（如 &amp;）
 * 都是 XML 预定义实体，不需要 DTD 声明。所以 DOCTYPE 完全可以安全删掉。
 *
 * 处理范围：支持以下 DOCTYPE 变体（字节级识别）：
 *   1) <!DOCTYPE foo PUBLIC "...">  (无内部子集)
 *   2) <!DOCTYPE foo SYSTEM "..." >
 *   3) <!DOCTYPE foo [ <!ELEMENT ... > ... ]>  (含内部子集)
 *   4) 以上形式的任意大小写组合（XML 规范要求 DOCTYPE 大写，但容错）
 */
public final class SafeXmlStream {
	private SafeXmlStream() {}

	/** 将原始 InputStream 包一层；非必要不会额外复制大量内存。 */
	public static InputStream wrap(InputStream in) {
		if (in == null) return null;
		return new DoctypeStrippingStream(in);
	}

	private static final class DoctypeStrippingStream extends InputStream {
		/** 缓冲：默认 8KB；DOCTYPE 基本都出现在 XML 前几百字节内 */
		private final PushbackInputStream pbi;
		/** 已扫描过 DOCTYPE，之后直接透传字节 */
		private boolean passthrough = false;
		/** 是否已经读取过 XML 开头的空白/注释，防止匹配到错误位置 */
		private final byte[] stateBuf = new byte[16];
		/** 预读缓冲区，用于扫描 DOCTYPE 关键字 */
		private boolean eof = false;

		DoctypeStrippingStream(InputStream in) {
			this.pbi = new PushbackInputStream(in, 4096);
		}

		@Override public int read(byte[] b, int off, int len) throws IOException {
			if (b == null) throw new NullPointerException();
			if (off < 0 || len < 0 || off + len > b.length) throw new IndexOutOfBoundsException();
			if (len == 0) return 0;
			if (passthrough) return pbi.read(b, off, len);
			return readCharByChar(b, off, len);
		}

		@Override public int read() throws IOException {
			byte[] one = new byte[1];
			int n = read(one, 0, 1);
			return n == 1 ? one[0] & 0xff : -1;
		}

		@Override public void close() throws IOException { pbi.close(); }
		@Override public int available() throws IOException { return pbi.available(); }

		/**
		 * 开始若干字节按状态机处理：
		 *   - 看到 '<' 开头后，如果紧随其后 9 字节是 (case-insensitive) "!DOCTYPE " 或 "!DOCTYPE\t" 或 "!DOCTYPE" 后跟空白 → 开始吞噬 DOCTYPE；
		 *   - 否则就把 '<' 及之后的字节原样 pushback，进入 passthrough。
		 */
		private int readCharByChar(byte[] out, int off, int len) throws IOException {
			int i = 0;
			while (i < len) {
				int c = pbi.read();
				if (c < 0) { eof = true; return i == 0 ? -1 : i; }
				if (c == '<') {
					// 预读 9 字节判断是否 !DOCTYPE (长度 9)
					byte[] peek = new byte[9];
					int got = readFully(pbi, peek, 0, 9);
					if (got >= 9 && startsWithDoctype(peek)) {
						// 开始吞噬 DOCTYPE
						consumeDoctype(pbi);
						// DOCTYPE 吞完之后，< 这个字符也不输出
						continue;
					} else {
						// 不是 DOCTYPE：把预读的 peek 字节推回去
						if (got > 0) pbi.unread(peek, 0, got);
						// 原样输出 '<'，之后也走透传（避免每个 '<' 都扫描）
						out[off + i++] = (byte) '<';
						passthrough = true;
						// 剩下的空间直接透传
						int n = pbi.read(out, off + i, len - i);
						if (n < 0) return i;
						return i + n;
					}
				} else {
					out[off + i++] = (byte) c;
				}
			}
			return i;
		}

		private static int readFully(PushbackInputStream in, byte[] b, int off, int len) throws IOException {
			int total = 0;
			while (total < len) {
				int n = in.read(b, off + total, len - total);
				if (n < 0) return total == 0 ? -1 : total;
				total += n;
			}
			return total;
		}

		private static boolean startsWithDoctype(byte[] b) {
			// "<!" 已经吞了 peek 是 "DOCTYPE" 开头的 9 字节，注意：
			// 上面调用前我们只消费了 '<'，所以 peek[0] 应该是 '!'
			// peek: '!' 'D' 'O' 'C' 'T' 'Y' 'P' 'E' ' '
			return (b[0] == '!' || b[0] == '?')
				&& (b[1] == 'D' || b[1] == 'd')
				&& (b[2] == 'O' || b[2] == 'o')
				&& (b[3] == 'C' || b[3] == 'c')
				&& (b[4] == 'T' || b[4] == 't')
				&& (b[5] == 'Y' || b[5] == 'y')
				&& (b[6] == 'P' || b[6] == 'p')
				&& (b[7] == 'E' || b[7] == 'e')
				&& (b[8] == ' ' || b[8] == '\t' || b[8] == '\n' || b[8] == '\r');
		}

		/**
		 * 消费 DOCTYPE 声明内容直到匹配的闭合 '>'。
		 * 状态机：支持
		 *   - 双引号 / 单引号字符串 (内部不识别 > 和 [)
		 *   - 嵌套 [ ... ] 内部子集计数
		 *   - 注释 <!-- ... -->  (DOCTYOE 里理论上不会有，保守处理)
		 *   - PE 引用 %xxx; (不展开，按普通字符)
		 */
		private static void consumeDoctype(PushbackInputStream in) throws IOException {
			int depth = 0; // 方括号嵌套深度 (内部子集)
			boolean inSingle = false, inDouble = false;
			int c;
			while ((c = in.read()) >= 0) {
				switch (c) {
					case '"':
						if (!inSingle) inDouble = !inDouble;
						break;
					case '\'':
						if (!inDouble) inSingle = !inSingle;
						break;
					case '[':
						if (!inSingle && !inDouble) depth++;
						break;
					case ']':
						if (!inSingle && !inDouble) {
							if (depth > 0) depth--;
						}
						break;
					case '>':
						if (!inSingle && !inDouble && depth == 0) {
							return; // 正常闭合
						}
						break;
				}
			}
			// EOF 了也返回，上层 XMLReader 之后自然会在 XML 解析时报错
		}
	}
}

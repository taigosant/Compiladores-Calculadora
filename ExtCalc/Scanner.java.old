
package ExtCalc;

import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.HashMap;

class Token {
	public int kind;    // token kind
	public int pos;     // token position in bytes in the source text (starting at 0)
	public int charPos; // token position in characters in the source text (starting at 0)
	public int col;     // token column (starting at 1)
	public int line;    // token line (starting at 1)
	public String val;  // token value
	public Token next;  // ML 2005-03-11 Peek tokens are kept in linked list
}

//-----------------------------------------------------------------------------------
// Buffer
//-----------------------------------------------------------------------------------
class Buffer {
	// This Buffer supports the following cases:
	// 1) seekable stream (file)
	//    a) whole stream in buffer
	//    b) part of stream in buffer
	// 2) non seekable stream (network, console)

	public static final int EOF = Character.MAX_VALUE + 1;
	private static final int MIN_BUFFER_LENGTH = 1024; // 1KB
	private static final int MAX_BUFFER_LENGTH = MIN_BUFFER_LENGTH * 64; // 64KB
	private byte[] buf;   // input buffer
	private int bufStart; // position of first byte in buffer relative to input stream
	private int bufLen;   // length of buffer
	private int fileLen;  // length of input stream (may change if stream is no file)
	private int bufPos;      // current position in buffer
	private RandomAccessFile file; // input stream (seekable)
	private InputStream stream; // growing input stream (e.g.: console, network)

	public Buffer(InputStream s) {
		stream = s;
		fileLen = bufLen = bufStart = bufPos = 0;
		buf = new byte[MIN_BUFFER_LENGTH];
	}

	public Buffer(String fileName) {
		try {
			file = new RandomAccessFile(fileName, "r");
			fileLen = (int) file.length();
			bufLen = Math.min(fileLen, MAX_BUFFER_LENGTH);
			buf = new byte[bufLen];
			bufStart = Integer.MAX_VALUE; // nothing in buffer so far
			if (fileLen > 0) setPos(0); // setup buffer to position 0 (start)
			else bufPos = 0; // index 0 is already after the file, thus setPos(0) is invalid
			if (bufLen == fileLen) Close();
		} catch (IOException e) {
			throw new FatalError("Could not open file " + fileName);
		}
	}

	// don't use b after this call anymore
	// called in UTF8Buffer constructor
	protected Buffer(Buffer b) {
		buf = b.buf;
		bufStart = b.bufStart;
		bufLen = b.bufLen;
		fileLen = b.fileLen;
		bufPos = b.bufPos;
		file = b.file;
		stream = b.stream;
		// keep finalize from closing the file
		b.file = null;
	}

	protected void finalize() throws Throwable {
		super.finalize();
		Close();
	}

	protected void Close() {
		if (file != null) {
			try {
				file.close();
				file = null;
			} catch (IOException e) {
				throw new FatalError(e.getMessage());
			}
		}
	}

	public int Read() {
		if (bufPos < bufLen) {
			return buf[bufPos++] & 0xff;  // mask out sign bits
		} else if (getPos() < fileLen) {
			setPos(getPos());         // shift buffer start to pos
			return buf[bufPos++] & 0xff; // mask out sign bits
		} else if (stream != null && ReadNextStreamChunk() > 0) {
			return buf[bufPos++] & 0xff;  // mask out sign bits
		} else {
			return EOF;
		}
	}

	public int Peek() {
		int curPos = getPos();
		int ch = Read();
		setPos(curPos);
		return ch;
	}

	// beg .. begin, zero-based, inclusive, in byte
	// end .. end, zero-based, exclusive, in byte
	public String GetString(int beg, int end) {
		int len = 0;
		char[] buf = new char[end - beg];
		int oldPos = getPos();
		setPos(beg);
		while (getPos() < end) buf[len++] = (char) Read();
		setPos(oldPos);
		return new String(buf, 0, len);
	}

	public int getPos() {
		return bufPos + bufStart;
	}

	public void setPos(int value) {
		if (value >= fileLen && stream != null) {
			// Wanted position is after buffer and the stream
			// is not seek-able e.g. network or console,
			// thus we have to read the stream manually till
			// the wanted position is in sight.
			while (value >= fileLen && ReadNextStreamChunk() > 0);
		}

		if (value < 0 || value > fileLen) {
			throw new FatalError("buffer out of bounds access, position: " + value);
		}

		if (value >= bufStart && value < bufStart + bufLen) { // already in buffer
			bufPos = value - bufStart;
		} else if (file != null) { // must be swapped in
			try {
				file.seek(value);
				bufLen = file.read(buf);
				bufStart = value; bufPos = 0;
			} catch(IOException e) {
				throw new FatalError(e.getMessage());
			}
		} else {
			// set the position to the end of the file, Pos will return fileLen.
			bufPos = fileLen - bufStart;
		}
	}
	
	// Read the next chunk of bytes from the stream, increases the buffer
	// if needed and updates the fields fileLen and bufLen.
	// Returns the number of bytes read.
	private int ReadNextStreamChunk() {
		int free = buf.length - bufLen;
		if (free == 0) {
			// in the case of a growing input stream
			// we can neither seek in the stream, nor can we
			// foresee the maximum length, thus we must adapt
			// the buffer size on demand.
			byte[] newBuf = new byte[bufLen * 2];
			System.arraycopy(buf, 0, newBuf, 0, bufLen);
			buf = newBuf;
			free = bufLen;
		}
		
		int read;
		try { read = stream.read(buf, bufLen, free); }
		catch (IOException ioex) { throw new FatalError(ioex.getMessage()); }
		
		if (read > 0) {
			fileLen = bufLen = (bufLen + read);
			return read;
		}
		// end of stream reached
		return 0;
	}
}

//-----------------------------------------------------------------------------------
// UTF8Buffer
//-----------------------------------------------------------------------------------
class UTF8Buffer extends Buffer {
	UTF8Buffer(Buffer b) { super(b); }

	public int Read() {
		int ch;
		do {
			ch = super.Read();
			// until we find a utf8 start (0xxxxxxx or 11xxxxxx)
		} while ((ch >= 128) && ((ch & 0xC0) != 0xC0) && (ch != EOF));
		if (ch < 128 || ch == EOF) {
			// nothing to do, first 127 chars are the same in ascii and utf8
			// 0xxxxxxx or end of file character
		} else if ((ch & 0xF0) == 0xF0) {
			// 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
			int c1 = ch & 0x07; ch = super.Read();
			int c2 = ch & 0x3F; ch = super.Read();
			int c3 = ch & 0x3F; ch = super.Read();
			int c4 = ch & 0x3F;
			ch = (((((c1 << 6) | c2) << 6) | c3) << 6) | c4;
		} else if ((ch & 0xE0) == 0xE0) {
			// 1110xxxx 10xxxxxx 10xxxxxx
			int c1 = ch & 0x0F; ch = super.Read();
			int c2 = ch & 0x3F; ch = super.Read();
			int c3 = ch & 0x3F;
			ch = (((c1 << 6) | c2) << 6) | c3;
		} else if ((ch & 0xC0) == 0xC0) {
			// 110xxxxx 10xxxxxx
			int c1 = ch & 0x1F; ch = super.Read();
			int c2 = ch & 0x3F;
			ch = (c1 << 6) | c2;
		}
		return ch;
	}
}

//-----------------------------------------------------------------------------------
// StartStates  -- maps characters to start states of tokens
//-----------------------------------------------------------------------------------
class StartStates {
	private static class Elem {
		public int key, val;
		public Elem next;
		public Elem(int key, int val) { this.key = key; this.val = val; }
	}

	private Elem[] tab = new Elem[128];

	public void set(int key, int val) {
		Elem e = new Elem(key, val);
		int k = key % 128;
		e.next = tab[k]; tab[k] = e;
	}

	public int state(int key) {
		Elem e = tab[key % 128];
		while (e != null && e.key != key) e = e.next;
		return e == null ? 0: e.val;
	}
}

//-----------------------------------------------------------------------------------
// Scanner
//-----------------------------------------------------------------------------------
public class Scanner {
	static final char EOL = '\n';
	static final int  eofSym = 0;
	static final int maxT = 45;
	static final int noSym = 45;


	public Buffer buffer; // scanner buffer

	Token t;           // current token
	int ch;            // current input character
	int pos;           // byte position of current character
	int charPos;       // position by unicode characters starting with 0
	int col;           // column number of current character
	int line;          // line number of current character
	int oldEols;       // EOLs that appeared in a comment;
	static final StartStates start; // maps initial token character to start state
	static final Map literals;      // maps literal strings to literal kinds

	Token tokens;      // list of tokens already peeked (first token is a dummy)
	Token pt;          // current peek token
	
	char[] tval = new char[16]; // token text used in NextToken(), dynamically enlarged
	int tlen;          // length of current token


	static {
		start = new StartStates();
		literals = new HashMap();
		start.set(109, 133); 
		start.set(118, 134); 
		start.set(40, 8); 
		start.set(41, 9); 
		start.set(101, 10); 
		start.set(122, 11); 
		start.set(117, 15); 
		start.set(100, 135); 
		start.set(116, 136); 
		start.set(113, 137); 
		start.set(99, 138); 
		start.set(115, 139); 
		start.set(111, 140); 
		start.set(110, 141); 
		start.set(Buffer.EOF, -1);

	}
	
	public Scanner (String fileName) {
		buffer = new Buffer(fileName);
		Init();
	}
	
	public Scanner(InputStream s) {
		buffer = new Buffer(s);
		Init();
	}
	
	void Init () {
		pos = -1; line = 1; col = 0; charPos = -1;
		oldEols = 0;
		NextCh();
		if (ch == 0xEF) { // check optional byte order mark for UTF-8
			NextCh(); int ch1 = ch;
			NextCh(); int ch2 = ch;
			if (ch1 != 0xBB || ch2 != 0xBF) {
				throw new FatalError("Illegal byte order mark at start of file");
			}
			buffer = new UTF8Buffer(buffer); col = 0; charPos = -1;
			NextCh();
		}
		pt = tokens = new Token();  // first token is a dummy
	}
	
	void NextCh() {
		if (oldEols > 0) { ch = EOL; oldEols--; }
		else {
			pos = buffer.getPos();
			// buffer reads unicode chars, if UTF8 has been detected
			ch = buffer.Read(); col++; charPos++;
			// replace isolated '\r' by '\n' in order to make
			// eol handling uniform across Windows, Unix and Mac
			if (ch == '\r' && buffer.Peek() != '\n') ch = EOL;
			if (ch == EOL) { line++; col = 0; }
		}

	}
	
	void AddCh() {
		if (tlen >= tval.length) {
			char[] newBuf = new char[2 * tval.length];
			System.arraycopy(tval, 0, newBuf, 0, tval.length);
			tval = newBuf;
		}
		if (ch != Buffer.EOF) {
			tval[tlen++] = (char)ch; 

			NextCh();
		}

	}
	


	void CheckLiteral() {
		String val = t.val;

		Object kind = literals.get(val);
		if (kind != null) {
			t.kind = ((Integer) kind).intValue();
		}
	}

	Token NextToken() {
		while (ch == ' ' ||
			ch >= 9 && ch <= 10 || ch == 13
		) NextCh();

		int recKind = noSym;
		int recEnd = pos;
		t = new Token();
		t.pos = pos; t.col = col; t.line = line; t.charPos = charPos;
		int state = start.state(ch);
		tlen = 0; AddCh();

		loop: for (;;) {
			switch (state) {
				case -1: { t.kind = eofSym; break loop; } // NextCh already done 
				case 0: {
					if (recKind != noSym) {
						tlen = recEnd - t.pos;
						SetScannerBehindT();
					}
					t.kind = recKind; break loop;
				} // NextCh already done
				case 1:
					if (ch == 'i') {AddCh(); state = 2; break;}
					else {state = 0; break;}
				case 2:
					if (ch == 's') {AddCh(); state = 3; break;}
					else {state = 0; break;}
				case 3:
					{t.kind = 1; break loop;}
				case 4:
					if (ch == 'z') {AddCh(); state = 5; break;}
					else {state = 0; break;}
				case 5:
					if (ch == 'e') {AddCh(); state = 6; break;}
					else {state = 0; break;}
				case 6:
					if (ch == 's') {AddCh(); state = 7; break;}
					else {state = 0; break;}
				case 7:
					{t.kind = 2; break loop;}
				case 8:
					{t.kind = 3; break loop;}
				case 9:
					{t.kind = 4; break loop;}
				case 10:
					{t.kind = 5; break loop;}
				case 11:
					if (ch == 'e') {AddCh(); state = 12; break;}
					else {state = 0; break;}
				case 12:
					if (ch == 'r') {AddCh(); state = 13; break;}
					else {state = 0; break;}
				case 13:
					if (ch == 'o') {AddCh(); state = 14; break;}
					else {state = 0; break;}
				case 14:
					{t.kind = 6; break loop;}
				case 15:
					if (ch == 'm') {AddCh(); state = 16; break;}
					else {state = 0; break;}
				case 16:
					{t.kind = 7; break loop;}
				case 17:
					if (ch == 's') {AddCh(); state = 18; break;}
					else {state = 0; break;}
				case 18:
					{t.kind = 8; break loop;}
				case 19:
					{t.kind = 9; break loop;}
				case 20:
					if (ch == 'o') {AddCh(); state = 21; break;}
					else {state = 0; break;}
				case 21:
					{t.kind = 11; break loop;}
				case 22:
					if (ch == 'n') {AddCh(); state = 23; break;}
					else {state = 0; break;}
				case 23:
					if (ch == 't') {AddCh(); state = 24; break;}
					else {state = 0; break;}
				case 24:
					if (ch == 'e') {AddCh(); state = 25; break;}
					else {state = 0; break;}
				case 25:
					{t.kind = 17; break loop;}
				case 26:
					if (ch == 'n') {AddCh(); state = 27; break;}
					else {state = 0; break;}
				case 27:
					if (ch == 't') {AddCh(); state = 28; break;}
					else {state = 0; break;}
				case 28:
					if (ch == 'a') {AddCh(); state = 29; break;}
					else {state = 0; break;}
				case 29:
					{t.kind = 18; break loop;}
				case 30:
					if (ch == 'e') {AddCh(); state = 31; break;}
					else {state = 0; break;}
				case 31:
					if (ch == 'n') {AddCh(); state = 32; break;}
					else {state = 0; break;}
				case 32:
					if (ch == 't') {AddCh(); state = 33; break;}
					else {state = 0; break;}
				case 33:
					if (ch == 'a') {AddCh(); state = 34; break;}
					else {state = 0; break;}
				case 34:
					{t.kind = 19; break loop;}
				case 35:
					if (ch == 'u') {AddCh(); state = 36; break;}
					else {state = 0; break;}
				case 36:
					if (ch == 'e') {AddCh(); state = 37; break;}
					else {state = 0; break;}
				case 37:
					if (ch == 'n') {AddCh(); state = 38; break;}
					else {state = 0; break;}
				case 38:
					if (ch == 't') {AddCh(); state = 39; break;}
					else {state = 0; break;}
				case 39:
					if (ch == 'a') {AddCh(); state = 40; break;}
					else {state = 0; break;}
				case 40:
					{t.kind = 20; break loop;}
				case 41:
					if (ch == 's') {AddCh(); state = 42; break;}
					else {state = 0; break;}
				case 42:
					if (ch == 'e') {AddCh(); state = 43; break;}
					else {state = 0; break;}
				case 43:
					if (ch == 'n') {AddCh(); state = 44; break;}
					else {state = 0; break;}
				case 44:
					if (ch == 't') {AddCh(); state = 45; break;}
					else {state = 0; break;}
				case 45:
					if (ch == 'a') {AddCh(); state = 46; break;}
					else {state = 0; break;}
				case 46:
					{t.kind = 21; break loop;}
				case 47:
					if (ch == 't') {AddCh(); state = 48; break;}
					else {state = 0; break;}
				case 48:
					if (ch == 'a') {AddCh(); state = 49; break;}
					else {state = 0; break;}
				case 49:
					{t.kind = 22; break loop;}
				case 50:
					if (ch == 'n') {AddCh(); state = 51; break;}
					else {state = 0; break;}
				case 51:
					if (ch == 't') {AddCh(); state = 52; break;}
					else {state = 0; break;}
				case 52:
					if (ch == 'a') {AddCh(); state = 53; break;}
					else {state = 0; break;}
				case 53:
					{t.kind = 23; break loop;}
				case 54:
					if (ch == 't') {AddCh(); state = 55; break;}
					else {state = 0; break;}
				case 55:
					if (ch == 'a') {AddCh(); state = 56; break;}
					else {state = 0; break;}
				case 56:
					{t.kind = 24; break loop;}
				case 57:
					if (ch == 'z') {AddCh(); state = 58; break;}
					else {state = 0; break;}
				case 58:
					if (ch == 'e') {AddCh(); state = 59; break;}
					else {state = 0; break;}
				case 59:
					{t.kind = 25; break loop;}
				case 60:
					if (ch == 'e') {AddCh(); state = 61; break;}
					else {state = 0; break;}
				case 61:
					{t.kind = 26; break loop;}
				case 62:
					if (ch == 'r') {AddCh(); state = 63; break;}
					else {state = 0; break;}
				case 63:
					if (ch == 'z') {AddCh(); state = 64; break;}
					else {state = 0; break;}
				case 64:
					if (ch == 'e') {AddCh(); state = 65; break;}
					else {state = 0; break;}
				case 65:
					{t.kind = 28; break loop;}
				case 66:
					if (ch == 'e') {AddCh(); state = 67; break;}
					else {state = 0; break;}
				case 67:
					{t.kind = 29; break loop;}
				case 68:
					if (ch == 's') {AddCh(); state = 69; break;}
					else {state = 0; break;}
				case 69:
					{t.kind = 30; break loop;}
				case 70:
					if (ch == 'e') {AddCh(); state = 71; break;}
					else {state = 0; break;}
				case 71:
					{t.kind = 31; break loop;}
				case 72:
					if (ch == 'i') {AddCh(); state = 73; break;}
					else {state = 0; break;}
				case 73:
					if (ch == 't') {AddCh(); state = 74; break;}
					else {state = 0; break;}
				case 74:
					if (ch == 'o') {AddCh(); state = 75; break;}
					else {state = 0; break;}
				case 75:
					{t.kind = 32; break loop;}
				case 76:
					if (ch == 'o') {AddCh(); state = 77; break;}
					else {state = 0; break;}
				case 77:
					if (ch == 'v') {AddCh(); state = 78; break;}
					else {state = 0; break;}
				case 78:
					if (ch == 'e') {AddCh(); state = 79; break;}
					else {state = 0; break;}
				case 79:
					{t.kind = 33; break loop;}
				case 80:
					{t.kind = 34; break loop;}
				case 81:
					if (ch == 't') {AddCh(); state = 82; break;}
					else {state = 0; break;}
				case 82:
					if (ch == 'o') {AddCh(); state = 83; break;}
					else {state = 0; break;}
				case 83:
					{t.kind = 35; break loop;}
				case 84:
					if (ch == 'z') {AddCh(); state = 85; break;}
					else {state = 0; break;}
				case 85:
					if (ch == 'e') {AddCh(); state = 86; break;}
					else {state = 0; break;}
				case 86:
					if (ch == 'n') {AddCh(); state = 87; break;}
					else {state = 0; break;}
				case 87:
					if (ch == 't') {AddCh(); state = 88; break;}
					else {state = 0; break;}
				case 88:
					if (ch == 'o') {AddCh(); state = 89; break;}
					else {state = 0; break;}
				case 89:
					if (ch == 's') {AddCh(); state = 90; break;}
					else {state = 0; break;}
				case 90:
					{t.kind = 36; break loop;}
				case 91:
					if (ch == 't') {AddCh(); state = 92; break;}
					else {state = 0; break;}
				case 92:
					if (ch == 'o') {AddCh(); state = 93; break;}
					else {state = 0; break;}
				case 93:
					if (ch == 's') {AddCh(); state = 94; break;}
					else {state = 0; break;}
				case 94:
					{t.kind = 37; break loop;}
				case 95:
					if (ch == 'e') {AddCh(); state = 96; break;}
					else {state = 0; break;}
				case 96:
					if (ch == 'n') {AddCh(); state = 97; break;}
					else {state = 0; break;}
				case 97:
					if (ch == 't') {AddCh(); state = 98; break;}
					else {state = 0; break;}
				case 98:
					if (ch == 'o') {AddCh(); state = 99; break;}
					else {state = 0; break;}
				case 99:
					if (ch == 's') {AddCh(); state = 100; break;}
					else {state = 0; break;}
				case 100:
					{t.kind = 38; break loop;}
				case 101:
					if (ch == 'e') {AddCh(); state = 102; break;}
					else {state = 0; break;}
				case 102:
					if (ch == 'n') {AddCh(); state = 103; break;}
					else {state = 0; break;}
				case 103:
					if (ch == 't') {AddCh(); state = 104; break;}
					else {state = 0; break;}
				case 104:
					if (ch == 'o') {AddCh(); state = 105; break;}
					else {state = 0; break;}
				case 105:
					if (ch == 's') {AddCh(); state = 106; break;}
					else {state = 0; break;}
				case 106:
					{t.kind = 39; break loop;}
				case 107:
					if (ch == 'e') {AddCh(); state = 108; break;}
					else {state = 0; break;}
				case 108:
					if (ch == 'n') {AddCh(); state = 109; break;}
					else {state = 0; break;}
				case 109:
					if (ch == 't') {AddCh(); state = 110; break;}
					else {state = 0; break;}
				case 110:
					if (ch == 'o') {AddCh(); state = 111; break;}
					else {state = 0; break;}
				case 111:
					if (ch == 's') {AddCh(); state = 112; break;}
					else {state = 0; break;}
				case 112:
					{t.kind = 40; break loop;}
				case 113:
					if (ch == 'e') {AddCh(); state = 114; break;}
					else {state = 0; break;}
				case 114:
					if (ch == 'n') {AddCh(); state = 115; break;}
					else {state = 0; break;}
				case 115:
					if (ch == 't') {AddCh(); state = 116; break;}
					else {state = 0; break;}
				case 116:
					if (ch == 'o') {AddCh(); state = 117; break;}
					else {state = 0; break;}
				case 117:
					if (ch == 's') {AddCh(); state = 118; break;}
					else {state = 0; break;}
				case 118:
					{t.kind = 41; break loop;}
				case 119:
					if (ch == 'e') {AddCh(); state = 120; break;}
					else {state = 0; break;}
				case 120:
					if (ch == 'n') {AddCh(); state = 121; break;}
					else {state = 0; break;}
				case 121:
					if (ch == 't') {AddCh(); state = 122; break;}
					else {state = 0; break;}
				case 122:
					if (ch == 'o') {AddCh(); state = 123; break;}
					else {state = 0; break;}
				case 123:
					if (ch == 's') {AddCh(); state = 124; break;}
					else {state = 0; break;}
				case 124:
					{t.kind = 42; break loop;}
				case 125:
					if (ch == 'e') {AddCh(); state = 126; break;}
					else {state = 0; break;}
				case 126:
					if (ch == 'n') {AddCh(); state = 127; break;}
					else {state = 0; break;}
				case 127:
					if (ch == 't') {AddCh(); state = 128; break;}
					else {state = 0; break;}
				case 128:
					if (ch == 'o') {AddCh(); state = 129; break;}
					else {state = 0; break;}
				case 129:
					if (ch == 's') {AddCh(); state = 130; break;}
					else {state = 0; break;}
				case 130:
					{t.kind = 43; break loop;}
				case 131:
					if (ch == 'l') {AddCh(); state = 132; break;}
					else {state = 0; break;}
				case 132:
					{t.kind = 44; break loop;}
				case 133:
					if (ch == 'a') {AddCh(); state = 1; break;}
					else if (ch == 'i') {AddCh(); state = 131; break;}
					else {state = 0; break;}
				case 134:
					if (ch == 'e') {AddCh(); state = 4; break;}
					else if (ch == 'i') {AddCh(); state = 22; break;}
					else {state = 0; break;}
				case 135:
					if (ch == 'o') {AddCh(); state = 142; break;}
					else if (ch == 'e') {AddCh(); state = 143; break;}
					else if (ch == 'u') {AddCh(); state = 84; break;}
					else {state = 0; break;}
				case 136:
					if (ch == 'r') {AddCh(); state = 144; break;}
					else {state = 0; break;}
				case 137:
					if (ch == 'u') {AddCh(); state = 145; break;}
					else {state = 0; break;}
				case 138:
					if (ch == 'i') {AddCh(); state = 146; break;}
					else if (ch == 'e') {AddCh(); state = 147; break;}
					else {state = 0; break;}
				case 139:
					if (ch == 'e') {AddCh(); state = 148; break;}
					else {state = 0; break;}
				case 140:
					if (ch == 'i') {AddCh(); state = 149; break;}
					else if (ch == 'n') {AddCh(); state = 57; break;}
					else {state = 0; break;}
				case 141:
					if (ch == 'o') {AddCh(); state = 150; break;}
					else {state = 0; break;}
				case 142:
					if (ch == 'i') {AddCh(); state = 17; break;}
					else if (ch == 'z') {AddCh(); state = 60; break;}
					else {state = 0; break;}
				case 143:
					if (ch == 'z') {AddCh(); state = 151; break;}
					else {state = 0; break;}
				case 144:
					if (ch == 'e') {AddCh(); state = 152; break;}
					else if (ch == 'i') {AddCh(); state = 26; break;}
					else {state = 0; break;}
				case 145:
					if (ch == 'a') {AddCh(); state = 153; break;}
					else if (ch == 'i') {AddCh(); state = 154; break;}
					else {state = 0; break;}
				case 146:
					if (ch == 'n') {AddCh(); state = 155; break;}
					else {state = 0; break;}
				case 147:
					if (ch == 'm') {AddCh(); state = 80; break;}
					else if (ch == 'n') {AddCh(); state = 81; break;}
					else {state = 0; break;}
				case 148:
					if (ch == 'i') {AddCh(); state = 156; break;}
					else if (ch == 't') {AddCh(); state = 157; break;}
					else if (ch == 's') {AddCh(); state = 41; break;}
					else {state = 0; break;}
				case 149:
					if (ch == 't') {AddCh(); state = 158; break;}
					else {state = 0; break;}
				case 150:
					if (ch == 'v') {AddCh(); state = 159; break;}
					else {state = 0; break;}
				case 151:
					recEnd = pos; recKind = 16;
					if (ch == 'e') {AddCh(); state = 160; break;}
					else if (ch == 'o') {AddCh(); state = 72; break;}
					else {t.kind = 16; break loop;}
				case 152:
					if (ch == 's') {AddCh(); state = 19; break;}
					else if (ch == 'z') {AddCh(); state = 161; break;}
					else {state = 0; break;}
				case 153:
					if (ch == 't') {AddCh(); state = 162; break;}
					else if (ch == 'r') {AddCh(); state = 30; break;}
					else {state = 0; break;}
				case 154:
					if (ch == 'n') {AddCh(); state = 163; break;}
					else {state = 0; break;}
				case 155:
					if (ch == 'c') {AddCh(); state = 20; break;}
					else if (ch == 'q') {AddCh(); state = 35; break;}
					else {state = 0; break;}
				case 156:
					if (ch == 's') {AddCh(); state = 164; break;}
					else {state = 0; break;}
				case 157:
					if (ch == 'e') {AddCh(); state = 165; break;}
					else {state = 0; break;}
				case 158:
					if (ch == 'o') {AddCh(); state = 166; break;}
					else if (ch == 'e') {AddCh(); state = 50; break;}
					else {state = 0; break;}
				case 159:
					if (ch == 'e') {AddCh(); state = 167; break;}
					else {state = 0; break;}
				case 160:
					if (ch == 's') {AddCh(); state = 168; break;}
					else if (ch == 'n') {AddCh(); state = 76; break;}
					else {state = 0; break;}
				case 161:
					if (ch == 'e') {AddCh(); state = 169; break;}
					else {state = 0; break;}
				case 162:
					if (ch == 'r') {AddCh(); state = 170; break;}
					else if (ch == 'o') {AddCh(); state = 62; break;}
					else {state = 0; break;}
				case 163:
					if (ch == 'z') {AddCh(); state = 66; break;}
					else if (ch == 'h') {AddCh(); state = 101; break;}
					else {state = 0; break;}
				case 164:
					recEnd = pos; recKind = 12;
					if (ch == 'c') {AddCh(); state = 107; break;}
					else {t.kind = 12; break loop;}
				case 165:
					recEnd = pos; recKind = 13;
					if (ch == 'n') {AddCh(); state = 47; break;}
					else if (ch == 'c') {AddCh(); state = 113; break;}
					else {t.kind = 13; break loop;}
				case 166:
					recEnd = pos; recKind = 14;
					if (ch == 'c') {AddCh(); state = 119; break;}
					else {t.kind = 14; break loop;}
				case 167:
					recEnd = pos; recKind = 15;
					if (ch == 'n') {AddCh(); state = 54; break;}
					else if (ch == 'c') {AddCh(); state = 125; break;}
					else {t.kind = 15; break loop;}
				case 168:
					if (ch == 's') {AddCh(); state = 171; break;}
					else {state = 0; break;}
				case 169:
					recEnd = pos; recKind = 27;
					if (ch == 'n') {AddCh(); state = 91; break;}
					else {t.kind = 27; break loop;}
				case 170:
					if (ch == 'o') {AddCh(); state = 172; break;}
					else {state = 0; break;}
				case 171:
					if (ch == 'e') {AddCh(); state = 173; break;}
					else {state = 0; break;}
				case 172:
					recEnd = pos; recKind = 10;
					if (ch == 'c') {AddCh(); state = 95; break;}
					else {t.kind = 10; break loop;}
				case 173:
					if (ch == 'i') {AddCh(); state = 68; break;}
					else if (ch == 't') {AddCh(); state = 70; break;}
					else {state = 0; break;}

			}
		}
		t.val = new String(tval, 0, tlen);
		return t;
	}
	
	private void SetScannerBehindT() {
		buffer.setPos(t.pos);
		NextCh();
		line = t.line; col = t.col; charPos = t.charPos;
		for (int i = 0; i < tlen; i++) NextCh();
	}
	
	// get the next token (possibly a token already seen during peeking)
	public Token Scan () {
		if (tokens.next == null) {
			return NextToken();
		} else {
			pt = tokens = tokens.next;
			return tokens;
		}
	}

	// get the next token, ignore pragmas
	public Token Peek () {
		do {
			if (pt.next == null) {
				pt.next = NextToken();
			}
			pt = pt.next;
		} while (pt.kind > maxT); // skip pragmas

		return pt;
	}

	// make sure that peeking starts at current scan position
	public void ResetPeek () { pt = tokens; }

} // end Scanner

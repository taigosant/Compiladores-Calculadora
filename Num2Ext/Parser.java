
package Num2Ext;

public class Parser {
	public static final int _EOF = 0;
	public static final int maxT = 11;

	static final boolean T = true;
	static final boolean x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public Errors errors;

	public String result;

String extMount(String  unidade, String dezena){
	String result;
	if(dezena.isEmpty()){
		result = unidade;	
	}else{
		result = dezena + ((unidade == "zero") ? "" : (" e " + unidade)); 
	}
	return result;
}



	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new Errors();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}
	
	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			la = t;
		}
	}
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}
	
	void N2Ext() {
		String u = ""; String d = "";
		u = Unidade();
		result = u; 
		if (StartOf(1)) {
			d = Dezena();
		}
		result = extMount(u,d); 
	}

	String  Unidade() {
		String  v;
		v = ""; 
		switch (la.kind) {
		case 1: {
			Get();
			v = "zero"; 
			break;
		}
		case 2: {
			Get();
			v = "um"; 
			break;
		}
		case 3: {
			Get();
			v = "dois"; 
			break;
		}
		case 4: {
			Get();
			v = "tres"; 
			break;
		}
		case 5: {
			Get();
			v = "quatro"; 
			break;
		}
		case 6: {
			Get();
			v = "cinco"; 
			break;
		}
		case 7: {
			Get();
			v = "seis"; 
			break;
		}
		case 8: {
			Get();
			v = "sete"; 
			break;
		}
		case 9: {
			Get();
			v = "oito"; 
			break;
		}
		case 10: {
			Get();
			v = "nove"; 
			break;
		}
		default: SynErr(12); break;
		}
		return v;
	}

	String  Dezena() {
		String  v;
		v = ""; 
		switch (la.kind) {
		case 1: {
			Get();
			break;
		}
		case 2: {
			Get();
			v = "dez"; 
			break;
		}
		case 3: {
			Get();
			v = "vinte"; 
			break;
		}
		case 4: {
			Get();
			v = "trinta"; 
			break;
		}
		case 5: {
			Get();
			v = "quarenta"; 
			break;
		}
		case 6: {
			Get();
			v = "cinquenta"; 
			break;
		}
		case 7: {
			Get();
			v = "sessenta"; 
			break;
		}
		case 8: {
			Get();
			v = "setenta"; 
			break;
		}
		case 9: {
			Get();
			v = "oitenta"; 
			break;
		}
		case 10: {
			Get();
			v = "noventa"; 
			break;
		}
		default: SynErr(13); break;
		}
		return v;
	}



	public void Parse() {
		la = new Token();
		la.val = "";		
		Get();
		N2Ext();
		Expect(0);

	}

	private static final boolean[][] set = {
		{T,x,x,x, x,x,x,x, x,x,x,x, x},
		{x,T,T,T, T,T,T,T, T,T,T,x, x}

	};
} // end Parser


class Errors {
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.out;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "\"0\" expected"; break;
			case 2: s = "\"1\" expected"; break;
			case 3: s = "\"2\" expected"; break;
			case 4: s = "\"3\" expected"; break;
			case 5: s = "\"4\" expected"; break;
			case 6: s = "\"5\" expected"; break;
			case 7: s = "\"6\" expected"; break;
			case 8: s = "\"7\" expected"; break;
			case 9: s = "\"8\" expected"; break;
			case 10: s = "\"9\" expected"; break;
			case 11: s = "??? expected"; break;
			case 12: s = "invalid Unidade"; break;
			case 13: s = "invalid Dezena"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}

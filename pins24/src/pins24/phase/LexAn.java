package pins24.phase;

import java.io.*;
import pins24.common.*;
import pins24.common.Token.Symbol;

/**
 * Leksikalni analizator.
 */
public class LexAn implements AutoCloseable {

	/** Izvorna datoteka. */
	private final Reader srcFile;

	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public LexAn(final String srcFileName) {
		try {
			srcFile = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srcFileName))));
			nextChar(); // Pripravi prvi znak izvorne datoteke (glej {@link nextChar}).
		} catch (FileNotFoundException __) {
			throw new Report.Error("Source file '" + srcFileName + "' not found.");
		}
	}

	@Override
	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file.");
		}
	}

	/** Trenutni znak izvorne datoteke (glej {@link nextChar}). */
	private int buffChar = '\n';

	/** Vrstica trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharLine = 0;

	/** Stolpec trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharColumn = 0;

	/**
	 * Prebere naslednji znak izvorne datoteke.
	 * 
	 * Izvorno datoteko beremo znak po znak. Trenutni znak izvorne datoteke je
	 * shranjen v spremenljivki {@link buffChar}, vrstica in stolpec trenutnega
	 * znaka izvorne datoteke sta shranjena v spremenljivkah {@link buffCharLine} in
	 * {@link buffCharColumn}.
	 * 
	 * Zacetne vrednosti {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} so {@code '\n'}, {@code 0} in {@code 0}: branje prvega
	 * znaka izvorne datoteke bo na osnovi vrednosti {@code '\n'} spremenljivke
	 * {@link buffChar} prvemu znaku izvorne datoteke priredilo vrstico 1 in stolpec
	 * 1.
	 * 
	 * Pri branju izvorne datoteke se predpostavlja, da je v spremenljivki
	 * {@link buffChar} ves "cas veljaven znak. Zunaj metode {@link nextChar} so vse
	 * spremenljivke {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} namenjene le branju.
	 * 
	 * Vrednost {@code -1} v spremenljivki {@link buffChar} pomeni konec datoteke
	 * (vrednosti spremenljivk {@link buffCharLine} in {@link buffCharColumn} pa
	 * nista ve"c veljavni).
	 */
	private void nextChar() {
		try {
			switch (buffChar) {
			case -2: // Noben znak "se ni bil prebran.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? 0 : 1;
				buffCharColumn = buffChar == -1 ? 0 : 1;
				return;
			case -1: // Konec datoteke je bil "ze viden.
				return;
			case '\n': // Prejsnji znak je koncal vrstico, zacne se nova vrstica.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? buffCharLine : buffCharLine + 1;
				buffCharColumn = buffChar == -1 ? buffCharColumn : 1;
				return;
			case '\t': // Prejsnji znak je tabulator, ta znak je morda potisnjen v desno.
				buffChar = srcFile.read();
				while (buffCharColumn % 8 != 0)
					buffCharColumn += 1;
				buffCharColumn += 1;
				return;
			default: // Prejsnji znak je brez posebnosti.
				buffChar = srcFile.read();
				buffCharColumn += 1;
				return;
			}
		} catch (IOException __) {
			throw new Report.Error("Cannot read source file.");
		}
	}

	/**
	 * Trenutni leksikalni simbol.
	 * 
	 * "Ce vrednost spremenljivke {@code buffToken} ni {@code null}, je simbol "ze
	 * prebran iz vhodne datoteke, ni pa "se predan naprej sintaksnemu analizatorju.
	 * Ta simbol je dostopen z metodama {@link peekToken} in {@link takeToken}.
	 */
	private Token buffToken = null;

	/**
	 * Prebere naslednji leksikalni simbol, ki je nato dostopen preko metod
	 * {@link peekToken} in {@link takeToken}.
	 */
	private String ime(){	//meni pomozna funkcija za dobivanje imen spremenljivk
		String tmp = "";
		tmp += (char) buffChar;
		nextChar();
		while(buffChar >= 'a' && buffChar <= 'z' || buffChar >= 'A' && buffChar <= 'Z' || buffChar == '_' || buffChar >= '0' && buffChar <= '9'){
			tmp += (char) buffChar;
			nextChar();
		}
		return tmp;
	}
	private String znak(){ //rabm dokoncat, gledas za ' \ pa znak 10 alpa 13
		nextChar();
		if(buffChar == '\\'){ //ima backslash
			nextChar();
			if(buffChar == '\\' || buffChar == '\'' || buffChar == 'n' || (buffChar >= 'A' && buffChar <= 'F' || buffChar >= '0' && buffChar <= '9')){	//je veljaven backslash
				if(buffChar >= 'A' && buffChar <= 'F' || buffChar >= '0' && buffChar <= '9'){
					String znak = "\'\\" + (char) buffChar;
					nextChar();
					if(buffChar >= 'A' && buffChar <= 'F' || buffChar >= '0' && buffChar <= '9'){
						znak += (char) buffChar;
					}
					else throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn-3, buffCharLine, buffCharColumn), "Napacno escapan char!");
					znak += "\'";
					return znak;
				}
				char tmp = (char) buffChar;
				nextChar();
				if(buffChar != '\''){
					throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn-3, buffCharLine, buffCharColumn+1), "Char more biti dolzine 1."); //se pravilno zakljuci
				}
				return "'\\" + tmp + "'";
			}
			throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn-2, buffCharLine, buffCharColumn+1), "Napacno escapan char!");
		}
		if(buffChar >= 32 && buffChar <= 126){
			char tmp = (char) buffChar;
			nextChar();
			if(buffChar == '\''){
				return "'" + tmp + (char) buffChar;
			}
			throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn-2, buffCharLine, buffCharColumn+1), "Char more biti dolzine 1.");
		}
		throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn-1, buffCharLine, buffCharColumn), "Neprepoznan znak.");
	}
	private String niz(){	//meni pomozna funkcija za dobivanje stringov, da nimam spet spagetov
		String tmp = "\"";
		nextChar();
		while(buffChar != '"'){
			if(buffChar >= 32 && buffChar <= 126){
				if(buffChar == '\\'){
					tmp += (char) buffChar;
					nextChar();
					if(buffChar != '"' && buffChar != '\\' && buffChar != 'n' && !(buffChar >= 'A' && buffChar <= 'F' || buffChar >= '0' && buffChar <= '9')){
						throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn-tmp.length(), buffCharLine, buffCharColumn), "Napacno escapan string!");
					}
					if(buffChar >= 'A' && buffChar <= 'F' || buffChar >= '0' && buffChar <= '9'){
						tmp += (char) buffChar;
						nextChar();
						if(buffChar >= 'A' && buffChar <= 'F' || buffChar >= '0' && buffChar <= '9'){
							tmp += (char) buffChar;
						}
						else throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn-tmp.length(), buffCharLine, buffCharColumn), "Napacno escapan string!");
					}
					else tmp += (char) buffChar;
				}
				else tmp += (char) buffChar;
			}
			else throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn-tmp.length(), buffCharLine, buffCharColumn), "Neprepoznan znak v stringu.");
			nextChar();
		}
		return tmp + "\"";
	}

	private void nextToken() {
		// *** TODO ***
		/*
		 * template za metodo k zmer pozabm
		 * case 'znak':
		 * 	preveri kr rabš
		 * 	nastaviš na nextChar()
		 * 	nakonc zmer al return alpa break, k gre za switch
		 */
		while(true){
			switch(buffChar){
			case '+':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Symbol.ADD, "+");
				nextChar();
				return;

			case '*':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Symbol.MUL, "*");
				nextChar();
				return;

			case '/':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Symbol.DIV, "/");
				nextChar();
				return;

			case '%':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Symbol.MOD, "%");
				nextChar();
				return;

			case '^':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Symbol.PTR, "^");
				nextChar();
				return;

			case '(':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Symbol.LPAREN, "(");
				nextChar();
				return;

			case ')':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Symbol.RPAREN, ")");
				nextChar();
				return;

			case '-':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Symbol.SUB, "-");
				nextChar();
				return;

			case '=':
				nextChar();
				if(buffChar == '='){	//torej gre za == operacijo
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn-1, buffCharLine, buffCharColumn), Symbol.EQU, "==");
					nextChar();
				}
				else {
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn-1), Symbol.ASSIGN, "=");
				}
				return;

			case ' ': case '\n':  case '\t': case 13: //bela polja spustiš, zato je while(true), k itak brejkaš alpa returnaš
				nextChar();
				break;

			case '&':
				nextChar();
				if(buffChar == '&'){
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn-1, buffCharLine, buffCharColumn), Symbol.AND, "&&"); 
				}
				else{
					throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "neveljaven znak");
				}
				nextChar();
				return;
			case '|':
				nextChar();
				if(buffChar == '|'){
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn-1, buffCharLine, buffCharColumn), Symbol.OR, "||"); 
				}
				else{
					throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "neveljaven znak");
				}
				nextChar();
				return;
			
			case '!':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Symbol.NOT, "!");
				nextChar();
				if(buffChar == '=') {
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn-1, buffCharLine, buffCharColumn), Symbol.NEQ, "!=");
					nextChar();
				}
				return; 
				
			case '>':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Symbol.GTH, ">");
				nextChar();
				if(buffChar == '='){
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn-1, buffCharLine, buffCharColumn), Symbol.GEQ, ">=");
					nextChar();
				}
				return;

			case '<':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Symbol.LTH, "<");
				nextChar();
				if(buffChar == '='){
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn-1, buffCharLine, buffCharColumn), Symbol.LEQ, "<=");
					nextChar();
				}
				return;
			
			case '#': {
				int tmp = buffCharLine;
				while(tmp == buffCharLine && buffChar != -1){
					nextChar();
				}
				break;
			}
			
			case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': {
				String tmp = String.valueOf((char) buffChar);
				for(nextChar(); buffChar > 47 && buffChar < 58; nextChar()){
					tmp += String.valueOf((char) buffChar);
				}
				if(buffChar >= 'a' && buffChar <= 'z' ||buffChar >= 'A' && buffChar <= 'Z' || buffChar == '_'){ //torej se spremenljivka začne na cifro
					throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Spremenljivka se ne sme zacet na cifro!");
				}
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - tmp.length(), buffCharLine, buffCharColumn-1), Symbol.INTCONST, tmp);
				return;
			}
				
			case '\'':{
				String znak = znak();
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - znak.length()+1, buffCharLine, buffCharColumn), Symbol.CHARCONST, znak);
				nextChar();
				return;
			}	
			
			case '\"':{
				String niz = niz();
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - niz.length() + 1, buffCharLine, buffCharColumn), Symbol.STRINGCONST, niz);
				nextChar();
				return;
			}

			case ',':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Symbol.COMMA, ",");
				nextChar();
				return;

			case -1:
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Symbol.EOF, "EOF");
				return;

			default:
				if(buffChar >= 'a' && buffChar <= 'z' || buffChar >= 'A' && buffChar <= 'Z' || buffChar == '_'){
					String ime = ime();
					switch (ime){
						case "fun":
							buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - ime.length(), buffCharLine, buffCharColumn-1), Symbol.FUN, ime);
							return;
						case "var":
							buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - ime.length(), buffCharLine, buffCharColumn-1), Symbol.VAR, ime);
							return;
						case "if":
							buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - ime.length(), buffCharLine, buffCharColumn-1), Symbol.IF, ime);
							return;
						case "then":
							buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - ime.length(), buffCharLine, buffCharColumn-1), Symbol.THEN, ime);
							return;
						case "else":
							buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - ime.length(), buffCharLine, buffCharColumn-1), Symbol.ELSE, ime);
							return;
						case "while":
							buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - ime.length(), buffCharLine, buffCharColumn-1), Symbol.WHILE, ime);
							return;
						case "do":
							buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - ime.length(), buffCharLine, buffCharColumn-1), Symbol.DO, ime);
							return;
						case "let":
							buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - ime.length(), buffCharLine, buffCharColumn-1), Symbol.LET, ime);
							return;
						case "in":
							buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - ime.length(), buffCharLine, buffCharColumn-1), Symbol.IN, ime);
							return;
						case "end":
							buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - ime.length(), buffCharLine, buffCharColumn-1), Symbol.END, ime);
							return;
						default:
							buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn - ime.length(), buffCharLine, buffCharColumn-1), Symbol.IDENTIFIER, ime);
							return;
					}
				}
				else throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Neprepoznan znak.");
		}
		}
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki ostane v lastnistvu leksikalnega
	 * analizatorja.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token peekToken() {
		if (buffToken == null)
			nextToken();
		return buffToken;
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki preide v lastnistvo klicoce kode.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token takeToken() {
		if (buffToken == null)
			nextToken();
		final Token thisToken = buffToken;
		buffToken = null;
		return thisToken;
	}

	// --- ZAGON ---

	/**
	 * Zagon leksikalnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'24 compiler (lexical analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (LexAn lexAn = new LexAn(cmdLineArgs[0])) {
				while (lexAn.peekToken() != null)
					System.out.println(lexAn.takeToken());
			}

			// Upajmo, da kdaj pridemo to te tocke.
			// A zavedajmo se sledecega:
			// 1. Prevod je zaradi napak v programu lahko napacen :-o
			// 2. Izvorni program se zdalec ni tisto, kar je programer hotel, da bi bil ;-)
			Report.info("Done.");
		} catch (Report.Error error) {
			// Izpis opisa napake.
			System.err.println(error.getMessage());
			System.exit(1);
		}
	}

}

package pins24.phase;

import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition;

import pins24.common.*;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;

	/**
	 * Ustvari nov sintaksni analizator.
	 * 
	 * @param srcFileName ime izvorne datoteke.
	 */
	public SynAn(final String srcFileName) {
		this.lexAn = new LexAn(srcFileName);
	}

	@Override
	public void close() {
		lexAn.close();
	}

	/**
	 * Prevzame leksikalni analizator od leksikalnega analizatorja in preveri, ali
	 * je prave vrste.
	 * 
	 * @param symbol Pricakovana vrsta leksikalnega simbola.
	 * @return Prevzeti leksikalni simbol.
	 */
	public Token check(Token.Symbol symbol) {
		final Token token = lexAn.takeToken();
		if (token.symbol() != symbol)
			throw new Report.Error(token, "Nepricakovan simbol '" + token.lexeme() + "', pricakoval " + symbol);
		return token;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	public void parse() {
		parseProgram();
		if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
			Report.warning(lexAn.peekToken(),
					"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
	}

	/**
	 * Opravi sintaksno analizo celega programa.
	 */
	private void parseProgram() {
		switch(lexAn.peekToken().symbol()){
			case FUN:
				parseDefinition();
				parseDefinition2();
				return;
			case VAR:
				parseDefinition();
				parseDefinition2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakovana definicija");
		}
	}
	private void parseDefinition2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR:
				parseDefinition();
				parseDefinition2();
				return;
			case EOF:
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakovana definicija ali konec datoteke");
		}
	}
	private void parseDefinition(){
		switch(lexAn.peekToken().symbol()){
			case FUN:
				check(Token.Symbol.FUN);
				check(Token.Symbol.IDENTIFIER);
				check(Token.Symbol.LPAREN);
				parseParameters();
				check(Token.Symbol.RPAREN);
				parseDefinition3();
				return;
			case VAR:
				check(Token.Symbol.VAR);
				check(Token.Symbol.IDENTIFIER);
				check(Token.Symbol.ASSIGN);
				parseInitializers();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakovana definicija.");
		}
	}
	private void parseDefinition3(){
		switch(lexAn.peekToken().symbol()){
			case FUN:
				return;
			case ASSIGN:
				check(Token.Symbol.ASSIGN);
				parseStatements();
				return;
			case EOF:
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Definition3 pricakovan.");
		}
	}
	private void parseParameters(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER:
				check(Token.Symbol.IDENTIFIER);
				parseParameters2();
				return;
			case RPAREN:
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakovan identifier ali zaklepaj, dobil " + lexAn.peekToken().symbol() + " " + lexAn.peekToken().lexeme());
		}
	}
	private void parseParameters2(){
		switch(lexAn.peekToken().symbol()){
			case RPAREN:
				return;
			case COMMA:
				check(Token.Symbol.COMMA);
				parseParameters();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakovan zaklepaj ali vejica, dobil " + lexAn.peekToken().symbol() + " " + lexAn.peekToken().lexeme());
		}
	}
	private void parseStatements(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case IF: case WHILE: case LET: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseStatement();
				parseStatements2();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "Pricakovan identifier, oklepaj, if, while, let, add, minus, negacija, kazalec ali konstanta, dobil pa " + lexAn.peekToken().symbol());
		}
	}
	private void parseStatements2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case END: case IN: case ELSE: case EOF:
				return;
			case COMMA:
				check(Token.Symbol.COMMA);
				parseStatements();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakovan fun, var, end, in, else, EOF ali vejica, dobil " + lexAn.peekToken().symbol() + " " + lexAn.peekToken().lexeme());
		}
	}
	private void parseStatement(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseExpression();
				parseExpression2();
				return;
			case IF:
				check(Token.Symbol.IF);
				parseExpression();
				check(Token.Symbol.THEN);
				parseStatements();
				parseStatementElse();
				check(Token.Symbol.END);
				return;
			case WHILE:
				check(Token.Symbol.WHILE);
				parseExpression();
				check(Token.Symbol.DO);
				parseStatements();
				check(Token.Symbol.END);
				return;
			case LET:
				check(Token.Symbol.LET);
				parseDefinicije();
				check(Token.Symbol.IN);
				parseStatements();
				check(Token.Symbol.END);
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Nepricakovan znak, pricakuje se identifier lparen add sub not ptr intconst charconst stringconst if while ali let.");
		}
	}
	private void parseStatementElse(){
		switch(lexAn.peekToken().symbol()){
			case END:
				return;
			case ELSE:
				check(Token.Symbol.ELSE);
				parseStatements();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Nepricakovan znak, pricakuje se end ali else.");
		}
	}
	private void parseExpression2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case COMMA: case END: case IN: case ELSE: case EOF:
				return;
			case ASSIGN:
				check(Token.Symbol.ASSIGN);
				parseExpression();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Nepricakovan znak, pricakuje se fun var comma end in else konec datoteke ali assign.");
		}
	}
	private void parseDefinicije(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR:
				parseDefinition();
				parseDefinicije2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakovana je bila definicija funkcije ali spremenljivke.");
		}
	}
	private void parseDefinicije2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR:
				parseDefinicije();
				return;
			case IN:
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakuje se definicija ali IN.");
		}
	}
	private void parseExpression(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseEAnd();
				parseEOR();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakovan identifier lparen add sub not ptr intconst charconst ali stringconst.");
		}
	}
	private void parseEOR(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case EOF:
				return;
			case OR:
				check(Token.Symbol.OR);
				parseEAnd();
				parseEOR();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakuje se fun rparen assign var comma then end do in else konec datoteke ali or.");
		}
	}
	private void parseEAnd(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseEComp();
				parseEAnd2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakuje se identifier lparen add sub not ptr intconst charconst ali stringconst.");
		}
	}
	private void parseEAnd2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case EOF:
				return;
			case AND:
				check(Token.Symbol.AND);
				parseEComp();
				parseEAnd2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakuje se fun rparen assign var comma then end do in else konec datoteke or ali and.");
		}
	}
	private void parseEComp(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseEAdit();
				parseEComp2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakuje se identifier lparen add sub not ptr intconst charconst ali stringconst.");
		}
	}
	private void parseEComp2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF:
				return;
			case EQU:
				check(Token.Symbol.EQU);
				parseEAdit();
				return;
			case NEQ:
				check(Token.Symbol.NEQ);
				parseEAdit();
				return;
			case GTH:
				check(Token.Symbol.GTH);
				parseEAdit();
				return;
			case LTH:
				check(Token.Symbol.LTH);
				parseEAdit();
				return;
			case GEQ:
				check(Token.Symbol.GEQ);
				parseEAdit();
				return;
			case LEQ:
				check(Token.Symbol.LEQ);
				parseEAdit();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "Pricakuje se fun rparen assign var comma then end do in else or and eof equ neq gth lth geq ali leq.");
		}
	}
	private void parseEAdit(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseEMul();
				parseEAdit2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Pricakuje se identifier lparen add sub not ptr intconst charconst ali stringconst.");
		}
	}
	private void parseEAdit2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF: case EQU: case NEQ: case GTH: case LTH: case GEQ: case LEQ:
				return;
			case ADD:
				check(Token.Symbol.ADD);
				parseEMul();
				parseEAdit2();
				return;
			case SUB:
				check(Token.Symbol.SUB);
				parseEMul();
				parseEAdit2();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "EAdit2 napaka.");
		}
	}
	private void parseEMul(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case RPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseEPrefix();
				parseEMul2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "EMul napaka.");
		}
	}
	private void parseEMul2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF: case EQU: case NEQ: case GTH: case LTH: case GEQ: case LEQ: case ADD: case SUB:
				return;
			case MUL:
				check(Token.Symbol.MUL);
				parseEPrefix();
				parseEMul2();
				return;
			case DIV:
				check(Token.Symbol.DIV);
				parseEPrefix();
				parseEMul2();
				return;
			case MOD:
				check(Token.Symbol.MOD);
				parseEPrefix();
				parseEMul2();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "EMul2 napaka.");
		}
	}
	private void parseEPrefix(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case RPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parsePrefix();
				parseEPostfix();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "EPrefix napaka.");
		}
	}
	private void parsePrefix(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case RPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parsePrefix2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "Prefix napaka.");
		}
	}
	private void parsePrefix2(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case INTCONST: case CHARCONST: case STRINGCONST:
				return;
			case ADD: case SUB: case NOT: case PTR:
				parsePrefixOp();
				parsePrefix2();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "Prefix2 napaka.");
		}
	}
	private void parsePrefixOp(){
		switch(lexAn.peekToken().symbol()){
			case ADD:
				check(Token.Symbol.ADD);
				return;
			case SUB:
				check(Token.Symbol.SUB);
				return;
			case NOT:
				check(Token.Symbol.NOT);
				return;
			case PTR:
				check(Token.Symbol.PTR);
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "PrefixOp napaka.");
		}
	}
	private void parseEPostfix(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case INTCONST: case CHARCONST: case STRINGCONST:
				parseEKoncni();
				parsePostfix();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "EPostfix napaka.");
		}
	}
	private void parsePostfix(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF: case EQU: case NEQ: case GTH: case LTH: case GEQ: case LEQ: case ADD: case SUB: case MUL: case DIV: case MOD:
				return;
			case PTR:
				check(Token.Symbol.PTR);
				parsePostfix();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "Postfix napaka.");
		}
	}
	private void parseEKoncni(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: 
				check(Token.Symbol.IDENTIFIER);
				parseArgs();
				return;
			case LPAREN:
				check(Token.Symbol.LPAREN);
				parseExpression();
				check(Token.Symbol.RPAREN);
				return;
			case INTCONST:
				check(Token.Symbol.INTCONST);
				return;
			case CHARCONST:
				check(Token.Symbol.CHARCONST);
				return;
			case STRINGCONST:
				check(Token.Symbol.STRINGCONST);
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "EKoncni napaka.");
		}
	}
	private void parseArgs(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF: case EQU: case NEQ: case GTH: case LTH: case GEQ: case LEQ: case ADD: case SUB: case MUL: case DIV: case MOD: case PTR:
				return;
			case LPAREN:
				check(Token.Symbol.LPAREN);
				parseArguments();
				check(Token.Symbol.RPAREN);
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "Dobil nepricakovan znak " + lexAn.peekToken().symbol());
		}
	}
	private void parseArguments(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseExpression();
				parseArguments2();
				return;
			case RPAREN: 
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "Arguments napaka.");
		}
	}
	private void parseArguments2(){
		switch(lexAn.peekToken().symbol()){
			case RPAREN: 
				return;
			case COMMA:
				check(Token.Symbol.COMMA);
				parseInitializer();
				parseInitializers2();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "Arguments2 napaka.");
		}
	}
	private void parseInitializers(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case IN: case EOF:
				return;
			case INTCONST: case STRINGCONST: case CHARCONST:
				parseInitializer();
				parseInitializers2();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "Pricakoval fun, var, in, case, EOF ali konstanto, dobil " + lexAn.peekToken().symbol());
		}
	}
	private void parseInitializers2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case IN: case EOF:
				return;
			case COMMA:
				check(Token.Symbol.COMMA);
				parseInitializer();
				parseInitializers2();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "Initializers2 napaka.");
		}
	}
	private void parseInitializer(){
		switch(lexAn.peekToken().symbol()){
			case INTCONST: 
				check(Token.Symbol.INTCONST);
				parseInitializer2();
				return;
			case CHARCONST:
				check(Token.Symbol.CHARCONST);
				return;
			case STRINGCONST: 
				check(Token.Symbol.STRINGCONST);
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "Initializer napaka.");
		}
	}
	private void parseInitializer2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case COMMA: case IN: case EOF:
				return;
			case MUL:
				check(Token.Symbol.MUL);
				parseConst();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "Initializer2 napaka.");
		}
	}
	private void parseConst(){
		switch(lexAn.peekToken().symbol()){
			case INTCONST:
				check(Token.Symbol.INTCONST);
				return;
			case CHARCONST:
				check(Token.Symbol.CHARCONST);
				return;
			case STRINGCONST:
				check(Token.Symbol.STRINGCONST);
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "Const napaka.");
		}
	}
	// --- ZAGON ---

	/**
	 * Zagon sintaksnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'24 compiler (syntax analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				synAn.parse();
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

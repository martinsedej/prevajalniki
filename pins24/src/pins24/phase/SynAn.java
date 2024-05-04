package pins24.phase;

import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition;

import pins24.common.*;
import pins24.common.AST.AtomExpr.Type;

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
				throw new Report.Error(lexAn.peekToken(), "|program| Pricakovan fun ali var, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|definition2| Pricakovana fun, var ali konec datoteke, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|definition| Pricakovan var ali fun, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|definition3| Pricakovan fun, = ali EOF, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|parameters| Pricakovan identifier ali zaklepaj, dobil " + lexAn.peekToken().symbol() + " " + lexAn.peekToken().lexeme());
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
				throw new Report.Error(lexAn.peekToken(), "|parameters2| Pricakovan zaklepaj ali vejica, dobil " + lexAn.peekToken().symbol() + " " + lexAn.peekToken().lexeme());
		}
	}
	private void parseStatements(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case IF: case WHILE: case LET: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseStatement();
				parseStatements2();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|statements| Pricakovan identifier, oklepaj, if, while, let, add, minus, negacija, kazalec ali konstanta, dobil pa " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|statements2| Pricakovan fun, var, end, in, else, EOF ali vejica, dobil " + lexAn.peekToken().symbol() + " " + lexAn.peekToken().lexeme());
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
				throw new Report.Error(lexAn.peekToken(), "|statement| Pricakovan je identifier, oklepaj, plus, minus, negacija, kazalec, konstante, if while ali let, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|statementElse| Pricakovan end ali else, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|expression2| Pricakovan fun, var, vejica, end, in, else, EOF ali =, dobil " + lexAn.peekToken().symbol());
		}
	}
	private void parseDefinicije(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR:
				parseDefinition();
				parseDefinicije2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "|definicije| Pricakovan fun ali var, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|definicije2| Pricakovan fun, var ali in, dobil " + lexAn.peekToken().symbol());
		}
	}
	private void parseExpression(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseEAnd();
				parseEOR();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "|expression| Pricakovan identifier, oklepaj, plus, minus, negacija, kazalec ali konstanto, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|eOR| Pricakuje se fun, zaklepaj, =, var, vejica, then, end, do, in, else, konec datoteke ali or, dobil " + lexAn.peekToken().symbol());
		}
	}
	private void parseEAnd(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseEComp();
				parseEAnd2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eAnd| Pricakuje se identifier, oklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|eAnd2| Pricakuje se fun, zaklepaj, =, var, vejica, then, end, do, in, else, konec datoteke, or ali and, dobil " + lexAn.peekToken().symbol());
		}
	}
	private void parseEComp(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseEAdit();
				parseEComp2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eComp| Pricakuje se identifier, oklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|eComp2| Pricakuje se fun, zaklepaj, =, var, vejica, then, end, do, in, else, or, and, EOF, ==, !=, >, <, >= ali <=, dobil " + lexAn.peekToken().symbol());
		}
	}
	private void parseEAdit(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseEMul();
				parseEAdit2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eAdit| Pricakuje se identifier, oklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|eAdit2| Pricakovan plus, sub ali zacetek drugega stavka, dobil " + lexAn.peekToken().symbol());
		}
	}
	private void parseEMul(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parseEPrefix();
				parseEMul2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eMul| Pricakovan identifier, zaklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|eMul2| Pricakovan *, /, % ali zacetek drugega stavka, dobil " + lexAn.peekToken().symbol());
		}
	}
	private void parseEPrefix(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parsePrefix();
				parseEPostfix();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "|ePrefix| Pricakovan identifier, oklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private void parsePrefix(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				parsePrefix2();
				return;
			default:
				throw new Report.Error(lexAn.peekToken(), "|prefix| Pricakoval identifier, oklepaj, plus, minus, negacija, kazalec ali konstanto, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|prefix2| Pricakovan prefix, identifier, okepaj ali konstanta, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|prefixOp| Pricakoval prefix, dobil " + lexAn.peekToken().symbol());
		}
	}
	private void parseEPostfix(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case INTCONST: case CHARCONST: case STRINGCONST:
				parseEKoncni();
				parsePostfix();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|ePostfix| Pricakovan identifier, oklepaj ali konstanta, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|postfix| Pricakovan kazalec ali zacetek drugega stavka, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|eKoncni| Pricakovan identifier, oklepaj ali konstanta, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|args| Pricakoval oklepaj ali zacetek drugega stavka, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|arguments| Pricakoval identifier, (, ),  +, -, !, ^ ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private void parseArguments2(){
		switch(lexAn.peekToken().symbol()){
			case RPAREN: 
				return;
			case COMMA:
				check(Token.Symbol.COMMA);
				parseExpression();
				parseArguments2();
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|arguments2| Pricakoval ) ali vejico, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|initializers| Pricakoval fun, var, in, case, EOF ali konstanto, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|initializers2| Pricakoval vejico, fun, var, in ali EOF, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|initializer| Pricakovana konstanta, dobil " + lexAn.peekToken().symbol());
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
				throw new Report.Error(lexAn.peekToken(), "|initializer2| Pricakovana fun, var, vejica, in EOF ali mnozenje, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseConst(){
		switch(lexAn.peekToken().symbol()){
			case INTCONST:
				Token intc = check(Token.Symbol.INTCONST);
				AST.AtomExpr atom = new AST.AtomExpr(Type.INTCONST, intc.lexeme());
				attrAST.attrLoc.put(atom, intc);
				return atom;
			case CHARCONST:
				check(Token.Symbol.CHARCONST);
				return;
			case STRINGCONST:
				check(Token.Symbol.STRINGCONST);
				return;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|const| Pricakovana konstanta, dobil " + lexAn.peekToken().symbol());
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

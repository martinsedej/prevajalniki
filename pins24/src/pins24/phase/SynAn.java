package pins24.phase;

import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition;
import java.util.*;

import pins24.common.*;
import pins24.common.AST.AtomExpr.Type;
import pins24.common.AST.UnExpr.Oper;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;
	private HashMap<AST.Node, Report.Locatable> attrLoc;

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
	public AST.Node parse(HashMap<AST.Node, Report.Locatable> attrLoc) {
		this.attrLoc = attrLoc;
		final AST.Nodes<AST.MainDef> defs = parseProgram();
		if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
			Report.warning(lexAn.peekToken(),
				"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
		return defs;
}

	/**
	 * Opravi sintaksno analizo celega programa.
	 */
	private AST.Nodes<AST.MainDef> parseProgram() {
		switch(lexAn.peekToken().symbol()){
			case FUN: {
				AST.FunDef fun = parseDefinition();
				List<AST.MainDef> maindef = parseDefinition2();
				AST.Nodes<AST.MainDef> tmp;
				if(maindef == null){
					maindef.add(fun);
					tmp = new AST.Nodes<AST.MainDef>(maindef);
				}
				return tmp;
			}
			case VAR: {
				AST.VarDef var = parseDefinition();
				List<AST.MainDef> maindef = parseDefinition2();
				AST.Nodes<AST.MainDef> tmp;
				if(maindef == null){
					maindef.add(var);
					tmp = new AST.Nodes<AST.MainDef>(maindef);
				}
				return tmp;
			}
			default:
				throw new Report.Error(lexAn.peekToken(), "|program| Pricakovan fun ali var, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.MainDef> parseDefinition2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR:
				AST.MainDef maindef = parseDefinition();
				List<AST.MainDef> maindefs = parseDefinition2();
				if(maindefs == null){
					maindefs = new ArrayList<AST.MainDef>();
				}
				maindefs.add(maindef);
				return maindefs;
			case EOF:
				return null;
			default:
				throw new Report.Error(lexAn.peekToken(), "|definition2| Pricakovana fun, var ali konec datoteke, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.MainDef parseDefinition(){
		switch(lexAn.peekToken().symbol()){
			case FUN: {
				Token fun = check(Token.Symbol.FUN);
				Token id = check(Token.Symbol.IDENTIFIER);
				Token lparen = check(Token.Symbol.LPAREN);
				List<AST.ParDef> pars = parseParameters();
				Token rparen = check(Token.Symbol.RPAREN);
				List<AST.Stmt> stmts = parseDefinition3();
				AST.MainDef maindef = new AST.FunDef(id.lexeme(), pars, stmts);
				return maindef;
			}
			case VAR: {
				Token var = check(Token.Symbol.VAR);
				Token id = check(Token.Symbol.IDENTIFIER);
				Token assign = check(Token.Symbol.ASSIGN);
				List<AST.Init> inits =  parseInitializers();
				AST.MainDef maindef = new AST.VarDef(id.lexeme(), inits);
				return maindef;
			}
			default:
				throw new Report.Error(lexAn.peekToken(), "|definition| Pricakovan var ali fun, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.Stmt> parseDefinition3(){
		switch(lexAn.peekToken().symbol()){
			case FUN:
				return null;
			case ASSIGN:
				Token assign = check(Token.Symbol.ASSIGN);
				List<AST.Stmt> stmts = parseStatements();
				return stmts;
			case EOF:
				return null;
			default:
				throw new Report.Error(lexAn.peekToken(), "|definition3| Pricakovan fun, = ali EOF, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.ParDef> parseParameters(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER:
				Token id = check(Token.Symbol.IDENTIFIER);
				AST.ParDef pardef = new AST.ParDef(id.lexeme());
				List<AST.ParDef> pardefs = parseParameters2();
				if(pardefs == null){
					pardefs = new ArrayList<AST.ParDef>();
				}
				pardefs.add(pardef);
				return pardefs;
			case RPAREN:
				return null;
			default:
				throw new Report.Error(lexAn.peekToken(), "|parameters| Pricakovan identifier ali zaklepaj, dobil " + lexAn.peekToken().symbol() + " " + lexAn.peekToken().lexeme());
		}
	}
	private List<AST.ParDef> parseParameters2(){
		switch(lexAn.peekToken().symbol()){
			case RPAREN:
				return null;
			case COMMA:
				Token comma = check(Token.Symbol.COMMA);
				List<AST.ParDef> pardefs = parseParameters();
				return pardefs;
			default:
				throw new Report.Error(lexAn.peekToken(), "|parameters2| Pricakovan zaklepaj ali vejica, dobil " + lexAn.peekToken().symbol() + " " + lexAn.peekToken().lexeme());
		}
	}
	private List<AST.Stmt> parseStatements(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case IF: case WHILE: case LET: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Stmt stmt = parseStatement();
				List<AST.Stmt> stmts = parseStatements2();
				if(stmts == null){
					stmts = new ArrayList<AST.Stmt>();
				}
				stmts.add(stmt);
				return stmts;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|statements| Pricakovan identifier, oklepaj, if, while, let, add, minus, negacija, kazalec ali konstanta, dobil pa " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.Stmt> parseStatements2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case END: case IN: case ELSE: case EOF:
				return null;
			case COMMA:
				Token comma = check(Token.Symbol.COMMA);
				List<AST.Stmt> stmts = parseStatements();
				return stmts;
			default:
				throw new Report.Error(lexAn.peekToken(), "|statements2| Pricakovan fun, var, end, in, else, EOF ali vejica, dobil " + lexAn.peekToken().symbol() + " " + lexAn.peekToken().lexeme());
		}
	}
	private AST.Stmt parseStatement(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST: {
				AST.Expr expr = parseExpression();
				parseExpression2();
				AST.Stmt stmt = new AST.ExprStmt(expr);
				return stmt;
			}
			case IF: {
				Token if = check(Token.Symbol.IF);
				AST.Expr expr = parseExpression();
				check(Token.Symbol.THEN);
				parseStatements();
				parseStatementElse();
				check(Token.Symbol.END);
				return;
			}
			case WHILE: {
				check(Token.Symbol.WHILE);
				parseExpression();
				check(Token.Symbol.DO);
				parseStatements();
				check(Token.Symbol.END);
				return;
			}
			case LET: {
				check(Token.Symbol.LET);
				parseDefinicije();
				check(Token.Symbol.IN);
				parseStatements();
				check(Token.Symbol.END);
				return;
			}
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
				Token add = check(Token.Symbol.ADD);
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
	private AST.Expr parseEPostfix(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Expr expr = parseEKoncni();
				AST.Expr expr2 = parsePostfix();
				if(expr2 != null){

				}
				return expr;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|ePostfix| Pricakovan identifier, oklepaj ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.UnExpr parsePostfix(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF: case EQU: case NEQ: case GTH: case LTH: case GEQ: case LEQ: case ADD: case SUB: case MUL: case DIV: case MOD:
				return null;
			case PTR:
				Token ptr = check(Token.Symbol.PTR);
				AST.UnExpr expr = parsePostfix();
				if(expr == null){
					expr = new AST.UnExpr(Oper.VALUEAT, expr);
				}
				return expr;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|postfix| Pricakovan kazalec ali zacetek drugega stavka, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEKoncni(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: {
				Token id = check(Token.Symbol.IDENTIFIER);
				List<AST.Expr> exprs = parseArgs();
				AST.Expr expr;
				if(exprs == null){
					expr = new AST.VarExpr(id.lexeme());
				}
				else {
					expr = new AST.CallExpr(id.lexeme(), exprs);
				}
				return expr;
			}
			case LPAREN: {
				Token lparen = check(Token.Symbol.LPAREN);
				AST.Expr expr = parseExpression();
				Token rparen = check(Token.Symbol.RPAREN);
				return expr;
			}
			case INTCONST: {
				Token intc = check(Token.Symbol.INTCONST);
				AST.AtomExpr atom = new AST.AtomExpr(Type.INTCONST, intc.lexeme());
				return atom;
			}
			case CHARCONST: {
				Token charc = check(Token.Symbol.CHARCONST);
				AST.AtomExpr atom = new AST.AtomExpr(Type.CHRCONST, charc.lexeme());
				return atom;
			}
			case STRINGCONST: {
				Token stringc = check(Token.Symbol.STRINGCONST);
				AST.AtomExpr atom = new AST.AtomExpr(Type.STRCONST, stringc.lexeme());
				return atom;
			}
			default: 
				throw new Report.Error(lexAn.peekToken(), "|eKoncni| Pricakovan identifier, oklepaj ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.Expr> parseArgs(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF: case EQU: case NEQ: case GTH: case LTH: case GEQ: case LEQ: case ADD: case SUB: case MUL: case DIV: case MOD: case PTR:
				return null;
			case LPAREN:
				Token lparen = check(Token.Symbol.LPAREN);
				List<AST.Expr> exprs = parseArguments();
				Token rparen = check(Token.Symbol.RPAREN);
				return exprs;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|args| Pricakoval oklepaj ali zacetek drugega stavka, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.Expr> parseArguments(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Expr expr = parseExpression();
				List<AST.Expr> exprs = parseArguments2();
				if(exprs == null){
					exprs = new ArrayList<AST.Expr>();
				}
				exprs.add(expr);
				return exprs;
			case RPAREN: 
				return null;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|arguments| Pricakoval identifier, (, ),  +, -, !, ^ ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.Expr> parseArguments2(){
		switch(lexAn.peekToken().symbol()){
			case RPAREN: 
				return null;
			case COMMA:
				check(Token.Symbol.COMMA);
				AST.Expr expr = parseExpression();
				List<AST.Expr> exprs = parseArguments2();
				if(exprs == null){
					exprs = new ArrayList<AST.Expr>();
				}
				exprs.add(expr);
				return exprs;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|arguments2| Pricakoval ) ali vejico, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.Init> parseInitializers(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case IN: case EOF:
				return null;
			case INTCONST: case STRINGCONST: case CHARCONST:
				AST.Init init = parseInitializer();
				List<AST.Init> inits = parseInitializers2();
				if(inits == null){
					inits = new ArrayList<AST.Init>();
				}
				inits.add(init);
				return inits;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|initializers| Pricakoval fun, var, in, case, EOF ali konstanto, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.Init> parseInitializers2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case IN: case EOF:
				return null;
			case COMMA:
				Token comma = check(Token.Symbol.COMMA);
				AST.Init init = parseInitializer();
				attrLoc.put(init, comma);
				List<AST.Init> init2 = parseInitializers2();
				List<AST.Init> inits;
				if(init2 == null){
					inits = new ArrayList<AST.Init>();
				}
				else {
					inits = new ArrayList<AST.Init>(init2);
				}
				inits.add(init);
				return inits;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|initializers2| Pricakoval vejico, fun, var, in ali EOF, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Init parseInitializer(){
		switch(lexAn.peekToken().symbol()){
			case INTCONST: {
				Token intc = check(Token.Symbol.INTCONST);
				AST.AtomExpr value = new AST.AtomExpr(Type.INTCONST, intc.lexeme());
				attrLoc.put(value, intc);
				AST.AtomExpr atom = parseInitializer2();
				AST.Init init;
				if(atom == null){
					init = new AST.Init(new AST.AtomExpr(Type.INTCONST, "1"), value);
					attrLoc.put(init, new Report.Location(intc, attrLoc.get(value)));
				}
				else {
					init = new AST.Init(atom, value);
					attrLoc.put(init, new Report.Location(intc, attrLoc.get(atom)));
				}
				return init;
			}
			case CHARCONST:{
				Token charc = check(Token.Symbol.CHARCONST);
				AST.AtomExpr atom = new AST.AtomExpr(Type.CHRCONST, charc.lexeme());
				AST.Init init = new AST.Init(new AST.AtomExpr(Type.INTCONST, "1"), atom);
				attrLoc.put(init, new Report.Location(charc, attrLoc.get(atom)));
				attrLoc.put(atom, charc);
				return init;
			}	
			case STRINGCONST: {
				Token stringc = check(Token.Symbol.STRINGCONST);
				AST.AtomExpr atom = new AST.AtomExpr(Type.STRCONST, stringc.lexeme());
				AST.Init init = new AST.Init(new AST.AtomExpr(Type.INTCONST, "1"), atom);
				attrLoc.put(init, new Report.Location(stringc, attrLoc.get(atom)));
				attrLoc.put(atom, stringc);
				return init;
			}
			default: 
				throw new Report.Error(lexAn.peekToken(), "|initializer| Pricakovana konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.AtomExpr parseInitializer2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case COMMA: case IN: case EOF:
				return null;
			case MUL:
				Token mul = check(Token.Symbol.MUL);
				AST.AtomExpr atom = parseConst();
				attrLoc.put(atom, mul);
				return atom;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|initializer2| Pricakovana fun, var, vejica, in EOF ali mnozenje, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.AtomExpr parseConst(){
		switch(lexAn.peekToken().symbol()){
			case INTCONST:{
				Token intc = check(Token.Symbol.INTCONST);
				AST.AtomExpr atom = new AST.AtomExpr(Type.INTCONST, intc.lexeme());
				attrLoc.put(atom, intc);
				return atom;
			}
			case CHARCONST:{
				Token charc = check(Token.Symbol.CHARCONST);
				AST.AtomExpr atom = new AST.AtomExpr(Type.CHRCONST, charc.lexeme());
				attrLoc.put(atom, charc);
				return atom;
			}
			case STRINGCONST:
				Token stringc = check(Token.Symbol.STRINGCONST);
				AST.AtomExpr atom = new AST.AtomExpr(Type.STRCONST, stringc.lexeme());
				attrLoc.put(atom, stringc);
				return atom;
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

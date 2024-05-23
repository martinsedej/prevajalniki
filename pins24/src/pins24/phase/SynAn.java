package pins24.phase;

import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pins24.common.*;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;
	private HashMap<AST.Node, Report.Locatable> attrLoc;
	private List<Token> prefixLoc = new ArrayList<Token>();

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
	public AST.Node parse(HashMap<AST.Node, Report.Locatable> attrLoc) {
    this.attrLoc = attrLoc;
    final AST.Nodes<AST.MainDef> defs = parseProgram();
    if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
        Report.warning(lexAn.peekToken(),
            "Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
    return defs;
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
	private AST.Nodes<AST.MainDef> parseProgram() {
		switch(lexAn.peekToken().symbol()){
			case FUN: {
				AST.MainDef def = parseDefinition();
				List<AST.MainDef> maindefs = parseDefinition2();
				List<AST.MainDef> list = new ArrayList<AST.MainDef>();
				list.add(def);
				if(maindefs != null) list.addAll(maindefs);
				return new AST.Nodes<AST.MainDef>(list);
			}
			case VAR: {
				AST.MainDef def = parseDefinition();
				List<AST.MainDef> maindefs = parseDefinition2();
				List<AST.MainDef> list = new ArrayList<AST.MainDef>();
				list.add(def);
				if(maindefs != null) list.addAll(maindefs);
				return new AST.Nodes<AST.MainDef>(list);
			}
			default:
				throw new Report.Error(lexAn.peekToken(), "|program| Pricakovan fun ali var, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.MainDef> parseDefinition2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR:
				AST.MainDef def = parseDefinition();
				List<AST.MainDef> maindefs = parseDefinition2();
				List<AST.MainDef> list = new ArrayList<AST.MainDef>();
				list.add(def);
				if(maindefs != null) list.addAll(maindefs);
				return list;
			case EOF:
				return null;
			default:
				throw new Report.Error(lexAn.peekToken(), "|definition2| Pricakovana fun, var ali konec datoteke, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.MainDef parseDefinition(){
		switch(lexAn.peekToken().symbol()){
			case FUN: {
				Token start = check(Token.Symbol.FUN);
				Token id = check(Token.Symbol.IDENTIFIER);
				check(Token.Symbol.LPAREN);
				List<AST.ParDef> parms = parseParameters();
				if(parms == null) parms = new ArrayList<AST.ParDef>();
				Token end = check(Token.Symbol.RPAREN);
				List<AST.Stmt> stmts = parseDefinition3();
				if(stmts == null) stmts = new ArrayList<AST.Stmt>();
				AST.FunDef fun = new AST.FunDef(id.lexeme(), parms, stmts);
				if(stmts.size() == 0) attrLoc.put(fun, new Report.Location(start, end));
				else
					attrLoc.put(fun, new Report.Location(start, attrLoc.get(stmts.getLast())));
				return fun;
			}
			case VAR: {
				Token start = check(Token.Symbol.VAR);
				Token id = check(Token.Symbol.IDENTIFIER);
				Token end = check(Token.Symbol.ASSIGN);
				List<AST.Init> inits = parseInitializers();
				if(inits == null){
					inits = new ArrayList<AST.Init>();
				}
				if(inits.size() == 0) {
					AST.AtomExpr num = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1");
					AST.AtomExpr value = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "0");
					attrLoc.put(value, new Report.Location(0,0));
					AST.Init init = new AST.Init(num, value);
					attrLoc.put(init, new Report.Location(0,0));
					inits.add(init);
					AST.VarDef var = new AST.VarDef(id.lexeme(), inits);
					attrLoc.put(var, new Report.Location(start, end));
					return var;
				}
				AST.VarDef var = new AST.VarDef(id.lexeme(), inits);
				attrLoc.put(var, new Report.Location(start, attrLoc.get(inits.getLast())));
				return var;
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
				check(Token.Symbol.ASSIGN);
				return parseStatements();
			case EOF:
				return null;
			default:
				throw new Report.Error(lexAn.peekToken(), "|definition3| Pricakovan fun, = ali EOF, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.ParDef> parseParameters(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER:
				Token c = check(Token.Symbol.IDENTIFIER);
				AST.ParDef par = new AST.ParDef(c.lexeme());
				attrLoc.put(par, c);
				List<AST.ParDef> pars = parseParameters2();
				if(pars == null) pars = new ArrayList<AST.ParDef>();
				pars.add(0, par);
				return pars;
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
				check(Token.Symbol.COMMA);
				return parseParameters();
			default:
				throw new Report.Error(lexAn.peekToken(), "|parameters2| Pricakovan zaklepaj ali vejica, dobil " + lexAn.peekToken().symbol() + " " + lexAn.peekToken().lexeme());
		}
	}
	private List<AST.Stmt> parseStatements(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case IF: case WHILE: case LET: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Stmt stmt = parseStatement();
				List<AST.Stmt> stmts = parseStatements2();
				List<AST.Stmt> list = new ArrayList<AST.Stmt>();
				list.add(stmt);
				if(stmts != null) list.addAll(stmts);
				return list;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|statements| Pricakovan identifier, oklepaj, if, while, let, add, minus, negacija, kazalec ali konstanta, dobil pa " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.Stmt> parseStatements2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case END: case IN: case ELSE: case EOF:
				return null;
			case COMMA:
				check(Token.Symbol.COMMA);
				return parseStatements();
			default:
				throw new Report.Error(lexAn.peekToken(), "|statements2| Pricakovan fun, var, end, in, else, EOF ali vejica, dobil " + lexAn.peekToken().symbol() + " " + lexAn.peekToken().lexeme());
		}
	}
	private AST.Stmt parseStatement(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST: {
				AST.Expr expr = parseExpression();
				AST.Expr expr2 = parseExpression2();
				if(expr2 != null) {
					AST.Stmt assign = new AST.AssignStmt(expr, expr2);
					attrLoc.put(assign, new Report.Location(attrLoc.get(expr), attrLoc.get(expr2)));
					return assign;
				}
				AST.ExprStmt stmt = new AST.ExprStmt(expr);
				attrLoc.put(stmt, attrLoc.get(expr));
				return stmt;
			}
			case IF: {
				Token start = check(Token.Symbol.IF);
				AST.Expr expr = parseExpression();
				check(Token.Symbol.THEN);
				List<AST.Stmt> statements = parseStatements();
				List<AST.Stmt> elseStatements = parseStatementElse();
				Token end = check(Token.Symbol.END);
				if(elseStatements == null) elseStatements = new ArrayList<AST.Stmt>();
				AST.IfStmt stmt = new AST.IfStmt(expr, statements, elseStatements);
				attrLoc.put(stmt, new Report.Location(start, end));
				return stmt;
			}
			case WHILE: {
				Token start = check(Token.Symbol.WHILE);
				AST.Expr expr = parseExpression();
				check(Token.Symbol.DO);
				List<AST.Stmt> stmts = parseStatements();
				Token end = check(Token.Symbol.END);
				AST.Stmt stmt = new AST.WhileStmt(expr, stmts);
				attrLoc.put(stmt, new Report.Location(start, end));
				return stmt;
			}
			case LET: {
				Token start = check(Token.Symbol.LET);
				List<AST.MainDef> maindefs = parseDefinicije();
				check(Token.Symbol.IN);
				List<AST.Stmt> stmts = parseStatements();
				Token end = check(Token.Symbol.END);
				AST.Stmt stmt = new AST.LetStmt(maindefs, stmts);
				attrLoc.put(stmt, new Report.Location(start, end));
				return stmt;
			}
			default:
				throw new Report.Error(lexAn.peekToken(), "|statement| Pricakovan je identifier, oklepaj, plus, minus, negacija, kazalec, konstante, if while ali let, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.Stmt> parseStatementElse(){
		switch(lexAn.peekToken().symbol()){
			case END:
				return null;
			case ELSE:
				check(Token.Symbol.ELSE);
				List<AST.Stmt> statements = parseStatements();
				return statements;
			default:
				throw new Report.Error(lexAn.peekToken(), "|statementElse| Pricakovan end ali else, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseExpression2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case COMMA: case END: case IN: case ELSE: case EOF:
				return null;
			case ASSIGN:
				check(Token.Symbol.ASSIGN);
				return parseExpression();
			default:
				throw new Report.Error(lexAn.peekToken(), "|expression2| Pricakovan fun, var, vejica, end, in, else, EOF ali =, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.MainDef> parseDefinicije(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR:
				AST.MainDef def = parseDefinition();
				List<AST.MainDef> maindefs = parseDefinicije2();
				List<AST.MainDef> list = new ArrayList<AST.MainDef>();
				list.add(def);
				if(maindefs != null) list.addAll(maindefs);
				return list;
			default:
				throw new Report.Error(lexAn.peekToken(), "|definicije| Pricakovan fun ali var, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.MainDef> parseDefinicije2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR:
				return parseDefinicije();
			case IN:
				return null;
			default:
				throw new Report.Error(lexAn.peekToken(), "|definicije2| Pricakovan fun, var ali in, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseExpression(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Expr and = parseEAnd();
				AST.Expr or = parseEOR(and);
				if(or == null) return and;
				return or;
			default:
				throw new Report.Error(lexAn.peekToken(), "|expression| Pricakovan identifier, oklepaj, plus, minus, negacija, kazalec ali konstanto, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEOR(AST.Expr and){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case EOF:
				return null;
			case OR:
				check(Token.Symbol.OR);
				AST.Expr and2 = parseEAnd();
				AST.BinExpr bin = new AST.BinExpr(AST.BinExpr.Oper.OR, and, and2);
				attrLoc.put(bin, new Report.Location(attrLoc.get(and), attrLoc.get(and2)));
				AST.Expr or = parseEOR(bin);
				if(or == null) return bin;
				return or;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eOR| Pricakuje se fun, zaklepaj, =, var, vejica, then, end, do, in, else, konec datoteke ali or, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEAnd(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Expr comp = parseEComp();
				AST.Expr and = parseEAnd2(comp);
				if(and == null) return comp;
				return and;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eAnd| Pricakuje se identifier, oklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEAnd2(AST.Expr expr){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case EOF:
				return null;
			case AND:
				check(Token.Symbol.AND);
				AST.Expr comp = parseEComp();
				AST.BinExpr bin = new AST.BinExpr(AST.BinExpr.Oper.AND, expr, comp);
				attrLoc.put(bin, new Report.Location(attrLoc.get(expr), attrLoc.get(comp)));
				AST.Expr and = parseEAnd2(bin);
				if(and == null) return bin;
				return and;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eAnd2| Pricakuje se fun, zaklepaj, =, var, vejica, then, end, do, in, else, konec datoteke, or ali and, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEComp(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Expr expr = parseEAdit();
				AST.Expr comp = parseEComp2(expr);
				if(comp == null) {
					return expr;
				}
				return comp;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eComp| Pricakuje se identifier, oklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEComp2(AST.Expr expr){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF:
				return null;
			case EQU: {
				check(Token.Symbol.EQU);
				AST.Expr adit = parseEAdit();
				AST.BinExpr comp = new AST.BinExpr(AST.BinExpr.Oper.EQU, expr, adit);
				attrLoc.put(comp, new Report.Location(attrLoc.get(expr), attrLoc.get(adit)));
				return comp;
			}
			case NEQ:{
				check(Token.Symbol.NEQ);
				AST.Expr adit = parseEAdit();
				AST.BinExpr comp = new AST.BinExpr(AST.BinExpr.Oper.NEQ, expr, adit);
				attrLoc.put(comp, new Report.Location(attrLoc.get(expr), attrLoc.get(adit)));
				return comp;
			}
			case GTH:{
				check(Token.Symbol.GTH);
				AST.Expr adit = parseEAdit();
				AST.BinExpr comp = new AST.BinExpr(AST.BinExpr.Oper.GTH, expr, adit);
				attrLoc.put(comp, new Report.Location(attrLoc.get(expr), attrLoc.get(adit)));
				return comp;
			}
			case LTH:{
				check(Token.Symbol.LTH);
				AST.Expr adit = parseEAdit();
				AST.BinExpr comp = new AST.BinExpr(AST.BinExpr.Oper.LTH, expr, adit);
				attrLoc.put(comp, new Report.Location(attrLoc.get(expr), attrLoc.get(adit)));
				return comp;
			}
			case GEQ:{
				check(Token.Symbol.GEQ);
				AST.Expr adit = parseEAdit();
				AST.BinExpr comp = new AST.BinExpr(AST.BinExpr.Oper.GEQ, expr, adit);
				attrLoc.put(comp, new Report.Location(attrLoc.get(expr), attrLoc.get(adit)));
				return comp;
			}
			case LEQ:{
				check(Token.Symbol.LEQ);
				AST.Expr adit = parseEAdit();
				AST.BinExpr comp = new AST.BinExpr(AST.BinExpr.Oper.LEQ, expr, adit);
				attrLoc.put(comp, new Report.Location(attrLoc.get(expr), attrLoc.get(adit)));
				return comp;
			}
			default: 
				throw new Report.Error(lexAn.peekToken(), "|eComp2| Pricakuje se fun, zaklepaj, =, var, vejica, then, end, do, in, else, or, and, EOF, ==, !=, >, <, >= ali <=, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEAdit(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Expr mulExpr = parseEMul();
				AST.Expr zaVrnt = parseEAdit2(mulExpr);
				if(zaVrnt == null) zaVrnt  = mulExpr;
				return zaVrnt;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eAdit| Pricakuje se identifier, oklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEAdit2(AST.Expr mulExpr){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF: case EQU: case NEQ: case GTH: case LTH: case GEQ: case LEQ:
				return null;
			case ADD: {
				check(Token.Symbol.ADD);
				AST.Expr naprej = parseEMul();
				AST.BinExpr bin = new AST.BinExpr(AST.BinExpr.Oper.ADD, mulExpr, naprej);
				attrLoc.put(bin, new Report.Location(attrLoc.get(mulExpr), attrLoc.get(naprej)));
				AST.Expr isto = parseEAdit2(bin);
				if(isto == null) return bin;
				return isto;
			}
			case SUB:{
				check(Token.Symbol.SUB);
				AST.Expr naprej = parseEMul();
				AST.BinExpr bin = new AST.BinExpr(AST.BinExpr.Oper.SUB, mulExpr, naprej);
				attrLoc.put(bin, new Report.Location(attrLoc.get(mulExpr), attrLoc.get(naprej)));
				AST.Expr isto = parseEAdit2(bin);
				if(isto == null) return bin;
				return isto;
			}
			default: 
				throw new Report.Error(lexAn.peekToken(), "|eAdit2| Pricakovan plus, sub ali zacetek drugega stavka, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEMul(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Expr prefixExpr = parseEPrefix();
				AST.Expr zaVrnt = parseEMul2(prefixExpr);
				if(zaVrnt == null) zaVrnt = prefixExpr;
				return zaVrnt;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eMul| Pricakovan identifier, zaklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEMul2(AST.Expr prefixExpr){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF: case EQU: case NEQ: case GTH: case LTH: case GEQ: case LEQ: case ADD: case SUB:
				return null;
			case MUL: {
				check(Token.Symbol.MUL);
				AST.Expr naprej = parseEPrefix();
				AST.BinExpr bin = new AST.BinExpr(AST.BinExpr.Oper.MUL, prefixExpr, naprej);
				attrLoc.put(bin, new Report.Location(attrLoc.get(prefixExpr), attrLoc.get(naprej)));
				AST.Expr isto = parseEMul2(bin);
				if(isto == null) return bin;
				return isto;
			}
			case DIV:{
				check(Token.Symbol.DIV);
				AST.Expr naprej = parseEPrefix();
				AST.BinExpr bin = new AST.BinExpr(AST.BinExpr.Oper.DIV, prefixExpr, naprej);
				attrLoc.put(bin, new Report.Location(attrLoc.get(prefixExpr), attrLoc.get(naprej)));
				AST.Expr isto = parseEMul2(bin);
				if(isto == null) return bin;
				return isto;
			}
			case MOD:{
				check(Token.Symbol.MOD);
				AST.Expr naprej = parseEPrefix();
				AST.BinExpr bin = new AST.BinExpr(AST.BinExpr.Oper.MOD, prefixExpr, naprej);
				attrLoc.put(bin, new Report.Location(attrLoc.get(prefixExpr), attrLoc.get(naprej)));
				AST.Expr isto = parseEMul2(bin);
				if(isto == null) return bin;
				return isto;
			}
			default: 
				throw new Report.Error(lexAn.peekToken(), "|eMul2| Pricakovan *, /, % ali zacetek drugega stavka, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEPrefix(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				List<AST.UnExpr.Oper> prefixneOperacije = parsePrefix();
				AST.Expr zaNazaj = parseEPostfix();
				if(prefixneOperacije != null){
					for(int i = prefixneOperacije.size()-1; i >= 0; i--){
						AST.Expr tmp = zaNazaj;
						zaNazaj = new AST.UnExpr(prefixneOperacije.get(i), zaNazaj);
						attrLoc.put(zaNazaj, new Report.Location(prefixLoc.get(i), attrLoc.get(tmp)));
					}
					prefixLoc = new ArrayList<Token>();
				}
				return zaNazaj;
			default:
				throw new Report.Error(lexAn.peekToken(), "|ePrefix| Pricakovan identifier, oklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.UnExpr.Oper> parsePrefix(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				return parsePrefix2();
			default:
				throw new Report.Error(lexAn.peekToken(), "|prefix| Pricakoval identifier, oklepaj, plus, minus, negacija, kazalec ali konstanto, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.UnExpr.Oper> parsePrefix2(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case INTCONST: case CHARCONST: case STRINGCONST:
				return null;
			case ADD: case SUB: case NOT: case PTR:
				AST.UnExpr.Oper oper = parsePrefixOp();
				List<AST.UnExpr.Oper> zaVrnt = new ArrayList<AST.UnExpr.Oper>();
				zaVrnt.add(oper);
				List<AST.UnExpr.Oper> operacije = parsePrefix2();
				if(operacije != null) {
					zaVrnt.addAll(operacije);
				}
				return zaVrnt;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|prefix2| Pricakovan prefix, identifier, okepaj ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.UnExpr.Oper parsePrefixOp(){
		switch(lexAn.peekToken().symbol()){
			case ADD: {
				Token c = check(Token.Symbol.ADD);
				prefixLoc.add(c);
				return AST.UnExpr.Oper.ADD;
			}
			case SUB:{
				Token c = check(Token.Symbol.SUB);
				prefixLoc.add(c);
				return AST.UnExpr.Oper.SUB;
			}
			case NOT:{
				Token c = check(Token.Symbol.NOT);
				prefixLoc.add(c);
				return AST.UnExpr.Oper.NOT;
			}
			case PTR:{
				Token c = check(Token.Symbol.PTR);
				prefixLoc.add(c);
				return AST.UnExpr.Oper.MEMADDR;
			}
			default: 
				throw new Report.Error(lexAn.peekToken(), "|prefixOp| Pricakoval prefix, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEPostfix(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Expr value = parseEKoncni();
				AST.Expr expr = parsePostfix(value);
				if(expr == null) expr = value;
				return expr;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|ePostfix| Pricakovan identifier, oklepaj ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parsePostfix(AST.Expr value){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF: case EQU: case NEQ: case GTH: case LTH: case GEQ: case LEQ: case ADD: case SUB: case MUL: case DIV: case MOD:
				return null;
			case PTR:
				Token c = check(Token.Symbol.PTR);
				AST.UnExpr expr = new AST.UnExpr(AST.UnExpr.Oper.VALUEAT, value);
				attrLoc.put(expr, new Report.Location(attrLoc.get(value), c));
				AST.Expr postfix = parsePostfix(expr);
				if(postfix == null) return expr;
				return postfix;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|postfix| Pricakovan kazalec ali zacetek drugega stavka, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEKoncni(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: {
				Token c = check(Token.Symbol.IDENTIFIER);
				Token[] arr = new Token[2];
				List<AST.Expr> args = parseArgs(arr);
				if(args == null) {
					AST.VarExpr var  = new AST.VarExpr(c.lexeme());
					attrLoc.put(var, c);
					return var;
				}
				AST.CallExpr call = new AST.CallExpr(c.lexeme(), args);
				attrLoc.put(call, new Report.Location(c, arr[1]));
				return call;
			}
				
			case LPAREN: {
				Token start = check(Token.Symbol.LPAREN);
				AST.Expr expr = parseExpression();
				Token end = check(Token.Symbol.RPAREN);
				attrLoc.put(expr, new Report.Location(start, end));
				return expr;
			}
			case INTCONST: {
				Token c = check(Token.Symbol.INTCONST);
				AST.AtomExpr atom = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, c.lexeme());
				attrLoc.put(atom, c);
				return atom;
			}
			case CHARCONST:{
				Token c = check(Token.Symbol.CHARCONST);
				AST.AtomExpr atom = new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, c.lexeme());
				attrLoc.put(atom, c);
				return atom;
			}
			case STRINGCONST:{
				Token c = check(Token.Symbol.STRINGCONST);
				AST.AtomExpr atom = new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, c.lexeme());
				attrLoc.put(atom, c);
				return atom;
			}
			default: 
				throw new Report.Error(lexAn.peekToken(), "|eKoncni| Pricakovan identifier, oklepaj ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.Expr> parseArgs(Token[] arr){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF: case EQU: case NEQ: case GTH: case LTH: case GEQ: case LEQ: case ADD: case SUB: case MUL: case DIV: case MOD: case PTR:
				return null;
			case LPAREN:
				Token start = check(Token.Symbol.LPAREN);
				List<AST.Expr> args = parseArguments();
				Token end = check(Token.Symbol.RPAREN);
				arr[0] = start;
				arr[1] = end;
				return args;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|args| Pricakoval oklepaj ali zacetek drugega stavka, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.Expr> parseArguments(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Expr expr = parseExpression();
				List<AST.Expr> args = parseArguments2(null);
				List<AST.Expr> list = new ArrayList<AST.Expr>();
				list.add(expr);
				if(args != null) list.addAll(args);
				return list;
			case RPAREN: 
				return new ArrayList<AST.Expr>();
			default: 
				throw new Report.Error(lexAn.peekToken(), "|arguments| Pricakoval identifier, (, ),  +, -, !, ^ ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.Expr> parseArguments2(List<AST.Expr> list){
		switch(lexAn.peekToken().symbol()){
			case RPAREN: 
				return null;
			case COMMA:
				check(Token.Symbol.COMMA);
				AST.Expr expr = parseExpression();
				if(list == null) list = new ArrayList<AST.Expr>();
				list.add(expr);
				parseArguments2(list);
				return list;
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
				List<AST.Init> list = new ArrayList<AST.Init>();
				list.add(init);
				if(inits != null) list.addAll(inits);
				return list;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|initializers| Pricakoval fun, var, in, case, EOF ali konstanto, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.Init> parseInitializers2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case IN: case EOF:
				return new ArrayList<AST.Init>();
			case COMMA:
				check(Token.Symbol.COMMA);
				AST.Init init = parseInitializer();
				List<AST.Init> inits = parseInitializers2();
				List<AST.Init> list = new ArrayList<AST.Init>();
				list.add(init);
				if(inits != null) list.addAll(inits);
				return list;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|initializers2| Pricakoval vejico, fun, var, in ali EOF, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Init parseInitializer(){
		switch(lexAn.peekToken().symbol()){
			case INTCONST: {
				Token c = check(Token.Symbol.INTCONST);
				AST.AtomExpr atom = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, c.lexeme());
				attrLoc.put(atom, c);
				AST.Init init = parseInitializer2(atom);
				if(init == null) {
					init = new AST.Init(new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1"), atom);
					attrLoc.put(init, c);
				}
				return init;
			}
			case CHARCONST: {
				Token c = check(Token.Symbol.CHARCONST);
				AST.AtomExpr atom = new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, c.lexeme());
				attrLoc.put(atom, c);
				AST.Init init = new AST.Init(new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1"), atom);
				attrLoc.put(init, c);
				return init;
			}
			case STRINGCONST: {
				Token c = check(Token.Symbol.STRINGCONST);
				AST.AtomExpr atom = new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, c.lexeme());
				attrLoc.put(atom, c);
				AST.Init init = new AST.Init(new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1"), atom);
				attrLoc.put(init, c);
				return init;
			}
			default: 
				throw new Report.Error(lexAn.peekToken(), "|initializer| Pricakovana konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Init parseInitializer2(AST.AtomExpr atom){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case COMMA: case IN: case EOF:
				return null;
			case MUL:
				check(Token.Symbol.MUL);
				AST.AtomExpr value = parseConst();
				AST.Init init = new AST.Init(atom, value);
				attrLoc.put(init, new Report.Location(attrLoc.get(atom), attrLoc.get(value)));
				return init;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|initializer2| Pricakovana fun, var, vejica, in EOF ali mnozenje, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.AtomExpr parseConst(){
		switch(lexAn.peekToken().symbol()){
			case INTCONST: {
				Token c = check(Token.Symbol.INTCONST);
				AST.AtomExpr atom = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, c.lexeme());
				attrLoc.put(atom, c);
				return atom;
			}
			case CHARCONST:{
				Token c = check(Token.Symbol.CHARCONST);
				AST.AtomExpr atom = new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, c.lexeme());
				attrLoc.put(atom, c);
				return atom;
			}
			case STRINGCONST:{
				Token c = check(Token.Symbol.STRINGCONST);
				AST.AtomExpr atom = new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, c.lexeme());
				attrLoc.put(atom, c);
				return atom;
			}
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

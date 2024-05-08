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
				AST.MainDef fun = parseDefinition();
				List<AST.MainDef> maindef = parseDefinition2();
				AST.Nodes<AST.MainDef> tmp;
				if(maindef == null){
					maindef = new ArrayList<AST.MainDef>();
				}
				maindef.add(fun);
				tmp = new AST.Nodes<AST.MainDef>(maindef);
				return tmp;
			}
			case VAR: {
				AST.MainDef var = parseDefinition();
				List<AST.MainDef> maindef = parseDefinition2();
				AST.Nodes<AST.MainDef> tmp;
				if(maindef == null){
					maindef = new ArrayList<AST.MainDef>();
				}
				maindef.add(var);
				tmp = new AST.Nodes<AST.MainDef>(maindef);
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
				AST.Stmt stmt = new AST.AssignStmt(expr, parseExpression2());
				return stmt;
			}
			case IF: {
				Token token_if = check(Token.Symbol.IF);
				AST.Expr condition = parseExpression();
				Token then = check(Token.Symbol.THEN);
				List<AST.Stmt> stmts = parseStatements();
				List<AST.Stmt> stmtsElse = parseStatementElse();
				Token end = check(Token.Symbol.END);
				AST.IfStmt ifStmt = new AST.IfStmt(condition, stmts, stmtsElse);
				return ifStmt;
			}
			case WHILE: {
				Token whileToken = check(Token.Symbol.WHILE);
				AST.Expr expr = parseExpression();
				Token doToken = check(Token.Symbol.DO);
				List<AST.Stmt> stmts = parseStatements();
				Token endToken = check(Token.Symbol.END);
				AST.Stmt stmt = new AST.WhileStmt(expr, stmts);
				return stmt;
			}
			case LET: {
				Token letToken = check(Token.Symbol.LET);
				List<AST.MainDef> maindefs = parseDefinicije();
				Token inToken = check(Token.Symbol.IN);
				List<AST.Stmt> stmts = parseStatements();
				Token endToken = check(Token.Symbol.END);
				AST.LetStmt letstmt = new AST.LetStmt(maindefs, stmts);
				return letstmt;
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
				Token elseToken = check(Token.Symbol.ELSE);
				List<AST.Stmt> stmts = parseStatements();
				return stmts;
			default:
				throw new Report.Error(lexAn.peekToken(), "|statementElse| Pricakovan end ali else, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseExpression2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR: case COMMA: case END: case IN: case ELSE: case EOF:
				return null;
			case ASSIGN:
				Token assign = check(Token.Symbol.ASSIGN);
				AST.Expr expr = parseExpression();
				return expr;
			default:
				throw new Report.Error(lexAn.peekToken(), "|expression2| Pricakovan fun, var, vejica, end, in, else, EOF ali =, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.MainDef> parseDefinicije(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR:
				AST.MainDef maindef = parseDefinition();
				List<AST.MainDef> maindefs = parseDefinicije2();
				if(maindefs == null){
					maindefs = new ArrayList<AST.MainDef>();
				}
				maindefs.add(maindef);
				return maindefs;
			default:
				throw new Report.Error(lexAn.peekToken(), "|definicije| Pricakovan fun ali var, dobil " + lexAn.peekToken().symbol());
		}
	}
	private List<AST.MainDef> parseDefinicije2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case VAR:
				List<AST.MainDef> maindefs = parseDefinicije();
				return maindefs;
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
				AST.Expr or = parseEOR();
				if(or == null){
					return and;
				}
				AST.BinExpr expr = new AST.BinExpr(AST.BinExpr.Oper.OR, and, or);
				return expr;
			default:
				throw new Report.Error(lexAn.peekToken(), "|expression| Pricakovan identifier, oklepaj, plus, minus, negacija, kazalec ali konstanto, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEOR(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case EOF:
				return null;
			case OR:
				Token or = check(Token.Symbol.OR);
				AST.Expr and = parseEAnd();
				AST.Expr or2 = parseEOR();
				if(or2 == null){
					return and;
				}
				AST.BinExpr expr = new AST.BinExpr(AST.BinExpr.Oper.OR, and, or2);
				return expr;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eOR| Pricakuje se fun, zaklepaj, =, var, vejica, then, end, do, in, else, konec datoteke ali or, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEAnd(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Expr comp = parseEComp();
				AST.Expr and2 = parseEAnd2();
				if(and2 == null){
					return comp;
				}
				AST.BinExpr expr = new AST.BinExpr(AST.BinExpr.Oper.AND, comp, and2);
				return expr;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eAnd| Pricakuje se identifier, oklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEAnd2(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case EOF:
				return null;
			case AND:
				Token and = check(Token.Symbol.AND);
				AST.Expr comp = parseEComp();
				AST.Expr and2 = parseEAnd2();
				if(and2 == null){
					return comp;
				}
				AST.BinExpr expr = new AST.BinExpr(AST.BinExpr.Oper.AND, comp, and2);
				return expr;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eAnd2| Pricakuje se fun, zaklepaj, =, var, vejica, then, end, do, in, else, konec datoteke, or ali and, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEComp(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Expr adit = parseEAdit();
				AST.BinExpr.Oper[] tipOperacije = new AST.BinExpr.Oper[1];
				AST.Expr comp2 = parseEComp2(tipOperacije);
				if(comp2 == null){
					return adit;
				}
				AST.BinExpr expr = new AST.BinExpr(tipOperacije[0], adit, comp2);
				return expr;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eComp| Pricakuje se identifier, oklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEComp2(AST.BinExpr.Oper[] tipOperacije){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF:
				return null;
			case EQU:{
				Token equ = check(Token.Symbol.EQU);
				tipOperacije[0] = AST.BinExpr.Oper.EQU;
				AST.Expr expr = parseEAdit();
				return expr;
			}
			case NEQ: {
				Token neq = check(Token.Symbol.NEQ);
				tipOperacije[0] = AST.BinExpr.Oper.NEQ;
				AST.Expr expr = parseEAdit();
				return expr;
			}
			case GTH: {
				Token gth = check(Token.Symbol.GTH);
				tipOperacije[0] = AST.BinExpr.Oper.GTH;
				AST.Expr expr = parseEAdit();
				return expr;
			}
			case LTH: {
				Token lth = check(Token.Symbol.LTH);
				tipOperacije[0] = AST.BinExpr.Oper.LTH;
				AST.Expr expr = parseEAdit();
				return expr;
			}
			case GEQ: {
				Token geq = check(Token.Symbol.GEQ);
				tipOperacije[0] = AST.BinExpr.Oper.GEQ;
				AST.Expr expr = parseEAdit();
				return expr;
			}
			case LEQ: {
				Token leq = check(Token.Symbol.LEQ);
				tipOperacije[0] = AST.BinExpr.Oper.LEQ;
				AST.Expr expr = parseEAdit();
				return expr;
			}
			default: 
				throw new Report.Error(lexAn.peekToken(), "|eComp2| Pricakuje se fun, zaklepaj, =, var, vejica, then, end, do, in, else, or, and, EOF, ==, !=, >, <, >= ali <=, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEAdit(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Expr mul = parseEMul();
				AST.BinExpr.Oper[] tipOperacije = new AST.BinExpr.Oper[1];
				AST.Expr adit2 = parseEAdit2(tipOperacije);
				if(adit2 == null){
					return mul;
				}
				AST.BinExpr expr = new AST.BinExpr(tipOperacije[0], mul, adit2);
				return expr;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eAdit| Pricakuje se identifier, oklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEAdit2(AST.BinExpr.Oper[] tipOperacije){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF: case EQU: case NEQ: case GTH: case LTH: case GEQ: case LEQ:
				return null;
			case ADD: {
				Token add = check(Token.Symbol.ADD);
				tipOperacije[0] = AST.BinExpr.Oper.ADD;
				AST.Expr mul = parseEMul();
				AST.Expr adit2 = parseEAdit2(null);
				if(adit2 == null) return mul;
				AST.BinExpr expr = new AST.BinExpr(AST.BinExpr.Oper.ADD, mul, adit2);
				return expr;
			}
			case SUB: {
				Token sub = check(Token.Symbol.SUB);
				tipOperacije[0] = AST.BinExpr.Oper.SUB;
				AST.Expr mul = parseEMul();
				AST.Expr adit2 = parseEAdit2(null);
				if(adit2 == null) return mul;
				AST.BinExpr expr = new AST.BinExpr(AST.BinExpr.Oper.SUB, mul, adit2);
				return expr;
			}
			default: 
				throw new Report.Error(lexAn.peekToken(), "|eAdit2| Pricakovan plus, sub ali zacetek drugega stavka, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEMul(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.UnExpr prefix = parseEPrefix();
				AST.BinExpr.Oper[] tipOperacije = new AST.BinExpr.Oper[1];
				AST.Expr mul2 = parseEMul2(tipOperacije);
				if(mul2 == null) return prefix;
				AST.BinExpr expr = new AST.BinExpr(tipOperacije[0], prefix, mul2);
				return expr;
			default:
				throw new Report.Error(lexAn.peekToken(), "|eMul| Pricakovan identifier, zaklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEMul2(AST.BinExpr.Oper[] tipOperacije){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF: case EQU: case NEQ: case GTH: case LTH: case GEQ: case LEQ: case ADD: case SUB:
				return null;
			case MUL: {
				Token mul = check(Token.Symbol.MUL);
				AST.UnExpr prefix = parseEPrefix();
				AST.Expr mul2= parseEMul2(null);
				if(mul2 == null) return prefix;
				AST.BinExpr expr = new AST.BinExpr(AST.BinExpr.Oper.MUL, prefix, mul2);
				tipOperacije[0] = AST.BinExpr.Oper.MUL;
				return expr;
			}
			case DIV: {
				Token div = check(Token.Symbol.MUL);
				AST.UnExpr prefix = parseEPrefix();
				AST.Expr mul2= parseEMul2(null);
				if(mul2 == null) return prefix;
				AST.BinExpr expr = new AST.BinExpr(AST.BinExpr.Oper.DIV, prefix, mul2);
				tipOperacije[0] = AST.BinExpr.Oper.MUL;
				return expr;
			}
			case MOD: {
				Token mod = check(Token.Symbol.MUL);
				AST.UnExpr prefix = parseEPrefix();
				AST.Expr mul2= parseEMul2(null);
				if(mul2 == null) return prefix;
				AST.BinExpr expr = new AST.BinExpr(AST.BinExpr.Oper.MOD, prefix, mul2);
				tipOperacije[0] = AST.BinExpr.Oper.MUL;
				return expr;
			}
			default: 
				throw new Report.Error(lexAn.peekToken(), "|eMul2| Pricakovan *, /, % ali zacetek drugega stavka, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.UnExpr parseEPrefix(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST: {
				AST.UnExpr prefix = parsePrefix();
				AST.Expr postfix = parseEPostfix();
				if(postfix == null) return prefix;
				AST.UnExpr expr = new AST.UnExpr(prefix.oper, postfix);
				return expr;
			}
			default:
				throw new Report.Error(lexAn.peekToken(), "|ePrefix| Pricakovan identifier, oklepaj, plus, minus, negacija, kazalec ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.UnExpr parsePrefix(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case ADD: case SUB: case NOT: case PTR: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.UnExpr expr = parsePrefix2();
				if(expr == null) return null;
				return expr;
			default:
				throw new Report.Error(lexAn.peekToken(), "|prefix| Pricakoval identifier, oklepaj, plus, minus, negacija, kazalec ali konstanto, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.UnExpr parsePrefix2(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case INTCONST: case CHARCONST: case STRINGCONST:
				return null;
			case ADD: case SUB: case NOT: case PTR:
				AST.UnExpr.Oper oper = parsePrefixOp();
				AST.UnExpr unExpr= parsePrefix2();
				AST.UnExpr expr = new AST.UnExpr(oper, unExpr);
				return expr;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|prefix2| Pricakovan prefix, identifier, okepaj ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.UnExpr.Oper parsePrefixOp(){
		switch(lexAn.peekToken().symbol()){
			case ADD:
				Token add = check(Token.Symbol.ADD);
				return AST.UnExpr.Oper.ADD;
			case SUB:
				Token sub = check(Token.Symbol.SUB);
				return AST.UnExpr.Oper.SUB;
			case NOT:
				Token not = check(Token.Symbol.NOT);
				return AST.UnExpr.Oper.NOT;
			case PTR:
				Token ptr = check(Token.Symbol.PTR);
				return AST.UnExpr.Oper.MEMADDR;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|prefixOp| Pricakoval prefix, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.Expr parseEPostfix(){
		switch(lexAn.peekToken().symbol()){
			case IDENTIFIER: case LPAREN: case INTCONST: case CHARCONST: case STRINGCONST:
				AST.Expr expr = parseEKoncni();
				AST.UnExpr.Oper expr2 = parsePostfix();
				if(expr2 == null) return expr;
				AST.UnExpr expression = new AST.UnExpr(expr2, expr);
				return expression;
			default: 
				throw new Report.Error(lexAn.peekToken(), "|ePostfix| Pricakovan identifier, oklepaj ali konstanta, dobil " + lexAn.peekToken().symbol());
		}
	}
	private AST.UnExpr.Oper parsePostfix(){
		switch(lexAn.peekToken().symbol()){
			case FUN: case RPAREN: case ASSIGN: case VAR: case COMMA: case THEN: case END: case DO: case IN: case ELSE: case OR: case AND: case EOF: case EQU: case NEQ: case GTH: case LTH: case GEQ: case LEQ: case ADD: case SUB: case MUL: case DIV: case MOD:
				return null;
			case PTR:
				Token ptr = check(Token.Symbol.PTR);
				AST.UnExpr.Oper oper = parsePostfix();
				if(oper == null) oper = AST.UnExpr.Oper.VALUEAT;
				return oper;
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

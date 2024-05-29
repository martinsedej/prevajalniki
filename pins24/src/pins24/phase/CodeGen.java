package pins24.phase;

import java.util.*;
import pins24.common.*;
import pins24.common.AST.AssignStmt;
import pins24.common.AST.BinExpr;
import pins24.common.AST.ExprStmt;
import pins24.common.AST.FunDef;
import pins24.common.AST.IfStmt;
import pins24.common.AST.Init;
import pins24.common.AST.LetStmt;
import pins24.common.AST.Nodes;
import pins24.common.AST.ParDef;
import pins24.common.AST.UnExpr;
import pins24.common.AST.WhileStmt;
import pins24.common.PDM.CodeInstr;
import pins24.common.PDM.DataInstr;

/**
 * Generiranje kode.
 */
public class CodeGen {

	@SuppressWarnings({ "doclint:missing" })
	public CodeGen() {
		throw new Report.InternalError();
	}

	/**
	 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 * predstavitve.
	 * 
	 * Atributi:
	 * <ol>
	 * <li>({@link Abstr}) lokacija kode, ki pripada posameznemu vozliscu;</li>
	 * <li>({@link SemAn}) definicija uporabljenega imena;</li>
	 * <li>({@link SemAn}) ali je dani izraz levi izraz;</li>
	 * <li>({@link Memory}) klicni zapis funkcije;</li>
	 * <li>({@link Memory}) dostop do parametra;</li>
	 * <li>({@link Memory}) dostop do spremenljivke;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo kodo programa;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo podatke programa.</li>
	 * </ol>
	 */
	public static class AttrAST extends Memory.AttrAST {

		/** Atribut: seznam ukazov, ki predstavljajo kodo programa. */
		public final Map<AST.Node, List<PDM.CodeInstr>> attrCode;

		/** Atribut: seznam ukazov, ki predstavljajo podatke programa. */
		public final Map<AST.Node, List<PDM.DataInstr>> attrData;

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST  Abstraktno sintaksno drevo z dodanimi atributi pomnilniske
		 *                 predstavitve.
		 * @param attrCode Attribut: seznam ukazov, ki predstavljajo kodo programa.
		 * @param attrData Attribut: seznam ukazov, ki predstavljajo podatke programa.
		 */
		public AttrAST(final Memory.AttrAST attrAST, final Map<AST.Node, List<PDM.CodeInstr>> attrCode,
				final Map<AST.Node, List<PDM.DataInstr>> attrData) {
			super(attrAST);
			this.attrCode = attrCode;
			this.attrData = attrData;
		}

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi generiranja
		 *                kode.
		 */
		public AttrAST(final AttrAST attrAST) {
			super(attrAST);
			this.attrCode = attrAST.attrCode;
			this.attrData = attrAST.attrData;
		}

		@Override
		public String head(final AST.Node node, final boolean highlighted) {
			final StringBuffer head = new StringBuffer();
			head.append(super.head(node, false));
			return head.toString();
		}

		@Override
		public void desc(final int indent, final AST.Node node, final boolean highlighted) {
			super.desc(indent, node, false);
			System.out.print(highlighted ? "\033[31m" : "");
			if (attrCode.get(node) != null) {
				List<PDM.CodeInstr> instrs = attrCode.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Code: ---\n");
					for (final PDM.CodeInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			if (attrData.get(node) != null) {
				List<PDM.DataInstr> instrs = attrData.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Data: ---\n");
					for (final PDM.DataInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			System.out.print(highlighted ? "\033[30m" : "");
			return;
		}

	}

	/**
	 * Izracuna kodo programa
	 * 
	 * @param memoryAttrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
	 *                      pomnilniske predstavitve.
	 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 *         predstavitve.
	 */
	public static AttrAST generate(final Memory.AttrAST memoryAttrAST) {
		AttrAST attrAST = new AttrAST(memoryAttrAST, new HashMap<AST.Node, List<PDM.CodeInstr>>(),
				new HashMap<AST.Node, List<PDM.DataInstr>>());
		(new CodeGenerator(attrAST)).generate();
		return attrAST;
	}

	/**
	 * Generiranje kode v abstraktnem sintaksnem drevesu.
	 */
	private static class CodeGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Stevec anonimnih label. */
		private int labelCounter = 0;

		/**
		 * Ustvari nov generator kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Sprozi generiranje kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 *         predstavitve.
		 */
		public AttrAST generate() {
			attrAST.ast.accept(new Generator(), null);
			return new AttrAST(attrAST, Collections.unmodifiableMap(attrAST.attrCode),
					Collections.unmodifiableMap(attrAST.attrData));
		}

		/** Obiskovalec, ki generira kodo v abstraktnem sintaksnem drevesu. */
		private class Generator implements AST.FullVisitor<List<PDM.CodeInstr>, Mem.Frame> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

		    /*** TODO ***/
			//parametre po vrednosti, stringe po naslovu
			
			@Override
			public List<PDM.CodeInstr> visit(final Nodes<? extends AST.Node> nodes, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				for (final AST.Node node : nodes)
					code.addAll(node.accept(this, arg));
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final ParDef parDef, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final FunDef funDef, final Mem.Frame arg) {
				Mem.Frame frame = attrAST.attrFrame.get(funDef);
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				code.add(new PDM.LABEL(funDef.name,null));
				//ret.add(new PDM.PUSH(4-frame.parsSize,null));
				code.add(new PDM.PUSH(8-frame.varsSize,null));
				code.add(new PDM.POPN(null));
				code.addAll(funDef.pars.accept(this, null));
				code.addAll(funDef.stmts.accept(this, null));
				code.add(new PDM.PUSH(frame.parsSize - 4,null));
				code.add(new PDM.RETN(frame, null));
				attrAST.attrCode.put(funDef, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.CallExpr callExpr, final Mem.Frame arg){
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				//pushas gor argumente
				
				//regn.fp
				code.add(new PDM.REGN(PDM.REGN.Reg.FP, null));
				//load ??
				code.add(new PDM.LOAD(null));
				//name funkcije
				code.add(new PDM.NAME(callExpr.name, null));
				//call
				code.add(new PDM.CALL(arg, null));
				attrAST.attrCode.put(callExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.VarExpr varExpr, final Mem.Frame arg){
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				Mem.Access access = attrAST.attrVarAccess.get(attrAST.attrDef.get(varExpr));
				
				if(access == null){	//torej je parameter i guess
					access = attrAST.attrParAccess.get(attrAST.attrDef.get(varExpr));
				}
				if(access instanceof Mem.AbsAccess){
					code.add(new PDM.LABEL(varExpr.name, null));
					code.add(new PDM.LOAD(null));
				}
				else if(access instanceof Mem.RelAccess){
					code.add(new PDM.REGN(PDM.REGN.Reg.FP, null));
					code.add(new PDM.PUSH(((Mem.RelAccess)access).offset, null));
					code.add(new PDM.OPER(PDM.OPER.Oper.ADD, null));
				}

				return code;
			}

			

			@Override
			public List<PDM.CodeInstr> visit(AST.VarDef varDef, Mem.Frame arg){
				if(attrAST.attrVarAccess.get(varDef) instanceof Mem.RelAccess){
					List<DataInstr> data = new ArrayList<DataInstr>();
					List<CodeInstr> code = new ArrayList<CodeInstr>();
					String anonString = ":" + String.valueOf(labelCounter);
					labelCounter++;
					data.add(new PDM.LABEL(anonString, null));
					Mem.RelAccess access = (Mem.RelAccess) attrAST.attrVarAccess.get(varDef);
					for(int i = 0; i < access.inits.size(); i++){
						data.add(new PDM.DATA(access.inits.get(i), null));
					}
					code.add(new PDM.REGN(PDM.REGN.Reg.FP, null));
					code.add(new PDM.PUSH(access.offset, null));
					code.add(new PDM.OPER(PDM.OPER.Oper.ADD, null));
					code.add(new PDM.LABEL(anonString, null));
					code.add(new PDM.INIT(null));

					return code;
				}
				else {
					List<DataInstr> dataInstr = new ArrayList<PDM.DataInstr>();
					dataInstr.add(new PDM.LABEL(varDef.name, null));	//dodam ime spremenljivke
					dataInstr.add(new PDM.SIZE(attrAST.attrVarAccess.get(varDef).size, null));	//rezerviram njen prostor
					String anonString = ":" + String.valueOf(labelCounter);	//dodam nou anon label za inits od spremenljivke
					dataInstr.add(new PDM.LABEL(anonString, null));
					labelCounter++;
					Mem.AbsAccess access = (Mem.AbsAccess) attrAST.attrVarAccess.get(varDef);
					for(int i = 0; i < access.inits.size(); i++){
						dataInstr.add(new PDM.DATA(access.inits.get(i), null));
					}
					attrAST.attrData.put(varDef, dataInstr);	//dodam kaj more bit u memoriju

					List<PDM.CodeInstr> codeInstr = new ArrayList<PDM.CodeInstr>();
					codeInstr.add(new PDM.NAME(varDef.name, null)); //katera spremenljivka se bo initializirala
					codeInstr.add(new PDM.NAME(anonString, null)); //naslov inita za spremenlivko
					codeInstr.add(new PDM.INIT(null));
					attrAST.attrCode.put(varDef, codeInstr);
					return codeInstr;					
				}
			}

			@Override
			public List<CodeInstr> visit(AST.AtomExpr atomExpr, Mem.Frame arg){
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				List<PDM.DataInstr> data = new ArrayList<PDM.DataInstr>();

				switch(atomExpr.type){
					case AST.AtomExpr.Type.INTCONST:{
						code.add(new PDM.PUSH(Integer.parseInt(atomExpr.value), null));
						attrAST.attrCode.put(atomExpr, code);
						return code;
					}
					case AST.AtomExpr.Type.CHRCONST:{
						code.add(new PDM.PUSH(Integer.parseInt(atomExpr.value), null));
						attrAST.attrCode.put(atomExpr, code);
						return code;
					}
					case AST.AtomExpr.Type.STRCONST:{
						String label = ":" + labelCounter;
						labelCounter++;
						data.add(new PDM.LABEL(label, null));
						Vector<Integer> inits = Memory.decodeStrConst(atomExpr, null);
						for(int i = 0; i < inits.size(); i++){
							data.add(new PDM.DATA(inits.get(i), null));
						}
						code.add(new PDM.NAME(label, null));
						attrAST.attrData.put(atomExpr, data);
						attrAST.attrCode.put(atomExpr, code);
						return code;
					}
					default:
						return null;
				}
			}


			@Override
			public List<PDM.CodeInstr> visit(final Init init, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final ExprStmt exprStmt, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				code.addAll(exprStmt.expr.accept(this, arg));
				attrAST.attrCode.put(exprStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AssignStmt assignStmt, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				code.addAll(assignStmt.dstExpr.accept(this, arg));
				code.addAll(assignStmt.srcExpr.accept(this, arg));
				attrAST.attrCode.put(assignStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final IfStmt ifStmt, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				code.addAll(ifStmt.cond.accept(this, arg));
				code.addAll(ifStmt.thenStmts.accept(this, arg));
				code.addAll(ifStmt.elseStmts.accept(this, arg));
				attrAST.attrCode.put(ifStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final WhileStmt whileStmt, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				code.addAll(whileStmt.cond.accept(this, arg));
				code.addAll(whileStmt.stmts.accept(this, arg));
				attrAST.attrCode.put(whileStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final LetStmt letStmt, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				code.addAll(letStmt.defs.accept(this, arg));
				code.addAll(letStmt.stmts.accept(this, arg));
				attrAST.attrCode.put(letStmt, code);
				return code;
			}
			@Override
			public List<PDM.CodeInstr> visit(final UnExpr unExpr, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				code.addAll(unExpr.expr.accept(this, arg));
				attrAST.attrCode.put(unExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final BinExpr binExpr, final Mem.Frame arg) {
				List<PDM.CodeInstr> code = new ArrayList<PDM.CodeInstr>();
				code.addAll(binExpr.fstExpr.accept(this, arg));
				code.addAll(binExpr.sndExpr.accept(this, arg));
				switch (binExpr.oper) {
					case ADD:
						code.add(new PDM.OPER(PDM.OPER.Oper.ADD, null));
						break;
					case SUB:
						code.add(new PDM.OPER(PDM.OPER.Oper.SUB, null));
						break;
					case MUL:
						code.add(new PDM.OPER(PDM.OPER.Oper.MUL, null));
						break;
					case DIV:
						code.add(new PDM.OPER(PDM.OPER.Oper.DIV, null));
						break;
						//TODO:
					default:
						break;
				}
				attrAST.attrCode.put(binExpr, code);
				return code;
			}
	}

	/**
	 * Generator seznama ukazov, ki predstavljajo kodo programa.
	 */
	public static class CodeSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov za inicializacijo staticnih spremenljivk. */
		private final Vector<PDM.CodeInstr> codeInitSegment = new Vector<PDM.CodeInstr>();

		/** Seznam ukazov funkcij. */
		private final Vector<PDM.CodeInstr> codeFunsSegment = new Vector<PDM.CodeInstr>();

		/** Klicni zapis funkcije {@code main}. */
		private Mem.Frame main = null;

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo kodo programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo kodo programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo kodo programa.
		 */
		public List<PDM.CodeInstr> codeSegment() {
			attrAST.ast.accept(new Generator(), null);
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("main", null));
			codeInitSegment.addLast(new PDM.CALL(main, null));
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("exit", null));
			codeInitSegment.addLast(new PDM.CALL(null, null));
			final Vector<PDM.CodeInstr> codeSegment = new Vector<PDM.CodeInstr>();
			codeSegment.addAll(codeInitSegment);
			codeSegment.addAll(codeFunsSegment);
			return Collections.unmodifiableList(codeSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo kodo programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.FunDef funDef, final Object arg) {
				if (funDef.stmts.size() == 0)
					return null;
				List<PDM.CodeInstr> code = attrAST.attrCode.get(funDef);
				codeFunsSegment.addAll(code);
				funDef.pars.accept(this, arg);
				funDef.stmts.accept(this, arg);
				switch (funDef.name) {
				case "main" -> main = attrAST.attrFrame.get(funDef);
				}
				return null;
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				switch (attrAST.attrVarAccess.get(varDef)) {
				case Mem.AbsAccess __: {
					List<PDM.CodeInstr> code = attrAST.attrCode.get(varDef);
					codeInitSegment.addAll(code);
					break;
				}
				case Mem.RelAccess __: {
					break;
				}
				default:
					throw new Report.InternalError();
				}
				return null;
			}

		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo podatke programa.
	 */
	public static class DataSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov, ki predstavljajo podatke programa. */
		private final Vector<PDM.DataInstr> dataSegment = new Vector<PDM.DataInstr>();

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo podatke programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public DataSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo podatke programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo podatke programa.
		 */
		public List<PDM.DataInstr> dataSegment() {
			attrAST.ast.accept(new Generator(), null);
			return Collections.unmodifiableList(dataSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo podatke programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(varDef);
				if (data != null)
					dataSegment.addAll(data);
				varDef.inits.accept(this, arg);
				return null;
			}

			@Override
			public Object visit(final AST.AtomExpr atomExpr, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(atomExpr);
				if (data != null)
					dataSegment.addAll(data);
				return null;
			}

		}

	}

	// --- ZAGON ---

	/**
	 * Zagon izracuna pomnilniske predstavitve kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'24 compiler (code generation):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				// abstraktna sintaksa:
				final Abstr.AttrAST abstrAttrAST = Abstr.constructAST(synAn);
				// semanticna analiza:
				final SemAn.AttrAST semanAttrAST = SemAn.analyze(abstrAttrAST);
				// pomnilniska predstavitev:
				final Memory.AttrAST memoryAttrAST = Memory.organize(semanAttrAST);
				// generiranje kode:
				final CodeGen.AttrAST codegenAttrAST = CodeGen.generate(memoryAttrAST);

				(new AST.Logger(codegenAttrAST)).log();
				{
					int addr = 0;
					final List<PDM.CodeInstr> codeSegment = (new CodeSegmentGenerator(codegenAttrAST)).codeSegment();
					{
						System.out.println("\n\033[1mCODE SEGMENT:\033[0m");
						for (final PDM.CodeInstr instr : codeSegment) {
							System.out.printf("%8d [%s] %s\n", addr, instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					final List<PDM.DataInstr> dataSegment = (new DataSegmentGenerator(codegenAttrAST)).dataSegment();
					{
						System.out.println("\n\033[1mDATA SEGMENT:\033[0m");
						for (final PDM.DataInstr instr : dataSegment) {
							System.out.printf("%8d [%s] %s\n", addr, (instr instanceof PDM.SIZE) ? " " : instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					System.out.println();
				}
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

}}

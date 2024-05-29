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
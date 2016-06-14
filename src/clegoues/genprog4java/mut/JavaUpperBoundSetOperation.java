package clegoues.genprog4java.mut;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import clegoues.genprog4java.java.JavaStatement;
import clegoues.genprog4java.main.ClassInfo;

public class JavaUpperBoundSetOperation extends JavaEditOperation {

	public JavaUpperBoundSetOperation(ClassInfo fileName, JavaStatement location) {
		super(Mutation.UBOUNDSET, fileName, location);
	}

	@Override
	public void edit(final ASTRewrite rewriter, AST ast, CompilationUnit cu) {
		final Map<ASTNode, List<ASTNode>> nodestmts = this.getLocation().getArrayAccesses(); 
		Set<ASTNode> parentnodes = nodestmts.keySet();
		// for each parent node which may have multiple array access instances 
		for(ASTNode parent: parentnodes){
			// create a newnode
			Block newnode = parent.getAST().newBlock();
			List<ASTNode> arrays = nodestmts.get(parent);

			// for each of the array access instances
			for( ASTNode  array : arrays){
				Expression index = ((ArrayAccess) array).getIndex();
				String arrayindex;
				if (!(index instanceof NumberLiteral)){
					// get the array index
					arrayindex = index.toString();
					arrayindex = arrayindex.replace("++", "");
					arrayindex = arrayindex.replace("--", "");

					// create if statement 
					IfStatement stmt = parent.getAST().newIfStatement();

					// with expression "index > arrayname.length" 
					InfixExpression expression = null;
					expression = parent.getAST().newInfixExpression();
					expression.setLeftOperand(parent.getAST().newSimpleName(arrayindex));
					expression.setOperator(Operator.GREATER_EQUALS);

					// and then part as "index = arrayname.length - 1"
					SimpleName qualifier = parent.getAST().newSimpleName(((ArrayAccess)array).getArray().toString());
					SimpleName name = parent.getAST().newSimpleName("length");
					expression.setRightOperand(parent.getAST().newQualifiedName(qualifier, name));
					stmt.setExpression(expression);

					Assignment thenexpression = null;
					thenexpression = parent.getAST().newAssignment();
					thenexpression.setLeftHandSide(parent.getAST().newSimpleName(arrayindex));
					thenexpression.setOperator(Assignment.Operator.ASSIGN);

					InfixExpression setupperboundexpression = null;
					setupperboundexpression = parent.getAST().newInfixExpression();
					SimpleName qualifier1 = parent.getAST().newSimpleName(((ArrayAccess)array).getArray().toString());
					SimpleName name1 = parent.getAST().newSimpleName("length");
					setupperboundexpression.setLeftOperand(parent.getAST().newQualifiedName(qualifier1, name1));
					setupperboundexpression.setOperator(Operator.MINUS);
					setupperboundexpression.setRightOperand(parent.getAST().newNumberLiteral("1"));

					thenexpression.setRightHandSide(setupperboundexpression);

					ExpressionStatement thenstmt = parent.getAST().newExpressionStatement(thenexpression);
					stmt.setThenStatement(thenstmt);

					// add if statement to newnode
					newnode.statements().add(stmt);
				}
				// append the existing content of parent node to newnode
				ASTNode stmt = (Statement)parent;
				stmt = ASTNode.copySubtree(parent.getAST(), stmt);
				newnode.statements().add(stmt);
				rewriter.replace(parent, newnode, null);
			}
		}	
	}
}
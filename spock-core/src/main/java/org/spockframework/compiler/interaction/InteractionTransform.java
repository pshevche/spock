package org.spockframework.compiler.interaction;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.spockframework.compiler.ErrorReporter;
import org.spockframework.compiler.SourceLookup;

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class InteractionTransform implements ASTTransformation {
  @Override
  public void visit(ASTNode[] nodes, SourceUnit source) {
    ErrorReporter errorReporter = new ErrorReporter(source);
    SourceLookup sourceLookup = new SourceLookup(source);

    try {
      for (ASTNode node : nodes) {
        if (!(node instanceof MethodNode)) {
          continue;
        }

        rewriteInteractionMethod((MethodNode) node, errorReporter, sourceLookup);
      }
    } finally {
      sourceLookup.close();
    }
  }

  private void rewriteInteractionMethod(MethodNode node, ErrorReporter errorReporter, SourceLookup sourceLookup) {
    if (!node.isVoidMethod()) {
      errorReporter.error("Interaction helper method '%s' must have a void return type", node.getName());
      return;
    }

    InteractionHelperRewriter rewriter = new InteractionHelperRewriter(node, errorReporter, sourceLookup);

    try {
      rewriter.rewrite();
    } catch (Exception e) {
      errorReporter.error("Unexpected error during compilation of interaction helper method '%s'. Maybe you have used invalid Spock syntax? Anyway, please file a bug report at https://issues.spockframework.org.",
        e, node.getName()
      );
    }
  }
}

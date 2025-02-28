package org.spockframework.compiler.interaction;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.spockframework.compiler.*;
import spock.lang.Specification;

import java.util.ListIterator;
import java.util.stream.Stream;

class InteractionHelperRewriter extends StatementReplacingVisitorSupport {

  private static final AstNodeCache nodeCache = new AstNodeCache();

  private final MethodNode methodNode;
  private final ErrorReporter errorReporter;
  private final SourceLookup sourceLookup;

  InteractionHelperRewriter(MethodNode methodNode, ErrorReporter errorReporter, SourceLookup sourceLookup) {
    this.methodNode = methodNode;
    this.errorReporter = errorReporter;
    this.sourceLookup = sourceLookup;
  }

  void rewrite() {
    addSpecificationParameter();
    rewriteInteractions();
  }

  private void addSpecificationParameter() {
    Parameter[] originalParams = methodNode.getParameters();
    Parameter specificationParam = new Parameter(ClassHelper.make(Specification.class), "spec");
    Parameter[] newParams = Stream.concat(Stream.of(specificationParam), Stream.of(originalParams)).toArray(Parameter[]::new);
    methodNode.setParameters(newParams);
  }

  private void rewriteInteractions() {
    InteractionRewriter interactionRewriter = new InteractionRewriter(
        new DefaultInteractionRewriteResources(methodNode, nodeCache, sourceLookup, errorReporter),
        null
    );
    ListIterator<Statement> statements = AstUtil.getStatements(methodNode).listIterator();
    while (statements.hasNext()) {
      Statement statement = statements.next();
      if (statement instanceof ExpressionStatement) {
        Statement replacement = interactionRewriter.rewrite((ExpressionStatement) statement);
        if (replacement != null) {
          statements.set(replacement);
        }
      }
    }
  }
}

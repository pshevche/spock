package spock.lang;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;
import org.spockframework.compiler.interaction.InteractionTransform;
import org.spockframework.util.Beta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Beta
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@GroovyASTTransformationClass(classes = InteractionTransform.class)
public @interface Interaction {
}

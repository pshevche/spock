/*
 * Copyright 2024 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.spockframework.smoke

import org.spockframework.EmbeddedSpecification
import org.spockframework.runtime.extension.ExtensionAnnotation
import org.spockframework.runtime.extension.IAnnotationDrivenExtension
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.SpecInfo
import org.spockframework.specs.extension.SpockSnapshotter
import spock.lang.Snapshot
import spock.lang.ResourceLock

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

class Interceptors extends EmbeddedSpecification {
  @Snapshot
  SpockSnapshotter snapshotter

  def setup() {
    runner.addClassImport(LifecycleTest)
    LifecycleRecorderAndContextTesterExtension.lifecycleOutline = ''
  }

  @ResourceLock("LifecycleRecorderAndContextTesterExtension.lifecycleOutline")
  def "interceptors are called in the correct order and with the correct context information"() {
    when:
    runner.runWithImports """
abstract class SuperSpec extends Specification {
  @Shared superShared = 0

  def superInstance = 0

  def setupSpec() {
  }

  def setup() {
  }

  def superFeature1() {
    expect: true
  }

  def superFeature2() {
    expect: true
  }

  def superDataFeature1() {
    expect: x
    where:
    x << [true, true]
  }

  def superDataFeature2() {
    expect: x
    where:
    x << [true, true]
  }

  def cleanup() {
  }

  def cleanupSpec() {
  }
}

@LifecycleTest
class SubSpec extends SuperSpec {
  @Shared subShared = 0

  def subInstance = 0

  def setupSpec() {
  }

  def setup() {
  }

  def subFeature1() {
    expect: true
  }

  def subFeature2() {
    expect: true
  }

  def subDataFeature1() {
    expect: x
    where:
    x << [true, true]
  }

  def subDataFeature2() {
    expect: x
    where:
    x << [true, true]
  }

  def cleanup() {
  }

  def cleanupSpec() {
  }
}
"""

    then:
    snapshotter.assertThat(LifecycleRecorderAndContextTesterExtension.lifecycleOutline).matchesSnapshot()
  }

  @ResourceLock("LifecycleRecorderAndContextTesterExtension.lifecycleOutline")
  def "non-method interceptors are called even without the respective method"() {
    when:
    runner.runWithImports """
abstract class SuperSpec extends Specification {
}

@LifecycleTest
class SubSpec extends SuperSpec {
  def foo() {
    expect: true
  }

  def bar() {
    expect: x
    where:
    x << [true, true]
  }
}
"""

    then:
    snapshotter.assertThat(LifecycleRecorderAndContextTesterExtension.lifecycleOutline).matchesSnapshot()
  }

  static class LifecycleRecorderAndContextTesterExtension implements IAnnotationDrivenExtension<LifecycleTest> {
    static lifecycleOutline = ''
    def indent = ''

    @Override
    void visitSpecAnnotation(LifecycleTest annotation, SpecInfo specInfo) {
      def allFeatures = specInfo.allFeatures
      specInfo.specsBottomToTop*.addSharedInitializerInterceptor {
        assertSpecContext(it)
        proceed(it, 'shared initializer', "$it.spec.name")
        assertSpecContext(it)
      }
      specInfo.allSharedInitializerMethods*.addInterceptor {
        assertSpecMethodContext(it)
        proceed(it, 'shared initializer method', "$it.spec.name.$it.method.name()")
        assertSpecMethodContext(it)
      }
      specInfo.addInterceptor {
        assertSpecContext(it)
        proceed(it, 'specification', "$it.spec.name")
        assertSpecContext(it)
      }
      specInfo.specsBottomToTop*.addSetupSpecInterceptor {
        assertSpecContext(it)
        proceed(it, 'setup spec', "$it.spec.name")
        assertSpecContext(it)
      }
      specInfo.allSetupSpecMethods*.addInterceptor {
        assertSpecMethodContext(it)
        proceed(it, 'setup spec method', "$it.spec.name.$it.method.name()")
        assertSpecMethodContext(it)
      }
      allFeatures*.addInterceptor {
        assertFeatureContext(it)
        proceed(it, 'feature', "$it.spec.name.$it.feature.name")
        assertFeatureContext(it)
      }
      specInfo.specsBottomToTop.each { spec ->
        spec.addInitializerInterceptor {
          assertIterationContext(it)
          proceed(it, 'initializer', "$it.spec.name.$it.feature.name[#$it.iteration.iterationIndex] / $spec.name")
          assertIterationContext(it)
        }
      }
      specInfo.allInitializerMethods*.addInterceptor {
        assertIterationMethodContext(it)
        proceed(it, 'initializer method', "$it.feature.parent.name.$it.feature.name[#$it.iteration.iterationIndex] / $it.spec.name.$it.method.name()")
        assertIterationMethodContext(it)
      }
      allFeatures.each { feature ->
        specInfo.allInitializerMethods.each { mi ->
          feature.addScopedMethodInterceptor(mi)  {
            assertIterationMethodContext(it)
            proceed(it, 'feature scoped initializer method', "$it.feature.parent.name.$it.feature.name[#$it.iteration.iterationIndex] / $it.spec.name.$it.method.name() / from $feature.name")
            assertIterationMethodContext(it)
          }
        }
        specInfo.allSetupMethods.each { mi ->
          feature.addScopedMethodInterceptor(mi) {
            assertIterationMethodContext(it)
            proceed(it, 'feature scoped setup method', "$it.feature.parent.name.$it.feature.name[#$it.iteration.iterationIndex] / $it.spec.name.$it.method.name() / from $feature.name")
            assertIterationMethodContext(it)
          }
        }
        specInfo.allCleanupMethods.each { mi ->
          feature.addScopedMethodInterceptor(mi) {
            assertIterationMethodContext(it)
            proceed(it, 'feature scoped cleanup method', "$it.feature.parent.name.$it.feature.name[#$it.iteration.iterationIndex] / $it.spec.name.$it.method.name() / from $feature.name")
            assertIterationMethodContext(it)
          }
        }
      }
      allFeatures*.addInitializerInterceptor {
        assertIterationContext(it)
        proceed(it, 'feature scoped initializer', "$it.spec.name.$it.feature.name[#$it.iteration.iterationIndex]")
        assertIterationContext(it)
      }
      allFeatures*.addIterationInterceptor {
        assertIterationContext(it)
        proceed(it, 'iteration', "$it.spec.name.$it.feature.name[#$it.iteration.iterationIndex]")
        assertIterationContext(it)
      }
      specInfo.specsBottomToTop.each { spec ->
        spec.addSetupInterceptor {
          assertIterationContext(it)
          proceed(it, 'setup', "$it.spec.name.$it.feature.name[#$it.iteration.iterationIndex] / $spec.name")
          assertIterationContext(it)
        }
      }
      allFeatures*.addSetupInterceptor {
        assertIterationContext(it)
        proceed(it, 'feature scoped setup', "$it.spec.name.$it.feature.name[#$it.iteration.iterationIndex]")
        assertIterationContext(it)
      }
      specInfo.allSetupMethods*.addInterceptor {
        assertIterationMethodContext(it)
        proceed(it, 'setup method', "$it.feature.parent.name.$it.feature.name[#$it.iteration.iterationIndex] / $it.spec.name.$it.method.name()")
        assertIterationMethodContext(it)
      }
      allFeatures*.featureMethod*.addInterceptor {
        assertIterationMethodContext(it)
        proceed(it, 'feature method', "$it.feature.parent.name.$it.feature.name[#$it.iteration.iterationIndex] / $it.spec.name.$it.method.name()")
        assertIterationMethodContext(it)
      }
      specInfo.specsBottomToTop.each { spec ->
        spec.addCleanupInterceptor {
          assertIterationContext(it)
          proceed(it, 'cleanup', "$it.spec.name.$it.feature.name[#$it.iteration.iterationIndex] / $spec.name")
          assertIterationContext(it)
        }
      }
      allFeatures*.addCleanupInterceptor {
        assertIterationContext(it)
        proceed(it, 'feature scoped cleanup', "$it.spec.name.$it.feature.name[#$it.iteration.iterationIndex]")
        assertIterationContext(it)
      }
      specInfo.allCleanupMethods*.addInterceptor {
        assertIterationMethodContext(it)
        proceed(it, 'cleanup method', "$it.feature.parent.name.$it.feature.name[#$it.iteration.iterationIndex] / $it.spec.name.$it.method.name()")
        assertIterationMethodContext(it)
      }
      specInfo.specsBottomToTop*.addCleanupSpecInterceptor {
        assertSpecContext(it)
        proceed(it, 'cleanup spec', "$it.spec.name")
        assertSpecContext(it)
      }
      specInfo.allCleanupSpecMethods*.addInterceptor {
        assertSpecMethodContext(it)
        proceed(it, 'cleanup spec method', "$it.spec.name.$it.method.name()")
        assertSpecMethodContext(it)
      }
      specInfo.allFixtureMethods*.addInterceptor {
        it.with {
          def specFixture = method.name.endsWith('Spec')
          if (specFixture) {
            assertSpecMethodContext(it)
          } else {
            assertIterationMethodContext(it)
          }
        }
        proceed(it, 'fixture method', "${it.feature?.with { feature -> "$feature.parent.name.$feature.name[#$it.iteration.iterationIndex] / " } ?: ''}$it.spec.name.$it.method.name()")
        it.with {
          def specFixture = method.name.endsWith('Spec')
          if (specFixture) {
            assertSpecMethodContext(it)
          } else {
            assertIterationMethodContext(it)
          }
        }
      }
    }

    static assertSpecCommonContext(IMethodInvocation invocation) {
      invocation.with {
        assert spec
        assert !feature
        assert !iteration
        assert instance == sharedInstance
        assert method
        instance.specificationContext.with {
          assert currentSpec
          try {
            currentFeature
            assert false: 'currentFeature should not be set'
          } catch (IllegalStateException ise) {
            assert ise.message == 'Cannot request current feature in @Shared context, or feature context'
          }
          try {
            currentIteration
            assert false: 'currentIteration should not be set'
          } catch (IllegalStateException ise) {
            assert ise.message == 'Cannot request current iteration in @Shared context, or feature context'
          }
        }
      }
    }

    static assertSpecContext(IMethodInvocation invocation) {
      assertSpecCommonContext(invocation)
      invocation.with {
        assert target != instance
        assert !method.reflection
        assert !method.name
      }
    }

    static assertSpecMethodContext(IMethodInvocation invocation) {
      assertSpecCommonContext(invocation)
      invocation.with {
        assert target == instance
        assert method.reflection
        assert method.name
      }
    }

    static assertFeatureCommonContext(IMethodInvocation invocation) {
      invocation.with {
        assert spec
        assert feature
        assert method
        instance.specificationContext.with {
          assert currentSpec
        }
      }
    }

    static assertFeatureContext(IMethodInvocation invocation) {
      assertFeatureCommonContext(invocation)
      invocation.with {
        assert !iteration
        assert instance == sharedInstance
        assert target != instance
        assert !method.reflection
        assert !method.name
        instance.specificationContext.with {
          try {
            currentFeature
            assert false: 'currentFeature should not be set'
          } catch (IllegalStateException ise) {
            assert ise.message == 'Cannot request current feature in @Shared context, or feature context'
          }
          try {
            currentIteration
            assert false: 'currentIteration should not be set'
          } catch (IllegalStateException ise) {
            assert ise.message == 'Cannot request current iteration in @Shared context, or feature context'
          }
        }
      }
    }

    static assertIterationCommonContext(IMethodInvocation invocation) {
      assertFeatureCommonContext(invocation)
      invocation.with {
        assert iteration
        assert instance != sharedInstance
        instance.specificationContext.with {
          assert currentFeature
          assert currentIteration
        }
      }
    }

    static assertIterationContext(IMethodInvocation invocation) {
      assertIterationCommonContext(invocation)
      invocation.with {
        assert target != instance
        assert !method.reflection
        assert !method.name
      }
    }

    static assertIterationMethodContext(IMethodInvocation invocation) {
      assertIterationCommonContext(invocation)
      invocation.with {
        assert target == instance
        assert method.reflection
        assert method.name
      }
    }

    void proceed(IMethodInvocation invocation, String type, String name) {
      lifecycleOutline += "$indent$type start ($name)\n"
      indent += '  '
      invocation.proceed()
      indent -= '  '
      lifecycleOutline += "$indent$type end ($name)\n"
    }
  }
}

@Retention(RetentionPolicy.RUNTIME)
@ExtensionAnnotation(Interceptors.LifecycleRecorderAndContextTesterExtension)
@interface LifecycleTest {
}

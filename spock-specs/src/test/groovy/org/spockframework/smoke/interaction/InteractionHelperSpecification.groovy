package org.spockframework.smoke.interaction

import org.spockframework.docs.interaction.Publisher
import org.spockframework.docs.interaction.Subscriber
import spock.lang.Specification
import spock.lang.Subject

class InteractionHelperSpecification extends Specification {

  @Subject
  def publisher = new Publisher()

  def "smoke test interaction helpers"() {
    given:
    Subscriber subscriber1 = Mock()
    Subscriber subscriber2 = Mock()
    publisher.subscribers << subscriber1
    publisher.subscribers << subscriber2

    when:
    publisher.send("Hello, World!")

    then:
    subscriber1.receivedExactlyOnce("Hello, World!")
    subscriber2.receivedExactlyOnce("Bye, World!")
  }

}

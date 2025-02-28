package org.spockframework.smoke.interaction

import org.spockframework.docs.interaction.Subscriber
import spock.lang.Interaction

@Category(Subscriber)
class SubscriberInteractions {

  @Interaction
  void receivedExactlyOnce(String message) {
    1 * this.receive(message)
  }
}

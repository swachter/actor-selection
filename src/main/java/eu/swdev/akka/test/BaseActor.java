package eu.swdev.akka.test;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class BaseActor extends AbstractActor {

  protected final LoggingAdapter log;

  protected BaseActor() {
    this.log = Logging.getLogger(getContext().system(), this);
  }

}

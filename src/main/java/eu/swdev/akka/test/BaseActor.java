package eu.swdev.akka.test;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import eu.swdev.akka.test.conf.InstBackendConf;

public class BaseActor extends AbstractActor {

  protected final ActorEnv env;
  protected final LoggingAdapter log;

  protected BaseActor(ActorEnv env) {
    this.env = env;
    this.log = Logging.getLogger(getContext().system(), this);
  }

  protected InstBackendConf conf() {
    return env.conf();
  }
}

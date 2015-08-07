package eu.swdev.akka.test;

import akka.actor.ActorRef;
import eu.swdev.akka.test.conf.InstBackendConf;

public interface ActorEnv {

  InstBackendConf conf();

  MongoUtil.MDatabase database();

  ActorRef selfRegistration();
  ActorRef execHandler();

  ActorRef crActor();
  ActorRef vodafoneOpRunner();

  ActorRef httpResponder();
}

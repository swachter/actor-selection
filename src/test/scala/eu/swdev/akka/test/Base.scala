package eu.swdev.akka.test

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.Config
import eu.swdev.akka.test.conf.{Environment, InstBackendConf}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Suite}

import scala.concurrent.duration._

/**
 * Base class for integrations tests.
 *
 * The base class takes care that an actor system is created and shutdown after all
 * tests are completed. In addition, it provides an InstBackendConf instance that
 * can be used by the tests.
 *
 * NB: all tests share the same actor system and use the same configuration.
 */
@RunWith(classOf[JUnitRunner])
abstract class Base(config: Config) extends TestKit(ActorSystem("test", config))
  with FunSuiteLike
  with BeforeAndAfterAll
  with ImplicitSender {

  self: Suite =>

  val conf = ConfUtil.getConf(system.settings.config, "instBackend", classOf[InstBackendConf])

  val log = Logging.getLogger(system, "test-logger")

  override protected def afterAll(): Unit = {
    log.debug("shutdown actor system")
    TestKit.shutdownActorSystem(system, 10 seconds, true)
  }

}

/**
 * Base class for tests that want to test interactions of top level actors.
 *
 * The base class takes care to instantiate all top level actors properly.
 *
 * @param config
 */
abstract class ActorItBase(config: Config) extends Base(config) {

  def this(cs: (Config => Config)*) = this(cs.foldLeft(Environment.getConfig)((accu, c) => c(accu)))

  val execHandler: ActorRef = system.actorOf(Props[ExecManager], "execManager")

}


package eu.swdev.akka.test

import akka.event.Logging
import akka.testkit.TestProbe
import eu.swdev.akka.test.ExecManager.{ContinueMsg, RegisterMsg}

import scala.concurrent.duration._

class ExecManagerIt extends ActorItBase(
  _.withValue("instBackend.timeoutCheckPeriodMillis", 100l),
  _.withValue("akka.loglevel", "DEBUG"),
  _.withValue("akka.actor.debug.lifecycle", "on")
) {

  test("a timed out execution") {

    val log = Logging.getLogger(system, "test")
    val ec = system.dispatcher

    log.debug(s"############################ test a timed out execution ###")

    val nextActor = TestProbe()
    val nextActorRef = nextActor.ref
    val nextActorPathString = nextActorRef.path.toStringWithoutAddress
    log.debug("nextActorPathString: " + nextActorPathString)

    val actorSelection = system.actorSelection(nextActorPathString)

    log.debug(s"nextActor: ${nextActorRef.path}")

    println("started")
    for (i <- 1 to 1000) {

      actorSelection.resolveOne(1 seconds).onComplete {
        case scala.util.Success(r) => log.debug(s"actor selection could be resolved - as: $actorSelection")
        case scala.util.Failure(t) => log.error(t, s"actor selection could not be resolved - as: $actorSelection")
      }(ec)

      // the execution must have a short timeout value because we will wait for the timeout below
      execHandler ! new RegisterMsg(nextActorRef, 1000)
      val execId = expectMsgType[String]

      log.debug(s"************ wait for ContinueMsg - execId: $execId")

      // expect a ContinueMsg

      try {
        println(s"i: $i")
        val continueMsg = nextActor.expectMsgType[ContinueMsg[String]](10 seconds)

        log.debug("waited for ContinueMsg")

        //assert(execCtx === continueMsg.execCtx)
        // the ContinuationMsg must contain a failure; it should have timed out
        assert(continueMsg.result === "timeout")
        assert(execId === continueMsg.execId)

//        execHandler ! ExecManager.COUNT_MSG
//        expectMsg(Result.success(0l))
      } finally {
        val as = system.actorSelection(nextActorPathString)
        as.resolveOne(1 seconds).onComplete {
          case scala.util.Success(r) => log.debug(s"actor selection could be resolved #2 - as: $as")
          case scala.util.Failure(t) => log.error(t, s"actor selection could not be resolved #2 - as: $as")
        }(ec)
      }
    }

  }

}


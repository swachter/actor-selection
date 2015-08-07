package eu.swdev.akka.test;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import scala.concurrent.duration.Duration;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lt;

/**
 * Allows to register pending executions, waits for execution results, and sends messages to the next actor that shall
 * process a result.
 *
 * In addition the ExecManager watches for timed out executions.
 */
public class ExecManager extends AbstractActor {

  //
  // === begin protocol ===
  //
  // (messages that are understood or sent by the ExecManager actor)
  /**
   * Registers a pending execution.
   *
   * Eventually a matching FinishedMsg should come in.
   */
  public static class RegisterMsg<C> {

    public final ActorRef next;
    public final long timeoutMillis;

    public RegisterMsg(ActorRef next, long timeoutMillis) {
      this.next = next;
      this.timeoutMillis = timeoutMillis;
    }
  }

  public static class ContinueMsg {

    public final String execId;

    public ContinueMsg(String execId) {
      this.execId = execId;
    }

  }

  /**
   * Perform a timeout check on the currently pending executions.
   */
  private static final Object TIMEOUT_CHECK_MSG = new Object();

  //
  // === end protocol ===
  //

  private static final String NEXT = "next";
  private static final String VALID_UNTIL = "validUntil";

  private final LoggingAdapter log;


  private final MongoCollection<Document> pendingExecutions;

  public ExecManager() {
    this.log = Logging.getLogger(getContext().system(), this);
    MongoDatabase db = MongoClients.create("mongodb://localhost:27017").getDatabase("actor-selection");
    pendingExecutions = db.getCollection("test");
    pendingExecutions.drop((r, t) -> System.out.println("dropped collection"));
    receive(ReceiveBuilder
        .match(RegisterMsg.class, this::register)
        .matchEquals(TIMEOUT_CHECK_MSG, this::timeoutCheck)
        .build());
    context().system().scheduler().schedule(
        Duration.create(100, TimeUnit.MILLISECONDS),
        Duration.create(100, TimeUnit.MILLISECONDS),
        self(),
        TIMEOUT_CHECK_MSG,
        context().dispatcher(),
        self()
    );
  }

  /**
   * Processes an RegisterMsg.
   *
   * @param msg required
   */
  private void register(RegisterMsg msg) {
    // capture the sender; the sender instance may have changed when the following asynchronous callback are called
    ActorRef sender = sender();

    // first persist the pending execution
    Document pendingExecution = new Document()
        .append(NEXT, msg.next.path().toStringWithoutAddress())
        .append("created", new Date())
        .append(VALID_UNTIL, new Date(System.currentTimeMillis() + msg.timeoutMillis));

    pendingExecutions.insertOne(pendingExecution, (result, throwable) -> {
      if (throwable == null) {
        // Mongo sets the "_id" property after an insert
        String execId = pendingExecution.getObjectId("_id").toHexString();
        log.debug("registered execution - execId: " + execId);
        // reply to the sender with the execId
        sender.tell(execId, self());
      } else {
        log.error(throwable, "register execution failed");
        sender.tell(throwable, self());
      }
    });
  }

  /**
   * Performs a timeout check and informs interested actors about timeouts.
   *
   * @param msg
   */
  private void timeoutCheck(Object msg) {
    //log.debug("timeoutCheck");
    pendingExecutions.findOneAndDelete(
        lt(VALID_UNTIL, new Date()),
        (doc, throwable) -> {
          if (throwable == null) {
            if (doc != null) {
              // a timed out execution was found
              // -> send another TIMEOUT_CHECK_MSG message to the actor itself in order to check for more time outs
              self().tell(TIMEOUT_CHECK_MSG, self());
              // send a timeout result to the next actor
              String execId = doc.getObjectId("_id").toHexString();
              log.debug("execution timed out - execId: " + execId);
              tellNext(execId, doc, "timeout");
            }
          } else {
            log.error(throwable, "could not process execution timeouts");
          }
        }
    );
  }

  private void tellNext(String execId, Document doc, String result) {
    ContinueMsg continueMsg = new ContinueMsg(execId);
    String nextPath = doc.getString(NEXT);
    log.debug("try to resolve next path: " + nextPath);

    ActorSelection as = context().system().actorSelection(nextPath);
    as.tell(continueMsg, self());
  }

}

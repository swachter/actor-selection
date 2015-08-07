package eu.swdev.akka.test;

import akka.event.LoggingAdapter;
import com.google.gson.Gson;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;

public class MongoUtil {

  public static MDatabase create(String connectionString, String database) {
    MongoDatabase db = MongoClients.create(connectionString).getDatabase(database);
    return new MDatabase(db);
  }

  /**
   * A simple wrapper around a Mongo database.
   */
  public static class MDatabase {

    private final MongoDatabase wrapped;

    public MDatabase(MongoDatabase wrapped) {
      this.wrapped = wrapped;
    }

    public MCollection getCollection(String name, LoggingAdapter log) {
      return new MCollection(wrapped.getCollection(name), log);
    }
  }

  /**
   * A simple wrapper around a Mongo collection.
   *
   * The wrapper takes care that unhandled exceptions that were thrown in callbacks are logged.
   */
  public static class MCollection {

    private final MongoCollection<Document> wrapped;
    private final LoggingAdapter log;

    public MCollection(MongoCollection<Document> wrapped, LoggingAdapter log) {
      this.wrapped = wrapped;
      this.log = log;
    }

    public void insertOne(Document doc, SingleResultCallback<Void> cb) {
      wrapped.insertOne(doc, logged(cb));
    }

    public void findOneAndDelete(Bson filter, SingleResultCallback<Document> cb) {
      wrapped.findOneAndDelete(filter, logged(cb));
    }

    public void findOneAndUpdate(Bson filter, Bson update, SingleResultCallback<Document> cb) {
      wrapped.findOneAndUpdate(filter, update, logged(cb));
    }

    public void count(SingleResultCallback<Long> cb) {
      wrapped.count(logged(cb));
    }

    private <T> SingleResultCallback<T> logged(SingleResultCallback<T> cb) {
      return new SingleResultCallback<T>() {
        @Override
        public void onResult(T result, Throwable t) {
          try {
            cb.onResult(result, t);
          } catch (Throwable t1) {
            log.error(t1, "unhandled exception in Mongo callback");
          }
        }
      };
    }
  }

  private static Gson GSON = new Gson();

  public static Document toDoc(Object obj) {
    String json = GSON.toJson(obj);
    return Document.parse(json);
  }

  public static <T> T fromDoc(Document doc, Class<T> clazz) {
    String json = doc.toJson();
    return GSON.fromJson(json, clazz);
  }

}

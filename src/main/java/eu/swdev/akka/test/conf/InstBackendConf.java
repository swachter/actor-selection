package eu.swdev.akka.test.conf;

public class InstBackendConf {

  public final IotConf iot;
  public final MongoConf mongo;
  public final int port;
  public final String bindToInterface;
  public final String callbackAddrAndPort;
  public final String callbackUrl;
  public final String commonBaseEventPath;
  public final TimeoutMillis timeoutMillis;
  public final long timeoutCheckPeriodMillis;

  public InstBackendConf(IotConf iot, MongoConf mongo, int port, String bindToInterface, String callbackAddrAndPort, String callbackUrl, String commonBaseEventPath, TimeoutMillis timeoutMillis, long timeoutCheckPeriodMillis) {
    this.iot = iot;
    this.mongo = mongo;
    this.port = port;
    this.bindToInterface = bindToInterface;
    this.callbackAddrAndPort = callbackAddrAndPort;
    this.callbackUrl = callbackUrl;
    this.commonBaseEventPath = commonBaseEventPath;
    this.timeoutMillis = timeoutMillis;
    this.timeoutCheckPeriodMillis = timeoutCheckPeriodMillis;
  }

  public static class TimeoutMillis {

    public final long setUser;
    public final long activateSimCard;

    public TimeoutMillis(long setUser, long activateSimCard) {
      this.setUser = setUser;
      this.activateSimCard = activateSimCard;
    }
  }

  public static class MongoConf {

    public final String host;
    public final String port;
    public final String database;

    public MongoConf(String host, String port, String database) {
      this.host = host;
      this.port = port;
      this.database = database;
    }

  }

}

package eu.swdev.akka.test.conf;

public class IotConf {
  public final String user;
  public final String password;
  public final String domain;
  public final String tenant;
  public final String appName;
  public final String roleName;
  public final String apiKeyName;

  public final String imsUrl;
  public final String m2mUrl;
  public final String ismUrl;

  public IotConf(String user, String password, String domain, String tenant, String appName, String roleName, String apiKeyName, String imsUrl, String m2mUrl, String ismUrl) {
    this.user = user;
    this.password = password;
    this.domain = domain;
    this.tenant = tenant;
    this.appName = appName;
    this.roleName = roleName;
    this.apiKeyName = apiKeyName;
    this.imsUrl = imsUrl;
    this.m2mUrl = m2mUrl;
    this.ismUrl = ismUrl;
  }
}

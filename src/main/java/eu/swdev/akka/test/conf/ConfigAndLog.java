package eu.swdev.akka.test.conf;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

import java.util.ArrayList;
import java.util.List;

public class ConfigAndLog {

  public final Config config;
  public final List<String> log;

  public ConfigAndLog(Config config, List<String> log) {
    this.config = config;
    this.log = log;
  }

  public ConfigAndLog withValue(String path, Object value) {
    return new ConfigAndLog(config.withValue(path, ConfigValueFactory.fromAnyRef(value)), log);
  }

  public ConfigAndLog withError(String msg) {
    List<String> l = new ArrayList<>(log);
    l.add(msg);
    return new ConfigAndLog(config, l);
  }

  public ConfigAndLog withErrors(List<String> msgs) {
    List<String> l = new ArrayList<>(log);
    l.addAll(msgs);
    return new ConfigAndLog(config, l);
  }

}

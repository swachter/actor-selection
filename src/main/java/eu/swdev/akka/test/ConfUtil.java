package eu.swdev.akka.test;

import com.google.gson.Gson;
import com.typesafe.config.Config;

/**
 * Utilities for accessing the configuration.
 *
 * The configuration is represented by an hierarchical json-like
 * structure.
 *
 * Configuration is handled by the HOCON configuration libary
 * (cf. https://github.com/typesafehub/config/blob/master/HOCON.md).
 */
public class ConfUtil {

  private static final Gson gson = new Gson();

  /**
   * Create a typed configuration object for a certain subpart of the
   * configuration.
   *
   * The json-like configuration information is converted into an object.
   *
   * @param <T> The type that represents the configuration part.
   * @param context required;
   * @param path required;
   * @param clazz required;
   * @return required;
   */
//  public static <T> T getConf(ActorContext context, String path, Class<T> clazz) {
//    return getConf(context.system().settings().config(), path, clazz);
//  }

  /**
   * Create a typed configuration object for a certain subpart of the
   * configuration.
   *
   * The json-like configuration information is converted into an object.
   *
   * @param <T> The type that represents the configuration part.
   * @param rootConfig required;
   * @param path required;
   * @param clazz required;
   * @return required;
   */
  public static <T> T getConf(Config rootConfig, String path, Class<T> clazz) {
    Config config = rootConfig.getConfig(path);
    if (config == null) {
      throw new RuntimeException("unknown configuration path: " + path);
    }
    String s = config.root().render();
    try {
      return gson.fromJson(s, clazz);
    } catch (Throwable t) {
      throw new RuntimeException("could not parse configuration: " + s);
    }
  }

}

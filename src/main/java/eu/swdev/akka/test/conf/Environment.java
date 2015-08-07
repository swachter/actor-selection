package eu.swdev.akka.test.conf;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static eu.swdev.akka.test.conf.ConfigKey.*;

public class Environment {

  private static final Logger log = LoggerFactory.getLogger(Environment.class);

  public static Config getConfig() {

    // Environment settings are represented by functions that transforms the configuration.

    Function<ConfigAndLog, ConfigAndLog> envSettings;

    if (isDockerEnvironment()) {

      log.info("use Docker environment");

      envSettings = dockerInstBackendPort().andThen(dockerInstBackendInterf()).andThen(dockerCallback())
          .andThen(dockerMongoConnection()).andThen(dockerMongoDatabase()).andThen(dockerTimeoutCheckMillis())
          .andThen(dockerCrUrls());

    } else {

      log.info("use local environment");

      envSettings = Function.identity();

    }

    Config rawConfig = ConfigFactory.load();

    ConfigAndLog cal = envSettings.apply(new ConfigAndLog(rawConfig, new ArrayList<>()));

    if (!cal.log.isEmpty()) {
      String errors = cal.log.stream().map(s -> "    " + s).collect(Collectors.joining("\n"));
      log.error("configuration errors:\n" + errors);
      throw new RuntimeException("configuration errors:\n" + errors);
    }

    return cal.config;
  }

  private static boolean isDockerEnvironment() {
    return "Docker".equals(getString("HOSTING_ENVIRONMENT"));
  }

  //
  //
  //

  private static ConfFunc dockerInstBackendPort() {
    return argList(param("INST_BACKEND_PORT", integer)
        .check(i -> i >= 1024 && i < 65536, "out of range - must be >= 1024 and <= 65536"))
        .apply(port -> configure(KEY_PORT, port), onError);
  }

  private static ConfFunc dockerInstBackendInterf() {
    return argList(param("INST_BACKEND_INTERFACE", string).withDefault("0.0.0.0"))
        .apply(interf -> configure(KEY_BIND_TO_INTERFACE, interf), onError);
  }

  private static ConfFunc dockerCallback() {
    return argList(param("HOSTNAME").required()).add(param("EXTERNAL_INST_BACKEND_PORT", integer).required())
        .apply(externalPort -> hostName -> {
          try {
            List<String> hosts = Files.readAllLines(Paths.get("/etc/hosts"));
            Optional<String> ip =
                hosts.stream().filter(l -> l.contains(hostName)).findFirst().map(l -> l.split("\\s+")[0]);
            if (ip.isPresent()) {
              String callbackAddrAndPort = ip.get() + ":" + externalPort;
              return cal -> {
                String callbackUrl =
                    "http://" + callbackAddrAndPort + "/" + getString(cal, KEY_COMMON_BASE_EVENT_PATH);
                return cal.withValue(KEY_CALLBACK_ADDR_AND_PORT, callbackAddrAndPort)
                    .withValue(KEY_CALLBACK_URL, callbackUrl);
              };
            } else {
              return cal -> cal.withError(
                  "could not determine IP address of docker container; the file '/etc/hosts' did not contain a matching entry - hostName: "
                      + hostName + "; hosts: " + hosts);
            }
          } catch (Throwable t) {
            return cal -> cal.withError(
                "could not determine IP address of docker container - hostName: " + hostName + "; exception: " + t
                    .getMessage());
          }
        }, onError);
  }

  private static ConfFunc dockerCrUrls() {
    ConfFunc ims =
        argList(param("CR_PORT_8088_TCP_ADDR").required()).add(param("CR_PORT_8088_TCP_PORT", integer).required())
            .apply(port -> addr -> configure(KEY_IMS_URL,
                cal -> "http://" + addr + ":" + port + "/" + getString(cal, KEY_IMS_PATH)), onError);
    ConfFunc ism =
        argList(param("CR_PORT_9091_TCP_ADDR").required()).add(param("CR_PORT_9091_TCP_PORT", integer).required())
            .apply(port -> addr -> configure(KEY_ISM_URL,
                cal -> "http://" + addr + ":" + port + "/" + getString(cal, KEY_ISM_PATH)), onError);
    ConfFunc m2m =
        argList(param("CR_PORT_8087_TCP_ADDR").required()).add(param("CR_PORT_8087_TCP_PORT", integer).required())
            .apply(port -> addr -> configure(KEY_M2M_URL,
                cal -> "http://" + addr + ":" + port + "/" + getString(cal, KEY_M2M_PATH)), onError);
    // combine all 3 configuration functions into a single function
    return cal -> m2m.apply(ism.apply(ims.apply(cal)));
  }

  private static ConfFunc dockerMongoConnection() {
    return argList(param("MONGO_PORT_27017_TCP_ADDR").required())
        .add(param("MONGO_PORT_27017_TCP_PORT", integer).required()).apply(port -> addr -> {
          ConfFunc cfh = configure(KEY_MONGO_HOST, addr);
          ConfFunc cfp = configure(KEY_MONGO_PORT, port);
          return cal -> cfh.apply(cfp.apply(cal));
        }, onError);
  }

  private static ConfFunc dockerMongoDatabase() {
    return argList(param("MONGO_DATABASE")).apply(database -> configure(KEY_MONGO_DATABASE, database), onError);
  }

  private static ConfFunc dockerTimeoutCheckMillis() {
    return argList(param("TIMEOUT_CHECK_PERIOD_MILLIS", long_))
        .apply(millis -> configure(KEY_TIMOUT_CHECK_PERIOD_MILLIS, millis), onError);
  }

  //
  //
  //

  private static String getString(String paramName) {
    return System.getenv(paramName);
  }

  private static <A> ArgList<ConfFunc, String, Function<A, ConfFunc>> argList(Param<A> param) {
    return ArgList.create(param);
  }

  private static Function<List<String>, ConfFunc> onError = errors -> cal -> cal.withErrors(errors);

  private interface ParamFunc<T> {
    CheckedValue<T, String> apply(String value);
  }

  //

  private static Param<String> param(String paramName) {
    return param(paramName, string);
  }

  private static <A> Param<A> param(String paramName, ParamFunc<A> func) {
    return new Param<>(paramName, func);
  }

  //

  private static ParamFunc<String> string = CheckedValue::just;

  private static ParamFunc<Integer> integer = value -> CheckedValue.just(Integer.parseInt(value));

  private static ParamFunc<Long> long_ = value -> CheckedValue.just(Long.parseLong(value));

  //
  //
  //

  private static class Param<T> implements Supplier<CheckedValue<T, String>> {

    private final String paramName;
    private final ParamFunc<T> func;

    public Param(String paramName, ParamFunc<T> func) {
      this.paramName = paramName;
      this.func = func;
    }

    @Override
    public CheckedValue<T, String> get() {
      try {
        return func.apply(getString(paramName));
      } catch (Throwable t) {
        return CheckedValue
            .error("invalid environment parameter - name: " + paramName + "; exception: " + t.getMessage());
      }
    }

    public Param<T> required() {
      return new Param<>(paramName,
          value -> func.apply(value).check(t -> t != null, "missing environment parameter - name: " + paramName));
    }

    public Param<T> check(Predicate<T> pred, String msg) {
      return new Param<>(paramName, value -> func.apply(value)
          .check(pred, "invalid environment parameter - name: " + paramName + "; msg: " + msg));
    }

    public Param<T> withDefault(T t) {
      return new Param<>(paramName, value -> func.apply(value).withDefault(t));
    }
  }

  //
  //
  //

  private static String getString(ConfigAndLog cal, String key) {
    return cal.config.getString(key);
  }

  private static ConfFunc configure(String key, Function<ConfigAndLog, Object> func) {
    return cal -> cal.withValue(key, func.apply(cal));
  }

  private static ConfFunc configure(String key, Object value) {
    if (value != null) {
      return cal -> cal.withValue(key, value);
    } else {
      return identity;
    }
  }

  private static final ConfFunc identity = cal -> cal;

}

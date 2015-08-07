package eu.swdev.akka.test.conf;

public abstract class ConfigKey {

  public static final String KEY_INST_BACKEND = "instBackend";

  public static final String KEY_COMMON_BASE_EVENT_PATH = KEY_INST_BACKEND + ".commonBaseEventPath";
  public static final String KEY_PORT = KEY_INST_BACKEND + ".port";
  public static final String KEY_BIND_TO_INTERFACE = KEY_INST_BACKEND + ".bindToInterface";
  public static final String KEY_CALLBACK_ADDR_AND_PORT = KEY_INST_BACKEND + ".callbackAddrAndPort";
  public static final String KEY_CALLBACK_URL = KEY_INST_BACKEND + ".callbackUrl";

  public static final String KEY_MONGO = KEY_INST_BACKEND + ".mongo";
  public static final String KEY_MONGO_HOST = KEY_MONGO + ".host";
  public static final String KEY_MONGO_PORT = KEY_MONGO + ".port";
  public static final String KEY_MONGO_DATABASE = KEY_MONGO + ".database";

  public static final String KEY_TIMOUT_CHECK_PERIOD_MILLIS = KEY_INST_BACKEND + ".timeoutCheckPeriodMillis";

  public static final String KEY_IOT = KEY_INST_BACKEND + ".iot";

  public static final String KEY_IMS_PATH = KEY_IOT + ".imsPath";
  public static final String KEY_M2M_PATH = KEY_IOT + ".m2mPath";
  public static final String KEY_ISM_PATH = KEY_IOT + ".ismPath";

  public static final String KEY_IMS_URL = KEY_IOT + ".imsUrl";
  public static final String KEY_M2M_URL = KEY_IOT + ".m2mUrl";
  public static final String KEY_ISM_URL = KEY_IOT + ".ismUrl";

 }

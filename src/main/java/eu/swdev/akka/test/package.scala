package eu.swdev.akka

import com.typesafe.config.{ConfigValueFactory, ConfigList}

package object test {
  implicit def toConfigList(values: Iterable[_ <: AnyRef]): ConfigList = {
    import scala.collection.JavaConverters._
    ConfigValueFactory.fromIterable(values.asJava)
  }

  implicit def anyValToConfigValue(value: AnyVal) = ConfigValueFactory.fromAnyRef(value)
  implicit def anyRefToConfigValue(value: AnyRef) = ConfigValueFactory.fromAnyRef(value)

}

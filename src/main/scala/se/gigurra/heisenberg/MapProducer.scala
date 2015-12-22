package se.gigurra.heisenberg

trait MapProducer[T] {
  def produce(t: T): Map[String, Any]
}

object MapProducer {

  def produce[T: MapProducer](t: T): Map[String, Any] = {
    implicitly[MapProducer[T]].produce(t)
  }

  def singleFieldObject[T: MapDataProducer](value: T, fieldName: String = "data"): Map[String, Any] = {
    Map(fieldName -> MapDataProducer.produce(value))
  }

}

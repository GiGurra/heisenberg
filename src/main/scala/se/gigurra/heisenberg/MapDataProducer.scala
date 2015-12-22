package se.gigurra.heisenberg

import scala.language.implicitConversions

trait MapDataProducer[T] {
  def produce(t: T): Any
}

object MapDataProducer {

  implicit def fcn2Producer[T](f: T => Any): MapDataProducer[T] = new MapDataProducer[T] {
    override def produce(t: T): Any = f(t)
  }

  def produce[T: MapDataProducer](t: T): Any = {
    implicitly[MapDataProducer[T]].produce(t)
  }

  implicit val byteProducer = new MapDataProducer[Byte] {
    override def produce(t: Byte): Any = t
  }

  implicit val shortProducer = new MapDataProducer[Short] {
    override def produce(t: Short): Any = t
  }

  implicit val intProducer = new MapDataProducer[Int] {
    override def produce(t: Int): Any = t
  }

  implicit val longProducer = new MapDataProducer[Long] {
    override def produce(t: Long): Any = t
  }

  implicit val bigintProducer = new MapDataProducer[BigInt] {
    override def produce(t: BigInt): Any = t.bigInteger
  }

  implicit val floatProducer = new MapDataProducer[Float] {
    override def produce(t: Float): Any = t
  }

  implicit val doubleProducer = new MapDataProducer[Double] {
    override def produce(t: Double): Any = t
  }

  implicit val stringProducer = new MapDataProducer[String] {
    override def produce(t: String): Any = t
  }

  implicit val booleanProducer = new MapDataProducer[Boolean] {
    override def produce(t: Boolean): Any = t
  }

  implicit def seqProducer[ElementType : MapDataProducer] = new MapDataProducer[Seq[ElementType]] {
    override def produce(t: Seq[ElementType]): Seq[Any] =  {
      val transformer = implicitly[MapDataProducer[ElementType]]
      val out = t.map(transformer.produce)
      out
    }
  }

  implicit def eitherProducer[L : MapDataProducer, R : MapDataProducer] = new MapDataProducer[Either[L, R]] {
    override def produce(t: Either[L, R]): Any =  {
      t match {
        case Left(left) => MapDataProducer.produce[L](left)
        case Right(right) => MapDataProducer.produce[R](right)
      }
    }
  }

  implicit def _setProducer[ElementType : MapDataProducer] = new MapDataProducer[Set[ElementType]] {
    override def produce(t: Set[ElementType]): Set[Any] =  {
      val transformer = implicitly[MapDataProducer[ElementType]]
      val out = t.map(transformer.produce)
      out
    }
  }

  implicit def mapProducer[ElementType : MapDataProducer] = new MapDataProducer[Map[String, ElementType]] {
    override def produce(t: Map[String, ElementType]): Map[String, Any] =  {
      val transformer = implicitly[MapDataProducer[ElementType]]
      val out = t.map(pair => pair._1 -> transformer.produce(pair._2))
      out
    }
  }

  def clsOf(x: Any): String  = {
    x match {
      case null => "<null>"
      case _ => x.getClass.toString
    }
  }

}
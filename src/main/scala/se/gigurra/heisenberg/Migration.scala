package se.gigurra.heisenberg

import MapData.SourceData

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object Migration {

  type Migrator[New, Old] = Old => New

  def parser[New, Old](newParser: MapParser[New],
                       oldParser: MapParser[Old],
                       migrator: Migrator[New, Old]): MigratingMapParser[New, Old] = {
    new MigratingMapParser(newParser, oldParser)(migrator)
  }

  case class MigratingMapParser[New, Old](_new: MapParser[New],
                                          _old: MapParser[Old])
                                         (implicit m: Migrator[New, Old]) extends MapParser[New] {

    def -->[Newer](newer: MapParser[Newer])(implicit m2: Migrator[Newer, New]): MigratingMapParser[Newer, New] = {
      MigratingMapParser[Newer, New](newer, this)
    }

    override def parse(data: SourceData): New = {
      Try(_new.parse(data)) match {
        case Success(result) => result
        case Failure(e) if !(_new eq _old) => implicitly[Migrator[New, Old]].apply(_old.parse(data))
        case Failure(e) => throw MigrationFailed(s"Failed to parse or migrate data from any known model version", e)
      }
    }
  }

  implicit def sameTypeMigrator[T]: Migrator[T, T] = new Migrator[T, T] { override def apply(t: T): T = t }
  implicit def parser2migratingParser[T](p: MapParser[T]): MigratingMapParser[T,T] = MigratingMapParser(p, p)

}

case class MigrationFailed(msg: String, cause: Throwable) extends RuntimeException(msg, cause)

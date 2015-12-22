package se.gigurra.heisenberg

import java.util.logging.Logger

import MapData.SourceData

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object Migration {

  val logger = Logger.getLogger(getClass.getName)

  type Migrator[New, Old] = Old => New

  def parser[New, Old](newParser: MapParser[New],
                       oldParser: MapParser[Old],
                       migrator: Migrator[New, Old],
                       warnOnMigrate: Boolean = true): MigratingMapParser[New, Old] = {
    new MigratingMapParser(newParser, oldParser, warnOnMigrate)(migrator)
  }

  case class MigratingMapParser[New, Old](_new: MapParser[New],
                                          _old: MapParser[Old],
                                          warnOnMigrate: Boolean)
                                         (implicit m: Migrator[New, Old]) extends MapParser[New] {

    def -->[Newer](newer: MapParser[Newer])(implicit m2: Migrator[Newer, New]): MigratingMapParser[Newer, New] = {
      MigratingMapParser[Newer, New](newer, this, warnOnMigrate)
    }

    override def parse(data: SourceData): New = {
      Try(_new.parse(data)) match {
        case Success(result) => result
        case Failure(eNew) if !(_new eq _old) =>
          Try(implicitly[Migrator[New, Old]].apply(_old.parse(data))) match {
            case Success(result) => result
            case Failure(e: MigrationFailed) =>
              e.attemptErrors = eNew +: e.attemptErrors
              throw e
            case Failure(e) => throw e
          }
        case Failure(e) => throw MigrationFailed(s"Failed to parse or migrate data from any known model version", Seq(e))
      }
    }
  }

  implicit def sameTypeMigrator[T]: Migrator[T, T] = new Migrator[T, T] { override def apply(t: T): T = t }
  implicit def parser2migratingParser[T](p: MapParser[T]): MigratingMapParser[T,T] = MigratingMapParser(p, p, warnOnMigrate = true)

}

case class MigrationFailed(msg: String, var attemptErrors: Seq[Throwable]) extends RuntimeException(msg) {
  override def getMessage: String = {
    "Migration failed with errors:\n" + attemptErrors.map(_.getMessage).mkString("\n")
  }
}

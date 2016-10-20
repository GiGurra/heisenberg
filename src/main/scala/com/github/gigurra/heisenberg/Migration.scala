package com.github.gigurra.heisenberg

import java.util.logging.Logger

import com.github.gigurra.heisenberg.MapData.SourceData

import scala.language.implicitConversions
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success, Try}

object Migration {

  val logger = Logger.getLogger(getClass.getName)

  type Migrator[New, Old] = Old => New

  def parser[New: WeakTypeTag, Old: WeakTypeTag](newParser: MapParser[New],
                                                 oldParser: MapParser[Old],
                                                 migrator: Migrator[New, Old],
                                                 warnOnMigrate: Boolean = false): MigratingMapParser[New, Old] = {
    new MigratingMapParser(newParser, oldParser, warnOnMigrate, migrator)
  }

  case class MigratingMapParser[New: WeakTypeTag, Old: WeakTypeTag](_new: MapParser[New],
                                                                    _old: MapParser[Old],
                                                                    warnOnMigrate: Boolean,
                                                                    migrator: Migrator[New, Old]) extends MapParser[New] {

    def -->[Newer](newer: MapParser[Newer])(implicit m2: Migrator[Newer, New], tag: WeakTypeTag[Newer]): MigratingMapParser[Newer, New] = {
      MigratingMapParser[Newer, New](newer, this, warnOnMigrate, m2)
    }

    override def parse(data: SourceData): New = {
      Try(_new.parse(data)) match {
        case Success(result) => result
        case Failure(eNew) if !(_new eq _old) =>
          if (warnOnMigrate)
            logger.warning(s"Attempting to migrate data from ${weakTypeTag[Old].tpe} -> ${weakTypeTag[New].tpe}")
          Try(migrator.apply(_old.parse(data))) match {
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

  implicit def sameTypeMigrator[T]: Migrator[T, T] = new Migrator[T, T] {
    override def apply(t: T): T = t
  }

  implicit def parser2migratingParser[T: WeakTypeTag](p: MapParser[T]): MigratingMapParser[T, T] = MigratingMapParser(p, p, warnOnMigrate = true, sameTypeMigrator[T])

}

case class MigrationFailed(msg: String, var attemptErrors: Seq[Throwable]) extends RuntimeException(msg) {
  override def getMessage: String = {
    "Migration failed with errors:\n" + attemptErrors.map(_.getMessage).mkString("\n")
  }
}

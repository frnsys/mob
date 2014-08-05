package test.helpers

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask

import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits._
import play.libs.Akka

import scala.concurrent._
import scala.concurrent.duration._

import models._

object MobHelpers {

  /** Simulates a user joining a mob
    * by creating an arbitrary iteratee
    * to consume the mob channel/enumerator.
    */
  def joinUser(username: String, mob: ActorRef) = {
    implicit val timeout = Timeout(1 second)
    val chatIteratee = Iteratee.foreach[JsValue] { event =>
    }

    mob ? (Join(username)) map {
      case Connected(channel) => {
        channel |>> chatIteratee
      }
    }
  }
}

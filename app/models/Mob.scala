package models

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import play.libs.Akka

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Success, Failure}

object Bot {

  def apply(mob: ActorRef) {

    // Create an Iteratee that log all messages to the console.
    val loggerIteratee = Iteratee.foreach[JsValue](event => Logger("bot").info(event.toString))

    implicit val timeout = Timeout(1 second)
    // Make the bot join the room
    mob ? (Join("Bot")) map {
      case Connected(botChannel) =>
        // Apply this Enumerator on the logger.
        botChannel |>> loggerIteratee
    }

    // Make the bot talk every 30 seconds
    Akka.system.scheduler.schedule(
      30 seconds,
      30 seconds,
      mob,
      Talk("Bot", "I'm still alive")
    )
  }

}

object Mob {

  implicit val timeout = Timeout(1 second)

  var mobs = Map.empty[String, ActorRef]

  def apply(slug:String): ActorRef = {
    if(mobs.contains(slug)) {
      mobs(slug)
    } else {
      val mobActor = Akka.system.actorOf(Props[Mob], slug)
      mobs = mobs + (slug -> mobActor)
      mobActor
    }
  }

  def join(slug:String, username:String): Future[Either[mvc.Result, (Iteratee[JsValue,_],Enumerator[JsValue])]] = {
    val mob = apply(slug)
    (mob ? Join(username)).map {

      case Connected(enumerator) =>

        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] { event =>
          mob ! Talk(username, (event \ "text").as[String])
        }.map { _ =>
          mob ! Quit(username)
        }

        Right(iteratee,enumerator)

      case CannotConnect(error) =>

        // Connection error

        // A finished Iteratee sending EOF
        val iteratee = Done[JsValue,Unit]((),Input.EOF)

        // Send an error and close the socket
        val enumerator =  Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))

        Right(iteratee,enumerator)

    }

  }

}

class Mob extends Actor {

  var members = Map.empty[String, Concurrent.Channel[JsValue]]

  def receive = {

    case Join(username) => {
      // Create an Enumerator to write to this socket
      //val channel =  Enumerator.imperative[JsValue]( onStart = self ! NotifyJoin(username))
      //val enumerator = Concurrent.unicast[JsValue]( onStart = (c) => self ! NotifyJoin(username))
      val enumerator = Concurrent.unicast[JsValue]{ channel =>
        // OnStart
        self ! NotifyJoin(username)
        members = members + (username -> channel)
      }
      if(members.contains(username)) {
        sender ! CannotConnect("This username is already used")
      } else {
        sender ! Connected(enumerator)
      }
    }

    case NotifyJoin(username) => {
      notifyAll("join", username, "has entered the room")
    }

    case Talk(username, text) => {
      notifyAll("talk", username, text)
    }

    case Quit(username) => {
      members = members - username
      notifyAll("quit", username, "has left the room")
    }

  }

  def notifyAll(kind: String, user: String, text: String) {
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind),
        "user" -> JsString(user),
        "message" -> JsString(text),
        "members" -> JsArray(
          members.keySet.toList.map(JsString)
        )
      )
    )
    members.foreach {
      case (_, channel) => channel.push(msg)
    }
  }

}

case class Join(username: String)
case class Quit(username: String)
case class Talk(username: String, text: String)
case class NotifyJoin(username: String)

case class Connected(enumerator:Enumerator[JsValue])
case class CannotConnect(msg: String)

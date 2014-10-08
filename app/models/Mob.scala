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
import scala.util.{Success, Failure, Random}

object Mob {

  implicit val timeout = Timeout(1 second)

  var mobs = Map.empty[String, ActorRef]

  // Creates or gets a mob.
  def apply(slug: String): ActorRef = {
    if(mobs.contains(slug)) {
      mobs(slug)
    } else {
      val mobActor = Akka.system.actorOf(Props[Mob], slug)
      mobs = mobs + (slug -> mobActor)

      Bot.populateMob(mobActor)

      mobActor
    }
  }

  // Remove a mob.
  def remove(mob: ActorRef) = {
    mobs = mobs - mob.path.name
  }

  def join(slug: String, username: String): Future[Either[mvc.Result, (Iteratee[JsValue,_],Enumerator[JsValue])]] = {
    val mob = apply(slug)
    join(mob, username)
  }

  def join(mob: ActorRef, username: String): Future[Either[mvc.Result, (Iteratee[JsValue,_],Enumerator[JsValue])]] = {
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
  var bots = Map.empty[String, ActorRef]

  def receive = {

    case Join(username) => {
      // Create an Enumerator to write to this socket
      val enumerator = Concurrent.unicast[JsValue]{ channel =>
        // OnStart
        members = members + (username -> channel)

        self ! NotifyJoin(username)

        // Send an initialization message
        // to the new user channel.
        messageInitial(channel)
      }
      if(members.contains(username)) {
        sender ! CannotConnect("This username is already used")
      } else {
        sender ! Connected(enumerator)
      }
    }

    case NotifyJoin(username) => {
      messageAll("join", username, "has entered the room")
    }

    case Talk(username, text) => {
      messageAll("talk", username, text)
    }

    case Quit(username) => {
      members = members - username

      if (members.isEmpty || onlyBots) {
        // Terminate if empty, or if
        // there are only bots left.
        die

      } else {
        messageAll("quit", username, "has left the room")
      }
    }

    case RegisterBot(username) => {
      bots = bots + (username -> sender)
    }

    case DeregisterBot(username) => {
      bots = bots - username
      self ! Quit(username)
    }

    case RandomUser(username) => {
      val members_ = members - username
      val username_ = members_.keys.toSeq(Random.nextInt(members_.size))
      sender ! RespondTo(username_)
    }
  }

  def die = {
    // Cleanup this mob's bots.
    bots.foreach {
      case(key, value) => {
        value ! PoisonPill
      }
    }

    // Stop tracking this mob in the
    // companion object.
    Mob.remove(self)

    // Terminate the mob.
    self ! PoisonPill
  }

  def messageAll(kind: String, user: String, text: String) {
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind),
        "user" -> JsString(user),
        "message" -> JsString(text)
      )
    )
    notifyAll(msg)
  }

  def messageInitial(channel: Concurrent.Channel[JsValue]) {
    val data = JsObject(
      Seq(
        "kind" -> JsString("initial"),
        "members" -> JsArray(
          members.keySet.toList.map(JsString)
        )
      )
    )
    channel.push(data)
  }

  def notifyAll(data: JsObject) {
    members.foreach {
      case (_, channel) => channel.push(data)
    }
  }

  def onlyBots = {
    members.size - bots.size == 0
  }

}

case class Join(username: String)
case class Quit(username: String)
case class Talk(username: String, text: String)
case class NotifyJoin(username: String)

case class RegisterBot(username: String)
case class DeregisterBot(username: String)

case class Connected(enumerator:Enumerator[JsValue])
case class CannotConnect(msg: String)

case class RandomUser(username: String)

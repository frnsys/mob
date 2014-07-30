package controllers

import models._

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._ 

import scala.concurrent._
import scala.util.{Success, Failure}

object Application extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def mob(slug: String, username: Option[String]) = Action { implicit request =>
    username.filterNot(_.isEmpty).map { username =>
      Ok(views.html.mob(slug, username))
    }.getOrElse {
      Redirect(routes.Application.index).flashing(
        "error" -> "Please choose a valid username."
      )
    }
  }

  def mobChat(slug: String, username: String) = WebSocket.tryAccept[JsValue] { request  =>
    Mob.join(slug, username)
  }
}
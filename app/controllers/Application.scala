package controllers

import models._

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._ 

import play.api.data._
import play.api.data.Forms._

case class MobData(mob: String, username: String)

object Application extends Controller {

  val mobForm = Form(
    mapping(
      "mob" -> nonEmptyText,
      "username" -> nonEmptyText
    )(MobData.apply)(MobData.unapply)
  )


  def index = Action { implicit request =>
    Ok(views.html.index(mobForm))
  }

  def joinMob = Action { implicit request =>
    mobForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.index(formWithErrors))
      },
      mobData => {
        Redirect(routes.Application.mob(mobData.mob, Option(mobData.username)))
      }
    )
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
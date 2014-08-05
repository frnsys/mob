import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

import akka.actor._
import akka.testkit.{TestActorRef, TestProbe}

import models._
import test.helpers._

@RunWith(classOf[JUnitRunner])
class MobSpec extends Specification {

  "Mob" should {
    implicit val actorSystem = ActorSystem("mobSpecSystem")

    "keep track of mobs" in new WithApplication{
      Mob.join("some_slug", "some_username")
      Mob.mobs.size mustEqual 1
    }

    "keep track of members" in new WithApplication{
      val mobActor = TestActorRef[Mob]
      MobHelpers.joinUser("some user", mobActor)

      Thread.sleep(50)
      mobActor.underlyingActor.members.size mustEqual 1
    }

    "keep track of bots separately from members" in new WithApplication{
      val mobActor = TestActorRef[Mob]
      val botActor = TestActorRef(new Bot("a username", mobActor))

      Thread.sleep(50)
      mobActor.underlyingActor.bots.size mustEqual 1
    }

    "should terminate if only bots remain" in new WithApplication{
      // Create a mob and populate it wth a bot and a user.
      val mobActor = TestActorRef[Mob]
      MobHelpers.joinUser("some user", mobActor)
      val botActor = TestActorRef(new Bot("a username", mobActor))

      Thread.sleep(50)
      mobActor.underlyingActor.members.size mustEqual 2

      val probe = TestProbe()
      probe watch mobActor

      mobActor ! Quit("some user")

      probe.expectTerminated(mobActor)
    }

    "should terminate its bots when it terminates" in new WithApplication{
      // Create a mob and populate it wth a bot and a user.
      val mobActor = TestActorRef[Mob]
      MobHelpers.joinUser("some user", mobActor)
      val botActor = TestActorRef(new Bot("a username", mobActor))

      Thread.sleep(50)
      mobActor.underlyingActor.members.size mustEqual 2

      val probe = TestProbe()
      probe watch botActor

      mobActor ! Quit("some user")

      probe.expectTerminated(botActor)
    }

  }

}

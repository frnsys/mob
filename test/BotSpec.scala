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
class BotSpec extends Specification {

  "Bot" should {
    implicit val actorSystem = ActorSystem("botSpecSystem")

    "be able to speak" in new WithApplication{
      Bot.generateSpeech.size must beGreaterThan(0)
    }

    "can populate a mob" in new WithApplication{
      val mobActor = TestActorRef[Mob]
      mobActor.underlyingActor.members.size mustEqual 0

      Bot.populateMob(mobActor)

      // This is all asynchronous,
      // so wait a bit. Later this test should be fixed up
      // to better support this.
      Thread.sleep(50)
      mobActor.underlyingActor.members.size must beGreaterThan(0)
    }

    "dies self-terminates and quits its mob" in new WithApplication{
      // Create a mob and populate it with a bot and a user.
      val mobActor = TestActorRef[Mob]
      MobHelpers.joinUser("some user", mobActor)
      val botActor = TestActorRef(new Bot("a username", mobActor))

      Thread.sleep(50)
      mobActor.underlyingActor.members.size mustEqual 2

      val probe = TestProbe()
      probe watch botActor

      botActor ! PoisonPill

      probe.expectTerminated(botActor)

      Thread.sleep(50)
      mobActor.underlyingActor.members.size mustEqual 1
    }

  }
}

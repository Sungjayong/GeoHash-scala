import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply

import java.util.UUID

import org.scalatest.wordspec.AnyWordSpecLike

class MyPersistenceSpec extends ScalaTestWithActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """) with AnyWordSpecLike {
  "Membership" should {

    "join member" in {
      val membership = testKit.spawn(MyPersistentBehavior())
      val probe = testKit.createTestProbe[StatusReply[MyPersistentBehavior.Summary]]
      membership ! MyPersistentBehavior.Join("foo", 42.213, 23.125, probe.ref)
      probe.expectMessage(StatusReply.Success(MyPersistentBehavior.Summary(Map("foo" -> (42.213, 23.125)))))
    }

    "leave member" in {
      val membership = testKit.spawn(MyPersistentBehavior())
      val probe = testKit.createTestProbe[StatusReply[MyPersistentBehavior.Summary]]
      membership ! MyPersistentBehavior.Join("foo", 42.213, 23.125, probe.ref)
      probe.expectMessage(StatusReply.Success(MyPersistentBehavior.Summary(Map("foo" -> (42.213, 23.125)))))
      membership ! MyPersistentBehavior.Leave("foo", probe.ref)
      probe.expectMessage(StatusReply.Success(MyPersistentBehavior.Summary(Map.empty)))
    }
  }
}

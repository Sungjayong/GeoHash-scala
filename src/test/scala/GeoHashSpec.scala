import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class GeoHashSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import Geohash._

  "GeoHash actor" must {

    "자식 액터 생성 후, read, write 명령을 보내고, 성공 알림 및 결과를 받아야 함." in {
      val recordProbe = createTestProbe[GeohashRecorded]()
      val readProbe = createTestProbe[RespondGeohash]()
      val geohashActor = spawn(Geohash())

      geohashActor ! Geohash.Update(requestId = 1, (57.64911004342139,10.407439861446619), "EVENT1", recordProbe.ref)
      recordProbe.expectMessage(Geohash.GeohashRecorded(requestId = 1))

      geohashActor ! Geohash.Update(requestId = 2, (57.64911004342139,10.407439526170492), "EVENT2", recordProbe.ref)
      recordProbe.expectMessage(Geohash.GeohashRecorded(requestId = 2))

      geohashActor ! Geohash.Get(requestId = 3, (57.64911004342139,10.407439861446619), readProbe.ref)
      val response1 = readProbe.receiveMessage()
      response1.requestId should ===(3)
      response1.value should ===(List("EVENT1", "EVENT2"))
    }
  }
}

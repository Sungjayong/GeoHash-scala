import Geohash._
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import GeohashCalc._

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object Geohash {
  final case class Get(requestId: Long, value: (Double, Double), replyTo: ActorRef[RespondGeohash]) extends Command
  final case class Update(requestId: Long, value: (Double, Double), event: String, replyTo: ActorRef[GeohashRecorded]) extends Command
  final case class Delete(value: String) extends Command
  final case class GeohashRecorded(requestId: Long)
  final case class RespondGeohash(requestId: Long, value : List[String])
  private case object RecordTimerKey
  sealed trait Command

  def apply(): Behavior[Command] =
    Behaviors.setup(context => new Geohash(context))
}

class Geohash(context: ActorContext[Command])
  extends AbstractBehavior[Geohash.Command](context) {
  var cashMap = Map[String, String]()
  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case Update(id, value, event, replyTo) =>
        val ghVal = encode(value._1, value._2)
        cashMap +=  ghVal -> event
        replyTo ! GeohashRecorded(id)
        // 30초 후 실행 로직
        // thread는 사용하지 않는다고 생각하자 => Behaviors.withTimers
        Behaviors.withTimers[Command] { timers =>
          timers.startTimerWithFixedDelay(RecordTimerKey, Delete(ghVal), 1 second)
          this
        }
        this

      case Get(id, value, replyTo) =>
        val ghVal = encode(value._1, value._2)
        val replyElements = List(ghVal, west(ghVal), east(ghVal),
                                south(ghVal), north(ghVal), southeast(ghVal),
                                southwest(ghVal), northeast(ghVal), northwest(ghVal))
                                .filter(s => cashMap.contains(s))
                                .map(s => cashMap(s))
        replyTo ! RespondGeohash(id, replyElements)
        this

      case Delete(s) =>
        cashMap -= s
        this
    }
  }
}
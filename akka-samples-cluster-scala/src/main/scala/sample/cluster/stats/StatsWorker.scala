package sample.cluster.stats

import GeohashCalc.{east, north, northeast, northwest, south, southeast, southwest, west}
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import sample.cluster.CborSerializable

import scala.concurrent.duration.DurationInt

//#worker
object StatsWorker {

  trait Command
  final case class Process(item: Any, replyTo: ActorRef[Processed]) extends Command with CborSerializable
  final case class Processed(item: Any) extends CborSerializable

  var cashMap = Map[String, String]()

  def apply(): Behavior[Command] = Behaviors.setup { ctx =>
//    Behaviors.withTimers { timers =>
      ctx.log.info("Worker starting up")
//      timers.startTimerWithFixedDelay(EvictCache, EvictCache, 30.seconds)
      withCache(ctx)
//    }
  }

  private def withCache(ctx: ActorContext[Command]): Behavior[Command] =
    Behaviors.receiveMessage {
        // Get 요청 시.
        case Process(ghVal: String, replyTo) => {
          val replyElements = List(ghVal, west(ghVal), east(ghVal),
                                    south(ghVal), north(ghVal), southeast(ghVal),
                                    southwest(ghVal), northeast(ghVal), northwest(ghVal))
                              .filter(s => cashMap.contains(s))
                              .map(s => cashMap(s))
          ctx.log.info("Worker processing request [{}]", replyElements)

          replyTo ! Processed(replyElements)
          Behaviors.same
        }

        // Update 요청 시.
        case Process(item: (String, String), replyTo) => {
          cashMap += item._1 -> item._2
          replyTo ! Processed()
          Behaviors.same
        }
    }
}
//#worker

package sample.cluster.stats

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import sample.cluster.CborSerializable
import sample.cluster.stats.GeohashCalc.encode

import scala.concurrent.duration._

//#service
object StatsService {

  sealed trait Command extends CborSerializable
  case class Get(tuple: (Double, Double), responseAdapter: ActorRef[Response]) extends Command
  case class Update(tuple: (Double, Double), event: String, responseAdapter: ActorRef[Response]) extends Command

  case object Stop extends Command

  sealed trait Response extends CborSerializable
  final case class JobResult(replyElements: String) extends Response
  final case class JobFailed(reason: String) extends Response

  def apply(workers: ActorRef[StatsWorker.Process]): Behavior[Command] = {
    Behaviors.setup { ctx =>
      // if all workers would crash/stop we want to stop as well
      ctx.watch(workers)
      Behaviors.receiveMessage {
        case Get(tuple, replyTo) =>
          ctx.log.info("Delegating request Get")
          val ghVal = encode(tuple._1, tuple._2)
          // create per request actor that collects replies from workers
          ctx.spawnAnonymous(GetAggregator(ghVal, workers, replyTo))
          Behaviors.same

        case Update(tuple, event, replyTo) =>
          ctx.log.info("Delegating request Update")
          val ghVal = encode(tuple._1, tuple._2)
          ctx.spawnAnonymous(UpdateAggregator(ghVal, event, workers, replyTo))
          Behaviors.same

        case Stop =>
          Behaviors.stopped
      }
    }
  }
}
object UpdateAggregator {
  sealed trait Event
  private case object Timeout extends Event
  private case class UpdateComplete() extends Event

  def apply(ghVal: String, event: String, workers: ActorRef[StatsWorker.Process], replyTo: ActorRef[StatsService.Response]): Behavior[Event] =
    Behaviors.setup { ctx =>
      ctx.setReceiveTimeout(3.seconds, Timeout)
      val responseAdapter = ctx.messageAdapter[StatsWorker.Processed](processed =>
        UpdateComplete()
      )
      workers ! StatsWorker.Process((ghVal, event), responseAdapter)

      waiting(replyTo)
    }
  private def waiting(replyTo: ActorRef[StatsService.Response]): Behavior[Event] =
    Behaviors.receiveMessage {
      case UpdateComplete() =>
        replyTo ! StatsService.JobResult("update ok") // client에게 전달.
        Behaviors.stopped

      case Timeout =>
        replyTo ! StatsService.JobFailed("Service unavailable, try again later")
        Behaviors.stopped
    }

}

object GetAggregator {
  sealed trait Event
  private case object Timeout extends Event
  private case class GetComplete(replyElements: Any) extends Event

  def apply(ghVal: String, workers: ActorRef[StatsWorker.Process], replyTo: ActorRef[StatsService.Response]): Behavior[Event] =
    Behaviors.setup { ctx =>
      ctx.setReceiveTimeout(3.seconds, Timeout)
      val responseAdapter = ctx.messageAdapter[StatsWorker.Processed](processed =>
        GetComplete(processed.item)
      )
      workers ! StatsWorker.Process(ghVal, responseAdapter)

      waiting(replyTo)
    }

  private def waiting(replyTo: ActorRef[StatsService.Response]): Behavior[Event] =
    Behaviors.receiveMessage {
      case GetComplete(replyElements: List[String]) =>
        replyTo ! StatsService.JobResult(replyElements.mkString(" ")) // client에게 전달.
        Behaviors.stopped

      case Timeout =>
        replyTo ! StatsService.JobFailed("Service unavailable, try again later")
        Behaviors.stopped
    }

}
//#service

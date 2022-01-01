import Geohash._
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import GeohashCalc._

object Geohash {
  final case class Get(requestId: Long, value: (Double, Double), replyTo: ActorRef[RespondGeohash]) extends Command
  final case class Update(requestId: Long, value: (Double, Double), event: String, replyTo: ActorRef[GeohashRecorded]) extends Command
  final case class Delete(value: String) extends Command
  final case class GeohashRecorded(requestId: Long)
  final case class RespondGeohash(requestId: Long, value : List[String])
  sealed trait Command

  def apply(): Behavior[Command] =
    Behaviors.setup(context => new Geohash(context))
}

class Geohash(context: ActorContext[Command])
  extends AbstractBehavior[Geohash.Command](context) {
  var cashMap = scala.collection.mutable.Map[String, String]()
  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case Update(id, value, event, replyTo) =>
        val ghVal = encode(value._1, value._2)
        cashMap +=  ghVal -> event
        replyTo ! GeohashRecorded(id)
        // 30초 후 실행 로직
        new Thread(new Runnable {
          override def run() = {
            Thread.sleep(30000)
            Delete(ghVal)
          }
        }).start()
        this

      case Get(id, value, replyTo) =>
        val ghVal = encode(value._1, value._2)
        val it = Iterator(ghVal, west(ghVal), east(ghVal), south(ghVal), north(ghVal),
          southeast(ghVal), southwest(ghVal), northeast(ghVal), northwest(ghVal))
        var replyElements = collection.mutable.ListBuffer.empty[String]
        while(it.hasNext) {
          val cashKey = it.next()
          if(cashMap.contains(cashKey))
            replyElements += cashMap(cashKey)
        }
        replyTo ! RespondGeohash(id, replyElements.toList)
        this
      case Delete(s) =>
        cashMap - s
        this
    }
  }

}
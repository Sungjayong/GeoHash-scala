import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}

import scala.concurrent.duration.DurationInt

object MyPersistentBehavior {
  sealed trait Command

  final case class Join(name: String, lat: Double, lon: Double) extends Command

  final case class Leave(name: String) extends Command

  final case class Get(name: String) extends Command

  sealed trait Eventextends CborSerializable

  final case class Joined(name: String, lat: Double, lon: Double) extends Event

  final case class Left(name: String) extends Event

  final case class Got(name: String) extends Event

  final case class Summary(info: Map[String, (Double, Double)]) extends CborSerializable

  final case class State(info: Map[String, (Double, Double)] = Map()) extends CborSerializable {
    def join(name: String, lat: Double, lon: Double): State = {
      copy(info = info + (name -> (lat, lon)))
    }

    def leave(name: String): State = {
      copy(info = info - name)
    }

    def get(name: String): State = {
      if (info.contains(name)) copy(info = Map(name -> info(name)))
      else State.empty
    }

    def toSummary: Summary =
      Summary(info)
  }

  object State {
    val empty = State(info = Map.empty)
  }

  val commandHandler: (State, Command) => Effect[Event, State] = { (state, command) =>
    command match {
      case Join(name, lat, lon) => Effect.persist(Joined(name, lat, lon))
      case Leave(name) => Effect.persist(Left(name))
      case Get(name) => Effect.persist(Got(name))
    }
  }

  val eventHandler: (State, Event) => State = { (state, event) =>
    event match {
      case Joined(name, lat, lon) => {
        state.join(name, lat, lon)
      }
      case Left(name) => {
        state.leave(name)
      }
      case Got(name) => {
        state.get(name)
      }
    }
  }

  def apply(entityId: String): Behavior[Command] = {
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId("membership", entityId),
      emptyState = State.empty,
      commandHandler = (state, cmd) => commandHandler(state, cmd),
      eventHandler = (state, evt) => eventHandler(state, evt))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))
  }

  def main(args: Array[String]): Unit = {
    val system: ActorSystem[Command] = ActorSystem(MyPersistentBehavior("name"), "actor")
    system ! MyPersistentBehavior.Join("a", 1.0, 2.0)
    system ! MyPersistentBehavior.Join("b", 2.0, 3.0)
    system ! MyPersistentBehavior.Leave("a")
    system ! MyPersistentBehavior.Join("c", 1.0, 2.0)
  }
}

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import scala.concurrent.duration.DurationInt

object MyPersistentBehavior {
  sealed trait Command
  final case class Join(name: String, lat: Double, lon: Double, replyTo: ActorRef[StatusReply[Summary]]) extends Command
  final case class Leave(name: String, replyTo: ActorRef[StatusReply[Summary]]) extends Command
  final case class Get(name: String, replyTo: ActorRef[StatusReply[Summary]]) extends Command

  sealed trait Event
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
      if(info.contains(name)) copy(info = Map(name -> info(name)))
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
      case Join(name, lat, lon, replyTo) => Effect.persist(Joined(name, lat, lon)).thenRun(updatedState => replyTo ! StatusReply.Success(updatedState.toSummary))
      case Leave(name, replyTo) => Effect.persist(Left(name)).thenRun(updatedState => replyTo ! StatusReply.Success(updatedState.toSummary))
      case Get(name, replyTo) => Effect.persist(Got(name)).thenRun(getState => replyTo ! StatusReply.Success(getState.toSummary))
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

  def apply(): Behavior[Command] = {
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("abc"),
      emptyState = State.empty,
      commandHandler = (state, cmd) => commandHandler(state, cmd),
      eventHandler = (state, evt) => eventHandler(state, evt))
        .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))
  }
}

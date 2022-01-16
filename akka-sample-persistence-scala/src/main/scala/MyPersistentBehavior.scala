import akka.Done
import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.stream.alpakka.cassandra.scaladsl.{CassandraSession}

object MyPersistentBehavior {
  sealed trait Command
  final case class Join(name: String, lat: Double, lon: Double, replyTo: ActorRef[StatusReply[Summary]]) extends Command
  final case class Leave(name: String, replyTo: ActorRef[StatusReply[Summary]]) extends Command
  final case class Get(name: String) extends Command

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
      case Get(name) => Effect.persist(Got(name))
    }
  }

  val eventHandler: (State, Event) => State = { (state, event) =>
    event match {
      case Joined(name, lat, lon) => {
        //TODO. Cassandra 연결.
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

  var Session: CassandraSession = null
  var Keyspace: String = ""
  val Table: String = "membership"

//  def apply(session: CassandraSession, keyspace: String): Behavior[Command] = {
  def apply(): Behavior[Command] = {
//    Session = session
//    Keyspace = keyspace
    EventSourcedBehavior[Command, Event, State](

      persistenceId = PersistenceId.ofUniqueId("abc"),
      emptyState = State.empty,
      commandHandler = (state, cmd) => commandHandler(state, cmd),
      eventHandler = (state, evt) => eventHandler(state, evt)
    )
  }
}

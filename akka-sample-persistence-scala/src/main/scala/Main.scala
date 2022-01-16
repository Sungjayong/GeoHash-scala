import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.cassandra.scaladsl.CassandraSessionRegistry

object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Behaviors.empty, "MembershipService")
    init(system)
  }

  def init(system: ActorSystem[_]): Unit = {

    // Get an existing session or start a new one with the given settings, makes it possible to share one session across plugins.
    val session = CassandraSessionRegistry(system).sessionFor(
      "akka.persistence.cassandra"
    )
    // use same keyspace for the item_popularity table as the offset store
    val keyspace =
      system.settings.config
        .getString("akka.projection.cassandra.offset-store.keyspace")
//    MyPersistentBehavior(session, keyspace)
    MyPersistentBehavior()
  }
}

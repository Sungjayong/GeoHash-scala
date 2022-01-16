import akka.Done
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession

import scala.concurrent.{ExecutionContext, Future}

class MembershipRepository {
  trait MembershipRepository {
//    def join(name: String, lat: Double, lon: Double): Future[Done]
    def leave(name: String): Future[Done]
//    def get(name: String): Future[Option[(Double, Double)]]
  }
  object MembershipRepositoryImpl {
    val table = "membership"
  }

  class MembershipRepositoryImpl(session: CassandraSession, keyspace: String)(
    implicit val ec: ExecutionContext) extends MembershipRepository {
    import MembershipRepositoryImpl.table

//    override def join(name: String, lat: Double, lon: Double): Future[Done] = {
//      session.executeWrite(s"INSERT INTO $keyspace.$table name, lat, lon VALUES (?, ?, ?)", name, lat, lon)
//    }
    override def leave(name: String) = {
      session.executeWrite(s"DELETE FROM $keyspace.$table WHERE IF name = ?", name)
    }
//    override def get(name: String): Future[Option[(Double, Double)]] = {
//      session.selectOne(s"SELECT lat, lon FROM $keyspace.$table WHERE name = ?", name)
//        .map(opt => opt.map(row => (row.getDouble("lat").doubleValue(), row.getDouble("lon").doubleValue())))
//    }
  }
}

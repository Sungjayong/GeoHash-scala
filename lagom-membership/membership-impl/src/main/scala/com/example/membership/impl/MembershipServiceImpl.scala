package com.example.membership.impl

import com.example.membership.api.MembershipService
import akka.Done
import akka.NotUsed
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.api.transport.BadRequest

/**
  * Implementation of the MembershipService.
  */
class MembershipServiceImpl(
  clusterSharding: ClusterSharding,
  persistentEntityRegistry: PersistentEntityRegistry
)(implicit ec: ExecutionContext)
  extends MembershipService {

  private def entityRef(id: String): EntityRef[MembershipCommand] =
    clusterSharding.entityRefFor(MembershipState.typeKey, id)

  implicit val timeout = Timeout(5.seconds)

  override def join(name: String): ServiceCall[NotUsed, Done] = ServiceCall {
    _ =>
      val ref = entityRef(name)
      ref
        .ask[Confirmation](replyTo => Join(name, replyTo))
        .map {
          case Accepted => Done
          case _        => throw BadRequest("Can't upgrade the greeting message.")
        }
  }

  override def leave(name: String): ServiceCall[NotUsed, Done] = ServiceCall {
    _ =>
      val ref = entityRef(name)
      ref
        .ask[Confirmation](replyTo => Leave(name, replyTo))
        .map {
          case Accepted => Done
          case _        => throw BadRequest("Can't upgrade the greeting message.")
        }
  }

  override def get(name: String): ServiceCall[NotUsed, String] = ServiceCall {
    _ =>
      val ref = entityRef(name)
      ref
        .ask[Greeting](replyTo => Get(name, replyTo))
        .map(greeting => greeting.name)
  }
}

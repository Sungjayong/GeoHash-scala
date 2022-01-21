package com.example.membership.impl

import play.api.libs.json.Json
import play.api.libs.json.Format

import java.time.LocalDateTime
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.ReplyEffect
import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import play.api.libs.json._

import scala.collection.immutable.Seq

object MembershipBehavior {
  def create(entityContext: EntityContext[MembershipCommand]): Behavior[MembershipCommand] = {
    val persistenceId: PersistenceId = PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)

    create(persistenceId)
      .withTagger(
        AkkaTaggerAdapter.fromLagom(entityContext, MembershipEvent.Tag)
      )

  }

  private[impl] def create(persistenceId: PersistenceId) = EventSourcedBehavior
      .withEnforcedReplies[MembershipCommand, MembershipEvent, MembershipState](
        persistenceId = persistenceId,
        emptyState = MembershipState.initial,
        commandHandler = (cart, cmd) => cart.applyCommand(cmd),
        eventHandler = (cart, evt) => cart.applyEvent(evt)
      )
}

case class MembershipState(info: Set[String]) {
  def applyCommand(cmd: MembershipCommand): ReplyEffect[MembershipEvent, MembershipState] =
    cmd match {
      case x: Join               => onJoin(x)
      case x: Leave              => onLeave(x)
      case x: Get                => onGet(x)
    }

  def applyEvent(evt: MembershipEvent): MembershipState =
    evt match {
      case JoinState(name) => updateJoin(name)
      case LeaveState(name) => updateLeave(name)
    }

  private def onJoin(cmd: Join)
  : ReplyEffect[MembershipEvent, MembershipState] = {
    Effect.persist(JoinState(cmd.name))
      //TODO. 현재, thenReply가 실행되지 않아, return 받지 못하는 중. => json format 문제. 해결.
          .thenReply(cmd.replyTo) { _ =>
            Accepted
          }
  }

  private def onLeave(cmd: Leave)
  : ReplyEffect[MembershipEvent, MembershipState] = {
    Effect.persist(LeaveState(cmd.name))
      .thenReply(cmd.replyTo) { _ =>
        Accepted
      }
  }

  private def onGet(cmd: Get)
  : ReplyEffect[MembershipEvent, MembershipState] = {
    if(info.contains(cmd.name)) Effect.reply(cmd.replyTo)(Greeting(s"${cmd.name} get!"))
    else Effect.reply(cmd.replyTo)(Greeting(s"nothing"))
  }

  private def updateJoin(name: String) = {
    copy(info = info + name)
  }

  private def updateLeave(name: String) = {
    copy(info = info - name)
  }
}

object MembershipState {
  def initial: MembershipState = MembershipState(Set.empty)

  val typeKey = EntityTypeKey[MembershipCommand]("MembershipAggregate")
  implicit val format: Format[MembershipState] = Json.format
}

/**
  * This interface defines all the events that the MembershipAggregate supports.
  */
sealed trait MembershipEvent extends AggregateEvent[MembershipEvent] {
  def aggregateTag: AggregateEventTag[MembershipEvent] = MembershipEvent.Tag
}

object MembershipEvent {
  val Tag: AggregateEventTag[MembershipEvent] = AggregateEventTag[MembershipEvent]
}

case class JoinState(name: String) extends MembershipEvent

object JoinState {
  implicit val format: Format[JoinState] = Json.format
}

case class LeaveState(name: String) extends MembershipEvent

object LeaveState {
  implicit val format: Format[LeaveState] = Json.format
}

/**
  * This is a marker trait for commands.
  * We will serialize them using Akka's Jackson support that is able to deal with the replyTo field.
  * (see application.conf)
  */
trait MembershipCommandSerializable

/**
  * This interface defines all the commands that the MembershipAggregate supports.
  */
sealed trait MembershipCommand
    extends MembershipCommandSerializable

case class Join(name: String, replyTo: ActorRef[Confirmation])
    extends MembershipCommand

case class Leave(name: String, replyTo: ActorRef[Confirmation])
    extends MembershipCommand

case class Get(name: String, replyTo: ActorRef[Greeting])
  extends MembershipCommand

final case class Greeting(name: String)

sealed trait Confirmation

case object Confirmation {
  implicit val format: Format[Confirmation] = new Format[Confirmation] {
    override def reads(json: JsValue): JsResult[Confirmation] = {
      if ((json \ "reason").isDefined)
        Json.fromJson[Rejected](json)
      else
        Json.fromJson[Accepted](json)
    }

    override def writes(o: Confirmation): JsValue = {
      o match {
        case acc: Accepted => Json.toJson(acc)
        case rej: Rejected => Json.toJson(rej)
      }
    }
  }
}

sealed trait Accepted extends Confirmation

case object Accepted extends Accepted {
  implicit val format: Format[Accepted] =
    Format(Reads(_ => JsSuccess(Accepted)), Writes(_ => Json.obj()))
}

case class Rejected(reason: String) extends Confirmation

object Rejected {
  implicit val format: Format[Rejected] = Json.format
}

/**
  * Akka serialization, used by both persistence and remoting, needs to have
  * serializers registered for every type serialized or deserialized. While it's
  * possible to use any serializer you want for Akka messages, out of the box
  * Lagom provides support for JSON, via this registry abstraction.
  *
  * The serializers are registered here, and then provided to Lagom in the
  * application loader.
  */
object MembershipSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    // state and events can use play-json, but commands should use jackson because of ActorRef[T] (see application.conf)
    JsonSerializer[JoinState],
    JsonSerializer[LeaveState],
    JsonSerializer[MembershipState],
    // the replies use play-json as well
    JsonSerializer[Confirmation],
    JsonSerializer[Accepted],
    JsonSerializer[Rejected]
  )
}

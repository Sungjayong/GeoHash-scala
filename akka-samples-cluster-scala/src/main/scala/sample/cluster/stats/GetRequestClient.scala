/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package sample.cluster.stats

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._

object GetRequestClient {

  sealed trait Event
  private case object Tick extends Event
  private case class ServiceResponse(result: StatsService.Response) extends Event

  def apply(service: ActorRef[StatsService.Get]): Behavior[Event] =
    Behaviors.setup { ctx =>
      Behaviors.withTimers { timers =>
        timers.startTimerWithFixedDelay(Tick, Tick, 4.seconds)
        val responseAdapter = ctx.messageAdapter(ServiceResponse)

        Behaviors.receiveMessage {
          case Tick =>
            ctx.log.info("Get Sending process request")
            service ! StatsService.Get((57.64911004342139,10.407439861446619), responseAdapter)
            Behaviors.same
          case ServiceResponse(result) =>
            ctx.log.info("Get Service result: {}", result)
            Behaviors.same
        }
      }
    }

}


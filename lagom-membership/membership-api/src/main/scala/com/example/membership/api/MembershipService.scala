package com.example.membership.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.Service.restCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json}

/**
  * The Membership service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the MembershipService.
  */
trait MembershipService extends Service {
  /**
   * Example: curl --location --request PUT 'http://localhost:9000/membership/Alice'
   */
  def join(name: String): ServiceCall[NotUsed, Done]
  /**
   * Example: curl --location --request DELETE 'http://localhost:9000/membership/Alice'
   */
  def leave(name: String): ServiceCall[NotUsed, Done]
  /**
   * Example: curl --location --request POST 'http://localhost:9000/membership/Alice'
   */
  def get(name: String): ServiceCall[NotUsed, String]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("membership")
      .withCalls(
        restCall(Method.PUT, "/membership/:name", join _),
        restCall(Method.DELETE, "/membership/:name", leave _),
        restCall(Method.POST, "/membership/:name", get _)
      )
      .withAutoAcl(true)
  }
}


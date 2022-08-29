package org.nimbleedge.envisedge

import models._
import scala.concurrent.duration._
import scala.collection.mutable.{Map => MutableMap}

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.Signal
import akka.actor.typed.PostStop

/** The Orchestrator is the top-level actor for the Envisedge system.
  *
  * It is an object of the Orchestrator class, and it is responsible for managing the lifecycle of the system.
  * It also handles the coordination of the different actors that make up the system. Orchestration is the process
  * of creating a system of actors that are responsible for the execution of processes and the communication between
  * them. In this case, the orchestrator uses the actors that are responsible for the execution of the processes and
  * the actors that are responsible for the communication between them.
  *
  * @param context The actor context for the orchestrator.
  * @param config The configuration for the orchestrator.
  * @return The behavior for the orchestrator.
  */
object Orchestrator {
  def apply(orcId: OrchestratorIdentifier): Behavior[Command] =
    Behaviors.setup(new Orchestrator(_, orcId))

  /** The command to start the orchestrator.
    *
    * This command is sent to the orchestrator when it is started.
    */
  trait Command

  // In case any Aggregator Termination
  private final case class AggregatorTerminated(actor: ActorRef[Aggregator.Command], aggId: AggregatorIdentifier)
    extends Orchestrator.Command

  // TODO
  // Add messages here
}

/** The map of aggregators that are currently running.
  *
  * This is a map of the aggregators that are currently running. The `key` is the identifier of the aggregator, and
  * the `value` is the actor that is responsible for the execution of the aggregator, and the actor is also responsible
  * for the communication between the aggregator and the orchestrator.
  *
  * @param context The actor context for the orchestrator.
  * @param orcId The identifier for the orchestrator.
  * @param aggregators The map of aggregators that are currently running.
  * @return The map of aggregators that are currently running.
  */
class Orchestrator(context: ActorContext[Orchestrator.Command], orcId: OrchestratorIdentifier) extends AbstractBehavior[Orchestrator.Command](context) {
  import Orchestrator._
  import FLSystemManager.{ RequestAggregator, AggregatorRegistered, RequestTrainer, RequestRealTimeGraph }

  // TODO
  // Add state and persistent information
  var aggIdToRef : MutableMap[AggregatorIdentifier, ActorRef[Aggregator.Command]] = MutableMap.empty
  context.log.info("Orchestrator {} started", orcId.name())
  
/** The actor that is in charge of getting the aggregator registered with the orchestrator.
  *
  * It determines if the aggregator is already registered, if not, it registers it. It then returns the
  * actor reference after registration. Aditionally, if the aggregator is not in the map, then we need
  * to request it from the FLSystemManager. This is done by sending a RequestAggregator message to the
  * FLSystemManager.
  *
  * @param aggId The identifier of the aggregator.
  * @return The actor reference of the aggregator.
  */
  private def getAggregatorRef(aggId: AggregatorIdentifier): ActorRef[Aggregator.Command] = {
    aggIdToRef.get(aggId) match {
        case Some(actorRef) =>
            actorRef
        case None =>
            context.log.info("Creating new aggregator actor for {}", aggId.name())
            val actorRef = context.spawn(Aggregator(aggId), s"aggregator-${aggId.name()}")
            context.watchWith(actorRef, AggregatorTerminated(actorRef, aggId))
            aggIdToRef += aggId -> actorRef
            actorRef
    }
  }

/** The onMessage method is the main method of the orchestrator. It is responsible for handling the different
 * commands that are sent to the orchestrator.
 *
 * @param msg The command that is sent to the orchestrator. It can be a RequestAggregator, RequestTrainer,
 *            RequestRealTimeGraph, or AggregatorRegistered message. It can also be an AggregatorTerminated
 *            message if the aggregator actor is terminated.
 * @return The behavior of the orchestrator.
 */
  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case trackMsg @ RequestAggregator(requestId, aggId, replyTo) =>
        if (aggId.getOrchestrator() != orcId) {
          context.log.info("Expected orchestrator id {}, found {}", orcId.name(), aggId.toString())
        } else {
          val actorRef = getAggregatorRef(aggId)
          replyTo ! AggregatorRegistered(requestId, actorRef)
        }
        this

      case trackMsg @ RequestTrainer(requestId, traId, replyTo) =>
        if (traId.getOrchestrator() != orcId) {
          context.log.info("Expected orchestrator id {}, found {}", orcId.name(), traId.toString())
        } else {
          val aggList = traId.getAggregators()
          val aggId = aggList.head
          val aggRef = getAggregatorRef(aggId)
          aggRef ! trackMsg
        }
        this
      
      case trackMsg @ RequestRealTimeGraph(requestId, entity, replyTo) =>
        val entityOrcId = entity match {
          case Left(x) => x
          case Right(x) => x.getOrchestrator()
        }

        if (entityOrcId != orcId) {
          context.log.info("Expected orchestrator id {}, found {}", orcId.name(), entityOrcId.name())
        } else {
          entity match {
            case Left(x) =>
              // Give current node's realTimeGraph
              context.log.info("Creating new realTimeGraph query actor for {}", entity)
              context.spawnAnonymous(RealTimeGraphQuery(
                creator = entity,
                aggIdToRefMap = aggIdToRef.toMap,
                traIds = None,
                requestId = requestId,
                requester = replyTo,
                timeout = 30.seconds
              ))
            case Right(x) =>
              // Will always include current aggregator at the head
              val aggList = x.getAggregators()
              val aggId = aggList.head
              val aggRef = getAggregatorRef(aggId)
              aggRef ! trackMsg
          }
        }
        this
      
      case AggregatorTerminated(actor, aggId) =>
        context.log.info("Aggregator with id {} has been terminated", aggId.toString())
        // TODO
        this
    }
 /** The onSignal method is the method that is called when the orchestrator receives a signal.
   *
   * @param signal The signal that is sent to the orchestrator.
   * @return The behavior of the orchestrator.
   */
  override def onSignal: PartialFunction[Signal,Behavior[Command]] = {
    case PostStop =>
      context.log.info("Orchestrator {} stopeed", orcId.toString())
      this
  }
}
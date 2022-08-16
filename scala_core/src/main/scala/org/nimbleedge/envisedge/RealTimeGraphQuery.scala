package org.nimbleedge.envisedge

import org.nimbleedge.envisedge.models._
import scala.concurrent.duration.FiniteDuration
import akka.actor.typed.Behavior
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.TimerScheduler
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.PostStop
import akka.actor.typed.Signal

import FLSystemManager.{ RespondRealTimeGraph }

// Map of Aggregator Identifiers is compulsory
// List of Trainer Identifiers is optional

object RealTimeGraphQuery {
	/** Query for real time graph
	 *
	 * This query is used to query the real time graph of the system with the given parameters.
	 * It is sent to the system manager actor which will forward it to the appropriate aggregator
	 * actor and will return the real time graph to the caller. The real time graph is updated when
	 * the system manager actor receives a request for the real time graph.
	 *
	 * @param creator Identifier of the system manager actor who created this query
	 * @param aggIdToRefMap Map of aggregator identifiers to actor references
	 * @param traIds List of trainer identifiers
	 * @param requestId Request ID
	 * @param requester Requester actor reference
	 * @param timeout Timeout for the query
	 * @return Real time graph
	 */
	def apply(
		creator: Either[OrchestratorIdentifier, AggregatorIdentifier],
		aggIdToRefMap: Map[AggregatorIdentifier, ActorRef[Aggregator.Command]],
		traIds: Option[List[TrainerIdentifier]],
		requestId: Long,
		requester: ActorRef[RespondRealTimeGraph],
		timeout: FiniteDuration
	) : Behavior[Command] = {
		Behaviors.setup { context =>
			Behaviors.withTimers { timers =>
				new RealTimeGraphQuery(creator, aggIdToRefMap, traIds, requestId, requester, timeout, context, timers)
			}
		}
	}

	trait Command
	/** The command needed to query the real time graph.
	 *
	 * This command is sent to the system manager actor to query the real time graph of the system.
	 */
	private case object CollectionTimeout extends Command

	final case class WrappedRespondRealTimeGraph(response: RespondRealTimeGraph) extends Command

	private final case class AggregatorTerminated(aggId: AggregatorIdentifier) extends Command
}

class RealTimeGraphQuery(
	/** Identifier of the system manager actor who created this query.
	 *
	 * This is an identifier that is used to identify the system manager actor who created this query.
	 * After the query is completed and the system manager actor has been identified, the system manager
	 * actor will send a response to the requester actor notifying it that the query has been completed.
	 *
	 * @see [[RealTimeGraphQuery.WrappedRespondRealTimeGraph]]
	 * @param creator Identifier of the system manager actor who created this query
	 * @param aggIdToRefMap Map of aggregator identifiers to actor references
	 * @param traIds List of trainer identifiers
	 * @param requestId Request ID
	 * @param requester Requester actor reference
	 * @param timeout Timeout for the query
	 * @param context Actor context of the system manager actor
	 * @param timers TimerScheduler of the system manager actor
	 */
	creator: Either[OrchestratorIdentifier, AggregatorIdentifier],
	aggIdToRefMap: Map[AggregatorIdentifier, ActorRef[Aggregator.Command]],
	traIds: Option[List[TrainerIdentifier]],
	requestId: Long,
	requester: ActorRef[RespondRealTimeGraph],
	timeout: FiniteDuration,
	context: ActorContext[RealTimeGraphQuery.Command],
	timers: TimerScheduler[RealTimeGraphQuery.Command]
) extends AbstractBehavior[RealTimeGraphQuery.Command](context) {
	import RealTimeGraphQuery._

	timers.startSingleTimer(CollectionTimeout, CollectionTimeout, timeout)

	private val respondRealTimeGraphAdapter = context.messageAdapter(WrappedRespondRealTimeGraph.apply)

	private var repliesSoFar = Map.empty[AggregatorIdentifier, TopologyTree]
	private var stillWaiting = aggIdToRefMap.keySet

  	context.log.info("RealTimeGraphQuery Actor for {} started", creator)

	if (aggIdToRefMap.isEmpty) {
		respondWhenAllCollected()
	} else {
		aggIdToRefMap.foreach {
			case (aggId, aggRef) =>
				context.watchWith(aggRef, AggregatorTerminated(aggId))
				aggRef ! FLSystemManager.RequestRealTimeGraph(0, Right(aggId), respondRealTimeGraphAdapter)
		}
	}

	override def onMessage(msg: Command): Behavior[Command] =
	/** This method is called when the system manager actor receives a command.
	 *
	 * If the command is a [[RealTimeGraphQuery.CollectionTimeout]], the system manager actor will
	 * send a response to the requester actor notifying it that the query has timed out. The system
	 * manager actor will then stop the query actor.
	 *
	 * If the command is a [[RealTimeGraphQuery.WrappedRespondRealTimeGraph]], the system manager actor
	 * will update the real time graph and send a response to the requester actor notifying it that the
	 * query has been completed. The system manager actor will then stop the query actor.
	 *
	 * @param msg Command received by the system manager actor
	 * @return Updated behavior of the system manager actor
	 */
		msg match {
			case WrappedRespondRealTimeGraph(response) => onRespondRealTimeGraph(response)
			case AggregatorTerminated(aggId) 	  => onAggregatorTerminated(aggId)
			case CollectionTimeout 				  => onCollectionTimeout()
		}

	private def onRespondRealTimeGraph(response: RespondRealTimeGraph) : Behavior[Command] = {
		/** This method is called when the system manager actor receives a response from an aggregator actor.
		 *
		 * The system manager actor will update the real time graph and send a response to the requester actor
		 * notifying it that the query has been completed. Also, the system manager actor will then stop the
		 * query actor.
		 *
		 * @param response Response received by the system manager actor
		 * @return Updated behavior of the system manager actor
		 */
		val realTimeGraph = response.realTimeGraph
		val aggId : AggregatorIdentifier = response.realTimeGraph match {

			// The Topology Tree will always be a node since
			// We are requesting realTimeGraph only from Orchestrator/Aggregators.

			case Leaf(x) => throw new NotImplementedError
			case Node(value, children) => value match {

				// OrchestratorIdentifier is never used!
				// The entity received here should not be Orchestrator Identifier
				// Since these are messages which will be received by self.

				case Left(x) => throw new NotImplementedError
				case Right(x) => x
			}
		}

		repliesSoFar += (aggId -> realTimeGraph)
		stillWaiting -= aggId
		respondWhenAllCollected()
	}

	private def onAggregatorTerminated(aggId: AggregatorIdentifier): Behavior[Command] = {
		/** This method is called when the system manager actor receives a notification that an aggregator actor has terminated.
		 *
		 * @param aggId Aggregator identifier
		 * @return Updated behavior of the system manager actor
		 */
		if (stillWaiting(aggId)) {
			// Send the empty List of children when Aggregator terminated.
			repliesSoFar += (aggId -> Node(Right(aggId), Set.empty))
			stillWaiting -= aggId
		}
		respondWhenAllCollected()
	}

	private def onCollectionTimeout(): Behavior[Command] = {
		// Send the empty List of children for Aggregators who didn't respond
		repliesSoFar ++= stillWaiting.map(aggId => aggId -> Node(Right(aggId), Set.empty))
		stillWaiting = Set.empty
		respondWhenAllCollected()
	}

	private def respondWhenAllCollected(): Behavior[Command] = {
		/** This method is called when all aggregators have responded.
		 *
		 * @return Updated behavior of the system manager actor
		 */
		if (stillWaiting.isEmpty) {
			// Construct a tree and return to requester
			val children : Set[TopologyTree] = {
				val trainers: Set[TopologyTree] = traIds match {
					case Some(list) => list.map(traId => Leaf(traId)).toSet
					case None => Set.empty
				}
				repliesSoFar.values.toSet ++ trainers
			}
			requester ! RespondRealTimeGraph(requestId, Node(creator, children))	
			Behaviors.stopped
		} else {
			this
		}
	}

	override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
		/** This method is called when the system manager actor receives a signal.
		 *
		 * If the signal is a [[RealTimeGraphQuery.CollectionTimeout]], the system manager actor will
		 * send a response to the requester actor notifying it that the query has timed out. The system
		 * manager actor will then stop the query actor.
		 *
		 * @param msg Signal received by the system manager actor
		 * @return Updated behavior of the system manager actor
		 */
		case PostStop =>
			context.log.info("RealTimeGraphQuery Actor for {} stopped", creator)
			this
	}
}
package org.nimbleedge.envisedge

import akka.actor.typed.Behavior
import akka.actor.typed.Signal
import akka.actor.typed.PostStop
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object SimulatorSupervisor {
	/** The object of the SimulatorSupervisor class
	 * which is in charge of defining the behavior
	 * of the supervisor actor.
	 * 
	 * @return the behavior
	 */
	// Update this to manager to other entities
	def apply(): Behavior[Nothing] =
		Behaviors.setup[Nothing](new SimulatorSupervisor(_))
}

/** The behavior of the supervisor actor.
  * 
  * This is the behavior of the supervisor actor itself. It is the top level behavior of the actor system
  * and is responsible for creating the simulation environment and supervising the other actors. It is helped
  * by the AbstractBehavior class which provides a lot of useful functionality around the supervision of
  * actors.
  *
  * @param context the actor context
  * @return the behavior
  */
class SimulatorSupervisor(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
	context.log.info("Simulator Supervisor started")

   /** The onMessage method is called when a message is received by the actor.
    * 
    * @param msg the message
    * @return the behavior
    */
	override def onMessage(msg: Nothing): Behavior[Nothing] = {
		Behaviors.unhandled
	}
	/** The onSignal method is called when a signal is received by the actor.
	  * 
	  * @param signal the signal
	  * @return the behavior
	  */
	override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
		case PostStop =>
			context.log.info("Simulator Supervisor stopped")
			this
	}
}
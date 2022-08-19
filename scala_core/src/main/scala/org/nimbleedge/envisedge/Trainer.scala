package org.nimbleedge.envisedge

import models._

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.Signal
import akka.actor.typed.PostStop

object Trainer {
    /**
     * The behavior of the Trainer actor.
     */
    def apply(traId: TrainerIdentifier): Behavior[Command] =
    // Behaviors.setup is a factory for creating behaviors.
        Behaviors.setup(new Trainer(_, traId))
    
    trait Command

    // TODO
    // Add messages here
}

class Trainer(context: ActorContext[Trainer.Command], traId: TrainerIdentifier) extends AbstractBehavior[Trainer.Command](context) {
    /** The behavior of the Trainer actor.
      *
      * The Trainer actor is in charge of training the model with the given data,
      * and then sending the trained model to the Supervisor.
      *
      * @param context The actor context.
      * @param traId The identifier of the Trainer actor.
      * @return The behavior of the Trainer actor.
      */
    import Trainer._

    // TODO
    // Add state and persistent information

    context.log.info("Trainer {} started", traId.toString())

    override def onMessage(msg: Command): Behavior[Command] =
    /** The onMessage method is responsible for handling the messages that are sent to the Trainer actor.
      * @param msg The message that is received.
      *
      */
        msg match {
            // TODO
            case _ =>
                this
        }
    
    override def onSignal: PartialFunction[Signal,Behavior[Command]] = {
        /** The onSignal method is responsible for handling the signals that are sent to the Trainer actor.
          * @param signal The signal that is received.
          * @return The behavior of the Trainer actor.
          */
        case PostStop =>
            context.log.info("Trainer {} stopped", traId.toString())
            this
    }
}
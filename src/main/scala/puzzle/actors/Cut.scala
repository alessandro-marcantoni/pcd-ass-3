package puzzle.actors

import akka.actor.typed.ActorRef
import puzzle.actors.Events.{Event, SnapshotRequest}

import scala.collection.mutable

object Cut {
  /**
   * Color representing the state of the Player during the cut of the system.
   */
  sealed trait Color
  case object Red extends Color
  case object White extends Color

  /**
   * Player state at the moment of snapshot request for the cut system
   * @param color The color representing the state
   * @param players The players on the cut
   */
  case class PlayerState(var color: Color = White, players: Set[ActorRef[Event]]) {
    var messages: mutable.Queue[Event] = mutable.Queue()
    var closed: mutable.Map[ActorRef[Event], Boolean] = mutable.Map()
    // Initialize data structures for the cut
    players foreach (p => closed += (p -> false))

    def turnRed(from: ActorRef[Event]): Unit = {
      color = Red
      players foreach (_ ! SnapshotRequest(from))
    }

    def closeChannel(channel: ActorRef[Event]): Unit =
      closed += (channel -> true)

    def allClosed: Boolean = closed forall (p => p._2)

    def enqueue(event: Event): Unit = messages.enqueue(event)

    def notClosed(a: ActorRef[Event]): Boolean = !closed(a)
  }
}

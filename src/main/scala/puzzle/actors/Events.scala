package puzzle.actors

import akka.actor.typed.ActorRef
import puzzle.actors.Events._

object Events {
  sealed trait Event
  sealed trait GameEvent extends Event {
    def from: ActorRef[Event]
  }
  sealed trait RemoteGameEvent extends GameEvent
  sealed trait LocalGameEvent extends GameEvent

  case class ActorsUpdated(actors: Set[ActorRef[Event]]) extends Event with CborSerializable
  case class Joined(player: ActorRef[Event]) extends Event with CborSerializable

  case class GameStateRequest(replyTo: ActorRef[Event]) extends Event with CborSerializable
  case class GameState(tiles: List[SerializableTile]) extends Event with CborSerializable

  case class LocalTileSelected(tile: SerializableTile, override val from: ActorRef[Event]) extends LocalGameEvent with CborSerializable
  case class RemoteTileSelected(tile: SerializableTile, override val from: ActorRef[Event]) extends RemoteGameEvent with CborSerializable

  case class LocalPuzzleCompleted(override val from: ActorRef[Event]) extends LocalGameEvent with CborSerializable
  case class RemotePuzzleCompleted(override val from: ActorRef[Event]) extends RemoteGameEvent with CborSerializable

  case class SnapshotRequest(from: ActorRef[Event]) extends Event with CborSerializable
  case class CutCompleted(replyTo: Option[ActorRef[Event]]) extends Event with CborSerializable
}

object EventConversions {
  trait EventConverter[A] {
    def toRemoteEvent(event: A): RemoteGameEvent
  }

  implicit val tileSelectedConverter: EventConverter[LocalTileSelected] = event => RemoteTileSelected(event.tile, event.from)
  implicit val puzzleCompletedConverter: EventConverter[LocalPuzzleCompleted] = event => RemotePuzzleCompleted(event.from)
  implicit val genericConverter: EventConverter[LocalGameEvent] = {
    case e: LocalTileSelected => toRemoteEvent(e)
    case e: LocalPuzzleCompleted => toRemoteEvent(e)
  }

  def toRemoteEvent[A](event: A)(implicit converter: EventConverter[A]): RemoteGameEvent =
    converter.toRemoteEvent(event)

}

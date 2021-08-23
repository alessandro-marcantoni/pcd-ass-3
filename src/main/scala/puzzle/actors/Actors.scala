package puzzle.actors

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.typed.Cluster
import puzzle.actors.Cut.PlayerState
import puzzle.actors.EventConversions._
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

object Actors {

  object RootBehavior {
    /**
     * If the role is "leader" (the first player), we create a [[GameCreator]].
     * If the role is "player" (not the first), we create a [[Joiner]].
     */
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>
      val cluster = Cluster(ctx.system)
      if (cluster.selfMember.hasRole("leader")) {
        ctx.spawn(GameCreator(), "player")
      }
      if (cluster.selfMember.hasRole("player")) {
        ctx.spawn(Joiner(), "joiner")
      }
      Behaviors.empty
    }
  }

  object Joiner {
    val JoinerServiceKey: ServiceKey[Event] = ServiceKey[Event]("Joiner")

    /**
     * The [[Joiner]] registers himself to the [[Receptionist]], which makes him visible from the other actors.
     * He is waiting for an [[Acceptor]] to accept him;
     * when he receives a [[Joined]] message, he requests the [[GameState]] to a [[Player]].
     * When he receives the response from that [[Player]], he de-registers himself from the [[Receptionist]],
     * he spawns a new [[Player]] and he becomes an [[Acceptor]] himself.
     */
    def apply(): Behavior[Event] = Behaviors.setup { ctx =>
      ctx.system.receptionist ! Receptionist.Register(JoinerServiceKey, ctx.self)
      Behaviors.receiveMessage {
        case Joined(player) =>
          ctx.log.info(s"JOINED ${ctx.self}")
          player ! GameStateRequest(ctx.self)
          Behaviors.same
        case GameState(tiles) =>
          ctx.system.receptionist ! Receptionist.Deregister(JoinerServiceKey, ctx.self)
          //val newPlayer: ActorRef[Event] = ctx.spawn(Player(tiles, PuzzleBoard(ctx)), "player")
          ctx.spawn(Acceptor(ctx.self), "acceptor")
          Player(tiles, PuzzleBoard(ctx))
        case _ => Behaviors.same
      }
    }
  }

  object Acceptor {
    /**
     * The [[Acceptor]] subscribes to the [[Receptionist]],
     * which will notify him if some [[Joiner]] registers of de-registers.
     * When a [[Joiner]] (or more) registers himself, the [[Acceptor]] sends him a message
     * with the [[ActorRef]] of a [[Player]] to contact.
     *
     * @param player The [[ActorRef]] of the [[Player]] that a [[Joiner]] should contact.
     */
    def apply(player: ActorRef[Event]): Behavior[Event] = Behaviors.setup { ctx =>
      val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing] {
        case Joiner.JoinerServiceKey.Listing(joiners) => ActorsUpdated(joiners)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(Joiner.JoinerServiceKey, subscriptionAdapter)
      running(player, ctx)
    }

    def running(player: ActorRef[Event], ctx: ActorContext[Event]): Behavior[Event] = Behaviors.receiveMessage {
      case ActorsUpdated(joiners) =>
        ctx.log.info(s"JOINERS: ${joiners.toString()}")
        joiners foreach (_ ! Joined(player))
        running(player, ctx)
      case _ => running(player, ctx)
    }
  }

  object Player {
    val PlayerServiceKey: ServiceKey[Event] = ServiceKey[Event]("Player")

    /**
     * The [[Player]] is always aware of the other [[Player]]s thanks to the [[Receptionist]].
     * It starts the game for the current player.
     *
     * @param tiles The current state of the game.
     * @param puzzle The instance of the game.
     */
    def apply(tiles: List[SerializableTile], puzzle: PuzzleBoard): Behavior[Event] = Behaviors.setup { ctx =>
      ctx.system.receptionist ! Receptionist.Register(PlayerServiceKey, ctx.self)
      val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing] {
        case Player.PlayerServiceKey.Listing(players) => ActorsUpdated(players)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(Player.PlayerServiceKey, subscriptionAdapter)
      puzzle.createTiles(Some(tiles))
      puzzle.paintPuzzle()
      puzzle.setVisible(true)
      running(ctx, puzzle, Set())
    }

    def running(ctx: ActorContext[Event], puzzle: PuzzleBoard, players: Set[ActorRef[Event]]): Behavior[Event] = Behaviors.receiveMessage {
      case ActorsUpdated(p) =>
        ctx.log.info(s"PLAYERS: ${p.toString()}")
        running(ctx, puzzle, p)
      case GameStateRequest(replyTo) =>
        if (players.size > 1) {
          val state = PlayerState(players = players diff Set(ctx.self))
          state.turnRed(ctx.self)
          onCut(ctx, puzzle, players, state, Some(replyTo))
        } else {
          replyTo ! GameState(puzzle.state())
          running(ctx, puzzle, players)
        }
      case SnapshotRequest(from) =>
        val state = PlayerState(players = players diff Set(ctx.self))
        state.turnRed(ctx.self)
        state.closeChannel(from)
        onCut(ctx, puzzle, players, state, Option.empty)
      case event: LocalGameEvent =>
        (players diff Set(ctx.self)) foreach (_ ! toRemoteEvent(event))
        running(ctx, puzzle, players)
      case event: RemoteGameEvent =>
        puzzle.selectionManager ! event
        running(ctx, puzzle, players)
      case _ => running(ctx, puzzle, players)
    }

    def onCut(ctx: ActorContext[Event], puzzle: PuzzleBoard, players: Set[ActorRef[Event]], state: PlayerState, replyTo: Option[ActorRef[Event]]): Behavior[Event] = Behaviors.receiveMessage {
      case ActorsUpdated(p) =>
        state.enqueue(ActorsUpdated(p))
        onCut(ctx, puzzle, players, state, replyTo)
      case GameStateRequest(from) =>
        if (state.notClosed(from)) state.enqueue(GameStateRequest(from))
        onCut(ctx, puzzle, players, state, replyTo)
      case SnapshotRequest(from) =>
        state.closeChannel(from)
        if (state.allClosed) ctx.self ! CutCompleted(replyTo)
        onCut(ctx, puzzle, players, state, replyTo)
      case event: GameEvent =>
        if (state.notClosed(event.from)) state.enqueue(event)
        onCut(ctx, puzzle, players, state, replyTo)
      case CutCompleted(replyTo) =>
        if (replyTo.nonEmpty) replyTo.get ! GameState(puzzle.state())
        running(ctx, puzzle, players)
      case _ => onCut(ctx, puzzle, players, state, replyTo)
    }
  }

  object GameCreator {
    /**
     * The [[GameCreator]] creates a new puzzle game, then it spawns the first [[Player]] and an [[Acceptor]].
     */
    def apply(): Behavior[Event] = Behaviors.setup { ctx =>
      val puzzle = PuzzleBoard(DistributedPuzzle.n, DistributedPuzzle.m, DistributedPuzzle.imagePath, ctx = ctx)
      puzzle.createTiles()
      //val player: ActorRef[Event] = ctx.spawn(Player(puzzle.state(), puzzle), "player")
      ctx.spawn(Acceptor(ctx.self), "acceptor")
      Player(puzzle.state(), puzzle)
    }
  }

}

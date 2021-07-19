package puzzle.actors

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.typed.Cluster
import puzzle.actors.Events._

object Events {
  sealed trait Event
  case class ActorsUpdated(actors: Set[ActorRef[Event]]) extends Event with CborSerializable
  case class Joined() extends Event with CborSerializable
  case class GameState(tiles: List[SerializableTile]) extends Event with CborSerializable
  case class LocalTileSelected(tile: SerializableTile) extends Event with CborSerializable
  case class RemoteTileSelected(tile: SerializableTile) extends Event with CborSerializable
}

import Actors._
/**
 * By far the system:
 * - spawns the actors by role:
 *    the first player is the leader (sbt "runMain puzzle.actors.DistributedPuzzle 25251")
 *    and the other players are joiners (sbt "runMain puzzle.actors.DistributedPuzzle").
 * - when the first player is spawned, it is an [[Acceptor]] waiting for [[Joiner]]s.
 * - when a [[Joiner]] is joined, it becomes an [[Acceptor]] himself waiting for [[Joiner]]s.
 * - the second phase involves starting the game by the [[Player]]:
 *   - the first player creates the game;
 *   - the others retrieve the game state.
 */
object Actors {

  object RootBehavior {
    /**
     * If the role is "leader" (the first player), we create an [[Acceptor]].
     * If the role is "player" (not the first), we create a [[Joiner]].
     */
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>
      val cluster = Cluster(ctx.system)
      if (cluster.selfMember.hasRole("leader")) {
        ctx.spawn(Acceptor(), "acceptor")
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
     * when he receives a [[Joined]] message, he de-registers himself and becomes an [[Acceptor]].
     */
    def apply(): Behavior[Event] = Behaviors.setup { ctx =>
      ctx.system.receptionist ! Receptionist.Register(JoinerServiceKey, ctx.self)
      Behaviors.receiveMessage {
        case Joined() =>
          ctx.log.info(s"JOINED ${ctx.self}")
          ctx.system.receptionist ! Receptionist.Deregister(JoinerServiceKey, ctx.self)
          ctx.spawn(Player(PuzzleBoard(ctx)), "player")
          Acceptor()
        case _ => Behaviors.same
      }
    }
  }

  object Acceptor {
    /**
     * The [[Acceptor]] subscribes to the [[Receptionist]],
     * which will notify him if some [[Joiner]] registers of de-registers.
     * When a [[Joiner]] (or more) registers himself, the [[Acceptor]] sends him a message to join him.
     */
    def apply(): Behavior[Event] = Behaviors.setup { ctx =>
      val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing] {
        case Joiner.JoinerServiceKey.Listing(joiners) => ActorsUpdated(joiners)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(Joiner.JoinerServiceKey, subscriptionAdapter)
      running(ctx)
    }

    def running(ctx: ActorContext[Event]): Behavior[Event] = Behaviors.receiveMessage {
      case ActorsUpdated(joiners) =>
        ctx.log.info(s"JOINERS: ${joiners.toString()}")
        joiners foreach (_ ! Joined())
        running(ctx)
      case _ => running(ctx)
    }
  }

  object Player {
    val PlayerServiceKey: ServiceKey[Event] = ServiceKey[Event]("Player")

    /**
     * The [[Player]] is always aware of the other [[Player]]s thanks to the [[Receptionist]].
     * It starts the game for the current player:
     * - if this is the first player, it should create the game from scratch;
     * - if not, it should create the game retrieving the current state (not implemented yet).
     */
    def apply(puzzle: PuzzleBoard): Behavior[Event] = Behaviors.setup { ctx =>
      ctx.system.receptionist ! Receptionist.Register(PlayerServiceKey, ctx.self)
      val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing] {
        case Player.PlayerServiceKey.Listing(players) => ActorsUpdated(players)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(Player.PlayerServiceKey, subscriptionAdapter)
      running(ctx, puzzle, Set())
    }

    def running(ctx: ActorContext[Event], puzzle: PuzzleBoard, players: Set[ActorRef[Event]]): Behavior[Event] = Behaviors.receiveMessage {
      case ActorsUpdated(p) =>
        ctx.log.info(s"PLAYERS: ${p.toString()}")
        if (puzzle.state().nonEmpty) {
          p diff players foreach (_ ! GameState(puzzle.state()))
        }
        running(ctx, puzzle, p)
      case GameState(tiles) =>
        tiles match {
          case t: List[SerializableTile] if !(t == puzzle.state()) =>
            puzzle.createTiles(Some(tiles))
            puzzle.paintPuzzle()
            puzzle.setVisible(true)
          case _ =>
        }
        running(ctx, puzzle, players)
      case _ => running(ctx, puzzle, players)
    }
  }

  object GameCreator {
    def apply(): Behavior[Event] = Behaviors.setup { ctx =>
      val puzzle = PuzzleBoard(DistributedPuzzle.n, DistributedPuzzle.m, DistributedPuzzle.imagePath, ctx = ctx)
      puzzle.createTiles(None)
      puzzle.paintPuzzle()
      puzzle.setVisible(true)
      ctx.spawn(Player(puzzle), "player")
      Behaviors.empty
    }
  }

}

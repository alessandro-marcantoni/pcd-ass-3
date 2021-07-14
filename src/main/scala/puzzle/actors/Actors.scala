package puzzle.actors

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.typed.Cluster
import puzzle.actors.Events._

object Events {
  sealed trait Event
  case class JoinersUpdated(joiners: Set[ActorRef[Event]]) extends Event with CborSerializable
  case class PlayersUpdated(players: Set[ActorRef[Event]]) extends Event with CborSerializable
  case class Joined(tiles: List[SerializableTile]) extends Event with CborSerializable
}

import Actors._
/**
 * By far the system:
 * - spawns the actors by role:
 *    the first player is the leader (sbt "runMain puzzle.actors.DistributedPuzzle 25251")
 *    and the other players are joiners (sbt "runMain puzzle.actors.DistributedPuzzle").
 * - when the first player is spawned, it is an [[Acceptor]] waiting for [[Joiner]]s.
 * - when a [[Joiner]] is joined, it becomes an [[Acceptor]] himself waiting for [[Joiner]]s.
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
        ctx.spawn(Player(), "player")
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
        case Joined(_) =>
          ctx.log.info(s"JOINED ${ctx.self}")
          ctx.system.receptionist ! Receptionist.Deregister(JoinerServiceKey, ctx.self)
          ctx.spawn(Player(), "player")
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
        case Joiner.JoinerServiceKey.Listing(joiners) => JoinersUpdated(joiners)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(Joiner.JoinerServiceKey, subscriptionAdapter)
      running(ctx)
    }

    def running(ctx: ActorContext[Event]): Behavior[Event] = Behaviors.receiveMessage {
      case JoinersUpdated(joiners) =>
        ctx.log.info(s"JOINERS: ${joiners.toString()}")
        joiners foreach (_ ! Joined(List()))
        running(ctx)
      case _ => Behaviors.same
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
    def apply(): Behavior[Event] = Behaviors.setup { ctx =>
      ctx.system.receptionist ! Receptionist.Register(PlayerServiceKey, ctx.self)
      val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing] {
        case Player.PlayerServiceKey.Listing(players) => PlayersUpdated(players)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(Player.PlayerServiceKey, subscriptionAdapter)
      val puzzle = PuzzleBoard(DistributedPuzzle.n, DistributedPuzzle.m, DistributedPuzzle.imagePath)
      puzzle.setVisible(true)
      Behaviors.empty
    }
  }

}

package puzzle.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import puzzle.actors.Actors.Player
import puzzle.actors.Events._

object SelectionManager {
  private var selectionActive: Boolean = false
  private var selectedTile: Option[List[SerializableTile]] = None

  def apply(puzzle: PuzzleBoard): Behavior[Event] = Behaviors.setup { ctx =>
    val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing] {
      case Player.PlayerServiceKey.Listing(players) => PlayersUpdated(players)
    }
    ctx.system.receptionist ! Receptionist.Subscribe(Player.PlayerServiceKey, subscriptionAdapter)
    running(ctx, puzzle, Set())
  }

  def running(ctx: ActorContext[Event], puzzle: PuzzleBoard, players: Set[ActorRef[Event]]): Behavior[Event] = Behaviors.receiveMessage {
    case PlayersUpdated(players) => running(ctx, puzzle, players)
    case LocalTileSelected(tile, listener) =>
      selectTile(tile, listener, puzzle)
      //players foreach (_ ! RemoteTileSelected(tile))
      running(ctx, puzzle, players)
    //case RemoteTileSelected(tile) => ???
    case _ => running(ctx, puzzle, players)
  }

  def selectTile(tile: SerializableTile, listener: Listener, puzzle: PuzzleBoard): Unit =
    if (selectionActive) {
      selectionActive = false
      swap(selectedTile.get.filter(t => t.player.equals(tile.player)).head, tile, puzzle)
      selectedTile = selectedTile match {
        case Some(list) => Some(list diff List(selectedTile.get.filter(t => t.player.equals(tile.player)).head))
        case _ => selectedTile
      }
      listener.onSwapPerformed()
    } else {
      selectionActive = true
      selectedTile = Some(selectedTile.getOrElse(List()) appended tile)
    }

  private def swap(st1: SerializableTile, st2: SerializableTile, puzzle: PuzzleBoard): Unit = {
    val t1 = puzzle.tiles.filter(t => t.currentPosition.equals(st1.currentPosition)).head
    val t2 = puzzle.tiles.filter(t => t.currentPosition.equals(st2.currentPosition)).head
    val pos: Int = t1.currentPosition
    t1.currentPosition = t2.currentPosition
    t2.currentPosition = pos
  }
}

trait Listener {
  def onSwapPerformed(): Unit
}

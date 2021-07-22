package puzzle.actors

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import puzzle.actors.Events._

import javax.swing.JOptionPane

object SelectionManager {
  var selectedTile: List[SerializableTile] = List()
  val SelectionManagerServiceKey: ServiceKey[Event] = ServiceKey[Event]("SelectionManager")

  def apply(puzzle: PuzzleBoard): Behavior[Event] = Behaviors.setup { ctx =>
    ctx.system.receptionist ! Receptionist.Register(SelectionManagerServiceKey, ctx.self)
    val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing] {
      case SelectionManager.SelectionManagerServiceKey.Listing(players) => ActorsUpdated(players)
    }
    ctx.system.receptionist ! Receptionist.Subscribe(SelectionManager.SelectionManagerServiceKey, subscriptionAdapter)
    running(ctx, puzzle, Set())
  }

  def running(ctx: ActorContext[Event], puzzle: PuzzleBoard, managers: Set[ActorRef[Event]]): Behavior[Event] = Behaviors.receiveMessage {
    case ActorsUpdated(players) =>
      running(ctx, puzzle, players)
    case LocalTileSelected(tile) =>
      selectTile(tile, puzzle)
      (managers diff Set(ctx.self)) foreach (_ ! RemoteTileSelected(tile))
      running(ctx, puzzle, managers)
    case RemoteTileSelected(tile) =>
      selectTile(tile, puzzle)
      running(ctx, puzzle, managers)
    case LocalPuzzleCompleted() =>
      (managers diff Set(ctx.self)) foreach (_ ! RemotePuzzleCompleted())
      JOptionPane.showMessageDialog(puzzle, "Puzzle Completed!", "", JOptionPane.INFORMATION_MESSAGE)
      Behaviors.empty
    case RemotePuzzleCompleted() =>
      JOptionPane.showMessageDialog(puzzle, "Puzzle Completed!", "", JOptionPane.INFORMATION_MESSAGE)
      Behaviors.empty
    case _ => running(ctx, puzzle, managers)
  }

  private def selectTile(tile: SerializableTile, puzzle: PuzzleBoard): Unit = if (!selectedTile.exists(t => t.currentPosition.equals(tile.currentPosition))) {
    tile match {
      case t: SerializableTile if selectedTile.exists(el => el.player.equals(tile.player)) =>
        swap(selectedTile.filter(t => t.player.equals(tile.player)).head, t, puzzle)
        selectedTile = selectedTile match {
          case list: List[SerializableTile] => list diff List(selectedTile.filter(e => e.player.equals(t.player)).head)
          case _ => selectedTile
        }
      case _ => selectedTile = selectedTile appended tile
    }
    puzzle.paintPuzzle()
    puzzle.checkSolution()
  }

  def setupTiles(tiles: List[SerializableTile]): Unit = selectedTile = tiles.filter(t => t.selected)

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

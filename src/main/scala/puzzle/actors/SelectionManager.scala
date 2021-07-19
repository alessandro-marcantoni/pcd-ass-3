package puzzle.actors

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import puzzle.actors.Events._

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
    case ActorsUpdated(players) => running(ctx, puzzle, players)
    case LocalTileSelected(tile, listener) =>
      selectTile(tile, listener, puzzle)
      (managers diff Set(ctx.self)) foreach (_ ! RemoteTileSelected(tile, listener))
      running(ctx, puzzle, managers)
    case RemoteTileSelected(tile, listener) =>
      //ctx.log.info(s"REMOTE TILE SELECTED: ${tile.toString}")
      selectTile(tile, listener, puzzle)
      running(ctx, puzzle, managers)
    case _ => running(ctx, puzzle, managers)
  }

  private def selectTile(tile: SerializableTile, listener: Listener, puzzle: PuzzleBoard): Unit =
    tile match {
      case t: SerializableTile if selectedTile.exists(el => el.player.equals(tile.player)) =>
        swap(selectedTile.filter(t => t.player.equals(tile.player)).head, t, puzzle)
        selectedTile = selectedTile match {
          case list: List[SerializableTile] => list diff List(selectedTile.filter(e => e.player.equals(t.player)).head)
          case _ => selectedTile
        }
        listener.onSwapPerformed()
      case _ => selectedTile = selectedTile appended tile
    }

  def setupTiles(tiles: List[SerializableTile]): Unit = selectedTile = tiles.filter(t => t.selected)

    /*if (selectionActive) {
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
  }*/


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

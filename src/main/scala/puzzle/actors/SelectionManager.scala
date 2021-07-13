package puzzle

class SelectionManager(
    var selectionActive: Boolean = false,
    var selectedTile: Tile = null
) {

  def selectTile(tile: Tile, listener: Listener): Unit = {
    if (selectionActive) {
      selectionActive = false
      swap(selectedTile, tile)
      listener.onSwapPerformed()
    } else {
      selectionActive = true
      selectedTile = tile
    }
  }

  private def swap(t1: Tile, t2: Tile): Unit = {
    val pos: Int = t1.currentPosition
    t1.currentPosition = t2.currentPosition
    t2.currentPosition = pos
  }
}

trait Listener {
  def onSwapPerformed(): Unit
}

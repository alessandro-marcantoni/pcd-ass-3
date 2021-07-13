package puzzle.actors

import java.awt.event.{MouseAdapter, MouseEvent}
import java.awt.{Color, Image}
import javax.swing.{BorderFactory, ImageIcon, JButton}

case class Tile(image: Image, originalPosition: Int, var currentPosition: Int) extends Comparable[Tile] {

  override def compareTo(o: Tile): Int = o match {
    case o: Tile if currentPosition < o.currentPosition => -1
    case o: Tile if currentPosition == o.currentPosition => 0
    case _: Tile => 1
  }

  def isInRightPlace: Boolean = currentPosition == originalPosition
}

case class TileButton(tile: Tile) extends JButton(new ImageIcon(tile.image)) {
  addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent): Unit =
      setBorder(BorderFactory.createLineBorder(Color.red))
  })
}

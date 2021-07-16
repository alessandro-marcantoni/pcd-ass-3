package puzzle.actors

import akka.actor.typed.ActorRef
import puzzle.actors.Events.Event

import java.awt.event.{MouseAdapter, MouseEvent}
import java.awt.{Color, Image}
import javax.swing.{BorderFactory, ImageIcon, JButton}
import scala.language.implicitConversions

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

/**
 * Version of a tile to be sent by Akka message.
 *
 * @param originalPosition original position of the [[Tile]]
 * @param currentPosition current position of the [[Tile]]
 * @param selected whether the [[Tile]] has been selected by some player
 * @param player the [[ActorRef]] of the player who selected the [[Tile]]
 */
case class SerializableTile(originalPosition: Int, currentPosition: Int, selected: Boolean, player: Option[ActorRef[Event]]) extends Comparable[SerializableTile] {
  override def compareTo(o: SerializableTile): Int = o match {
    case o: SerializableTile if currentPosition < o.currentPosition => -1
    case o: SerializableTile if currentPosition == o.currentPosition => 0
    case _: SerializableTile => 1
  }
}

object SerializableTile {
  implicit def tileToSerializableTile(t: Tile): SerializableTile =
    SerializableTile(t.originalPosition, t.currentPosition, selected = false, None)
}

// The idea is to send the game state as List[SerializableTile]

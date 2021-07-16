package puzzle.actors

import java.awt.image.{BufferedImage, CropImageFilter, FilteredImageSource}
import java.awt.{BorderLayout, Color, GridLayout, Image}
import java.io.File
import javax.imageio.ImageIO
import javax.swing._
import scala.util.Random

case class PuzzleBoard(rows: Int,
                       columns: Int,
                       imagePath: String,
                       var tiles: List[Tile] = List(),
                       selectionManager: SelectionManager = new SelectionManager(),
                       private var panel: JPanel = null) extends JFrame {
  setup()

  private def setup(): Unit = {
    setTitle("Distributed puzzle")
    setResizable(false)
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

    val board: JPanel = new JPanel()
    board.setBorder(BorderFactory.createLineBorder(Color.gray))
    board.setLayout(new GridLayout(rows, columns, 0, 0))
    getContentPane.add(board, BorderLayout.CENTER)
    panel = board
  }

  def createTiles(t: Option[List[SerializableTile]]): Unit = {
    val image: BufferedImage = ImageIO.read(new File(imagePath))
    val imageWidth: Int = image.getWidth(null)
    val imageHeight: Int = image.getHeight(null)
    var position: Int = 0

    val positions: List[Int] = t match {
      case Some(tls) =>
        tls.sorted.map(tl => tl.currentPosition)
      case None =>
        Random.shuffle(LazyList.iterate(0)(_ + 1).take(rows * columns).toList)
    }
    tiles = List()
    (0 until rows) foreach { i =>
      (0 until columns) foreach { j =>
        val imagePortion: Image = createImage(new FilteredImageSource(image.getSource,
          new CropImageFilter(j * imageWidth / columns, i * imageHeight / rows, imageWidth / columns, imageHeight / rows)))
        tiles = tiles.appended(Tile(imagePortion, position, positions(position)))
        position = position + 1
      }
    }
  }

  def paintPuzzle(): Unit = {
    panel.removeAll()
    tiles = tiles.sorted
    tiles foreach { tile => {
      val btn: TileButton = TileButton(tile)
      panel.add(btn)
      btn.setBorder(BorderFactory.createLineBorder(Color.gray))
      btn.addActionListener(_ =>
        selectionManager.selectTile(tile, () => {
          paintPuzzle()
          checkSolution()
        }))
    }
    }
    pack()
    setLocationRelativeTo(null)
  }

  private def checkSolution(): Unit = if (tiles.forall(tile => tile.isInRightPlace))
    JOptionPane.showMessageDialog(this, "Puzzle Completed!", "", JOptionPane.INFORMATION_MESSAGE)

}

object PuzzleBoard {
  def apply(): PuzzleBoard = new PuzzleBoard(DistributedPuzzle.n, DistributedPuzzle.m, DistributedPuzzle.imagePath)
  def apply(tiles: List[Tile]): PuzzleBoard = new PuzzleBoard(DistributedPuzzle.n, DistributedPuzzle.m, DistributedPuzzle.imagePath, tiles)
}

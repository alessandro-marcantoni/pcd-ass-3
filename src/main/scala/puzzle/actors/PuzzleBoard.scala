package puzzle

import java.awt.image.{BufferedImage, CropImageFilter, FilteredImageSource}
import java.awt.{BorderLayout, Color, GridLayout, Image}
import java.io.File
import javax.imageio.ImageIO
import javax.swing.{BorderFactory, JFrame, JOptionPane, JPanel, WindowConstants}
import scala.util.Random

case class PuzzleBoard(
    rows: Int,
    columns: Int,
    imagePath: String,
    var tiles: List[Tile] = List(),
    selectionManager: SelectionManager = new SelectionManager()
) extends JFrame {
  setup()

  private def setup(): Unit = {
    setTitle("Distributed puzzle")
    setResizable(false)
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

    val board: JPanel = new JPanel()
    board.setBorder(BorderFactory.createLineBorder(Color.gray))
    board.setLayout(new GridLayout(rows, columns, 0, 0))
    getContentPane.add(board, BorderLayout.CENTER)

    createTiles(imagePath)
    paintPuzzle(board)
  }

  private def createTiles(imagePath: String): Unit = {
    val image: BufferedImage = ImageIO.read(new File(imagePath))
    val imageWidth: Int = image.getWidth(null)
    val imageHeight: Int = image.getHeight(null)
    var position: Int = 0

    var randomPositions = LazyList.iterate(0)(_ + 1).take(rows * columns).toList
    randomPositions = Random.shuffle(randomPositions)

    (0 until rows) foreach { i =>
      (0 until columns) foreach { j =>
        val imagePortion: Image = createImage(
          new FilteredImageSource(
            image.getSource,
            new CropImageFilter(
              j * imageWidth / columns,
              i * imageHeight / rows,
              imageWidth / columns,
              imageHeight / rows
            )
          )
        )
        tiles = tiles.appended(
          Tile(imagePortion, position, randomPositions(position))
        )
        position = position + 1
      }
    }
  }

  private def paintPuzzle(board: JPanel): Unit = {
    board.removeAll()
    tiles = tiles.sorted
    tiles foreach { tile =>
      {
        val btn: TileButton = TileButton(tile)
        board.add(btn)
        btn.setBorder(BorderFactory.createLineBorder(Color.gray))
        btn.addActionListener(_ =>
          selectionManager.selectTile(
            tile,
            () => {
              paintPuzzle(board)
              checkSolution()
            }
          )
        )
      }
    }
    pack()
    setLocationRelativeTo(null)
  }

  private def checkSolution(): Unit = {
    if (tiles.forall(tile => tile.isInRightPlace))
      JOptionPane.showMessageDialog(
        this,
        "Puzzle Completed!",
        "",
        JOptionPane.INFORMATION_MESSAGE
      )
  }

}

package puzzle

object DistributedPuzzle extends App {
  val n: Int = 3
  val m: Int = 5
  val imagePath: String = "res/bletchley-park-mansion.jpg"

  val puzzle: PuzzleBoard = PuzzleBoard(n, m, imagePath)
  puzzle.setVisible(true)
}

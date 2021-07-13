package puzzle

import akka.actor.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory

object DistributedPuzzle {
  val n: Int = 3
  val m: Int = 5
  val imagePath: String = "res/bletchley-park-mansion.jpg"

  def main(args: Array[String]): Unit = {
    val puzzle: PuzzleBoard = PuzzleBoard(n, m, imagePath)
    initializeCluster()
    puzzle.setVisible(true)
  }

  def initializeCluster(): Unit = {
    val config = ConfigFactory.load("puzzle")
    ActorSystem[Nothing](RootBehavior(), "Player", config)
  }

  object RootBehavior {
    def apply(): Behavior[Nothing] =
      Behaviors.setup[Nothing] { context =>
        context.spawn(behavior, "Child")
        Behaviors.empty
      }
  }

}

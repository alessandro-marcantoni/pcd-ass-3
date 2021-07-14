package puzzle.actors

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import puzzle.actors.Actors.RootBehavior

object DistributedPuzzle {
  val n: Int = 3
  val m: Int = 5
  val imagePath: String = "res/bletchley-park-mansion.jpg"

  def main(args: Array[String]): Unit = {
    startup(
      if (args.isEmpty) (0, "player")
      else (args.toSeq.map(_.toInt).head, "leader")
    )
  }

  def startup(parameters: (Int, String)): Unit = {
    val config = ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.port=${parameters._1}
      akka.cluster.roles = [${parameters._2}]
      """).withFallback(ConfigFactory.load("puzzle"))
    ActorSystem[Nothing](RootBehavior(), "ClusterSystem", config)
  }

}

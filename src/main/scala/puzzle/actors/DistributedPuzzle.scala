package puzzle.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.ClusterEvent._
import akka.cluster.typed.{Cluster, Subscribe}
import com.typesafe.config.ConfigFactory
import puzzle.PuzzleBoard

object DistributedPuzzle {
  val n: Int = 3
  val m: Int = 5
  val imagePath: String = "res/bletchley-park-mansion.jpg"

  def main(args: Array[String]): Unit = {
    val puzzle: PuzzleBoard = PuzzleBoard(n, m, imagePath)
    initializeCluster(
      if (args.isEmpty) (0, "player")
      else (args.toSeq.map(_.toInt).head, "leader")
    )
    puzzle.setVisible(true)
  }

  def initializeCluster(parameters: (Int, String)): Unit = {
    val config = ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.port=${parameters._1}
      akka.cluster.roles = [${parameters._2}]
      """).withFallback(ConfigFactory.load("puzzle"))
    ActorSystem[Nothing](RootBehavior(), "ClusterSystem", config)
  }

  object RootBehavior {
    def apply(): Behavior[Nothing] =
      Behaviors.setup[Nothing] { context =>
        context.spawn(EventListener(), "EventListener")
        Behaviors.empty
      }
  }

  object EventListener {
    sealed trait Event
    private final case class ReachabilityChange(
        reachabilityEvent: ReachabilityEvent
    ) extends Event
    private final case class MemberChange(event: MemberEvent) extends Event

    def apply(): Behavior[Event] =
      Behaviors.setup { ctx =>
        val memberEventAdapter: ActorRef[MemberEvent] =
          ctx.messageAdapter(MemberChange)
        Cluster(ctx.system).subscriptions ! Subscribe(
          memberEventAdapter,
          classOf[MemberEvent]
        )

        val reachabilityAdapter: ActorRef[ReachabilityEvent] =
          ctx.messageAdapter(ReachabilityChange)
        Cluster(ctx.system).subscriptions ! Subscribe(
          reachabilityAdapter,
          classOf[ReachabilityEvent]
        )

        Behaviors.receiveMessage { msg =>
          {
            msg match {
              case ReachabilityChange(reachabilityEvent) =>
                reachabilityEvent match {
                  case UnreachableMember(member) =>
                    ctx.log.info(s"Member $member detected as unreachable")
                  case ReachableMember(member) =>
                    ctx.log.info(s"Member $member back to reachable")
                }
              case MemberChange(changeEvent) =>
                changeEvent match {
                  case MemberUp(member) =>
                    ctx.log.info(s"Member ${member.address} is Up")
                  case MemberRemoved(member, previousStatus) =>
                    ctx.log.info(
                      s"Member is Removed: ${member.address} after $previousStatus"
                    )
                  case _: MemberEvent => // ignore
                }
            }
            Behaviors.same
          }
        }

      }
  }

}

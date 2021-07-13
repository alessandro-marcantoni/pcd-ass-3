package puzzle.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent.{MemberEvent, MemberRemoved, MemberUp, ReachabilityEvent, ReachableMember, UnreachableMember}
import akka.cluster.typed.{Cluster, Subscribe}

object Actors {

  object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
      context.spawn(EventListener(), "EventListener")
      Behaviors.empty
    }
  }

  object EventListener {
    sealed trait Event
    private final case class ReachabilityChange(reachabilityEvent: ReachabilityEvent) extends Event
    private final case class MemberChange(event: MemberEvent) extends Event

    def apply(): Behavior[Event] = Behaviors.setup { ctx =>
      val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(MemberChange)
      Cluster(ctx.system).subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])

      val reachabilityAdapter: ActorRef[ReachabilityEvent] = ctx.messageAdapter(ReachabilityChange)
      Cluster(ctx.system).subscriptions ! Subscribe(reachabilityAdapter, classOf[ReachabilityEvent])

      Behaviors.receiveMessage { msg => {
        msg match {
          case ReachabilityChange(reachabilityEvent) => reachabilityEvent match {
            case UnreachableMember(member) => ctx.log.info(s"Member $member detected as unreachable")
            case ReachableMember(member) => ctx.log.info(s"Member $member back to reachable")
          }
          case MemberChange(changeEvent) => changeEvent match {
            case MemberUp(member) => ctx.log.info(s"Member ${member.address} is Up")
            case MemberRemoved(member, previousStatus) => ctx.log.info(s"Member is Removed: ${member.address} after $previousStatus")
            case _: MemberEvent => // ignore
          }
        }
        Behaviors.same
      }
      }

    }
  }

}

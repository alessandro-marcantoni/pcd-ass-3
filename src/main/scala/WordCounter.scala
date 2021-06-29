import Actors.Main
import Messages._
import Utils.GraphicItems._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import scalafx.application.{JFXApp, Platform}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.control.TextArea
import scalafx.scene.Scene

import java.io.File
import scala.collection.convert.ImplicitConversions.`seq AsJavaList`

object Messages {
  sealed trait Message
  case class Parameters(directory: File, ignoredWordsPath: String, nWords: Int) extends Message
  case class Directory(directory: File) extends Message
  case class Document(file: File) extends Message
  case class WordList(words: List[String]) extends Message
  case class Occurrences(occurrences: List[(String,Int)]) extends Message
}

object GUI extends JFXApp {
  val system: ActorSystem[Message] = ActorSystem[Message](Main(), "system")
  stage = new PrimaryStage {
    title = "Word Counter"
    scene = new Scene(Utils.GraphicItems.width, Utils.GraphicItems.height) {
      content = getGuiElements(system)
    }
  }

  val output: TextArea = getGuiElements(system).get(0).asInstanceOf[TextArea]

  object Renderer {
    def apply(): Behavior[Message] = Behaviors.receive { (ctx, msg) =>
      msg match {
        case Occurrences(occurrences) =>
          ctx.log.info(occurrences.toString())
          output.setText(occurrences.toString())
          Behaviors.same
      }
    }
  }

}

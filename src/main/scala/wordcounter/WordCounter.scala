package wordcounter

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.control.TextArea
import scalafx.scene.{Node, Scene}
import wordcounter.Actors.Main
import wordcounter.Messages.{Message, Occurrences}
import wordcounter.Utils.GraphicItems.getGuiElements

import java.io.File

object Messages {
  sealed trait Message

  case class Parameters(directory: File, ignoredWordsPath: String, nWords: Int) extends Message
  case class Directory(directory: File) extends Message
  case class Document(file: File) extends Message
  case class WordList(words: List[String]) extends Message
  case class Occurrences(occurrences: List[(String, Int)]) extends Message
}

object GUI extends JFXApp {
  val system: ActorSystem[Message] = ActorSystem[Message](Main(), "system")
  val elements: Seq[Node] = getGuiElements(system)
  stage = new PrimaryStage {
    title = "Word Counter"
    scene = new Scene(Utils.GraphicItems.width, Utils.GraphicItems.height) {
      content = elements
    }
  }

  val output: TextArea = elements.head.asInstanceOf[TextArea]

  object Renderer {
    def apply(): Behavior[Message] = Behaviors.receive { (_, msg) =>
      msg match {
        case Occurrences(occurrences) =>
          var text: String = ""
          occurrences foreach (e => text += (e._1 + "\t->\t" + e._2 + "\n"))
          output.setText(text)
          Behaviors.same
      }
    }
  }

}

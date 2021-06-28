import Actors.Main
import Messages._
import Utils.GraphicItems._
import akka.actor.typed.ActorSystem
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene

import java.io.File

object Messages {
  sealed trait Message
  case class Parameters(directory: File, ignoredWordsPath: String, nWords: Int) extends Message
  case class Directory(directory: File) extends Message
  case class Document(file: File) extends Message
  case class WordList(words: List[String]) extends Message
}

object GUI extends JFXApp {
  val system: ActorSystem[Message] = ActorSystem[Message](Main(), "system")
  stage = new PrimaryStage {
    title = "Word Counter"
    scene = new Scene(Utils.GraphicItems.width, Utils.GraphicItems.height) {
      content = getGuiElements(system)
    }
  }
}

object WordCounter extends App {
  val parameters: Parameters = Parameters(new File("./res/pdfs"), "./res/ignored.txt", 10)
  val system = ActorSystem[Message](Main(), "system")
  system ! parameters
}

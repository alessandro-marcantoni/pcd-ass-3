import Actors.Main
import Messages._
import akka.actor.typed.ActorSystem
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.Button

import java.io.File

object Messages {
  sealed trait Message
  case class Parameters(directory: File, ignoredWordsPath: String, nWords: Int) extends Message
  case class Directory(directory: File) extends Message
  case class Document(file: File) extends Message
  case class WordList(words: List[String]) extends Message
}

object GUI extends JFXApp {
  stage = new PrimaryStage {
    title = "Word Counter"
    scene = new Scene(800, 600) {
      val btnStart = new Button("Start")
      val btnStop = new Button("Stop")
      content = List(btnStart, btnStop)
    }
  }
}

object WordCounter extends App {
  val parameters: Parameters = Parameters(new File("./res/pdfs"), "./res/ignored.txt", 10)
  val system = ActorSystem[Message](Main(), "system")
  system ! parameters
}

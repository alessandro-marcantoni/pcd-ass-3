import Actors.Main
import Messages._
import Utils.GraphicItems._
import akka.actor.typed.ActorSystem
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.{Button, TextField}

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
      val tfdPdfs: TextField = setPdfsTextField()
      val btnDirectoryChooser: Button = setBtnDirectoryChooser(stage, tfdPdfs)
      val tfdIgnored: TextField = setIgnoredTextField()
      val btnFileChooser: Button = setBtnFileChooser(stage, tfdIgnored)
      val btnStart: Button = setStartButton()
      val btnStop: Button = setStopButton()
      content = List(btnStart, btnStop, btnDirectoryChooser, tfdPdfs, btnFileChooser, tfdIgnored)
    }
  }
}

object WordCounter extends App {
  val parameters: Parameters = Parameters(new File("./res/pdfs"), "./res/ignored.txt", 10)
  val system = ActorSystem[Message](Main(), "system")
  system ! parameters
}

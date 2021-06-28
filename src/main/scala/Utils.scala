import GUI.stage
import Messages.{Message, Parameters}
import akka.actor.typed.ActorSystem
import scalafx.scene.Node
import scalafx.scene.control.{Button, TextField}
import scalafx.stage.{DirectoryChooser, FileChooser, Stage}

import java.io.File

object Utils {
  object CorrectParameters {
    def unapply(parameters: Parameters): Option[Parameters] =
      if (parameters.directory.exists() && parameters.directory.isDirectory) Some(parameters) else None
  }

  object GraphicItems {
    val width: Double = 800
    val height: Double = 600

    def getGuiElements(system: ActorSystem[Message]): List[Node] = {
      val tfdPdfs: TextField = setPdfsTextField()
      val btnDirectoryChooser: Button = setBtnDirectoryChooser(stage, tfdPdfs)
      val tfdIgnored: TextField = setIgnoredTextField()
      val btnFileChooser: Button = setBtnFileChooser(stage, tfdIgnored)
      val btnStart: Button = setStartButton()
      btnStart.onAction = _ => system ! Parameters(new File(tfdPdfs.text.value), tfdIgnored.text.value, 10)
      val btnStop: Button = setStopButton()
      btnStop.onAction = _ => system.terminate()
      List(btnStart, btnStop, btnDirectoryChooser, tfdPdfs, btnFileChooser, tfdIgnored)
    }

    def setStartButton(): Button = {
      val btnStart = new Button("Start")
      btnStart.layoutX = width * 0.8
      btnStart.layoutY = height * 0.9
      btnStart
    }

    def setStopButton(): Button = {
      val btnStop = new Button("Stop")
      btnStop.layoutX = width * 0.9
      btnStop.layoutY = height * 0.9
      btnStop
    }

    def setPdfsTextField(): TextField = {
      val tfdPdfs = new TextField()
      tfdPdfs.layoutX = width * 0.05
      tfdPdfs.layoutY = height * 0.05
      tfdPdfs
    }

    def setBtnDirectoryChooser(stage: Stage, tfd: TextField): Button = {
      val btnDirectoryChooser = new Button("Choose pdf directory")
      btnDirectoryChooser.layoutX = width * 0.25
      btnDirectoryChooser.layoutY = height * 0.05
      btnDirectoryChooser.onAction = _ => {
        val directoryChooser = new DirectoryChooser()
        directoryChooser.initialDirectory = new File(System.getProperty("user.dir"))
        tfd.text = directoryChooser.showDialog(stage).getAbsolutePath
      }
      btnDirectoryChooser
    }

    def setIgnoredTextField(): TextField = {
      val tfdIgnored = new TextField()
      tfdIgnored.layoutX = width * 0.05
      tfdIgnored.layoutY = height * 0.15
      tfdIgnored
    }

    def setBtnFileChooser(stage: Stage, tfd: TextField): Button = {
      val btnFileChooser = new Button("Choose ignored file")
      btnFileChooser.layoutX = width * 0.25
      btnFileChooser.layoutY = height * 0.15
      btnFileChooser.onAction = _ => {
        val fileChooser = new FileChooser()
        fileChooser.initialDirectory = new File(System.getProperty("user.dir"))
        tfd.text = fileChooser.showOpenDialog(stage).getAbsolutePath
      }
      btnFileChooser
    }
  }
}

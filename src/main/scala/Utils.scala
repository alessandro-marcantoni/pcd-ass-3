import GUI.stage
import Messages.{Message, Parameters}
import akka.actor.typed.ActorSystem
import scalafx.scene.Node
import scalafx.scene.control.{Button, TextArea, TextField}
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
      val outputArea: TextArea = setOutputView()
      val tfdNumber: TextField = setNumberTextField()
      val btnNumber: Button = setNumberButton()
      btnStart.onAction = _ => system ! Parameters(new File(tfdPdfs.text.value), tfdIgnored.text.value, tfdNumber.text.value.toIntOption.getOrElse(0))
      val btnStop: Button = setStopButton()
      btnStop.onAction = _ => system.terminate()
      List(outputArea, btnStart, btnStop, btnDirectoryChooser, tfdPdfs, btnFileChooser, tfdIgnored, tfdNumber, btnNumber)
    }

    def setNumberTextField(): TextField = {
      val tfdNum = new TextField()
      tfdNum.layoutX = width * 0.05
      tfdNum.layoutY = height * 0.25
      tfdNum
    }

    def setNumberButton(): Button = {
      val btnNum = new Button("Choose number of words")
      btnNum.layoutX = width * 0.25
      btnNum.layoutY = height * 0.25
      btnNum.setMinWidth(width * 0.2)
      btnNum
    }

    def setOutputView(): TextArea = {
      val outputView = new TextArea()
      outputView.layoutX = width * 0.5
      outputView.layoutY = height * 0.05
      outputView.setMaxWidth(width * 0.4)
      outputView.setMinHeight(height * 0.9)
      outputView
    }

    def setStartButton(): Button = {
      val btnStart = new Button("Start")
      btnStart.layoutX = width * 0.05
      btnStart.layoutY = height * 0.9
      btnStart.setMinWidth(width * 0.15)
      btnStart
    }

    def setStopButton(): Button = {
      val btnStop = new Button("Stop")
      btnStop.layoutX = width * 0.3
      btnStop.layoutY = height * 0.9
      btnStop.setMinWidth(width * 0.15)
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
      btnDirectoryChooser.setMinWidth(width * 0.2)
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
      btnFileChooser.setMinWidth(width * 0.2)
      btnFileChooser.onAction = _ => {
        val fileChooser = new FileChooser()
        fileChooser.initialDirectory = new File(System.getProperty("user.dir"))
        tfd.text = fileChooser.showOpenDialog(stage).getAbsolutePath
      }
      btnFileChooser
    }
  }
}

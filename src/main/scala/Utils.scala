import Messages.Parameters

object Utils {


  object CorrectParameters {
    def unapply(parameters: Parameters): Option[Parameters] =
      if (parameters.directory.exists() && parameters.directory.isDirectory) Some(parameters) else None
  }
}

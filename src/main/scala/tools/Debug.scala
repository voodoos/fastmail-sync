package tools

object Debug {
  private var debugging = true

  def debug(message : String, lvl : String) : Unit = {
    if (debugging) println("[" + lvl + "] " + message)
  }
  def info(message : String) : Unit = debug(message, "info")
  def warn(message : String) : Unit = debug(message, "warning")
}

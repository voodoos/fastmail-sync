package server.generic

trait Versioned {
  def id: String
  def version: Long
}
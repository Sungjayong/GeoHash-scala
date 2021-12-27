import Geohash._

object Main {
  def main(args: Array[String]): Unit = {
    println(encode(57.64911,10.40744, 12))
    println(decode("u4pruydqqvj8"))
    println(east("u4pruydqqvj8"))
    println(west("u4pruydqqvj8"))
    println(south("u4pruydqqvj8"))
    println(north("u4pruydqqvj8"))
    println(southeast("u4pruydqqvj8"))
    println(northeast("u4pruydqqvj8"))
    println(southwest("u4pruydqqvj8"))
    println(northwest("u4pruydqqvj8"))
  }
}

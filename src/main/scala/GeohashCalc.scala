import scala.annotation.tailrec

object GeohashCalc {
  val LAT_RANGE: (Double, Double) = (-90, 90)
  val LON_RANGE: (Double, Double) = (-180, 180)
  val BASE32 = "0123456789bcdefghjimnpqrstuvwxyz"
  val BITS = Array(16, 8, 4, 2, 1)

  def encode(lat: Double, lon: Double, precision: Int): String = {
    require(LAT_RANGE._1 < lat && lat < LAT_RANGE._2 )
    require(LON_RANGE._1 < lon && lon < LON_RANGE._2 )
    val p = precision * 5 / 2
    val latList = makeBits(lat, LAT_RANGE, p)
    val lonList = makeBits(lon, LON_RANGE, p)
    makeGeoHash(latList, lonList)
  }

  def encode(lat: Double, lon: Double): String = {
     encode(lat,lon, 12)
  }

  private def makeGeoHash(latList: List[Boolean], lonList: List[Boolean]) = {
    (lonList zip latList flatMap { case (a, b) => Seq(a, b) }).grouped(5).map(toBase32).mkString
  }

  private def toBase32(bin: Seq[Boolean]): Char = {
    BASE32((bin zip BITS).collect {case (true, x) => x}.sum)
  }

  private def makeBits(num: Double, range: (Double, Double), p: Int): List[Boolean] = {
    if(p == 0) return Nil
    val avg = mid(range._1, range._2)
    if(avg < num) true :: makeBits(num, (avg, range._2), p - 1)
    else false :: makeBits(num, (range._1, avg), p - 1)
  }

  def decode(s: String): (Double, Double) = {
    val (lonBit, latBit) = toBits(s)
    val lon = mid(toDecimal(LON_RANGE, lonBit))
    val lat = mid(toDecimal(LAT_RANGE, latBit))
    (lat, lon)
  }

  private def toDecimal(range: (Double, Double), bits: List[Boolean]) =
    bits.foldLeft(range)((acc, bit) => if (bit) (mid(acc), acc._2) else (acc._1, mid(acc)))

  private def mid(a: (Double, Double)): Double = { (a._1 + a._2) / 2 }

  private def toBits(s: String) = {
    s.map(c => fillZero(BASE32.indexOf(c).toBinaryString))
      .mkString
      .toList
      .map(_.toString.toInt)
      .map(elem => if (elem == 1) true else false)
      .foldRight((List[Boolean](), List[Boolean]())) { case (b, (a1, a2)) => (b :: a2, a1) }
  }

  @tailrec
  private def fillZero(s: String): String = {
    if(s.length == 5) s
    else fillZero("0".concat(s))
  }

  def east(s: String): String = {
    val newLonList = moveUp(toBits(s)._1.reverse).reverse
    val latList = toBits(s)._2
    makeGeoHash(latList, newLonList)
  }

  def west(s: String): String = {
    val newLonList = moveDown(toBits(s)._1.reverse).reverse
    val latList = toBits(s)._2
    makeGeoHash(latList, newLonList)
  }

  def south(s: String): String = {
    val lonList = toBits(s)._1
    val newLatList = moveDown(toBits(s)._2.reverse).reverse
    makeGeoHash(newLatList, lonList)
  }

  def north(s: String): String = {
    val lonList = toBits(s)._1
    val newLatList = moveUp(toBits(s)._2.reverse).reverse
    makeGeoHash(newLatList, lonList)
  }

  def southeast(s: String): String = {
    val newLonList = moveUp(toBits(s)._1.reverse).reverse
    val newLatList = moveDown(toBits(s)._2.reverse).reverse
    makeGeoHash(newLatList, newLonList)
  }

  def northeast(s: String): String = {
    val newLonList = moveUp(toBits(s)._1.reverse).reverse
    val newLatList = moveUp(toBits(s)._2.reverse).reverse
    makeGeoHash(newLatList, newLonList)
  }

  def southwest(s: String): String = {
    val newLonList = moveDown(toBits(s)._1.reverse).reverse
    val newLatList = moveDown(toBits(s)._2.reverse).reverse
    makeGeoHash(newLatList, newLonList)
  }

  def northwest(s: String): String = {
    val newLonList = moveDown(toBits(s)._1.reverse).reverse
    val newLatList = moveUp(toBits(s)._2.reverse).reverse
    makeGeoHash(newLatList, newLonList)
  }

  def moveUp(ll: List[Boolean]): List[Boolean] = {
    if(!ll.head) true :: ll.tail
    else false :: moveUp(ll.tail)
  }

  def moveDown(ll: List[Boolean]): List[Boolean] = {
    if(ll.head) false :: ll.tail
    else true :: moveDown(ll.tail)
  }
}

package demo

import org.apache.spark.sql.SparkSession
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SalesRankDecreaseTest extends FunSuite{

  val session: SparkSession = SparkSession
    .builder
    .appName("demo-app")
    .master("local[*]")
    .config("spark.ui.port", 4340)
    .getOrCreate()

  val dirPath: String = System.getProperty("user.dir")

  val directory: String = dirPath + "/testdata/"

  val startTime: Long = System.currentTimeMillis()

  SalesRankDecrease.run(directory, session)

  println(s"Time used: ${(System.currentTimeMillis() - startTime) / 60000d} minutes")

}

package demo

import org.apache.spark.sql.SparkSession
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RelativeSalesDecreaseTest extends FunSuite{

  val session: SparkSession = SparkSession
    .builder
    .appName("demo-app")
    .master("local[*]")
    .config("spark.ui.port", 4340)
    .getOrCreate()

  val directory = "/Users/shadrackantwi/Workspace/parsejson/testdata/"

  val startTime: Long = System.currentTimeMillis()

  RelativeSalesDecrease.run(directory, session)

  println(s"Time used: ${(System.currentTimeMillis() - startTime) / 60000d} minutes")

}
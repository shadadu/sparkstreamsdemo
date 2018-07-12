package demo

import com.typesafe.scalalogging.StrictLogging
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.StreamingQuery

object Input extends StrictLogging {

  def readJsonData(path: String, session: SparkSession): StreamingQuery = {

    session.sql("set spark.sql.streaming.schemaInference=true")
    val inputDf = session.readStream.format("json").json(path)

    inputDf.printSchema()

    val query: StreamingQuery = inputDf.writeStream
      .format("memory")
      .queryName("QueryTable")
      .start()

    query

  }

}
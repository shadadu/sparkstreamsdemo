package demo

import org.apache.spark.sql.{Column, DataFrame, SparkSession}
import org.apache.spark.sql.expressions.{Window, WindowSpec}
import org.apache.spark.sql.functions._

/*

    I made the assumption that the transaction times for the registers are normally distributed. I find the mean of the
    difference between consecutive order_times for the same register as transaction duration.
    I add a multiple ( x 1.96) of the standard deviation of these to the mean to obtain an upper bound for the transaction
    duration.

    A register is in the "transaction state" when the time between two consecutive order_times are less than this bound
    Otherwise it is in "not accepting transaction state"

    * */

object RegisterState {

  def run(dirPath: String, session: SparkSession): Unit = {

    val query = Input.readJsonData(dirPath, session)
    val df = session.sql("SELECT * FROM QueryTable")

    val spark = session

    import spark.implicits._

    val windowSpec: WindowSpec = Window.orderBy("order_time")

    val lagDF = df
      .withColumn("lag_time", lag(new Column("order_time"), 1, 1).over(windowSpec))
      .toDF()

    val transactDurDf = lagDF.withColumn("transaction_duration", when(col("lag_time").isNull, lit(0))
      .otherwise(col("lag_time")) - when(col("order_time").isNull, lit(0))
      .otherwise(col("order_time")))

    val transactionDurationRdd = transactDurDf
      .select("transaction_duration")
      .rdd
      .map(_.fieldIndex("transaction_duration").toString.toDouble)

    val transactDurMean: Double = transactionDurationRdd.mean()
    val transactDurStdDev: Double = transactionDurationRdd.stdev()
    val upperBoundTransactDuration = transactDurMean + 1.96 * transactDurStdDev

    val func = (st: String) => if(st.toDouble < upperBoundTransactDuration) 1 else 0
    val defUDF = udf(func)

    val registerStates: DataFrame = transactDurDf
      .withColumn("transaction_state", defUDF(new Column("transaction_duration") ))
      .select("register", "order_time", "transaction_state")
      .na.drop()

    registerStates.printSchema()

    registerStates.show()

    query.stop()

  }

}

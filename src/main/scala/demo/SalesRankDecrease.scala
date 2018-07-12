package demo

import org.apache.spark.sql.expressions.{Window, WindowSpec}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column, SparkSession}

object SalesRankDecrease {

  def run(directoryPath: String, session: SparkSession): Unit = {

    val query = Input.readJsonData(directoryPath, session)

    query.awaitTermination(2000L)

    val queryDf = session
      .sql("SELECT amount, store_number, department, id, register, order_id, FROM_UNIXTIME(order_time/1000, 'yyyy/MM/dd HH:mm:ss') AS dt_order_time FROM QueryTable")

    val windowSpec: WindowSpec = Window.partitionBy("department")

    val threeHourTimeWindowSpec = window(queryDf("dt_order_time"), windowDuration = "3 hour", slideDuration = "1 hour")
      .as("3_hour_time_window")

    val threeHourlySales = queryDf.groupBy(threeHourTimeWindowSpec, queryDf("department"), queryDf("store_number"))
      .agg(sum(queryDf("amount")).as("3_hourly_sales_by_store_number"))
      .withColumn("3_hourly_sales_by_department", sum("3_hourly_sales_by_store_number")
        .over(windowSpec))

    val spark = session
    import spark.implicits._

    val lagWindowSpec: WindowSpec = Window.orderBy("3_hourly_sales_by_department")

    val lagDF = threeHourlySales
      .withColumn("lag_time", lag(new Column("3_hourly_sales_by_department"), offset = 1, defaultValue = 1)
        .over(lagWindowSpec))
      .toDF()

    val salesChangeDf = lagDF.withColumn("change_in_sales", when(col("lag_time").isNull, lit(0))
      .otherwise(col("3_hourly_sales_by_department")) - when(col("lag_time").isNull, lit(0))
      .otherwise(col("3_hourly_sales_by_department")) - col("lag_time") )


    val w = Window.orderBy($"change_in_sales")

    val salesChangePercentRanks = salesChangeDf.withColumn("change_in_sales", percent_rank()
      .over(w)
      .alias("sales_percentage_rank"))

    salesChangePercentRanks.show()

  }

}

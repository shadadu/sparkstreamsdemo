package demo

import org.apache.spark.sql.{Column, DataFrame, SparkSession}
import org.apache.spark.sql.expressions.{Window, WindowSpec}
import org.apache.spark.sql.functions._


object RelativeSalesDecrease {

  def run(directoryPath: String, session: SparkSession): Unit = {

    val query = Input.readJsonData(directoryPath, session)

    val spark = session
    import spark.implicits._

    val inputDf = session
      .sql("SELECT amount, store_number, department, id, register, order_id, FROM_UNIXTIME(order_time/1000, 'yyyy/MM/dd HH:mm:ss') AS dt_order_time FROM QueryTable")

    val deptWindowSpec: WindowSpec = Window.partitionBy("department")

    val sixHourTimeWindowSpec: Column = window(inputDf("dt_order_time"),"6 hour", "1 hour")
      .as("6_hour_time_window")

    val totalSalesPerSixHour: DataFrame = inputDf
      .groupBy(sixHourTimeWindowSpec, inputDf("department"), inputDf("store_number"))
      .agg(avg("amount").as("6_hourly_sales_by_store_number"))
      .withColumn("6_hourly_sales_by_department", avg("6_hourly_sales_by_store_number")
        .over(deptWindowSpec))

    val lagWindowSpec: WindowSpec = Window.orderBy("6_hourly_sales_by_department")

    val lagDF: DataFrame = totalSalesPerSixHour
      .withColumn("lag_time", lag("6_hourly_sales_by_department", 1, 1)
        .over(lagWindowSpec))

    val salesChangeDf = lagDF.withColumn("change_in_sales", when(col("lag_time").isNull, lit(0))
      .otherwise(col("6_hourly_sales_by_department")) - when(col("lag_time").isNull, lit(0))
      .otherwise(col("6_hourly_sales_by_department")) - col("lag_time") )

    val w = Window.orderBy("6_hourly_sales_by_department")

    val salesChangeRank = salesChangeDf.withColumn("change_in_sales", percent_rank()
      .over(w)
      .alias("sales_percentage_rank"))

    salesChangeRank.show()

    query.stop()
  }

}

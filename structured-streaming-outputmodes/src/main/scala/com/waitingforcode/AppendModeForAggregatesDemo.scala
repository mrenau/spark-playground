package com.waitingforcode


import org.apache.spark.sql.execution.streaming.MemoryStream
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.{SQLContext, SparkSession, functions}

import java.sql.Timestamp

object AppendModeForAggregatesDemo {

  def main(args: Array[String]): Unit = {
    val sparkSession = SparkSession.builder().master("local[*]")
      .config("spark.sql.shuffle.partitions", "1")
      .getOrCreate()

    implicit val sparkContext: SQLContext = sparkSession.sqlContext
    import sparkSession.implicits._
    val events = MemoryStream[TimestampedEvent]

    events.addData(
      TimestampedEvent(1, Timestamp.valueOf("2024-04-01 09:00:00")),
      TimestampedEvent(2, Timestamp.valueOf("2024-04-01 09:02:00")),
      TimestampedEvent(3, Timestamp.valueOf("2024-04-01 09:04:00")),
      TimestampedEvent(4, Timestamp.valueOf("2024-04-01 09:12:50"))
    )

    val query = events.toDF()
      .withWatermark("eventTime", "20 minutes")
      .groupBy(functions.window($"eventTime", "10 minutes")).count()
      .writeStream.outputMode(OutputMode.Append())
      .format("console").option("truncate", false).start()

    query.explain(true)
    query.processAllAvailable()
    events.addData(
      TimestampedEvent(1, Timestamp.valueOf("2024-04-01 09:00:00")),
      TimestampedEvent(2, Timestamp.valueOf("2024-04-01 10:02:50")) // advances the watermark
    )
    query.processAllAvailable()


    events.addData(
      TimestampedEvent(1, Timestamp.valueOf("2024-04-01 09:00:00")),
    )
    query.processAllAvailable()
  }

}
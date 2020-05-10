// /home/rstudio/spark/bin/spark-shell --repositories http://packages.confluent.io/maven/ --packages org.apache.kafka:kafka_2.11:5.4.1-ce,io.confluent:kafka-avro-serializer:5.4.1,io.confluent:kafka-schema-registry:5.4.1,org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.5,org.apache.spark:spark-avro_2.11:2.4.5
// https://blog.engineering.publicissapient.fr/2017/09/27/spark-comprendre-et-corriger-lexception-task-not-serializable/package sparklyr.confluent.avro

import java.util.Properties

import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, Dataset, Row}
import za.co.absa.abris.examples.utils.ExamplesUtils._
import org.apache.spark.sql.SparkSession
import za.co.absa.abris.avro.functions.from_confluent_avro
import za.co.absa.abris.avro.functions.to_confluent_avro
import org.apache.spark.sql.functions.struct

object Bridge {
  def getSchemaRegistryConfig(topic: String, master: String, startingOffsets: String, kafkaUrl: String,
             schemaRegistryUrl: String, logLevel: String, jobName: String) = {
    val properties = new Properties()
	properties.setProperty("job.name", jobName)
	properties.setProperty("job.master", master)
	properties.setProperty("key.schema.id", "latest")
	properties.setProperty("value.schema.id", "latest")
	properties.setProperty("value.schema.naming.strategy", "topic.name")
	properties.setProperty("schema.name", "native_complete")
	properties.setProperty("schema.registry.topic", topic)
	properties.setProperty("option.subscribe", topic)
	properties.setProperty("schema.namespace", "all-types.test")
	properties.setProperty("key.schema.id", "latest")
	properties.setProperty("log.level", logLevel)
	properties.setProperty("schema.registry.url", schemaRegistryUrl)
	properties.getSchemaRegistryConfigurations("option.subscribe")
  }
  def stream_read(topic: String, master: String, startingOffsets: String, kafkaUrl: String,
             schemaRegistryUrl: String, logLevel: String, jobName: String) = {
    val properties = new Properties()
	properties.setProperty("job.name", jobName)
	properties.setProperty("job.master", master)
	properties.setProperty("key.schema.id", "latest")
	properties.setProperty("value.schema.id", "latest")
	properties.setProperty("value.schema.naming.strategy", "topic.name")
	properties.setProperty("schema.name", "native_complete")
	properties.setProperty("schema.registry.topic", topic)
	properties.setProperty("option.subscribe", topic)
	properties.setProperty("schema.namespace", "all-types.test")
	properties.setProperty("key.schema.id", "latest")
	properties.setProperty("log.level", logLevel)
	properties.setProperty("schema.registry.url", schemaRegistryUrl)
	val spark = getSparkSession(properties, "job.name", "job.master", "log.level")
	val schemaRegistryConfig = properties.getSchemaRegistryConfigurations("option.subscribe")
    val stream = spark.readStream.format("kafka").option("startingOffsets", startingOffsets).option("kafka.bootstrap.servers", kafkaUrl).addOptions(properties)
    stream.load().select(from_confluent_avro(col("value"), schemaRegistryConfig) as 'value)
  }
  
  def stream_write(topic: String, dataFrame: Dataset[Row], kafkaUrl: String, schemaRegistryUrl: String,                
                   valueSchemaNamingStrategy: String, avroRecordName: String,
				   avroRecordNamespace: String, checkpointLocation: String) = {
    val registryConfig = Map(
      "schema.registry.topic" -> topic,
      "schema.registry.url" -> schemaRegistryUrl,
      "value.schema.naming.strategy" -> valueSchemaNamingStrategy,
      "schema.name" -> avroRecordName,
      "schema.namespace"-> avroRecordNamespace
      )
	val allColumns = struct(dataFrame.columns.head, dataFrame.columns.tail: _*)
    dataFrame.select(to_confluent_avro(allColumns, registryConfig) as 'value).writeStream.format("kafka").option("kafka.bootstrap.servers", kafkaUrl).option("topic", topic).option("checkpointLocation", checkpointLocation).start()
  }
}
// val x = Bridge.stream_read("parameter").select("value.timestamp", "value.id", "value.side")
// Bridge.stream_write("parameter_2", x, avroRecordName="value", avroRecordNamespace="indicator")
// Bridge.stream_read("parameter_2").writeStream.format("console").start()





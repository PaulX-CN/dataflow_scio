# dataflow_scio

Create a topic in PubSub and run a quick test.

```bash
gcloud pubsub topics create myTopic
gcloud pubsub subscriptions create --topic myTopic mySubscription
gcloud pubsub topics publish myTopic --message "hello"
gcloud pubsub subscriptions pull --auto-ack mySubscription
```

Run below commands to create dataset and table in bigquery:

```bash
bq --location=US mk --dataset dataflow_scio
bq mk --table dataflow_scio.person 'src/main/resources/dataflow_scio(table_schema).json'
```

Compile the .proto file to java class
```bash
export SRC_DIR=src/main/resources
export DST_DIR=src/main/java
protoc -I=$SRC_DIR --java_out=$DST_DIR $SRC_DIR/person.proto
```

Finally, compile the project and run:
```bash
runMain org.paul.BigQueryDump --runner=DataflowRunner \
--inputTopic=projects/$PROJECT_NAME/topics/myTopic --tableName=$PROJECT_NAME.dataflow_scio.person
```
# dataflow_scio

### use with pubsub

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


Optional: run the python script to randomly push events to pubsub.
```bash
pip3 install -r pubsub_proto/requirements.txt
cd pubsub_proto
protoc -I=. --python_out=. person.proto
python pubsub_proto/publisher.py
```


### Use with Kafka

1. Install Kafka on GCE 
  see tutorial here: https://www.learningjournal.guru/courses/kafka/kafka-foundation-training/kafka-in-gcp/
2. Make changes to the configuration file:

   ```bash
   sudo nano /opt/kafka/config/producer.properties
   ```
   change below line to :
   
   **bootstrap.servers=PUBLIC_HOST:9092**
   
   also change this:
   ```bash
   sudo nano /opt/kafka/config/server.properties
   ```
   
   change below line to :
   
   **advertised.listeners=PLAINTEXT://PUBLIC_HOST:9092**
3. Restart zookeeper and kafka so that zookeeper knows about the new host.
   ```bash
   sudo systemctl restart zookeeper & \
   sudo systemctl restart kafka
   ```
4. Make a new topic
   ```bash
   kafka-topics.sh --create --zookeeper PUBLIC_HOST:2181 --replication-factor 1 --partitions 1 --topic test
   ```
5. run the project
    ```bash
    runMain org.paul.BigQueryDump --runner=DataflowRunner --inputTopic=test \
    --inputBroker=35.237.237.235:9092 --tableName=dataflow_scio.person```

6. Optional: run python script to publish to Kafka

    ```bash
    python3 pubsub_proto/kafka_publisher.py
    ```   
7. Check your bigquery!
   ```sql
   SELECT count(name) from `dataflow_scio.person` where name like '%kafka%'
   ```
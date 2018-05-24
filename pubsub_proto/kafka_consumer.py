from kafka import KafkaConsumer
import person_pb2

consumer = KafkaConsumer('test', bootstrap_servers='35.237.237.235:9092',
                         api_version=(0, 10, 1))
consumer.subscribe(["test"])

print(consumer.poll())
for msg in consumer:
    p = person_pb2.Person()
    p.ParseFromString(msg.value)
    print(p)
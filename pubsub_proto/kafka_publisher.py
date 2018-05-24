from kafka import KafkaProducer
import person_pb2
import random
import time

producer = KafkaProducer(bootstrap_servers='35.237.237.235:9092',
                         api_version=(0, 10, 1))

def generateRandomPerson():
    p = person_pb2.Person()
    p.id = random.randint(0, 10000)
    p.name = "kafka-" + str(p.id)

    sex = random.randint(0, 1)
    if sex:
        p.gender = person_pb2.Male
    else:
        p.gender = person_pb2.Female

    current_ts = int(time.time())
    p.ts = current_ts
    return p

while True:
    p = generateRandomPerson()

    future = producer.send('test', p.SerializeToString())
    result = future.get(timeout=10)
    print(result)
    time.sleep(0.2)
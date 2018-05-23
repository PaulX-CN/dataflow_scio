import person_pb2
from google.cloud import pubsub
import random
import time
import os

topic_name = "myTopic"
project_name = "trim-glazing-160719"

topic = 'projects/{project_id}/topics/{topic}'.format(
     project_id=project_name,
     topic=topic_name)

def generateRandomPerson():
    p = person_pb2.Person()
    p.id = random.randint(0, 10000)
    p.name = "id-" + str(p.id)

    sex = random.randint(0, 1)
    if sex:
        p.gender = person_pb2.Male
    else:
        p.gender = person_pb2.Female

    current_ts = int(time.time())
    p.ts = current_ts
    return p

publisher = pubsub.PublisherClient()

while True:
    p = generateRandomPerson()
    publisher.publish(topic, p.SerializeToString())
    time.sleep(0.1)

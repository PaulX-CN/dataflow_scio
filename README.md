# dataflow_scio

```bash
gcloud pubsub topics create myTopic
gcloud pubsub subscriptions create --topic myTopic mySubscription
gcloud pubsub topics publish myTopic --message "hello"
gcloud pubsub subscriptions pull --auto-ack mySubscription
```
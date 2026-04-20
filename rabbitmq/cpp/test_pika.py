#!/usr/bin/env python3
import pika

print("Connecting to localhost:5672...")

credentials = pika.PlainCredentials('admin', '5Edidada')
parameters = pika.ConnectionParameters(
    host='localhost',
    port=5672,
    credentials=credentials
)

try:
    connection = pika.BlockingConnection(parameters)
    print("Connected successfully!")
    
    channel = connection.channel()
    print("Channel opened!")
    
    channel.queue_declare(queue='test_queue', durable=True)
    print("Queue declared!")
    
    connection.close()
    print("Test completed successfully!")
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()

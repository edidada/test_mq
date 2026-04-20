#include <amqp.h>
#include <amqp_tcp_socket.h>
#include <stdio.h>
#include <stdlib.h>

int main() {
    amqp_connection_state_t conn = amqp_new_connection();
    amqp_socket_t *socket = amqp_tcp_socket_new(conn);
    
    if (!socket) {
        fprintf(stderr, "Error creating socket\n");
        return 1;
    }
    
    printf("Connecting to localhost:5672...\n");
    int status = amqp_socket_open(socket, "localhost", 5672);
    if (status != AMQP_STATUS_OK) {
        fprintf(stderr, "Failed to open socket: %d\n", status);
        amqp_destroy_connection(conn);
        return 1;
    }
    printf("Socket connected!\n");
    
    printf("Logging in as admin/5Edidada...\n");
    amqp_rpc_reply_t ret = amqp_login_with_properties(
        conn, 
        "/",  // vhost
        0,    // channel_max
        AMQP_DEFAULT_FRAME_SIZE,
        AMQP_DEFAULT_HEARTBEAT,
        NULL,  // amqp_table_t* properties (can be NULL)
        AMQP_SASL_METHOD_PLAIN, 
        "admin", 
        "5Edidada"
    );
    
    if (ret.reply_type != AMQP_RESPONSE_NORMAL) {
        fprintf(stderr, "Login failed!\n");
        if (ret.reply_type == AMQP_RESPONSE_LIBRARY_EXCEPTION) {
            fprintf(stderr, "Library error: %d\n", ret.library_error);
        } else if (ret.reply_type == AMQP_RESPONSE_SERVER_EXCEPTION) {
            fprintf(stderr, "Server exception\n");
            if (ret.reply.id == AMQP_CONNECTION_CLOSE_METHOD) {
                amqp_connection_close_t *close = (amqp_connection_close_t *)ret.reply.decoded;
                fprintf(stderr, "Server close reason: %d - %.*s\n", 
                        close->reply_code, 
                        (int)close->reply_text.len, 
                        (char *)close->reply_text.bytes);
            }
        }
        amqp_destroy_connection(conn);
        return 1;
    }
    
    printf("Login successful!\n");
    
    // Open channel
    amqp_channel_open(conn, 1);
    ret = amqp_get_rpc_reply(conn);
    if (ret.reply_type != AMQP_RESPONSE_NORMAL) {
        fprintf(stderr, "Failed to open channel\n");
        amqp_connection_close(conn, AMQP_REPLY_SUCCESS);
        amqp_destroy_connection(conn);
        return 1;
    }
    
    printf("Channel opened successfully!\n");
    
    // Declare queue (durable)
    amqp_queue_declare_ok_t *queue_ret = amqp_queue_declare(
        conn, 
        1,  // channel
        amqp_cstring_bytes("test_queue"),
        1,  // passive
        1,  // durable
        0,  // exclusive
        0,  // auto_delete
        amqp_empty_table
    );
    
    if (!queue_ret) {
        fprintf(stderr, "Failed to declare queue\n");
        amqp_connection_close(conn, AMQP_REPLY_SUCCESS);
        amqp_destroy_connection(conn);
        return 1;
    }
    
    printf("Queue declared successfully!\n");
    
    amqp_connection_close(conn, AMQP_REPLY_SUCCESS);
    amqp_destroy_connection(conn);
    
    printf("Test completed successfully!\n");
    return 0;
}

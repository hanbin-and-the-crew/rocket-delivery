package org.sparta.slack.infrastructure.slack;

class SlackApiTransportException extends RuntimeException {

    SlackApiTransportException(String message) {
        super(message);
    }

    SlackApiTransportException(Throwable cause) {
        super(cause);
    }

    SlackApiTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}

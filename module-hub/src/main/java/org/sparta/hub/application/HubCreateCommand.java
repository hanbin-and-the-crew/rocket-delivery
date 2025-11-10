package org.sparta.hub.application;

public record HubCreateCommand(String name, String address, Double latitude, Double longitude) {
    public static HubCreateCommand of(String name, String address, Double latitude, Double longitude) {
        return new HubCreateCommand(name, address, latitude, longitude);
    }
}

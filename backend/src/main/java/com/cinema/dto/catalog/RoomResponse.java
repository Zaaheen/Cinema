package com.cinema.dto.catalog;

import com.cinema.entity.Room;

public record RoomResponse(Long id, String name, String status) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getStatus());
    }
}

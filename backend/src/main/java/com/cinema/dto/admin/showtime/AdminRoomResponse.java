package com.cinema.dto.admin.showtime;

import com.cinema.entity.Room;

public record AdminRoomResponse(Long id, String name, String status) {
    public static AdminRoomResponse from(Room room) {
        return new AdminRoomResponse(room.getId(), room.getName(), room.getStatus());
    }
}

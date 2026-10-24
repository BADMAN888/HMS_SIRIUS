package com.sirius.sirius.store.enums;

public enum RoomStatus {
    AVAILABLE,        // кімната вільна, готова для бронювання
    OCCUPIED,         // кімната зайнята гостем
    RESERVED,         // кімната заброньована, але ще не заселена
    CLEANING,         // кімната на прибиранні
    DIRTY
}

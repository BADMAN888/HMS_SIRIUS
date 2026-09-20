package com.sirius.sirius.store.enums;

public enum RoomStatus {
    AVAILABLE,        // кімната вільна, готова для бронювання
    OCCUPIED,         // кімната зайнята гостем
    RESERVED,         // кімната заброньована, але ще не заселена
    CLEANING,         // кімната на прибиранні
    OUT_OF_SERVICE,   // кімната недоступна (наприклад, через ремонт)
    OUT_OF_ORDER      // кімната тимчасово виведена з експлуатації, не для гостей
}

package com.sirius.sirius.store.enums;

public enum PriceStatus {

    ACTIVE,      // ціна зараз використовується
    INACTIVE,    // вимкнена / не використовується
    SCHEDULED,   // запланована (ще не вступила в дію)
    EXPIRED      // застаріла ціна (період закінчився)
}

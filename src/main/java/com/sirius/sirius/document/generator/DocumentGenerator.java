package com.sirius.sirius.document.generator;

import com.sirius.sirius.document.dto.DocumentData;
import com.sirius.sirius.document.model.DocumentFormat;
import com.sirius.sirius.document.model.DocumentType;

public interface DocumentGenerator {
    DocumentFormat format();
    byte[] generate(DocumentType type, DocumentData data);
}

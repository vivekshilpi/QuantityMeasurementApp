package com.app.quantitymeasurement.dto;

import com.app.quantitymeasurement.model.OperationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "QuantityMeasurementDTO", description = "Structured response for quantity operations")
public class QuantityMeasurementDTO {

    private Long id;
    private Double thisValue;
    private String thisUnit;
    private String thisMeasurementType;
    private Double thatValue;
    private String thatUnit;
    private String thatMeasurementType;
    private OperationType operationType;
    private String result;
    private boolean error;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static QuantityMeasurementDTO success(
            OperationType operationType,
            QuantityDTO thisQuantity,
            QuantityDTO thatQuantity,
            String result
    ) {
        return new QuantityMeasurementDTO(
                null,
                thisQuantity == null ? null : thisQuantity.getValue(),
                thisQuantity == null ? null : thisQuantity.getUnit(),
                thisQuantity == null ? null : thisQuantity.getMeasurementType(),
                thatQuantity == null ? null : thatQuantity.getValue(),
                thatQuantity == null ? null : thatQuantity.getUnit(),
                thatQuantity == null ? null : thatQuantity.getMeasurementType(),
                operationType,
                result,
                false,
                null,
                null
        );
    }

    public static QuantityMeasurementDTO failure(
            OperationType operationType,
            QuantityDTO thisQuantity,
            QuantityDTO thatQuantity,
            String errorMessage
    ) {
        return new QuantityMeasurementDTO(
                null,
                thisQuantity == null ? null : thisQuantity.getValue(),
                thisQuantity == null ? null : thisQuantity.getUnit(),
                thisQuantity == null ? null : thisQuantity.getMeasurementType(),
                thatQuantity == null ? null : thatQuantity.getValue(),
                thatQuantity == null ? null : thatQuantity.getUnit(),
                thatQuantity == null ? null : thatQuantity.getMeasurementType(),
                operationType,
                null,
                true,
                errorMessage,
                null
        );
    }
}

package akka.price;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public final class GetPriceRequest {

    private final String objectName;
}

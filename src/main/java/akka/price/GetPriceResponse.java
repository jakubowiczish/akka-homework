package akka.price;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
@AllArgsConstructor
public final class GetPriceResponse {

    private final String objectName;
    private final long price;

    @Setter
    private long queriesCounter;

    public final boolean isPriceValid() {
        return price > 0;
    }

    public static GetPriceResponse chooseLowerPriceResponse(Object o1, Object o2) {
        final GetPriceResponse firstResponse = (GetPriceResponse) o1;
        final GetPriceResponse secondResponse = (GetPriceResponse) o2;
        return firstResponse.getPrice() < secondResponse.getPrice()
                ? firstResponse
                : secondResponse;
    }

    public static GetPriceResponse toGetPriceResponse(Object objectResponse) {
        return (GetPriceResponse) objectResponse;
    }


}

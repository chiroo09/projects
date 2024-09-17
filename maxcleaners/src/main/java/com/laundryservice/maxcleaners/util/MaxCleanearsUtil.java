package com.laundryservice.maxcleaners.util;

import com.laundryservice.maxcleaners.constant.enums.OrderType;
import com.laundryservice.maxcleaners.model.Item;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Author Tejesh
 */
@Component
public class MaxCleanearsUtil {
    public BigDecimal calculateTotalPrice(List<Item> items, OrderType orderType) {
        BigDecimal totalPrice = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Add $1 per item if the order is Express
        if (orderType == OrderType.EXPRESS) {
            totalPrice = totalPrice.add(BigDecimal.valueOf(items.size()));
        }

        return totalPrice;
    }

}

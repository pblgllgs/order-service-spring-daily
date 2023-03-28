package com.pblgllgs.OrderService.service;

import com.pblgllgs.OrderService.model.OrderRequest;
import com.pblgllgs.OrderService.model.OrderResponse;

public interface OrderService {
    long placeOrder(OrderRequest orderRequest);

    OrderResponse getOrderDetails(long orderId);
}

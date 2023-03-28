package com.pblgllgs.OrderService.service;


import com.pblgllgs.OrderService.entity.Order;
import com.pblgllgs.OrderService.exception.CustomException;
import com.pblgllgs.OrderService.external.client.PaymentService;
import com.pblgllgs.OrderService.external.client.ProductService;
import com.pblgllgs.OrderService.external.response.PaymentResponse;
import com.pblgllgs.OrderService.model.OrderResponse;
import com.pblgllgs.OrderService.model.PaymentMode;
import com.pblgllgs.OrderService.model.ProductResponse;
import com.pblgllgs.OrderService.repository.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    OrderService orderService = new OrderServiceImpl(orderRepository, productService, paymentService, restTemplate);

    @Test
    @DisplayName("Get Order - Success Scenario")
    void test_When_Order_Success(){
        //Mocking
        Order order = getMockOrder();

        when(orderRepository.findById(anyLong())).
                thenReturn(Optional.of(order));

        when(restTemplate.getForObject(
                "http://PRODUCT-SERVICE/product/"+ order.getProductId(),
                ProductResponse.class
        )).thenReturn(getMockProductResponse());

        when(restTemplate.getForObject(
                "http://PAYMENT-SERVICE/payment/order/"+ order.getId(),
                PaymentResponse.class
        )).thenReturn(getMockPaymentResponse());

        //Actual
        OrderResponse orderResponse = orderService.getOrderDetails(1);

        //Verification
        verify(orderRepository, times(1)).findById(anyLong());

        verify(restTemplate, times(1)).getForObject(
                "http://PRODUCT-SERVICE/product/"+ order.getProductId(),
                ProductResponse.class);

        verify(restTemplate, times(1)).getForObject(
                "http://PAYMENT-SERVICE/payment/order/"+ order.getId(),
                PaymentResponse.class);

        //Assert
        assertNotNull(orderResponse);
        assertEquals(order.getId(), orderResponse.getOrderId());
    }

    @Test
    @DisplayName("Get Order - Failure Scenario")
    void test_When_Get_Order_NOT_FOUND_then_Not_Found(){
        when(orderRepository.findById(anyLong()))
                .thenReturn(Optional.ofNullable(null));
        CustomException exception = assertThrows(CustomException.class,
                () -> orderService.getOrderDetails(1));
        assertEquals("NOT_FOUND", exception.getErrorCode());
        assertEquals(404, exception.getStatus());
        verify(orderRepository, times(1)).findById(anyLong());
    }

    private PaymentResponse getMockPaymentResponse() {
        return PaymentResponse.builder()
                .paymentId(1)
                .paymentDate(Instant.now())
                .paymentMode(PaymentMode.CASH)
                .amount(200)
                .orderId(1)
                .status("ACCEPTED")
                .build();
    }

    private ProductResponse getMockProductResponse() {
        return ProductResponse.builder()
                .productId(2)
                .productName("iphone")
                .price(100)
                .quantity(200)
                .build();
    }

    private Order getMockOrder() {
        return Order.builder()
                .orderStatus("PLACED")
                .orderDate(Instant.now())
                .id(1)
                .amount(100)
                .quantity(200)
                .productId(2)
                .build();
    }
}
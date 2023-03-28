package com.pblgllgs.OrderService.service;

import com.pblgllgs.OrderService.entity.Order;
import com.pblgllgs.OrderService.exception.CustomException;
import com.pblgllgs.OrderService.external.client.PaymentService;
import com.pblgllgs.OrderService.external.client.ProductService;
import com.pblgllgs.OrderService.external.request.PaymentRequest;
import com.pblgllgs.OrderService.external.response.PaymentResponse;
import com.pblgllgs.OrderService.model.OrderRequest;
import com.pblgllgs.OrderService.model.OrderResponse;
import com.pblgllgs.OrderService.model.ProductResponse;
import com.pblgllgs.OrderService.repository.OrderRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Log4j2
@AllArgsConstructor
public class OrderServiceImpl implements OrderService{

    private OrderRepository orderRepository;

    private ProductService productService;

    private PaymentService paymentService;

    private RestTemplate restTemplate;
    @Override
    public long placeOrder(OrderRequest orderRequest) {
        log.info("Placing Order Request {}", orderRequest);
        productService.reduceQuantity(
                orderRequest.getProductId(),
                orderRequest.getQuantity()
        );
        log.info("Creating Order wit status CREATED");
        Order order = Order.builder()
                .amount(orderRequest.getTotalAmount())
                .orderStatus("CREATED")
                .productId(orderRequest.getProductId())
                .orderDate(Instant.now())
                .quantity(orderRequest.getQuantity())
                .build();
        order =  orderRepository.save(order);
        log.info("Calling payment service to complete the payment");
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMode(orderRequest.getPaymentMode())
                .amount(orderRequest.getTotalAmount())
                .build();

        String orderStatus = null;
        try{
            paymentService.doPayment(paymentRequest);
            log.info("Payment successfully. Changing the order status");
            orderStatus ="PLACED";
        }catch (Exception e){
            log.error("Error occurred in payment. Changing order status to PAYMENT_FAILED");
            orderStatus = "PAYMENT_FAILED";
        }

        order.setOrderStatus(orderStatus);
        orderRepository.save(order);

        log.info("Order Places successfully with order id {}", order.getId());
        return order.getId();
    }

    @Override
    public OrderResponse getOrderDetails(long orderId) {
        log.info("Get order details for order id {}", orderId);
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new CustomException("Order not found for the orderId "+ orderId,"NOT_FOUND", 404));
        log.info("Invoking Product service to fetch the product details for order id: {}",orderId);
        ProductResponse productResponse = restTemplate.getForObject(
                "http://PRODUCT-SERVICE/product/"+ order.getProductId(),
                ProductResponse.class);

        log.info("Getting payment information for the payment service");

        PaymentResponse paymentResponse = restTemplate.getForObject(
                "http://PAYMENT-SERVICE/payment/order/"+ order.getId(),
                PaymentResponse.class

        );

        OrderResponse.ProductDetails productDetails =  OrderResponse.ProductDetails.builder()
                .productName(productResponse.getProductName())
                .productId(productResponse.getProductId())
                .build();

        OrderResponse.PaymentDetails paymentDetails = OrderResponse.PaymentDetails.builder()
                .paymentId(paymentResponse.getPaymentId())
                .paymentStatus(paymentResponse.getStatus())
                .paymentDate(paymentResponse.getPaymentDate())
                .paymentMode(paymentResponse.getPaymentMode())
                .build();

        OrderResponse orderResponse = OrderResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .productDetails(productDetails)
                .amount(order.getAmount())
                .paymentDetails(paymentDetails)
                .build();
        return orderResponse;
    }
}

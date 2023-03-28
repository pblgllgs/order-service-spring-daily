package com.pblgllgs.OrderService.controller;

import com.pblgllgs.OrderService.model.OrderRequest;
import com.pblgllgs.OrderService.model.OrderResponse;
import com.pblgllgs.OrderService.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@Log4j2
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/placeOrder")
    @PreAuthorize("hasAuthority('Customer')")
    public ResponseEntity<Long> placeOrder(@RequestBody OrderRequest orderRequest){
        long orderId = orderService.placeOrder(orderRequest);
        log.info("Order id: {}", orderId);
        return new ResponseEntity<>(orderId, HttpStatus.OK);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('Admin') || hasAuthority('Customer')")
    public ResponseEntity<OrderResponse> getOrderDetails(@PathVariable("orderId") long orderId){
        log.info("Getting the order with id: {}", orderId);
        return new ResponseEntity<>(orderService.getOrderDetails(orderId), HttpStatus.OK);
    }

}

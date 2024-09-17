package com.laundryservice.maxcleaners.controller;

import com.laundryservice.maxcleaners.config.JwtUtil;
import com.laundryservice.maxcleaners.constant.enums.OrderStatus;
import com.laundryservice.maxcleaners.dto.LoginRequest;
import com.laundryservice.maxcleaners.dto.OrderRequest;
import com.laundryservice.maxcleaners.dto.OrderUpdateRequest;
import com.laundryservice.maxcleaners.dto.SignupRequest;
import com.laundryservice.maxcleaners.exception.*;
import com.laundryservice.maxcleaners.model.Customer;
import com.laundryservice.maxcleaners.model.Order;
import com.laundryservice.maxcleaners.model.Response;
import com.laundryservice.maxcleaners.repository.OrderRepository;
import com.laundryservice.maxcleaners.service.CustomerService;
import com.laundryservice.maxcleaners.service.OrderService;
import com.laundryservice.maxcleaners.util.CheckAvailabilityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import com.laundryservice.maxcleaners.model.Item;

/**
 * Author Tejesh
 */
@RestController
@RequestMapping("/maxcleaners")
public class MaxCleanerController {

    private static final Logger logger = LoggerFactory.getLogger(MaxCleanerController.class);

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtTokenUtil;

    @Autowired
    private CheckAvailabilityUtil checkAvailabilityUtil;

    @Autowired
    private OrderRepository orderRepository;

    @Value("${spring.admin.phone}")
    private String adminPhoneNumber;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");


    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        logger.info("Received signup request for phone number: {}", signupRequest.getPhoneNumber());
        Customer customer = customerService.signup(signupRequest);
        String token = jwtTokenUtil.generateToken(customer);
        logger.info("Signup successful for phone number: {}", signupRequest.getPhoneNumber());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Signup successful");
        response.put("token", token);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<Map<String, String>> signin(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Received signin request for phone number: {}", loginRequest.getPhoneNumber());
        Customer customer = customerService.login(loginRequest);
        String token = jwtTokenUtil.generateToken(customer);
        logger.info("Signin successful for phone number: {}", loginRequest.getPhoneNumber());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Signin successful");
        response.put("token", token);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/signout")
    public ResponseEntity<Map<String, String>> signout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        Map<String, String> response = new HashMap<>();

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // Remove "Bearer " prefix

            if (jwtTokenUtil.isTokenBlacklisted(token)) {
                response.put("message", "Token already signed out");
                return ResponseEntity.badRequest().body(response);
            }

            jwtTokenUtil.blacklistToken(token);
            response.put("message", "Sign-out successful");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "No token provided");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/checkAddressAvailability")
    public ResponseEntity<Response> validAddressToServe(@RequestParam("address") String address) throws Exception {
        ResponseEntity<Response> response = null;
        Response res = null;
        try {
            logger.info("validAddressToServe revcieved address::" + address);
            res = checkAvailabilityUtil.valiadeAddressToServe(address);
            if (res.getServiceStatus()) {
                response = new ResponseEntity<>(res, HttpStatus.OK);
            } else {
                response = new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            // TODO: handle exception
            res = new Response();
            res.setMessage(e.getMessage());
            response = new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
        }

        return response;

    }

    /**
     * Create a new order
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody OrderRequest orderRequest, HttpServletRequest request) {
        try {
            // Extract token from request header
           String token= extractToken(request);
            // Extract phone number from token
            String phoneNumber = jwtTokenUtil.extractPhoneNumber(token);
            Customer customer = customerService.findByPhoneNumber(phoneNumber);

            if (customer == null) {
                throw new UnauthorizedException("Customer not found for phone number: " + phoneNumber);
            }

            // Get pickup date directly from orderRequest
            LocalDateTime pickupDate = orderRequest.getOrder().getPickupDate();
            if (pickupDate == null) {
                throw new InvalidRequestException("Pickup date is required.");
            }

            // Prepare the order with pickup date
            Order order = orderRequest.getOrder();
            order.setPickupDate(pickupDate);

            // Create the order
            Order newOrder = orderService.createOrder(order, customer.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Order created successfully");
            response.put("order", newOrder);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to create order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }


    /**
     * Get order by ID
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderById(@PathVariable("orderId") Long orderId) {
        try {
            Order order = orderService.getOrder(orderId);

            if (order == null) {
                throw new ResourceNotFoundException("Order not found with ID: " + orderId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Order found");
            response.put("order", order);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error fetching order: " + e.getMessage());
        }
    }

    /**
     * Update an existing order with price, items, and status.
     */
    @PutMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> updateOrder(@PathVariable Long orderId,
                                                           @RequestBody OrderUpdateRequest updateRequest,
                                                           HttpServletRequest request) {
        try {
            // Extract the token from the request header
            String token = extractToken(request);

            // Extract phone number from the token
            String phoneNumber = jwtTokenUtil.extractPhoneNumber(token);
            logger.info("Extracted phone number from token: {}", phoneNumber);

            // Check if the phone number matches the admin's phone number
            if (!phoneNumber.equals(adminPhoneNumber)) {
                logger.warn("Access denied for phone number: {}", phoneNumber);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Access denied: Only admin can update the order");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Log the incoming update request
            logger.info("Received request to update order with ID: {}", orderId);
            logger.debug("Update request details: {}", updateRequest);

            // Update the order
            Order updatedOrder = orderService.updateOrder(orderId, updateRequest);

            // Return the response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Order updated successfully");
            response.put("order", updatedOrder);

            return ResponseEntity.ok(response);
        } catch (OrderNotFoundException e) {
            logger.error("OrderNotFoundException: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Order not found: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (OrderAlreadyUpdatedException e) {
            logger.error("OrderAlreadyUpdatedException: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Order is already up-to-date: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        } catch (InvalidRequestException e) {
            logger.error("InvalidRequestException: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error occurred while updating order: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    /**
     * retrive all the orders based on filter
     */
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> retriveOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable,
            HttpServletRequest request) {

        try {
            // Extract the token from the request header
            String token = extractToken(request);

            // Extract phone number from the token
            String phoneNumber = jwtTokenUtil.extractPhoneNumber(token);
            logger.info("Extracted phone number from token: {}", phoneNumber);

            // Check if the phone number matches the admin's phone number
            if (!phoneNumber.equals(adminPhoneNumber)) {
                logger.warn("Access denied for phone number: {}", phoneNumber);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Access denied: Only admin can view orders");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Log the incoming request
            logger.info("Received request to fetch orders with filters - Start Date: {}, End Date: {}, City: {}", startDate, endDate, city);

            // Fetch orders
            Page<Order> orders = orderService.retriveOrders(startDate, endDate, city, status, pageable);

            if (orders.isEmpty()) {
                logger.warn("No orders found for the given filters - Start Date: {}, End Date: {}, City: {}", startDate, endDate, city);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "No orders found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Return the response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Orders fetched successfully");
            response.put("orders", orders);

            return ResponseEntity.ok(response);
        } catch (UnauthorizedException e) {
            logger.error("Unauthorized access: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Unauthorized access: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (OrderNotFoundException e) {
            logger.error("OrderNotFoundException occurred: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "No orders found: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (InvalidRequestException e) {
            logger.error("InvalidRequestException occurred: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid request parameters: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error occurred while fetching orders: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        throw new UnauthorizedException("Token is missing or invalid");
    }


}

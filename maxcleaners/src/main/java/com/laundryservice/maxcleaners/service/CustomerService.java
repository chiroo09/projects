package com.laundryservice.maxcleaners.service;

import com.laundryservice.maxcleaners.dto.LoginRequest;
import com.laundryservice.maxcleaners.dto.SignupRequest;
import com.laundryservice.maxcleaners.exception.CustomException;
import com.laundryservice.maxcleaners.model.Customer;
import com.laundryservice.maxcleaners.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;


/**
 * Author Tejesh
 */
@Service
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CustomerService(CustomerRepository customerRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Customer signup(SignupRequest request) {
        logger.info("Attempting to sign up customer with phone number: {}", request.getPhoneNumber());

        if (customerRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            logger.error("Phone number {} is already registered", request.getPhoneNumber());
            throw new CustomException("Phone number already registered", HttpStatus.BAD_REQUEST.value());
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = (authentication != null && !"anonymousUser".equals(authentication.getName()))
                ? authentication.getName()
                : "system";

        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setAddress(request.getAddress());
        customer.setCreatedBy(createdBy);
        customer.setUpdatedBy(createdBy);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        Customer savedCustomer = customerRepository.save(customer);
        logger.info("Customer with phone number {} registered successfully", request.getPhoneNumber());

        return savedCustomer;
    }

    public Customer login(LoginRequest request) {
        logger.info("Attempting to log in customer with phone number: {}", request.getPhoneNumber());

        Customer customer = customerRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND.value()));

        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            logger.error("Invalid password for phone number: {}", request.getPhoneNumber());
            throw new CustomException("Invalid password", HttpStatus.UNAUTHORIZED.value());
        }

        logger.info("Customer with phone number {} logged in successfully", request.getPhoneNumber());
        return customer;
    }

    public Customer findByPhoneNumber(String phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND.value()));
    }
}

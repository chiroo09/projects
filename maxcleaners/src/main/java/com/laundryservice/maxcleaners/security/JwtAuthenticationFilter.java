package com.laundryservice.maxcleaners.security;

import com.laundryservice.maxcleaners.model.Customer;
import com.laundryservice.maxcleaners.service.CustomerService;
import com.laundryservice.maxcleaners.config.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Author Tejesh
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    @Lazy
    private CustomerService customerService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            // Extract the token from the request header
            String token = extractTokenFromRequest(request);
            logger.debug("Extracted token: {}", token);

            if (token != null && jwtUtil.isTokenValid(token, jwtUtil.extractPhoneNumber(token))) {
                // Extract phone number from the token
                String phoneNumber = jwtUtil.extractPhoneNumber(token);
                logger.debug("Extracted phone number: {}", phoneNumber);

                // Find the customer by phone number
                Customer customer = customerService.findByPhoneNumber(phoneNumber);
                logger.debug("Found customer: {}", customer);

                // Create authentication object
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        phoneNumber,
                        null,
                        new ArrayList<>()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                logger.info("Successfully authenticated customer with phone number: {}", phoneNumber);
            }
        } catch (Exception e) {
            // Log the exception with full stack trace for debugging
            logger.error("Error occurred during JWT processing: {}", e.getMessage(), e);

            // Set HTTP status and custom error message (optional)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired token");
            return;  // Terminate the request processing at this point
        }

        // Continue with the next filter in the chain
        chain.doFilter(request, response);
    }


    private String extractTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}

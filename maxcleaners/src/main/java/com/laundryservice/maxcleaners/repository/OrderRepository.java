package com.laundryservice.maxcleaners.repository;

import com.laundryservice.maxcleaners.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

/**
 * Author Tejesh
 */
public interface OrderRepository extends JpaRepository<Order, Long>,OrderCustomRepository {
}

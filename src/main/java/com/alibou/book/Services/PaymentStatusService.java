package com.alibou.book.Services;

import com.alibou.book.DTO.PaymentStatusResponseDTO;
import com.alibou.book.DTO.Projections.*;
import com.alibou.book.DTO.UserSummaryDTOs;
import com.alibou.book.Entity.PaymentStatuss;
import com.alibou.book.Repositories.PaymentStatusRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PaymentStatusService {

    private final PaymentStatusRepository paymentStatusRepository;
    private final UserDetailsService userDetailsService;



    public PaymentStatusService(PaymentStatusRepository paymentStatusRepository, UserDetailsService userDetailsService) {
        this.paymentStatusRepository = paymentStatusRepository;
        this.userDetailsService = userDetailsService;
    }



    public PaymentStatuss save(PaymentStatuss status) {
        return paymentStatusRepository.save(status);
    }

    public List<PaymentStatuss> findAll() {
        return paymentStatusRepository.findAll();
    }

    public Optional<PaymentStatuss> findById(Long id) {
        return paymentStatusRepository.findById(id);
    }

    public Optional<PaymentStatuss> findByExternalRef(String externalRef) {
        return paymentStatusRepository.findByExternalRef(externalRef);
    }

    public void deleteById(Long id) {
        paymentStatusRepository.deleteById(id);
    }



    //payment For Farmer

    public List<PaymentStatusResponseDTO> paymentForFarmer(Long userId) {
        List<PaymentStatuss> payments = paymentStatusRepository.findByUser_Id(userId);

        return payments.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    private PaymentStatusResponseDTO mapToDto(PaymentStatuss entity) {
        PaymentStatusResponseDTO dto = new PaymentStatusResponseDTO();
        dto.setId(entity.getId());
        dto.setTxStatus(entity.getTxStatus());
        dto.setPayer(entity.getPayer());
        dto.setPayee(entity.getPayee());
        dto.setAmount(entity.getAmount());
        dto.setValue(entity.getValue());
        dto.setTransactionId(entity.getTransactionId());
        dto.setExternalRef(entity.getExternalRef());
        dto.setThirdPartyRef(entity.getThirdPartyRef());
        dto.setSecret(entity.getSecret());
        dto.setTimestamp(entity.getTimestamp());

        // Map Customer
//        if (entity.getCustomer() != null) {
//            UserSummaryDTOs customer = new UserSummaryDTOs();
//            customer.setId(Long.valueOf(entity.getCustomer().getId()));
//            customer.setFirstname(entity.getCustomer().getFirstname());
//            customer.setLastname(entity.getCustomer().getLastname());
//            customer.setPhoneNummber(entity.getCustomer().getPhoneNummber());
//            customer.setFullName(entity.getCustomer().getFullName());
//
//
//
//            dto.setCustomer(customer);
//        }

        return dto;
    }




    public Double getTotalRevenue() {
        Double total = paymentStatusRepository.calculateTotalRevenue();
        return total != null ? total : 0.0;
    }

    public Double getRevenueByUser(Long userId) {
        Double userRevenue = paymentStatusRepository.calculateRevenueByUser(userId);
        return userRevenue != null ? userRevenue : 0.0;
    }

    public Double getRevenueBetweenDates(LocalDateTime start, LocalDateTime end) {
        Double periodRevenue = paymentStatusRepository.calculateRevenueBetweenDates(start, end);
        return periodRevenue != null ? periodRevenue : 0.0;
    }









    // Daily Payment Summary
    public List<DailyPaymentSummary> getDailyPaymentSummary(
            LocalDate startDate,
            LocalDate endDate,
            int paymentStatus) {

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        return paymentStatusRepository.getDailyPaymentSummary(
                start, end, paymentStatus);
    }

    // Weekly Payment Stats
    public List<WeeklyPaymentStats> getWeeklyPaymentStats(
            LocalDate startDate,
            LocalDate endDate) {

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        return paymentStatusRepository.getWeeklyPaymentStats(start, end);
    }

    // Monthly User Payment Summary
//    public List<UserMonthlyPaymentSummary> getUserMonthlyPaymentSummary(
//            int year,
//            int paymentStatus) {
//
//        return paymentStatusRepository.getUserMonthlyPaymentSummary(
//                year, paymentStatus);
//    }

    // Find Payment by Transaction and User
    public PaymentStatuss getPaymentByTransactionAndUser(
            long transactionId,
            Long userId) {

        return paymentStatusRepository.findByTransactionIdAndUser(
                        transactionId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Payment not found with transactionId: " + transactionId +
                                " and userId: " + userId));
    }



    // Status codes (consider moving to enum)
    public static final int SUCCESSFUL_PAYMENT_STATUS = 1;
    public static final int FAILED_PAYMENT_STATUS = 0;

//    public List<WeeklyRevenueSummary> getWeeklyRevenue(int year, int month) {
//        // Validate month range
//        if (month < 1 || month > 12) {
//            throw new IllegalArgumentException("Month must be between 1 and 12");
//        }
//
//        return paymentStatusRepository.findWeeklyRevenueByMonth(
//                year,
//                month,
//                SUCCESSFUL_PAYMENT_STATUS);
//    }



    public List<WeeklyRevenueSummary> getWeeklyRevenue(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        // Validate month range
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        return paymentStatusRepository.findWeeklyRevenueByMonth(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX),
                SUCCESSFUL_PAYMENT_STATUS);
    }




    public List<DailyPaymentSummary> getDailyPaymentsSummary(int year, int month, int statusCode) {
            LocalDate startDate = LocalDate.of(year, month, 1);
    LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return paymentStatusRepository.getDailyPaymentTotals(
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX),
                statusCode
        );
    }

    public List<MonthlyRevenueSummary> getMonthlyRevenue(int year) {
        int successStatusCode = 1; // Your successful payment status code
        return paymentStatusRepository.getMonthlyRevenue(year, successStatusCode);
    }
}


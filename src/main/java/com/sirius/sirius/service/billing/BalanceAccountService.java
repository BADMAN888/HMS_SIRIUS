package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.BalanceAccountResponse;
import com.sirius.sirius.dto.BalanceTransactionResponse;
import com.sirius.sirius.mappers.BalanceAccountMapper;
import com.sirius.sirius.mappers.BalanceTransactionMapper;
import com.sirius.sirius.store.entity.biling.BalanceAccountEntity;
import com.sirius.sirius.store.entity.biling.BalanceTransactionEntity;
import com.sirius.sirius.store.entity.biling.CustomerEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.biling.PaymentEntity;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.enums.BalanceAccountStatus;
import com.sirius.sirius.store.enums.BalanceTransactionType;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.enums.PaymentMethod;
import com.sirius.sirius.store.repository.BalanceAccountRepository;
import com.sirius.sirius.store.repository.BalanceTransactionRepository;
import com.sirius.sirius.store.repository.CustomerRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.HotelRepository;
import com.sirius.sirius.store.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BalanceAccountService {

    private final BalanceAccountRepository balanceAccountRepository;
    private final BalanceTransactionRepository balanceTransactionRepository;
    private final CustomerRepository customerRepository;
    private final HotelRepository hotelRepository;
    private final FolioRepository folioRepository;
    private final PaymentRepository paymentRepository;

    private final BalanceAccountMapper balanceAccountMapper;
    private final BalanceTransactionMapper balanceTransactionMapper;

    private final Clock clock;

    @Transactional(readOnly = true)
    public BalanceAccountResponse getByCustomerAndHotel(
            Long customerId,
            Long hotelId
    ) {
        BalanceAccountEntity account = balanceAccountRepository
                .findByCustomerIdAndHotelId(customerId, hotelId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Balance account not found for customer " + customerId
                                + " and hotel " + hotelId
                ));

        return balanceAccountMapper.toResponse(account);
    }

    @Transactional
    public BalanceAccountResponse getOrCreate(
            Long customerId,
            Long hotelId
    ) {
        BalanceAccountEntity account = balanceAccountRepository
                .findByCustomerIdAndHotelId(customerId, hotelId)
                .orElseGet(() -> createAccount(customerId, hotelId));

        return balanceAccountMapper.toResponse(account);
    }

    @Transactional
    public BalanceAccountResponse deposit(
            Long customerId,
            Long hotelId,
            BigDecimal amount,
            String description
    ) {
        validateAmount(amount);

        BalanceAccountEntity account = getOrCreateEntity(customerId, hotelId);
        validateActive(account);

        account.setBalance(account.getBalance().add(amount));

        balanceAccountRepository.save(account);

        createTransaction(
                account,
                BalanceTransactionType.DEPOSIT,
                amount,
                description,
                null
        );

        return balanceAccountMapper.toResponse(account);
    }

    @Transactional
    public BalanceAccountResponse refund(
            Long accountId,
            BigDecimal amount,
            String description
    ) {
        validateAmount(amount);

        BalanceAccountEntity account = getAccountForUpdate(accountId);
        validateActive(account);

        account.setBalance(account.getBalance().add(amount));

        balanceAccountRepository.save(account);

        createTransaction(
                account,
                BalanceTransactionType.REFUND,
                amount,
                description,
                null
        );

        return balanceAccountMapper.toResponse(account);
    }

    @Transactional
    public BalanceAccountResponse debit(
            Long accountId,
            BigDecimal amount,
            String description
    ) {
        validateAmount(amount);

        BalanceAccountEntity account = getAccountForUpdate(accountId);
        validateActive(account);
        validateSufficientBalance(account, amount);

        account.setBalance(account.getBalance().subtract(amount));

        balanceAccountRepository.save(account);

        createTransaction(
                account,
                BalanceTransactionType.ADJUSTMENT_DEBIT,
                amount,
                description,
                null
        );

        return balanceAccountMapper.toResponse(account);
    }

    @Transactional
    public BalanceAccountResponse payFolio(
            Long accountId,
            Long folioId,
            BigDecimal amount,
            String description
    ) {
        validateAmount(amount);

        BalanceAccountEntity account = getAccountForUpdate(accountId);
        FolioEntity folio = folioRepository.findByIdForUpdate(folioId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Folio not found with id: " + folioId
                ));

        validateActive(account);
        validateOpenFolio(folio);
        validateSameHotel(account, folio);
        validateSufficientBalance(account, amount);

        account.setBalance(account.getBalance().subtract(amount));

        PaymentEntity payment = PaymentEntity.builder()
                .folio(folio)
                .method(PaymentMethod.BALANCE)
                .amount(amount)
                .description(description)
                .paidAt(LocalDateTime.now(clock))
                .build();

        folio.setPaidAmount(folio.getPaidAmount().add(amount));

        paymentRepository.save(payment);
        folio.getPayments().add(payment);
        folioRepository.save(folio);

        balanceAccountRepository.save(account);

        createTransaction(
                account,
                BalanceTransactionType.FOLIO_PAYMENT,
                amount,
                description,
                folio
        );

        return balanceAccountMapper.toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<BalanceTransactionResponse> getTransactions(Long accountId) {
        if (!balanceAccountRepository.existsById(accountId)) {
            throw new EntityNotFoundException(
                    "Balance account not found with id: " + accountId
            );
        }

        return balanceTransactionRepository
                .findAllByBalanceAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(balanceTransactionMapper::toResponse)
                .toList();
    }

    private BalanceAccountEntity getOrCreateEntity(
            Long customerId,
            Long hotelId
    ) {
        return balanceAccountRepository
                .findByCustomerIdAndHotelId(customerId, hotelId)
                .orElseGet(() -> createAccount(customerId, hotelId));
    }

    private BalanceAccountEntity createAccount(
            Long customerId,
            Long hotelId
    ) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer not found with id: " + customerId
                ));

        HotelEntity hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Hotel not found with id: " + hotelId
                ));

        BalanceAccountEntity account = new BalanceAccountEntity();
        account.setCustomer(customer);
        account.setHotel(hotel);
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency("UAH");
        account.setStatus(BalanceAccountStatus.ACTIVE);

        return balanceAccountRepository.save(account);
    }

    private BalanceAccountEntity getAccountForUpdate(Long accountId) {
        return balanceAccountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Balance account not found with id: " + accountId
                ));
    }

    private void createTransaction(
            BalanceAccountEntity account,
            BalanceTransactionType type,
            BigDecimal amount,
            String description,
            FolioEntity folio
    ) {
        BalanceTransactionEntity transaction = new BalanceTransactionEntity();

        transaction.setBalanceAccount(account);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(account.getBalance());
        transaction.setDescription(description);
        transaction.setCreatedAt(LocalDateTime.now(clock));

        if (folio != null) {
            transaction.setFolio(folio);
            transaction.setReservation(folio.getReservation());
        }

        balanceTransactionRepository.save(transaction);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than 2 decimal places"
            );
        }
    }

    private void validateActive(BalanceAccountEntity account) {
        if (account.getStatus() != BalanceAccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Balance account is not active"
            );
        }
    }

    private void validateOpenFolio(FolioEntity folio) {
        if (folio.getStatus() != FolioStatus.OPEN) {
            throw new IllegalStateException(
                    "Cannot pay a closed folio"
            );
        }
    }

    private void validateSameHotel(
            BalanceAccountEntity account,
            FolioEntity folio
    ) {
        if (!account.getHotel().getId().equals(folio.getHotel().getId())) {
            throw new IllegalArgumentException(
                    "Balance account and folio belong to different hotels"
            );
        }
    }

    private void validateSufficientBalance(
            BalanceAccountEntity account,
            BigDecimal amount
    ) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Insufficient balance"
            );
        }
    }
}
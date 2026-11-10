package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.BalanceAccountResponse;
import com.sirius.sirius.dto.BalanceTransactionResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.mappers.BalanceAccountMapper;
import com.sirius.sirius.mappers.BalanceTransactionMapper;
import com.sirius.sirius.store.entity.biling.BalanceAccountEntity;
import com.sirius.sirius.store.entity.biling.BalanceTransactionEntity;
import com.sirius.sirius.store.entity.biling.FolioEntity;
import com.sirius.sirius.store.entity.biling.PaymentEntity;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.enums.BalanceAccountStatus;
import com.sirius.sirius.store.enums.BalanceTransactionType;
import com.sirius.sirius.store.enums.FolioStatus;
import com.sirius.sirius.store.enums.PaymentMethod;
import com.sirius.sirius.store.repository.BalanceAccountRepository;
import com.sirius.sirius.store.repository.BalanceTransactionRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.HotelRepository;
import com.sirius.sirius.store.repository.PaymentRepository;
import com.sirius.sirius.store.repository.ProfileRepository;
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
    private final ProfileRepository profileRepository;
    private final HotelRepository hotelRepository;
    private final FolioRepository folioRepository;
    private final PaymentRepository paymentRepository;

    private final BalanceAccountMapper balanceAccountMapper;
    private final BalanceTransactionMapper balanceTransactionMapper;

    private final Clock clock;

    @Transactional(readOnly = true)
    public BalanceAccountResponse getByProfileAndHotel(
            Long profileId,
            Long hotelId
    ) {
        validateProfileExists(profileId);

        BalanceAccountEntity account = balanceAccountRepository
                .findByCustomerIdAndHotelId(profileId, hotelId)
                .orElseThrow(() -> new NotFoundException(
                        "Balance account not found for profile "
                                + profileId + " and hotel " + hotelId
                ));

        return balanceAccountMapper.toResponse(account);
    }

    @Transactional
    public BalanceAccountResponse getOrCreate(
            Long profileId,
            Long hotelId
    ) {
        ProfileEntity profile = profileRepository
                .findByIdForUpdate(profileId)
                .orElseThrow(() -> new NotFoundException(
                        "Profile not found with id: " + profileId
                ));

        HotelEntity hotel = findHotel(hotelId);

        BalanceAccountEntity account = balanceAccountRepository
                .findByCustomerIdAndHotelId(profileId, hotelId)
                .orElseGet(() -> createAccount(profile, hotel));

        return balanceAccountMapper.toResponse(account);
    }

    @Transactional
    public BalanceAccountResponse deposit(
            Long profileId,
            Long hotelId,
            BigDecimal amount,
            String description
    ) {
        validateAmount(amount);

        ProfileEntity profile = profileRepository
                .findByIdForUpdate(profileId)
                .orElseThrow(() -> new NotFoundException(
                        "Profile not found with id: " + profileId
                ));

        HotelEntity hotel = findHotel(hotelId);

        BalanceAccountEntity account = balanceAccountRepository
                .findByProfileIdAndHotelIdForUpdate(profileId, hotelId)
                .orElseGet(() -> createAccount(profile, hotel));

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

        FolioEntity folio = folioRepository.findByIdForUpdate(folioId)
                .orElseThrow(() -> new NotFoundException(
                        "Folio not found with id: " + folioId
                ));

        BalanceAccountEntity account = getAccountForUpdate(accountId);

        validateActive(account);
        validateOpenFolio(folio);
        validateSameHotel(account, folio);
        validateFolioPayer(account, folio);
        validateSufficientBalance(account, amount);
        validateFolioBalance(folio, amount);

        account.setBalance(account.getBalance().subtract(amount));

        PaymentEntity payment = PaymentEntity.builder()
                .folio(folio)
                .method(PaymentMethod.BALANCE)
                .balanceAccount(account)
                .amount(amount)
                .description(description)
                .paidAt(LocalDateTime.now(clock))
                .build();

        PaymentEntity savedPayment = paymentRepository.save(payment);

        folio.addPayment(savedPayment);

        balanceAccountRepository.save(account);
        folioRepository.save(folio);

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
    public List<BalanceTransactionResponse> getTransactions(
            Long accountId
    ) {
        if (!balanceAccountRepository.existsById(accountId)) {
            throw new NotFoundException(
                    "Balance account not found with id: " + accountId
            );
        }

        return balanceTransactionRepository
                .findAllByBalanceAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(balanceTransactionMapper::toResponse)
                .toList();
    }

    private BalanceAccountEntity createAccount(
            ProfileEntity profile,
            HotelEntity hotel
    ) {
        BalanceAccountEntity account = new BalanceAccountEntity();

        account.setCustomer(profile);
        account.setHotel(hotel);
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency("UAH");
        account.setStatus(BalanceAccountStatus.ACTIVE);

        return balanceAccountRepository.save(account);
    }

    private ProfileEntity findProfile(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException(
                        "Profile not found with id: " + profileId
                ));
    }

    private void validateProfileExists(Long profileId) {
        if (!profileRepository.existsById(profileId)) {
            throw new NotFoundException(
                    "Profile not found with id: " + profileId
            );
        }
    }

    private HotelEntity findHotel(Long hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new NotFoundException(
                        "Hotel not found with id: " + hotelId
                ));
    }

    private BalanceAccountEntity getAccountForUpdate(Long accountId) {
        return balanceAccountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new NotFoundException(
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
        BalanceTransactionEntity transaction =
                new BalanceTransactionEntity();

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
            throw new ValidationException(
                    "Amount must be greater than zero"
            );
        }

        if (amount.stripTrailingZeros().scale() > 2) {
            throw new ValidationException(
                    "Amount cannot have more than 2 decimal places"
            );
        }
    }

    private void validateActive(BalanceAccountEntity account) {
        if (account.getStatus() != BalanceAccountStatus.ACTIVE) {
            throw new ConflictException(
                    "Balance account is not active"
            );
        }
    }

    private void validateOpenFolio(FolioEntity folio) {
        if (folio.getStatus() != FolioStatus.OPEN) {
            throw new ConflictException(
                    "Cannot pay a closed folio"
            );
        }
    }

    private void validateSameHotel(
            BalanceAccountEntity account,
            FolioEntity folio
    ) {
        if (!account.getHotel().getId().equals(folio.getHotel().getId())) {
            throw new ValidationException(
                    "Balance account and folio belong to different hotels"
            );
        }
    }

    private void validateFolioPayer(
            BalanceAccountEntity account,
            FolioEntity folio
    ) {
        if (folio.getPayer() == null) {
            throw new ValidationException(
                    "Folio payer is not specified"
            );
        }

        if (!(folio.getPayer() instanceof ProfileEntity)) {
            throw new ConflictException(
                    "Only a profile can pay a folio using profile advance"
            );
        }

        if (!folio.getPayer().getId()
                .equals(account.getCustomer().getId())) {
            throw new ConflictException(
                    "Balance account does not belong to the folio payer"
            );
        }
    }

    private void validateSufficientBalance(
            BalanceAccountEntity account,
            BigDecimal amount
    ) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new ConflictException(
                    "Insufficient balance"
            );
        }
    }

    private void validateFolioBalance(
            FolioEntity folio,
            BigDecimal amount
    ) {
        BigDecimal balance = folio.getBalance();

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException(
                    "Folio has no outstanding balance"
            );
        }

        if (amount.compareTo(balance) > 0) {
            throw new ConflictException(
                    "Payment amount exceeds the outstanding folio balance"
            );
        }
    }
}

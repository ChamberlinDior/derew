package com.oviro.service;

import com.oviro.dto.request.SavedAddressRequest;
import com.oviro.dto.response.SavedAddressResponse;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.SavedAddress;
import com.oviro.model.User;
import com.oviro.repository.SavedAddressRepository;
import com.oviro.repository.UserRepository;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final SavedAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public List<SavedAddressResponse> getMyAddresses() {
        UUID userId = securityContextHelper.getCurrentUserId();
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().map(SavedAddressResponse::from).toList();
    }

    @Transactional
    public SavedAddressResponse create(SavedAddressRequest request) {
        UUID userId = securityContextHelper.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));

        if (request.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        SavedAddress address = SavedAddress.builder()
                .user(user)
                .label(request.getLabel())
                .addressName(request.getAddressName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .type(request.getType())
                .isDefault(request.isDefault())
                .build();

        return SavedAddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public SavedAddressResponse update(UUID id, SavedAddressRequest request) {
        UUID userId = securityContextHelper.getCurrentUserId();
        SavedAddress address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Adresse", id.toString()));

        if (request.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        address.setLabel(request.getLabel());
        address.setAddressName(request.getAddressName());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setType(request.getType());
        address.setDefault(request.isDefault());

        return SavedAddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public void delete(UUID id) {
        UUID userId = securityContextHelper.getCurrentUserId();
        SavedAddress address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Adresse", id.toString()));
        addressRepository.delete(address);
    }

    @Transactional
    public SavedAddressResponse setDefault(UUID id) {
        UUID userId = securityContextHelper.getCurrentUserId();
        SavedAddress address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Adresse", id.toString()));
        addressRepository.clearDefaultForUser(userId);
        address.setDefault(true);
        return SavedAddressResponse.from(addressRepository.save(address));
    }
}
